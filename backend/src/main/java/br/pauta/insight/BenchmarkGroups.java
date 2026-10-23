package br.pauta.insight;

import br.pauta.domain.Match;
import br.pauta.domain.Side;
import br.pauta.ingest.MatchRepository;
import br.pauta.metrics.Rate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Define quais times compõem cada referência. */
@Component
public class BenchmarkGroups {

    /** Só entram no quartil superior times com pelo menos este número de mapas (evita "elite" de 2 jogos). */
    static final int MIN_MAPS_FOR_TOP = 8;

    private final Set<String> allTeams;
    private final List<String> topQuartile;

    public BenchmarkGroups(MatchRepository repository) {
        Map<String, Rate> rounds = new HashMap<>();
        Map<String, Integer> maps = new HashMap<>();
        for (Match m : repository.findAll()) {
            for (Side side : Side.values()) {
                String team = m.teamName().get(side);
                rounds.merge(team, new Rate(m.roundsWon().get(side), m.rounds().size()), Rate::plus);
                maps.merge(team, 1, Integer::sum);
            }
        }
        this.allTeams = Set.copyOf(rounds.keySet());
        List<String> eligible = rounds.keySet().stream()
                .filter(t -> maps.get(t) >= MIN_MAPS_FOR_TOP)
                .sorted(Comparator.comparingDouble((String t) -> rounds.get(t).value()).reversed())
                .toList();
        this.topQuartile = eligible.subList(0, (int) Math.ceil(eligible.size() / 4.0));
    }

    public Set<String> teams(Benchmark benchmark) {
        return switch (benchmark) {
            case LEAGUE -> allTeams;
            case TOP25 -> Set.copyOf(topQuartile);
        };
    }

    public List<String> topQuartile() {
        return topQuartile;
    }
}
