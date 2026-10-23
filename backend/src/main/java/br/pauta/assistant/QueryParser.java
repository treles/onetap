package br.pauta.assistant;

import java.util.Collection;

/**
 * Entende a pergunta sem modelo generativo: intenção por regras de palavras-chave (em ordem de prioridade)
 * e entidades por dicionário (situação e mapa).
 */
final class QueryParser {

    enum Intent {
        PLAN, TOP_PROBLEM, STRENGTHS, SITUATION, MAP, MAPS, VOD, LOSSES, METHOD, SEARCH
    }

    record Parsed(String question, String normalized, Intent intent, String situationId, String map) {
    }

    private QueryParser() {
    }

    static Parsed parse(String question, Collection<String> knownMaps) {
        String n = TextNormalizer.normalize(question);
        String situation = Vocabulary.situationIn(n).orElse(null);
        String map = Vocabulary.mapIn(n, knownMaps).orElse(null);
        return new Parsed(question, n, intent(n, situation, map), situation, map);
    }

    private static Intent intent(String n, String situation, String map) {
        boolean aboutMethod = word(n, "o que e", "o que sao", "o que significa", "como calcula", "como calculam",
                "como calculamos", "como funciona", "valor p", "p valor", "media da liga", "rounds em jogo")
                || prefix(n, "quartil", "veredito", "metodolog", "wilson", "definic", "signific");
        boolean methodTerm = prefix(n, "evidencia", "intervalo", "saldo", "referencia", "amostra");
        if (aboutMethod || (methodTerm && situation == null && map == null)) {
            return Intent.METHOD;
        }
        if (prefix(n, "plano", "planej", "cronograma", "semana", "proposta", "rotina")) {
            return Intent.PLAN;
        }
        if (prefix(n, "vod", "rever", "revis", "assisti", "replay", "clipe") || word(n, "quais rounds", "que rounds")) {
            return Intent.VOD;
        }
        if (situation != null) {
            return Intent.SITUATION;
        }
        if (map != null) {
            return Intent.MAP;
        }
        if (prefix(n, "mapa")) {
            return Intent.MAPS;
        }
        if (prefix(n, "perdemos", "perdendo", "derrota", "perde")) {
            return Intent.LOSSES;
        }
        if (word(n, "ponto forte", "pontos fortes", "melhor", "melhores", "bem", "bom", "bons") || prefix(n, "forte",
                "acert", "qualidade", "vantage")) {
            return Intent.STRENGTHS;
        }
        if (prefix(n, "problema", "pior", "fraco", "fraca", "fraquez", "erro", "erra", "dificuldade", "atencao",
                "falha", "principal", "prioridade", "foco", "focar", "ruim")) {
            return Intent.TOP_PROBLEM;
        }
        if (prefix(n, "melhorar", "evolu", "treino", "treinar", "ajud", "dica", "sugest", "fazer", "proximo",
                "subir", "ganhar")) {
            return Intent.PLAN;
        }
        return Intent.SEARCH;
    }

    /** Alguma palavra do texto começa com o prefixo. */
    private static boolean prefix(String n, String... prefixes) {
        return TextNormalizer.hasPrefix(n, prefixes);
    }

    /** O texto contém a expressão exata, com palavras inteiras. */
    private static boolean word(String n, String... phrases) {
        String padded = " " + n + " ";
        for (String p : phrases) {
            if (padded.contains(" " + p + " ")) {
                return true;
            }
        }
        return false;
    }
}
