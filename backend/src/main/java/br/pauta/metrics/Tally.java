package br.pauta.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Acumulador de uma situação para um time: contagem geral, contagem por mapa
 * e os rounds de fracasso (fila de VOD).
 */
public final class Tally {

    private Rate overall = Rate.ZERO;
    private final Map<String, Rate> byMap = new TreeMap<>();
    private final List<RoundRef> failures = new ArrayList<>();

    void add(boolean success, RoundRef ref) {
        overall = overall.plus(success);
        byMap.merge(ref.map(), Rate.ZERO.plus(success), Rate::plus);
        if (!success) {
            failures.add(ref);
        }
    }

    /** Soma apenas contagens (sem fila de VOD): usado para agregados como a liga. */
    void addCounts(String map, Rate rate) {
        overall = overall.plus(rate);
        byMap.merge(map, rate, Rate::plus);
    }

    public Rate overall() {
        return overall;
    }

    public int successes() {
        return overall.successes();
    }

    public int total() {
        return overall.total();
    }

    public double rate() {
        return overall.value();
    }

    public Map<String, Rate> byMap() {
        return Collections.unmodifiableMap(byMap);
    }

    public Rate map(String map) {
        return byMap.getOrDefault(map, Rate.ZERO);
    }

    public List<RoundRef> failures() {
        return Collections.unmodifiableList(failures);
    }
}
