package br.pauta.insight;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/** Estatística mínima usada pelo produto, toda explicável em uma frase. */
public final class Stats {

    private Stats() {
    }

    /**
     * Valor-p bilateral do teste de score para uma proporção: "se a taxa real do time fosse igual à referência,
     * qual a chance de ver uma diferença pelo menos deste tamanho?". É o mesmo teste que gera o intervalo de
     * Wilson, então o veredito nunca contradiz o intervalo exibido.
     */
    public static double scoreTestPValue(int successes, int n, double reference) {
        if (n == 0 || reference <= 0 || reference >= 1) {
            return 1.0;
        }
        double z = (successes - n * reference) / Math.sqrt(n * reference * (1 - reference));
        return Math.min(1.0, 2 * (1 - normalCdf(Math.abs(z))));
    }

    /** CDF da normal padrão (aproximação de Abramowitz e Stegun 7.1.26, erro < 1e-7). */
    static double normalCdf(double x) {
        double t = 1 / (1 + 0.3275911 * Math.abs(x) / Math.sqrt(2));
        double poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))));
        double erf = 1 - poly * Math.exp(-x * x / 2);
        return x >= 0 ? (1 + erf) / 2 : (1 - erf) / 2;
    }

    /**
     * Benjamini-Hochberg: devolve, para cada item, se ele continua significativo controlando a taxa de falsas
     * descobertas em {@code q}. Com dezenas de comparações por time, algumas diferenças aparecem por acaso;
     * este passo desconta isso.
     */
    public static <T> boolean[] benjaminiHochberg(List<T> items, ToDoubleFunction<T> pValue, double q) {
        int m = items.size();
        boolean[] result = new boolean[m];
        int[] order = IntStream.range(0, m).boxed()
                .sorted(Comparator.comparingDouble(i -> pValue.applyAsDouble(items.get(i))))
                .mapToInt(Integer::intValue).toArray();
        int cutoff = -1;
        for (int rank = 0; rank < m; rank++) {
            if (pValue.applyAsDouble(items.get(order[rank])) <= (rank + 1) * q / m) {
                cutoff = rank;
            }
        }
        for (int rank = 0; rank <= cutoff; rank++) {
            result[order[rank]] = true;
        }
        return result;
    }
}
