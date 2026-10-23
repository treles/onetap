package br.pauta.insight;

import br.pauta.domain.TeamNames;
import br.pauta.NotFoundException;
import br.pauta.domain.Match;
import br.pauta.ingest.MatchRepository;
import br.pauta.metrics.Rate;
import br.pauta.metrics.RoundRef;
import br.pauta.metrics.Situation;
import br.pauta.metrics.SituationEngine;
import br.pauta.metrics.Tally;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Monta a Pauta: compara o time com a referência (sem o próprio time e ponderada pelos mapas que ele jogou),
 * atribui vereditos e ordena pelo que está em jogo.
 */
@Service
public class OpportunityService {

    static final int MAX_HIGHLIGHTS = 5;

    private final SituationEngine engine;
    private final MatchRepository repository;
    private final BenchmarkGroups groups;
    private final RecommendationService recommendations;

    public OpportunityService(SituationEngine engine, MatchRepository repository, BenchmarkGroups groups,
                              RecommendationService recommendations) {
        this.engine = engine;
        this.repository = repository;
        this.groups = groups;
        this.recommendations = recommendations;
    }

    public record Overview(int maps, int mapsWon, int rounds, int roundsWon, double roundWinRate) {
    }

    public record TeamReport(String team, String displayName, Benchmark benchmark, String benchmarkLabel,
                             int benchmarkTeams, Overview overview, RecommendationService.Recommendation recommendation,
                             List<PautaItem> attention, List<PautaItem> strengths, List<PautaItem> situations) {
    }

    public record SituationDetail(PautaItem overall, List<PautaItem> byMap) {
    }

    public TeamReport report(String team, Benchmark benchmark) {
        List<Match> matches = requireTeam(team);
        List<PautaItem> all = items(team, benchmark);

        Comparator<PautaItem> byEvidenceThenStake = Comparator
                .comparing((PautaItem i) -> i.verdict().ordinal())
                .thenComparing(PautaItem::atStake, Comparator.reverseOrder());
        List<PautaItem> situations = all.stream().filter(i -> i.map() == null).sorted(byEvidenceThenStake).toList();
        List<PautaItem> attention = all.stream()
                .filter(i -> i.verdict().hasEvidence() && i.atStake() > 0)
                .sorted(byEvidenceThenStake).limit(MAX_HIGHLIGHTS).toList();
        List<PautaItem> strengths = all.stream()
                .filter(i -> i.verdict().hasEvidence() && i.atStake() < 0)
                .sorted(Comparator.comparing((PautaItem i) -> i.verdict().ordinal()).thenComparing(PautaItem::atStake))
                .limit(MAX_HIGHLIGHTS).toList();

        Set<String> group = groups.teams(benchmark);
        int groupSize = group.contains(team) ? group.size() - 1 : group.size();
        var recommendation = recommendations.build(attention, strengths, i -> vodQueue(team, i.situationId(), i.map()).size());
        return new TeamReport(team, TeamNames.display(team), benchmark, benchmark.label(), groupSize,
                overview(team, matches), recommendation, attention, strengths, situations);
    }

    public SituationDetail situation(String team, String situationId, Benchmark benchmark) {
        requireTeam(team);
        Situation s = requireSituation(situationId);
        List<PautaItem> items = items(team, benchmark).stream().filter(i -> i.situationId().equals(s.id())).toList();
        PautaItem overall = items.stream().filter(i -> i.map() == null).findFirst()
                .orElseThrow(() -> new NotFoundException("SITUATION_NOT_OBSERVED",
                        "A situação '" + situationId + "' não ocorreu para " + team));
        List<PautaItem> byMap = items.stream().filter(i -> i.map() != null)
                .sorted(Comparator.comparing(PautaItem::n).reversed()).toList();
        return new SituationDetail(overall, byMap);
    }

    public List<RoundRef> vodQueue(String team, String situationId, String map) {
        requireTeam(team);
        Situation s = requireSituation(situationId);
        return engine.team(s.id(), team).failures().stream()
                .filter(r -> map == null || map.isBlank() || r.map().equals(map))
                .toList();
    }

