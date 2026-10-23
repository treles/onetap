package br.pauta.domain;

import java.util.List;
import java.util.Map;

/**
 * Um round ja limpo.
 *
 * @param number    numero 0-based, como no JSON (o round "1" exibido ao usuario e o number 0)
 * @param kills     abates entre adversarios, ordenados por tempo
 * @param plantSite "A", "B", "C" ou vazio quando nao houve plant
 */
public record Round(int number, Side winner, Side attacker, String plantSite, String result,
                    Map<Side, BuyType> buy, Map<Side, Integer> loadout, List<Kill> kills) {

    public boolean isPistol() {
        return number == 0 || number == 12;
    }

    public boolean isOvertime() {
        return number >= 24;
    }

    public Side defender() {
        return attacker.other();
    }

    public boolean planted() {
        return plantSite != null && !plantSite.isEmpty();
    }

    public boolean wonBy(Side side) {
        return winner == side;
    }

    /** Numero exibido para humanos (1-based), usado na fila de VOD. */
    public int displayNumber() {
        return number + 1;
    }
}
