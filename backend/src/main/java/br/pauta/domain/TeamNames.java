package br.pauta.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Converte o nome do arquivo ("ufu_saints_auryn") em nome legível ("UFU Saints Auryn"). */
public final class TeamNames {

    private static final Set<String> ACRONYMS = Set.of(
            "ufu", "usp", "ufmt", "fei", "ceub", "uscs", "umc", "ufpr", "ufg", "ufmg", "uff", "pucc", "pucgo",
            "ime", "caap", "a2e", "unifesp", "ufrj", "ufpb", "iff", "fatec", "pg", "gc", "gdu", "utfpr", "bbn");

    private TeamNames() {
    }

    public static String display(String team) {
        return Arrays.stream(team.split("_"))
                .map(w -> ACRONYMS.contains(w) ? w.toUpperCase()
                        : w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /** Prefixo do arquivo -> nome do torneio/fase. */
    public static String tournament(String prefix) {
        return switch (prefix) {
            case "UNIVAVA_C1" -> "Univavá Classificatória 1";
            case "UNIVAVA_C2" -> "Univavá Classificatória 2";
            case "UNIVAVA_C3" -> "Univavá Classificatória 3";
            case "UNIVAVA_C4" -> "Univavá Classificatória 4";
            case "UNIVAVA_G" -> "Univavá Fase de Grupos";
            case "RUSHE" -> "Rush Series Esquenta";
            case "RUSH2" -> "Rush Series Esquenta 2";
            case "RUSH_INCLUSIVO" -> "Rush Series Inclusivo";
            default -> prefix;
        };
    }

    public static String map(String map) {
        return map == null || map.isEmpty() ? map : Character.toUpperCase(map.charAt(0)) + map.substring(1);
    }
}
