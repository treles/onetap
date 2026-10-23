package br.pauta.metrics;

/** Contagem imutável de sucessos em ocorrências. */
public record Rate(int successes, int total) {

    public static final Rate ZERO = new Rate(0, 0);

    public double value() {
        return total == 0 ? 0 : (double) successes / total;
    }

    public Rate plus(Rate other) {
        return new Rate(successes + other.successes, total + other.total);
    }

    public Rate minus(Rate other) {
        return new Rate(successes - other.successes, total - other.total);
    }

    public Rate plus(boolean success) {
        return new Rate(successes + (success ? 1 : 0), total + 1);
    }
}
