package br.pauta.metrics.situations;

import br.pauta.domain.Kill;
import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 20,2%. */
@Component
public class FirstDeathTraded implements Situation {

    /** Janela de trade usada na análise (5 s após o primeiro abate). */
    static final long TRADE_WINDOW_MS = 5_000;

    @Override
    public String id() {
        return "first-death-traded";
    }

    @Override
    public String label() {
        return "Trade do primeiro abatido";
    }

    @Override
    public String definition() {
        return "Rounds em que o time sofreu o primeiro abate. Sucesso = quem fez esse abate morreu em até 5 segundos (trade).";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.kills().isEmpty()) {
            return;
        }
        Kill first = round.kills().getFirst();
        if (first.victimSide() != side) {
            return;
        }
        boolean traded = round.kills().stream()
                .skip(1)
                .anyMatch(k -> k.victim().equals(first.killer()) && k.timeMs() - first.timeMs() <= TRADE_WINDOW_MS);
        acc.add(traded);
    }
}
