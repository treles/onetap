package br.pauta.ingest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchFileNameTest {

    @Test
    void parseNomePadrao() {
        var f = MatchFileName.parse("UNIVAVA_C1_036_27-06-26_macklogic_red_13_x_5_octacore_javascript_lotus.json");
        assertEquals("UNIVAVA_C1", f.tournament());
        assertEquals("036", f.number());
        assertEquals("27-06-26", f.date());
        assertEquals("macklogic_red", f.teamA());
        assertEquals(13, f.scoreA());
        assertEquals(5, f.scoreB());
        assertEquals("octacore_javascript", f.teamB());
        assertEquals("lotus", f.map());
        assertEquals("UNIVAVA_C1_036_27-06-26_macklogic_red_13_x_5_octacore_javascript_lotus", f.fileId());
    }

    @Test
    void parsePrefixoComUnderscoreESufixoDeLetra() {
        var f = MatchFileName.parse("RUSH_INCLUSIVO_014a_08-08-26_caap_auroras_4_x_13_macklogic_rainbow_split.json");
        assertEquals("RUSH_INCLUSIVO", f.tournament());
        assertEquals("014a", f.number());
        assertEquals("caap_auroras", f.teamA());
        assertEquals("macklogic_rainbow", f.teamB());
        assertEquals("split", f.map());
    }

    @Test
    void parsePlacarDeProrrogacao() {
        var f = MatchFileName.parse("RUSH2_015_21-06-26_totale_umc_12_x_14_ufmt_turuna_ascent.json");
        assertEquals(12, f.scoreA());
        assertEquals(14, f.scoreB());
    }

    @Test
    void rejeitaNomeForaDoPadrao() {
        assertThrows(InvalidMatchFileException.class, () -> MatchFileName.parse("partida_qualquer.json"));
    }

    @Test
    void rejeitaPlacarEmpatado() {
        assertThrows(InvalidMatchFileException.class,
                () -> MatchFileName.parse("RUSH2_001_20-06-26_a_13_x_13_b_ascent.json"));
    }
}
