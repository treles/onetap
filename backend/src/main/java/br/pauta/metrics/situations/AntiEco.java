package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;
import br.pauta.domain.BuyType;
/** Liga: 89,7%. */
@Component
public class AntiEco implements Situation {

    @Override
    public String id() {
        return "anti-eco";
    }

    @Override
    public String label() {
        return "Anti-eco";
    }

    @Override
    public String definition() {
        return "Fora do pistol, rounds em que o time tinha compra alta (semi-full ou full) e o adversário estava em eco. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (!round.isPistol() && round.buy().get(side).isHigh() && round.buy().get(side.other()) == BuyType.ECO) {
            acc.add(round.wonBy(side));
        }
    }
}
