package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 51,8% (defesa 48,2%). octacore_python: 41/99. */
@Component
public class AttackRounds implements Situation {

    @Override
    public String id() {
        return "attack";
    }

    @Override
    public String label() {
        return "Rounds de ataque";
    }

    @Override
    public String definition() {
        return "Todos os rounds em que o time atacou. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.attacker() == side) {
            acc.add(round.wonBy(side));
        }
    }
}
