package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 71,3% (n = 6.584). */
@Component
public class FirstBloodConversion implements Situation {

    @Override
    public String id() {
        return "first-blood-conversion";
    }

    @Override
    public String label() {
        return "Converter o first blood";
    }

    @Override
    public String definition() {
        return "Rounds em que o time fez o primeiro abate (sem contar autoabates e fogo amigo). Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.kills().isEmpty()) {
            return;
        }
        if (round.kills().getFirst().killerSide() == side) {
            acc.add(round.wonBy(side));
        }
    }
}
