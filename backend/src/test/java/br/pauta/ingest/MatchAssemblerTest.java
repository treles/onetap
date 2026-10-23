package br.pauta.ingest;

import br.pauta.domain.BuyType;
import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Usa uma partida minima montada a mao (src/test/resources/fixtures) que exercita cada regra de limpeza. */
class MatchAssemblerTest {

    private static final String FIXTURE = "TESTE_001_01-01-26_alpha_2_x_0_beta_ascent.json";

    private Match match;
    private IngestStats stats;

    @BeforeEach
    void setUp() throws Exception {
        stats = new IngestStats();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/" + FIXTURE)) {
            RawMatch raw = JsonMapper.builder().build().readValue(in, RawMatch.class);
            match = MatchAssembler.assemble(MatchFileName.parse(FIXTURE), raw, stats);
        }
    }

    @Test
    void mapeiaRedBlueParaNomePeloPlacar() {
        assertEquals("alpha", match.teamName().get(Side.RED));
        assertEquals("beta", match.teamName().get(Side.BLUE));
        assertEquals(Side.RED, match.sideOf("alpha").orElseThrow());
    }

    @Test
    void removeObservador() {
        assertEquals(4, match.players().size());
        assertEquals(1, stats.observersFiltered);
    }

    @Test
    void removeAutoabateEFogoAmigoEReconciliaComPlacar() {
        assertEquals(4, stats.rawKillEvents);
        assertEquals(1, stats.selfKillsRemoved);
        assertEquals(1, stats.teamKillsRemoved);
        assertEquals(0, stats.killReconciliationMismatches);
        assertEquals(4, stats.playerMatchLines);
    }

    @Test
    void ordenaAbatesPorTempo() {
        Round r = match.rounds().get(1);
        assertEquals(1, r.kills().size());
        assertEquals("r2", r.kills().getFirst().killer());
        assertEquals(Side.RED, r.kills().getFirst().killerSide());
    }

    @Test
    void derivaLadoAtacantePlantECompra() {
        Round pistol = match.rounds().get(0);
        assertTrue(pistol.isPistol());
        assertEquals(Side.RED, pistol.attacker());
        assertTrue(pistol.planted());
        assertEquals("A", pistol.plantSite());

        Round second = match.rounds().get(1);
        assertFalse(second.planted());
        assertEquals(BuyType.SEMI, second.buy().get(Side.RED));   // 2 x 4500 = 9000
        assertEquals(BuyType.ECO, second.buy().get(Side.BLUE));   // 2 x 900 = 1800
    }
}
