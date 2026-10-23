package br.pauta.metrics;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.ingest.MatchRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Executa todas as {@link Situation} sobre todas as partidas uma vez e guarda o resultado por time e por mapa.
 * Os dados são estáticos, então calcular na inicialização é mais simples do que recalcular a cada requisição.
 */
@Component
public class SituationEngine {

    private final List<Situation> situations;
    /** situação -> time -> acumulador */
    private final Map<String, Map<String, Tally>> byTeam = new HashMap<>();

    public SituationEngine(List<Situation> situations, MatchRepository repository) {
        this.situations = situations.stream().sorted(Comparator.comparing(Situation::id)).toList();
        compute(repository.findAll());
    }

    private void compute(List<Match> matches) {
        for (Situation s : situations) {
            byTeam.put(s.id(), new HashMap<>());
        }
        for (Match m : matches) {
            for (Round r : m.rounds()) {
                for (Side side : Side.values()) {
                    String team = m.teamName().get(side);
                    RoundRef ref = RoundRef.of(m.fileId(), m.tournament(), m.date(), m.map(),
                            m.teamName().get(side.other()), r.displayNumber(), m.rounds().size(),
                            m.roundsWon().get(side), m.roundsWon().get(side.other()));
                    for (Situation s : situations) {
                        Tally tally = byTeam.get(s.id()).computeIfAbsent(team, t -> new Tally());
                        s.evaluate(m, r, side, success -> tally.add(success, ref));
                    }
                }
            }
        }
    }

    public List<Situation> situations() {
        return situations;
    }

    public Optional<Situation> find(String id) {
        return situations.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public Tally team(String situationId, String team) {
        return byTeam.getOrDefault(situationId, Map.of()).getOrDefault(team, new Tally());
    }

    /** Taxa geral da liga inteira (todos os times, todos os mapas). */
    public Tally league(String situationId) {
        Tally sum = new Tally();
        byTeam.getOrDefault(situationId, Map.of()).values()
                .forEach(t -> t.byMap().forEach((map, rate) -> sum.addCounts(map, rate)));
        return sum;
    }

    /** Contagens por mapa somando os times informados. */
    public Map<String, Rate> groupByMap(String situationId, Collection<String> teams) {
        Map<String, Rate> sum = new TreeMap<>();
        Map<String, Tally> all = byTeam.getOrDefault(situationId, Map.of());
        for (String team : teams) {
            Tally t = all.get(team);
            if (t != null) {
                t.byMap().forEach((map, rate) -> sum.merge(map, rate, Rate::plus));
            }
        }
        return sum;
    }
}
