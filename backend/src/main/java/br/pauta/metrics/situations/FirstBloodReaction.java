package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 28,7%. */
@Component
public class FirstBloodReaction implements Situation {

    @Override
    public String id() {
        return "first-blood-reaction";
    }

    @Override
    public String label() {
        return "Reagir ao first blood sofrido";
    }

    @Override
    public String definition() {
        return "Rounds em que o time sofreu o primeiro abate. Sucesso = venceu o round mesmo assim.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (!round.kills().isEmpty() && round.kills().getFirst().victimSide() == side) {
            acc.add(round.wonBy(side));
        }
    }
}
