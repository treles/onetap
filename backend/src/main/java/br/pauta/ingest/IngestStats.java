package br.pauta.ingest;

import java.util.ArrayList;
import java.util.List;

/** Contadores mutaveis preenchidos durante a ingestao; viram o {@link DataQualityReport}. */
public class IngestStats {

    int filesRead;
    int filesRejected;
    int rounds;
    int rawKillEvents;
    int selfKillsRemoved;
    int teamKillsRemoved;
    int observersFiltered;
    int roundsWithoutKills;
    int playerMatchLines;
    int killReconciliationMismatches;
    final List<String> rejected = new ArrayList<>();

    public DataQualityReport toReport(String dataDir, int matches) {
        return new DataQualityReport(dataDir, filesRead, filesRejected, List.copyOf(rejected), matches, rounds,
                rawKillEvents, selfKillsRemoved, teamKillsRemoved, observersFiltered, roundsWithoutKills,
                playerMatchLines, killReconciliationMismatches);
    }
}
