package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 50% por definicao (todo pistol tem um vencedor e um perdedor). */
@Component
public class PistolRound implements Situation {

    @Override
    public String id() {
        return "pistol";
    }

    @Override
    public String label() {
        return "Rounds de pistol";
    }

    @Override
    public String definition() {
        return "Rounds 1 e 13 de cada mapa. Sucesso = venceu o round.";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.isPistol()) {
            acc.add(round.wonBy(side));
        }
    }
}
