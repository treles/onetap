package br.pauta.insight;

/**
 * Uma linha da Pauta: uma situação, no geral ({@code map == null}) ou num mapa específico.
 *
 * @param benchmarkRate taxa esperada pela referência. No geral, é ponderada pelos mapas que o time jogou.
 * @param atStake       (taxa da referência - taxa do time) x ocorrências. Positivo = oportunidade de melhoria,
 *                      negativo = ponto forte; na unidade da situação (normalmente rounds).
 * @param pValue        valor-p do teste de score contra a referência
 */
public record PautaItem(String id, String situationId, String map, String label, String definition, String unit,
                        int successes, int n, double teamRate, double benchmarkRate,
                        double ciLow, double ciHigh, double atStake, double pValue, Verdict verdict) {

    public PautaItem withVerdict(Verdict v) {
        return new PautaItem(id, situationId, map, label, definition, unit, successes, n, teamRate, benchmarkRate,
                ciLow, ciHigh, atStake, pValue, v);
    }
}
