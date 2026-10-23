package br.pauta.domain;

/** Lado do time dentro de um JSON. O nome real do time vem do nome do arquivo. */
public enum Side {
    RED, BLUE;

    public Side other() {
        return this == RED ? BLUE : RED;
    }

    public static Side of(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            throw new IllegalArgumentException("teamId vazio");
        }
        return Side.valueOf(teamId.toUpperCase());
    }
}
