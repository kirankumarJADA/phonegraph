package com.phonegraph.kgretrieval.controller;

import com.phonegraph.kgretrieval.model.Phone;
import com.phonegraph.kgretrieval.model.PhoneEmbedding;
import com.phonegraph.kgretrieval.repository.neo4j.PhoneGraphRepository;
import com.phonegraph.kgretrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RetrievalController — REST API for the KG & Retrieval microservice.
 *
 * These endpoints let you test Neo4j graph search, pgvector semantic
 * search, and the hybrid fusion independently — useful both for
 * development and for the ablation study in RQ3 (vector-only vs
 * graph-only vs hybrid).
 */
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalService retrievalService;
    private final PhoneGraphRepository graphRepository;

    /** Health check — confirms the service is up and can be queried. */
    @GetMapping("/health")
    public String health() {
        return "KG & Retrieval Service is running";
    }

    /** Diagnostic: how many phones per brand are actually in Neo4j. */
    @GetMapping("/stats/brands")
    public List<PhoneGraphRepository.BrandCount> brandStats() {
        return graphRepository.countPhonesByBrand();
    }

    /** Graph-only search: exact constraint matching (brand, price, feature). */
    @GetMapping("/graph")
    public List<Phone> graphSearch(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "5G") String feature,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return retrievalService.graphSearch(brand, maxPrice, feature, limit);
    }

    /** Semantic-only search: natural language meaning matching via pgvector. */
    @GetMapping("/semantic")
    public List<PhoneEmbedding> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return retrievalService.semanticSearch(query, limit);
    }

    /** Hybrid search: combines graph + semantic results (the core GraphRAG approach). */
    @GetMapping("/hybrid")
    public RetrievalService.HybridResult hybridSearch(
            @RequestParam String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return retrievalService.hybridSearch(query, brand, maxPrice, limit);
    }
}
