package br.pauta.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Um mapa jogado (um arquivo JSON).
 *
 * @param fileId     nome do arquivo sem ".json" - identificador usado na fila de VOD
 * @param tournament prefixo do arquivo (UNIVAVA_C1, RUSH2, ...)
 * @param teamName   nome do time de cada lado, derivado do nome do arquivo
 */
public record Match(String fileId, String matchId, String tournament, String date, String map,
                   Map<Side, String> teamName, Map<Side, Integer> roundsWon,
                   List<PlayerLine> players, List<Round> rounds) {

    public Optional<Side> sideOf(String team) {
        return teamName.entrySet().stream()
                .filter(e -> e.getValue().equals(team))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean involves(String team) {
        return teamName.containsValue(team);
    }

    public Side winner() {
        return roundsWon.get(Side.RED) > roundsWon.get(Side.BLUE) ? Side.RED : Side.BLUE;
    }
}
