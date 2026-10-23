package br.pauta.insight;

/** Referência de comparação da Pauta. Em ambos os casos o próprio time é excluído do grupo. */
public enum Benchmark {
    LEAGUE("Média da liga"),
    TOP25("Quartil superior (25% melhores times em rounds vencidos)");

    private final String label;

    Benchmark(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Benchmark parse(String value) {
        for (Benchmark b : values()) {
            if (b.name().equalsIgnoreCase(value)) return b;
        }
        throw new IllegalArgumentException("Referência inválida: '" + value + "'. Valores aceitos: league, top25");
    }
}
