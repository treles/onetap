package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 29,2% (complementar ao pos-plant). */
@Component
public class Retake implements Situation {

    @Override
    public String id() {
        return "retake";
    }

    @Override
    public String label() {
        return "Retake (defesa)";
    }

    @Override
    public String definition() {
        return "Rounds de defesa em que a spike foi plantada. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.defender() == side && round.planted()) {
            acc.add(round.wonBy(side));
        }
    }
}
