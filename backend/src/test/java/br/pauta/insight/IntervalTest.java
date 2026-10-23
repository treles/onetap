package br.pauta.insight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntervalTest {

    @Test
    void wilsonBateComOScriptPython() {
        // octacore_python no ataque: 41/99 -> IC95 [32%, 51%] em tools/analise/metrics.py
        Interval ci = Interval.wilson(41, 99);
        assertEquals(0.322, ci.low(), 0.001);
        assertEquals(0.513, ci.high(), 0.001);
    }

    @Test
    void amostraVaziaNaoQuebra() {
        Interval ci = Interval.wilson(0, 0);
        assertEquals(0, ci.low());
        assertEquals(0, ci.high());
    }

    @Test
    void cdfNormal() {
        assertEquals(0.5, Stats.normalCdf(0), 1e-7);
        assertEquals(0.975, Stats.normalCdf(1.959964), 1e-6);
        assertEquals(0.025, Stats.normalCdf(-1.959964), 1e-6);
    }

    @Test
    void valorPConsistenteComWilson() {
        // a referência fica fora do IC 95% exatamente quando p < 0,05 (os dois vêm do mesmo teste de score)
        Interval ci = Interval.wilson(41, 99);
        assertTrue(Stats.scoreTestPValue(41, 99, ci.high() + 0.002) < 0.05);
        assertTrue(Stats.scoreTestPValue(41, 99, ci.high() - 0.002) > 0.05);
        // conta à mão: z = (41 - 99*0,518) / sqrt(99*0,518*0,482) = -10,282 / 4,9717 = -2,068 -> p = 0,0386
        assertEquals(0.0386, Stats.scoreTestPValue(41, 99, 0.518), 0.0005);
    }

    @Test
    void benjaminiHochberg() {
        // m = 4, q = 0,10: limiares 0,025 / 0,05 / 0,075 / 0,10 -> passam os dois menores
        List<Double> p = List.of(0.04, 0.20, 0.01, 0.09);
        boolean[] result = Stats.benjaminiHochberg(p, Double::doubleValue, 0.10);
        assertArrayEquals(new boolean[]{true, false, true, false}, result);
    }
}
