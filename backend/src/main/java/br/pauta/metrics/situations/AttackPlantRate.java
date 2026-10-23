package br.pauta.metrics.situations;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.metrics.Situation;
import org.springframework.stereotype.Component;

/** Liga: 70,3%. */
@Component
public class AttackPlantRate implements Situation {

    @Override
    public String id() {
        return "attack-plant-rate";
    }

    @Override
    public String label() {
        return "Chegar ao plant no ataque";
    }

    @Override
    public String definition() {
        return "Rounds de ataque. Sucesso = plantou a spike (a unidade aqui são plants, não rounds).";
    }

    @Override
    public String unit() {
        return "plants";
    }

    @Override
    public void evaluate(Match match, Round round, Side side, Recorder acc) {
        if (round.attacker() == side) {
            acc.add(round.planted());
        }
    }
}
