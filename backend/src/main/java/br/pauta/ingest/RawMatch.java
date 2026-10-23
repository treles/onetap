package br.pauta.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Espelho do JSON da organizacao, apenas com os campos usados pelo produto.
 * Campos nao declarados (posicoes, cards, titulos...) sao descartados na leitura para economizar memoria.
 * Dicionario completo dos campos: docs/ESTRATEGIA.md, secao 5.2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMatch(MatchInfo matchInfo, List<RawPlayer> players, List<RawTeam> teams,
                       List<RawRound> roundResults) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchInfo(String matchId, String mapId, long gameStartMillis, long gameLengthMillis,
                            String gameVersion) {
    }

    /** {@code stats} e {@code characterId} sao null para observadores. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawPlayer(String puuid, String gameName, String tagLine, String teamId,
                            String characterId, boolean isObserver, RawStats stats) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawStats(int score, int roundsPlayed, int kills, int deaths, int assists) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawTeam(String teamId, boolean won, int roundsPlayed, int roundsWon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawRound(int roundNum, String roundResult, String roundCeremony, String winningTeam,
                           String winningTeamRole, String bombPlanter, String bombDefuser, String plantSite,
                           List<RawPlayerRound> playerStats) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawPlayerRound(String puuid, List<RawKill> kills, List<RawDamage> damage, RawEconomy economy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawKill(long timeSinceRoundStartMillis, String killer, String victim, List<String> assistants,
                          FinishingDamage finishingDamage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinishingDamage(String damageType, String damageItem) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawDamage(String receiver, int damage, int headshots, int bodyshots, int legshots) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawEconomy(int loadoutValue, String weapon, String armor, int spent, int remaining) {
    }
}
