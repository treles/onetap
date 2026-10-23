package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 70,8%. Complementar ao retake (defesa), que deve dar 29,2%. */
@Component
public class PostPlant implements Situation {

    @Override
    public String id() {
        return "post-plant";
    }

    @Override
    public String label() {
        return "Pós-plant (ataque)";
    }

    @Override
    public String definition() {
        return "Rounds de ataque em que a spike foi plantada. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.attacker() == side && round.planted()) {
            acc.add(round.wonBy(side));
        }
    }
}
