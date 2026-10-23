package br.pauta.ingest;

import br.pauta.domain.BuyType;
import br.pauta.domain.Kill;
import br.pauta.domain.Match;
import br.pauta.domain.PlayerLine;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import br.pauta.ingest.RawMatch.RawKill;
import br.pauta.ingest.RawMatch.RawPlayer;
import br.pauta.ingest.RawMatch.RawPlayerRound;
import br.pauta.ingest.RawMatch.RawRound;
import br.pauta.ingest.RawMatch.RawTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Converte o JSON bruto no dominio limpo. Concentra as regras de limpeza descobertas na analise da base:
 * <ol>
 *   <li>nome do time: o lado cujo roundsWon e igual ao placar A do arquivo e o time A;</li>
 *   <li>observadores (isObserver=true, stats=null) sao removidos;</li>
 *   <li>autoabates (bomba, queda...) e fogo amigo sao removidos dos eventos de abate;</li>
 *   <li>o lado atacante de cada round e derivado de winningTeam + winningTeamRole.</li>
 * </ol>
 */
public final class MatchAssembler {

    private MatchAssembler() {
    }

    public static Match assemble(MatchFileName file, RawMatch raw, IngestStats stats) {
        Map<Side, String> names = new EnumMap<>(Side.class);
        Map<Side, Integer> roundsWon = new EnumMap<>(Side.class);
        for (RawTeam t : raw.teams()) {
            Side side = Side.of(t.teamId());
            names.put(side, t.roundsWon() == file.scoreA() ? file.teamA() : file.teamB());
            roundsWon.put(side, t.roundsWon());
        }
        if (names.size() != 2 || new HashSet<>(names.values()).size() != 2) {
            throw new InvalidMatchFileException("Nao foi possivel mapear Red/Blue pelo placar em " + file.fileId());
        }

        Map<String, Side> sideOf = new HashMap<>();
        List<PlayerLine> players = new ArrayList<>();
        for (RawPlayer p : raw.players()) {
            if (p.isObserver() || p.stats() == null) {
                stats.observersFiltered++;
                continue;
            }
            Side side = Side.of(p.teamId());
            sideOf.put(p.puuid(), side);
            players.add(new PlayerLine(p.puuid(), p.gameName(), p.tagLine(), side, p.characterId(),
                    p.stats().kills(), p.stats().deaths(), p.stats().assists(), p.stats().roundsPlayed()));
        }

        Map<String, Integer> cleanKillsByPlayer = new HashMap<>();
        List<Round> rounds = new ArrayList<>();
        for (RawRound r : raw.roundResults()) {
            Round round = assembleRound(r, sideOf, stats);
            round.kills().forEach(k -> cleanKillsByPlayer.merge(k.killer(), 1, Integer::sum));
            rounds.add(round);
        }

        for (PlayerLine p : players) {
            stats.playerMatchLines++;
            if (cleanKillsByPlayer.getOrDefault(p.puuid(), 0) != p.kills()) {
                stats.killReconciliationMismatches++;
            }
        }
        stats.rounds += rounds.size();

        return new Match(file.fileId(), raw.matchInfo().matchId(), file.tournament(), file.date(), file.map(),
                names, roundsWon, List.copyOf(players), List.copyOf(rounds));
    }

    private static Round assembleRound(RawRound r, Map<String, Side> sideOf, IngestStats stats) {
        Side winner = Side.of(r.winningTeam());
        Side attacker = "Attacker".equals(r.winningTeamRole()) ? winner : winner.other();

        Map<Side, Integer> loadout = new EnumMap<>(Map.of(Side.RED, 0, Side.BLUE, 0));
        List<Kill> kills = new ArrayList<>();
        for (RawPlayerRound ps : r.playerStats()) {
            Side side = sideOf.get(ps.puuid());
            if (side != null && ps.economy() != null) {
                loadout.merge(side, ps.economy().loadoutValue(), Integer::sum);
            }
            if (ps.kills() == null) {
                continue;
            }
            for (RawKill k : ps.kills()) {
                stats.rawKillEvents++;
                Side killerSide = sideOf.get(k.killer());
                Side victimSide = sideOf.get(k.victim());
                if (k.killer() == null || k.killer().equals(k.victim())) {
                    stats.selfKillsRemoved++;
                    continue;
                }
                if (killerSide == null || victimSide == null || killerSide == victimSide) {
                    stats.teamKillsRemoved++;
                    continue;
                }
                String damageType = k.finishingDamage() == null ? null : k.finishingDamage().damageType();
                String damageItem = k.finishingDamage() == null ? null : k.finishingDamage().damageItem();
                kills.add(new Kill(k.timeSinceRoundStartMillis(), k.killer(), killerSide, k.victim(), victimSide,
                        k.assistants() == null ? List.of() : List.copyOf(k.assistants()), damageType, damageItem));
            }
        }
        kills.sort(Comparator.comparingLong(Kill::timeMs));
        if (kills.isEmpty()) {
            stats.roundsWithoutKills++;
        }

        Map<Side, BuyType> buy = new EnumMap<>(Side.class);
        loadout.forEach((side, value) -> buy.put(side, BuyType.of(value)));

        String plantSite = r.plantSite() == null ? "" : r.plantSite();
        return new Round(r.roundNum(), winner, attacker, plantSite, r.roundResult(),
                Map.copyOf(buy), Map.copyOf(loadout), List.copyOf(kills));
    }
}
