package br.pauta.api;

import br.pauta.domain.TeamNames;
import br.pauta.domain.Match;
import br.pauta.domain.Side;
import br.pauta.ingest.MatchRepository;
import br.pauta.insight.Benchmark;
import br.pauta.insight.LossBreakdownService;
import br.pauta.insight.OpportunityService;
import br.pauta.metrics.RoundRef;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final MatchRepository repository;
    private final OpportunityService opportunities;
    private final LossBreakdownService losses;

    public TeamController(MatchRepository repository, OpportunityService opportunities, LossBreakdownService losses) {
        this.repository = repository;
        this.opportunities = opportunities;
        this.losses = losses;
    }

    public record TeamSummary(String team, String displayName, int matches, int mapsWon, int rounds, int roundsWon,
                              double roundWinRate, List<String> tournaments) {
    }

    @GetMapping
    public List<TeamSummary> teams() {
        Map<String, List<Match>> byTeam = new TreeMap<>();
        for (Match m : repository.findAll()) {
            for (String t : m.teamName().values()) {
                byTeam.computeIfAbsent(t, k -> new ArrayList<>()).add(m);
            }
        }
        return byTeam.entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TeamSummary::displayName))
                .toList();
    }

    /** Relatório completo do time: visão geral, recomendação, pontos de atenção, pontos fortes e todas as situações. */
    @GetMapping("/{team}/report")
    public OpportunityService.TeamReport report(@PathVariable String team,
                                                @RequestParam(defaultValue = "league") String benchmark) {
        return opportunities.report(team, Benchmark.parse(benchmark));
    }

    /** Uma situação no geral e quebrada por mapa. */
    @GetMapping("/{team}/situations/{situationId}")
    public OpportunityService.SituationDetail situation(@PathVariable String team, @PathVariable String situationId,
                                                        @RequestParam(defaultValue = "league") String benchmark) {
        return opportunities.situation(team, situationId, Benchmark.parse(benchmark));
    }

    /** Fila de VOD: rounds em que o time falhou na situação (opcionalmente só num mapa). */
    @GetMapping("/{team}/situations/{situationId}/vod")
    public List<RoundRef> vod(@PathVariable String team, @PathVariable String situationId,
                              @RequestParam(required = false) String map) {
        return opportunities.vodQueue(team, situationId, map);
    }

    /** Onde o time perdeu rounds, em categorias excludentes, comparado com a distribuição da liga. */
    @GetMapping("/{team}/losses")
    public LossBreakdownService.Breakdown losses(@PathVariable String team) {
        return losses.breakdown(team);
    }

    private static TeamSummary summarize(String team, List<Match> matches) {
        int mapsWon = 0, rounds = 0, roundsWon = 0;
        TreeSet<String> tournaments = new TreeSet<>();
        for (Match m : matches) {
            Side side = m.sideOf(team).orElseThrow();
            if (m.winner() == side) mapsWon++;
            rounds += m.rounds().size();
            roundsWon += m.roundsWon().get(side);
            tournaments.add(m.tournament());
        }
        return new TeamSummary(team, TeamNames.display(team), matches.size(), mapsWon, rounds, roundsWon,
                rounds == 0 ? 0 : (double) roundsWon / rounds, List.copyOf(tournaments));
    }
}
