package br.pauta.domain;

/**
 * Tipo de compra do time no round, pela soma do loadoutValue dos 5 jogadores.
 * Limiares validados pela matriz da liga (eco vs full 11%, semi vs full 22,5%, semi-full vs full 41,5%).
 */
public enum BuyType {
    ECO, SEMI, SEMI_FULL, FULL;

    public static BuyType of(int teamLoadout) {
        if (teamLoadout < 5_000) return ECO;
        if (teamLoadout < 15_000) return SEMI;
        if (teamLoadout < 20_000) return SEMI_FULL;
        return FULL;
    }

    public boolean isLow() {
        return this == ECO || this == SEMI;
    }

    public boolean isHigh() {
        return this == SEMI_FULL || this == FULL;
    }
}
