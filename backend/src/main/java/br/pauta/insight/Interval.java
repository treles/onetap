package br.pauta.insight;

/**
 * Intervalo de confianca de Wilson (95%) para uma proporcao.
 * Leitura: "com esta amostra, a taxa real do time provavelmente esta entre low e high".
 */
public record Interval(double low, double high) {

    private static final double Z = 1.96;

    public static Interval wilson(int successes, int n) {
        if (n <= 0) {
            return new Interval(0, 0);
        }
        double p = (double) successes / n;
        double z2 = Z * Z;
        double den = 1 + z2 / n;
        double center = (p + z2 / (2.0 * n)) / den;
        double half = Z * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / den;
        return new Interval(center - half, center + half);
    }

    public boolean contains(double value) {
        return value >= low && value <= high;
    }
}
