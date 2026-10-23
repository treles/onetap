package br.pauta.ingest;

import java.util.List;

/**
 * Relatorio de integridade da base, exposto em GET /api/health/data.
 * A reconciliacao compara os abates limpos (sem autoabate e fogo amigo) com stats.kills de cada jogador:
 * na base oficial o esperado e 0 divergencias em 3.360 linhas jogador-partida.
 */
public record DataQualityReport(String dataDir, int filesRead, int filesRejected, List<String> rejectedFiles,
                                int matches, int rounds, int rawKillEvents, int selfKillsRemoved,
                                int teamKillsRemoved, int observersFiltered, int roundsWithoutKills,
                                int playerMatchLines, int killReconciliationMismatches) {

    public double killReconciliationRate() {
        return playerMatchLines == 0 ? 0 : 1.0 - (double) killReconciliationMismatches / playerMatchLines;
    }
}
