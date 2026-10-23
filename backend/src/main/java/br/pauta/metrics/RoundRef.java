package br.pauta.metrics;

import br.pauta.domain.TeamNames;

/**
 * Aponta um round específico para revisão no VOD. {@code round} é 1-based (como o jogador vê).
 *
 * @param fileId      arquivo de origem (permite conferir o round no JSON)
 * @param tournament  nome legível do torneio/fase
 * @param date        data no formato dd/mm/aa
 * @param matchRounds total de rounds daquele mapa (para desenhar o histórico de rounds)
 * @param teamScore   placar final do time naquele mapa
 * @param opponentScore placar final do adversário naquele mapa
 */
public record RoundRef(String fileId, String tournament, String date, String map, String opponent, int round,
                       int matchRounds, int teamScore, int opponentScore) {

    public static RoundRef of(String fileId, String tournamentPrefix, String date, String map, String opponent,
                              int round, int matchRounds, int teamScore, int opponentScore) {
        return new RoundRef(fileId, TeamNames.tournament(tournamentPrefix), date.replace('-', '/'), map,
                TeamNames.display(opponent), round, matchRounds, teamScore, opponentScore);
    }
}
