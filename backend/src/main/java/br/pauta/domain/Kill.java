package br.pauta.domain;

import java.util.List;

/** Abate entre adversarios (autoabates e fogo amigo sao removidos na ingestao). */
public record Kill(long timeMs, String killer, Side killerSide, String victim, Side victimSide,
                   List<String> assistants, String damageType, String damageItem) {
}
