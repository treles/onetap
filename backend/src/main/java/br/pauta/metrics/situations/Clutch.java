package br.pauta.metrics.situations;

import br.pauta.domain.Kill;
import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Liga: 6,4%. */
@Component
public class Clutch implements Situation {

    @Override
    public String id() {
        return "clutch";
    }

    @Override
    public String label() {
        return "Clutch 1 contra 2 ou mais";
    }

    @Override
    public String definition() {
        return "Rounds em que o time ficou com 1 jogador vivo contra 2 ou mais adversários. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        Map<Side, Integer> alive = new EnumMap<>(Map.of(Side.RED, 5, Side.BLUE, 5));
        for (Kill k : round.kills()) {
            alive.merge(k.victimSide(), -1, Integer::sum);
            if (alive.get(side) == 1 && alive.get(side.other()) >= 2) {
                acc.add(round.wonBy(side));
                return;
            }
        }
    }
}
