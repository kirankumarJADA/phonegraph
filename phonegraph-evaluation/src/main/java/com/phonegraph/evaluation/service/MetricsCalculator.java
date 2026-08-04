package com.phonegraph.evaluation.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MetricsCalculator — computes the standard information retrieval
 * metrics specified in the PhoneGraph proposal (Section: Evaluation
 * Metrics): P@5, MRR, nDCG@10.
 *
 * All three compare the ranked list PhoneGraph actually returned
 * (candidatePhones, in ranked order from the KG & Retrieval Service's
 * fusion step) against the ground-truth relevant phones for that
 * question (from the benchmark's relevant_phones field).
 *
 * Relevance is treated as BINARY here (a phone is either relevant or
 * not) — this is a reasonable simplification for a dissertation-scale
 * system, and matches how P@5/MRR are conventionally computed when no
 * graded relevance judgments are available.
 */
@Component
public class MetricsCalculator {

    /** Precision@5: of the top 5 returned phones, what fraction are relevant? */
    public double precisionAt5(List<String> returned, List<String> relevant) {
        if (returned == null || returned.isEmpty() || relevant == null || relevant.isEmpty()) {
            return 0.0;
        }
        int k = Math.min(5, returned.size());
        long hits = returned.subList(0, k).stream()
                .filter(p -> containsPhone(relevant, p))
                .count();
        return (double) hits / k;
    }

    /** Mean Reciprocal Rank: 1 / (rank of the first relevant phone), or 0 if none found. */
    public double reciprocalRank(List<String> returned, List<String> relevant) {
        if (returned == null || relevant == null || relevant.isEmpty()) {
            return 0.0;
        }
        for (int i = 0; i < returned.size(); i++) {
            if (containsPhone(relevant, returned.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /** nDCG@10: normalised discounted cumulative gain over the top 10 results. */
    public double ndcgAt10(List<String> returned, List<String> relevant) {
        if (returned == null || returned.isEmpty() || relevant == null || relevant.isEmpty()) {
            return 0.0;
        }
        int k = Math.min(10, returned.size());

        double dcg = 0.0;
        for (int i = 0; i < k; i++) {
            if (containsPhone(relevant, returned.get(i))) {
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2)); // log2(rank+1)
            }
        }

        // Ideal DCG: all relevant phones (up to k) placed at the top
        int idealHits = Math.min(relevant.size(), k);
        double idcg = 0.0;
        for (int i = 0; i < idealHits; i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }

        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    /** Fuzzy match — handles minor formatting differences between KG names and benchmark names. */
    private boolean containsPhone(List<String> relevantList, String candidate) {
        return relevantList.stream().anyMatch(r ->
                r.equalsIgnoreCase(candidate)
                        || r.toLowerCase().contains(candidate.toLowerCase())
                        || candidate.toLowerCase().contains(r.toLowerCase())
        );
    }
}
