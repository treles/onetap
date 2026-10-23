package br.pauta.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Normalização de texto em português para busca: minúsculas, sem acento, sem pontuação, sem palavras vazias. */
final class TextNormalizer {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das", "em", "no", "na", "nos",
            "nas", "por", "pra", "para", "com", "sem", "e", "ou", "que", "qual", "quais", "se", "ao", "aos", "eu",
            "nosso", "nossa", "nossos", "nossas", "meu", "minha", "time", "equipe", "gente", "voce", "me", "mais",
            "muito", "ser", "esta", "estamos", "estao", "foi", "sao", "tem", "ter", "isso", "esse", "essa", "como",
            "onde", "quando", "the", "of");

    private TextNormalizer() {
    }

    /** "Pós-plant na ASCENT?" -> "pos plant na ascent". */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Termos indexáveis: normalizados, sem palavras vazias e com um radical simples (plural). */
    static List<String> terms(String text) {
        List<String> out = new ArrayList<>();
        for (String t : normalize(text).split(" ")) {
            if (t.isEmpty() || STOPWORDS.contains(t)) {
                continue;
            }
            out.add(stem(t));
        }
        return out;
    }

    static String stem(String t) {
        if (t.length() > 4 && t.endsWith("oes")) return t.substring(0, t.length() - 3) + "ao";
        if (t.length() > 4 && t.endsWith("is")) return t.substring(0, t.length() - 2) + "l";
        if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) return t.substring(0, t.length() - 1);
        return t;
    }

    /** O texto normalizado contém alguma palavra que começa com um dos prefixos (respeitando o início da palavra). */
    static boolean hasPrefix(String normalized, String... prefixes) {
        String padded = " " + normalized;
        for (String p : prefixes) {
            if (padded.contains(" " + p)) {
                return true;
            }
        }
        return false;
    }
}
