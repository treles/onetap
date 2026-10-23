package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 20,2%. */
@Component
public class LowBuyVsFull implements Situation {

    @Override
    public String id() {
        return "low-buy-vs-full";
    }

    @Override
    public String label() {
        return "Compra baixa contra compra alta";
    }

    @Override
    public String definition() {
        return "Fora do pistol, rounds em que o time estava em eco ou semi e o adversário em semi-full ou full. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (!round.isPistol() && round.buy().get(side).isLow() && round.buy().get(side.other()).isHigh()) {
            acc.add(round.wonBy(side));
        }
    }
}
