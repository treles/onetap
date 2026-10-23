package br.pauta.insight;

import br.pauta.NotFoundException;
import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.ingest.MatchRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * "Onde perdemos rounds": classifica cada round perdido em UMA categoria (a primeira que se aplica, na ordem
 * do enum). Funciona para qualquer time, inclusive os que ficam acima da média em tudo.
 */
@Service
public class LossBreakdownService {

    public enum Category {
        PISTOL("Pistol perdido"),
        LOW_BUY("Compra baixa contra compra alta"),
        POST_PLANT("Ataque: plantou e perdeu"),
        ATTACK_NO_PLANT("Ataque: não conseguiu plantar"),
        RETAKE("Defesa: spike plantada (retake perdido)"),
        DEFENSE_NO_PLANT("Defesa: eliminada antes do plant");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        static Category of(Round r, Side side) {
            if (r.isPistol()) return PISTOL;
            if (r.buy().get(side).isLow() && r.buy().get(side.other()).isHigh()) return LOW_BUY;
            if (r.attacker() == side) return r.planted() ? POST_PLANT : ATTACK_NO_PLANT;
            return r.planted() ? RETAKE : DEFENSE_NO_PLANT;
        }
    }

    public record Slice(Category id, String label, int rounds, double share, double leagueShare) {
    }

    public record Breakdown(int roundsLost, List<Slice> categories) {
    }

    private final MatchRepository repository;
    private final Map<Category, Integer> league = new EnumMap<>(Category.class);
    private final int leagueLost;

    public LossBreakdownService(MatchRepository repository) {
        this.repository = repository;
        int lost = 0;
        for (Match m : repository.findAll()) {
            for (Round r : m.rounds()) {
                // todo round tem exatamente um perdedor
                league.merge(Category.of(r, r.winner().other()), 1, Integer::sum);
                lost++;
            }
        }
        this.leagueLost = lost;
    }

    public Breakdown breakdown(String team) {
        List<Match> matches = repository.findByTeam(team);
        if (matches.isEmpty()) {
            throw new NotFoundException("TEAM_NOT_FOUND", "Time inexistente na base: " + team);
        }
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        int lost = 0;
        for (Match m : matches) {
            Side side = m.sideOf(team).orElseThrow();
            for (Round r : m.rounds()) {
                if (!r.wonBy(side)) {
                    counts.merge(Category.of(r, side), 1, Integer::sum);
                    lost++;
                }
            }
        }
        int total = lost;
        List<Slice> slices = Arrays.stream(Category.values())
                .map(c -> new Slice(c, c.label(), counts.getOrDefault(c, 0),
                        total == 0 ? 0 : (double) counts.getOrDefault(c, 0) / total,
                        leagueLost == 0 ? 0 : (double) league.getOrDefault(c, 0) / leagueLost))
                .toList();
        return new Breakdown(total, slices);
    }
}
