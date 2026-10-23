package br.pauta.insight;

/** O que o produto pode afirmar sobre a diferença entre o time e a referência. */
public enum Verdict {
    /** Diferença significativa mesmo descontando as múltiplas comparações (Benjamini-Hochberg, q = 10%). */
    STRONG,
    /** Referência fora do intervalo de 95% do time, mas sem passar no controle de múltiplas comparações. */
    INDICATION,
    /** Referência dentro do intervalo: a diferença pode ser acaso. */
    NOT_PROVEN,
    /** Menos de {@link #MIN_SAMPLE} ocorrências: não concluir nada. */
    SMALL_SAMPLE;

    public static final int MIN_SAMPLE = 20;
    public static final double ALPHA = 0.05;
    public static final double FDR = 0.10;

    public boolean hasEvidence() {
        return this == STRONG || this == INDICATION;
    }
}
