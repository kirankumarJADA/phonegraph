package com.phonegraph.kgretrieval.service;

import com.phonegraph.kgretrieval.model.Phone;
import com.phonegraph.kgretrieval.model.PhoneEmbedding;
import com.phonegraph.kgretrieval.repository.jpa.PhoneEmbeddingRepository;
import com.phonegraph.kgretrieval.repository.neo4j.PhoneGraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RetrievalService — the heart of the KG & Retrieval microservice.
 *
 * This implements the hybrid retrieval described in the PhoneGraph
 * proposal: graph traversal (Neo4j) + vector similarity (pgvector),
 * fused into a single ranked result set (Figure 1, Layer 3).
 *
 * This is deliberately kept separate from the LLM Orchestrator
 * (Step 5) — this service ONLY retrieves candidate phones. It does
 * NOT call GPT-4o-mini. That separation matches the microservice
 * boundaries in the proposal architecture diagram.
 */
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final PhoneGraphRepository graphRepository;
    private final PhoneEmbeddingRepository embeddingRepository;
    private final EmbeddingClient embeddingClient;

    /** Pure graph search: exact constraint matching via Cypher. */
    public List<Phone> graphSearch(String brand, Integer maxPrice, String feature, int limit) {
        return graphRepository.findByConstraints(
                brand,
                maxPrice != null ? maxPrice : Integer.MAX_VALUE,
                feature != null ? feature : "5G",
                limit
        );
    }

    /** Pure semantic search: meaning-based matching via pgvector. */
    public List<PhoneEmbedding> semanticSearch(String naturalLanguageQuery, int limit) {
        List<Double> queryVector = embeddingClient.embed(naturalLanguageQuery);
        String vectorLiteral = embeddingClient.toVectorLiteral(queryVector);
        return embeddingRepository.findSimilar(vectorLiteral, limit);
    }

    /**
     * Hybrid search — runs both graph and semantic search, then fuses
     * the results. Phones appearing in BOTH result sets are ranked
     * higher (they satisfy the exact constraints AND match the meaning
     * of the query) — a simple but effective fusion strategy for a
     * dissertation-scale system.
     */
    public HybridResult hybridSearch(String naturalLanguageQuery, String brand, Integer maxPrice, int limit) {
        List<Phone> graphResults = graphSearch(brand, maxPrice, "5G", limit * 2);
        List<PhoneEmbedding> semanticResults = semanticSearch(naturalLanguageQuery, limit * 2);

        Map<String, Integer> fusionScore = new LinkedHashMap<>();

        for (Phone p : graphResults) {
            fusionScore.merge(p.getName(), 1, Integer::sum); // graph match = weight 1
        }
        for (PhoneEmbedding e : semanticResults) {
            fusionScore.merge(e.getPhoneName(), 3, Integer::sum); // semantic match = weight 3 (understands query meaning)
        }

        List<String> rankedNames = fusionScore.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        return new HybridResult(graphResults, semanticResults, rankedNames);
    }

    /** Simple DTO carrying all three views of the result for transparency/debugging. */
    public record HybridResult(
            List<Phone> graphMatches,
            List<PhoneEmbedding> semanticMatches,
            List<String> fusedRanking
    ) {}
}
