package br.pauta.domain;

/** Linha de um jogador numa partida (observadores ja filtrados). */
public record PlayerLine(String puuid, String gameName, String tagLine, Side side, String agentId,
                         int kills, int deaths, int assists, int roundsPlayed) {
}
