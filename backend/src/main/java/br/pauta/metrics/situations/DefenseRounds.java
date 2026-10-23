package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 48,2%. */
@Component
public class DefenseRounds implements Situation {

    @Override
    public String id() {
        return "defense";
    }

    @Override
    public String label() {
        return "Rounds de defesa";
    }

    @Override
    public String definition() {
        return "Todos os rounds em que o time defendeu. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.defender() == side) {
            acc.add(round.wonBy(side));
        }
    }
}
