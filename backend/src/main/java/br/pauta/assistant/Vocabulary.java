package br.pauta.assistant;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Como treinadores e jogadores falam de cada situação. Serve para reconhecer a situação na pergunta e para
 * enriquecer o índice (o documento de pós-plant também é encontrado por "depois do plant").
 */
final class Vocabulary {

    /** Expressões já normalizadas (sem acento, minúsculas). A mais longa vence: "pos plant" antes de "plant". */
    static final Map<String, List<String>> SITUATIONS = new LinkedHashMap<>();

    static {
        SITUATIONS.put("post-plant", List.of("pos plant", "posplant", "post plant", "depois do plant",
                "depois de plantar", "apos o plant"));
        SITUATIONS.put("retake", List.of("retake", "retomar", "retomada", "defuse", "desarm"));
        SITUATIONS.put("attack-plant-rate", List.of("chegar ao plant", "taxa de plant", "plantar", "plant", "plantio"));
        SITUATIONS.put("attack", List.of("lado de ataque", "ataque", "atacar", "atacando", "atacante", "ataca"));
        SITUATIONS.put("defense", List.of("lado de defesa", "segurar o site", "defesa", "defender", "defendendo",
                "defensor"));
        SITUATIONS.put("first-blood-reaction", List.of("first blood sofrido", "primeiro abate sofrido",
                "sofrer o first", "sofrer first", "sofremos o first", "sofremos first", "sofrendo o first",
                "sofre o first", "tomar o first", "tomamos o first", "levar o first", "levamos o first",
                "perdemos o first", "perder o first", "4 contra 5", "4v5", "4x5",
                "reagir", "reacao"));
        SITUATIONS.put("first-death-traded", List.of("first death", "primeira morte", "primeiro morto",
                "jogar em dupla", "trade", "trada", "tradou", "troca de abate", "trocar o abate"));
        SITUATIONS.put("first-blood-conversion", List.of("vantagem numerica", "5 contra 4", "5v4", "5x4",
                "primeiro abate", "first blood", "abrir o round", "converter", "conversao", "fb"));
        SITUATIONS.put("pistol", List.of("pistol", "pistola", "primeiro round"));
        SITUATIONS.put("anti-eco", List.of("anti eco", "antieco", "contra eco", "contra o eco", "eco adversario",
                "eco do adversario"));
        SITUATIONS.put("low-buy-vs-full", List.of("compra baixa", "force buy", "force", "forcar", "eco", "economia",
                "save", "semi buy", "meia compra"));
        SITUATIONS.put("clutch", List.of("clutch", "ultimo vivo", "sozinho", "1 contra", "um contra", "1v", "1x"));
    }

    private record Phrase(String text, String situationId) {
    }

    private static final List<Phrase> BY_LENGTH = SITUATIONS.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(p -> new Phrase(p, e.getKey())))
            .sorted(Comparator.comparingInt((Phrase p) -> p.text().length()).reversed())
            .toList();

    private Vocabulary() {
    }

    /**
     * Situações citadas, na ordem em que aparecem no texto. Cada trecho conta uma vez só, para a expressão
     * mais longa: em "pos plant" não sobra "plant" para virar taxa de plant.
     */
    static List<String> situationsIn(String normalized) {
        StringBuilder text = new StringBuilder(" " + normalized + " ");
        Map<Integer, String> found = new TreeMap<>();
        for (Phrase p : BY_LENGTH) {
            int at = text.indexOf(" " + p.text());
            if (at < 0) {
                continue;
            }
            found.putIfAbsent(at, p.situationId());
            for (int i = at + 1; i <= at + p.text().length(); i++) {
                text.setCharAt(i, '#');
            }
        }
        return found.values().stream().distinct().toList();
    }

    static Optional<String> situationIn(String normalized) {
        return situationsIn(normalized).stream().findFirst();
    }

    /** Mapas citados, entre os conhecidos (normalizados). */
    static Optional<String> mapIn(String normalized, Collection<String> knownMaps) {
        List<String> tokens = List.of(normalized.split(" "));
        return knownMaps.stream().filter(tokens::contains).findFirst();
    }

    /** Palavras extras para indexar junto com o documento da situação. */
    static String keywords(String situationId) {
        return String.join(" ", SITUATIONS.getOrDefault(situationId, List.of()));
    }
}
