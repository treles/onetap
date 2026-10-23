package br.pauta.assistant;

import br.pauta.domain.TeamNames;
import br.pauta.insight.PautaItem;
import br.pauta.insight.Verdict;
import br.pauta.metrics.RoundRef;

import java.util.Locale;

/** Formatação das frases-modelo em português (porcentagem com vírgula, veredito por extenso, round para o VOD). */
final class Fmt {

    private Fmt() {
    }

    static String pct(double v) {
        return String.format(Locale.ROOT, "%.0f%%", v * 100);
    }

    /** "1 round", "7 rounds", "1 plant", "6 plants" (arredondado). */
    static String count(double value, String pluralUnit) {
        long n = Math.round(value);
        return n + " " + (n == 1 ? pluralUnit.substring(0, pluralUnit.length() - 1) : pluralUnit);
    }

    static String verdict(Verdict v) {
        return switch (v) {
            case STRONG -> "evidência forte";
            case INDICATION -> "indício";
            case NOT_PROVEN -> "sem evidência: a diferença pode ser acaso";
            case SMALL_SAMPLE -> "amostra pequena: menos de " + Verdict.MIN_SAMPLE + " ocorrências";
        };
    }

    static String unit(PautaItem i) {
        return "plants".equals(i.unit()) ? "plants" : "rounds";
    }

    /** "48% contra 61% da referência em 52 ocorrências (indício)". */
    static String comparison(PautaItem i) {
        return pct(i.teamRate()) + " contra " + pct(i.benchmarkRate()) + " da referência em " + i.n()
                + " ocorrências (" + verdict(i.verdict()) + ")";
    }

    /** "cerca de 7 rounds em jogo" ou "cerca de 4 rounds a favor". */
    static String stake(PautaItem i) {
        double s = i.atStake();
        if (Math.abs(s) < 0.5) {
            return "praticamente no nível da referência";
        }
        return "cerca de " + count(Math.abs(s), unit(i)) + (s > 0 ? " em jogo" : " a favor");
    }

    /** "Ascent, round 14 contra UFU Saints (Univavá Fase de Grupos, 12/09/26), placar 11 a 13". */
    static String round(RoundRef r) {
        return TeamNames.map(r.map()) + ", round " + r.round() + " contra " + r.opponent() + " (" + r.tournament()
                + ", " + r.date() + "), placar " + r.teamScore() + " a " + r.opponentScore();
    }

    static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