    /** Todos os itens (geral + por mapa) de um time, já com vereditos. */
    public List<PautaItem> items(String team, Benchmark benchmark) {
        Set<String> group = groups.teams(benchmark);
        List<PautaItem> items = new ArrayList<>();
        for (Situation s : engine.situations()) {
            Tally t = engine.team(s.id(), team);
            if (t.total() == 0) {
                continue;
            }
            Map<String, Rate> reference = new TreeMap<>(engine.groupByMap(s.id(), group));
            if (group.contains(team)) {
                t.byMap().forEach((map, rate) -> reference.merge(map, rate, Rate::minus));
            }
            Rate referenceOverall = reference.values().stream().reduce(Rate.ZERO, Rate::plus);

            double expected = 0;
            for (var e : t.byMap().entrySet()) {
                double ref = rateOr(reference.get(e.getKey()), referenceOverall);
                expected += e.getValue().total() * ref;
                items.add(item(s, e.getKey(), e.getValue(), ref));
            }
            items.add(item(s, null, t.overall(), expected / t.total()));
        }
        return applyMultipleComparisonControl(items);
    }

    private static double rateOr(Rate rate, Rate fallback) {
        return rate != null && rate.total() > 0 ? rate.value() : fallback.value();
    }

    private static PautaItem item(Situation s, String map, Rate rate, double reference) {
        Interval ci = Interval.wilson(rate.successes(), rate.total());
        double p = Stats.scoreTestPValue(rate.successes(), rate.total(), reference);
        Verdict v = rate.total() < Verdict.MIN_SAMPLE ? Verdict.SMALL_SAMPLE
                : p < Verdict.ALPHA ? Verdict.INDICATION : Verdict.NOT_PROVEN;
        String label = map == null ? s.label() : s.label() + " em " + TeamNames.map(map);
        String id = map == null ? s.id() : s.id() + "@" + map;
        return new PautaItem(id, s.id(), map, label, s.definition(), s.unit(), rate.successes(), rate.total(),
                rate.value(), reference, ci.low(), ci.high(), (reference - rate.value()) * rate.total(), p, v);
    }

    /** Itens com amostra suficiente passam pelo Benjamini-Hochberg; os aprovados viram STRONG. */
    private static List<PautaItem> applyMultipleComparisonControl(List<PautaItem> items) {
        List<PautaItem> tested = items.stream().filter(i -> i.verdict() != Verdict.SMALL_SAMPLE).toList();
        boolean[] strong = Stats.benjaminiHochberg(tested, PautaItem::pValue, Verdict.FDR);
        List<PautaItem> result = new ArrayList<>(items.stream().filter(i -> i.verdict() == Verdict.SMALL_SAMPLE).toList());
        for (int i = 0; i < tested.size(); i++) {
            result.add(strong[i] ? tested.get(i).withVerdict(Verdict.STRONG) : tested.get(i));
        }
        return result;
    }

    private static Overview overview(String team, List<Match> matches) {
        int mapsWon = 0, rounds = 0, roundsWon = 0;
        for (Match m : matches) {
            var side = m.sideOf(team).orElseThrow();
            if (m.winner() == side) mapsWon++;
            rounds += m.rounds().size();
            roundsWon += m.roundsWon().get(side);
        }
        return new Overview(matches.size(), mapsWon, rounds, roundsWon, rounds == 0 ? 0 : (double) roundsWon / rounds);
    }

    private List<Match> requireTeam(String team) {
        List<Match> matches = repository.findByTeam(team);
        if (matches.isEmpty()) {
            throw new NotFoundException("TEAM_NOT_FOUND", "Time inexistente na base: " + team);
        }
        return matches;
    }

    private Situation requireSituation(String situationId) {
        return engine.find(situationId)
                .orElseThrow(() -> new NotFoundException("SITUATION_NOT_FOUND", "Situação inexistente: " + situationId));
    }
}
