package br.pauta.assistant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Índice invertido com ranqueamento BM25, em memória. Montado uma vez por time e referência;
 * cada busca percorre só as listas dos termos da pergunta.
 */
final class Bm25Index {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    /** Título e palavras-chave pesam mais que o corpo do texto. */
    private static final int TITLE_WEIGHT = 2;

    record Hit(KnowledgeDoc doc, double score) {
    }

    private record Posting(int doc, int tf) {
    }

    private final List<KnowledgeDoc> docs;
    private final int[] lengths;
    private final double avgLength;
    private final Map<String, List<Posting>> postings = new HashMap<>();
    private final Map<String, KnowledgeDoc> byId = new HashMap<>();

    Bm25Index(List<KnowledgeDoc> docs) {
        this.docs = List.copyOf(docs);
        docs.forEach(d -> byId.put(d.id(), d));
        this.lengths = new int[docs.size()];
        long total = 0;
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeDoc d = docs.get(i);
            Map<String, Integer> tf = new HashMap<>();
            List<String> heavy = TextNormalizer.terms(d.title() + " " + d.keywords());
            for (int w = 0; w < TITLE_WEIGHT; w++) {
                heavy.forEach(t -> tf.merge(t, 1, Integer::sum));
            }
            TextNormalizer.terms(d.text()).forEach(t -> tf.merge(t, 1, Integer::sum));
            int length = tf.values().stream().mapToInt(Integer::intValue).sum();
            lengths[i] = length;
            total += length;
            int doc = i;
            tf.forEach((term, n) -> postings.computeIfAbsent(term, k -> new ArrayList<>()).add(new Posting(doc, n)));
        }
        this.avgLength = docs.isEmpty() ? 1 : (double) total / docs.size();
    }

    int size() {
        return docs.size();
    }

    Optional<KnowledgeDoc> doc(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    List<Hit> search(String query, int limit, Predicate<KnowledgeDoc> filter) {
        double[] scores = new double[docs.size()];
        for (String term : TextNormalizer.terms(query).stream().distinct().toList()) {
            List<Posting> list = postings.get(term);
            if (list == null) {
                continue;
            }
            double idf = Math.log(1 + (docs.size() - list.size() + 0.5) / (list.size() + 0.5));
            for (Posting p : list) {
                double norm = K1 * (1 - B + B * lengths[p.doc()] / avgLength);
                scores[p.doc()] += idf * p.tf() * (K1 + 1) / (p.tf() + norm);
            }
        }
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0 && filter.test(docs.get(i))) {
                hits.add(new Hit(docs.get(i), scores[i]));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }
}
