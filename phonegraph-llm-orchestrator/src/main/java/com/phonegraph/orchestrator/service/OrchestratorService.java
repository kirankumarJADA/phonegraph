package com.phonegraph.orchestrator.service;

import com.phonegraph.orchestrator.model.KgRetrievalResult;
import com.phonegraph.orchestrator.model.RecommendationRequest;
import com.phonegraph.orchestrator.model.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * OrchestratorService — the heart of the LLM Orchestrator microservice.
 *
 * This implements the full pipeline described in Section 3.6 of the
 * PhoneGraph proposal:
 *   1. Retrieve candidate phones from the KG & Retrieval Service
 *   2. Build a constrained prompt using only those facts
 *   3. Send the prompt to GLM-5.2
 *   4. Check the response against the KG whitelist for hallucinations
 *   5. Return the final answer plus hallucination diagnostics
 *
 * Step 5 (the hallucination check) is what RQ1 depends on — every
 * response processed here can be scored as hallucinated or clean,
 * which feeds directly into the benchmark evaluation (Step 6/7).
 */
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final KgRetrievalClient kgRetrievalClient;
    private final NvidiaLlmClient nvidiaLlmClient;
    private final PromptBuilder promptBuilder;
    private final HallucinationChecker hallucinationChecker;

    @Value("${phonegraph.nvidia.model:zai-org/glm-5.2}")
    private String modelName;

    public RecommendationResponse getRecommendation(RecommendationRequest request) {
        // Step 1 — Retrieve candidates from the KG & Retrieval Service
       KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(
                request.getQuery(),
                request.getBrand(),
                request.getMaxPrice(),
                10
        );

        // Use fusedRanking order — it prioritises semantic matches (weight 3)
        // over graph matches (weight 1), so the most relevant phones come first.
        // Build candidate list by looking up phone data in the order of fusedRanking.
        List<String> rankedNames = retrieval.getFusedRanking() != null
                ? retrieval.getFusedRanking() : List.of();

        // Create a lookup map from both graph and semantic results
        java.util.Map<String, Map<String, Object>> phoneLookup = new java.util.LinkedHashMap<>();
        if (retrieval.getGraphMatches() != null) {
            for (Map<String, Object> p : retrieval.getGraphMatches()) {
                String name = String.valueOf(p.getOrDefault("name", p.getOrDefault("phoneName", "unknown")));
                phoneLookup.putIfAbsent(name, p);
            }
        }
        if (retrieval.getSemanticMatches() != null) {
            for (Map<String, Object> p : retrieval.getSemanticMatches()) {
                String name = String.valueOf(p.getOrDefault("phoneName", p.getOrDefault("name", "unknown")));
                phoneLookup.putIfAbsent(name, p);
            }
        }

        // Build candidates in fusedRanking order (best matches first)
        List<Map<String, Object>> candidates = new java.util.ArrayList<>();
        for (String name : rankedNames) {
            Map<String, Object> phone = phoneLookup.get(name);
            if (phone != null) {
                candidates.add(phone);
            }
        }
        // Fallback: if fusedRanking is empty, use whatever we have
        if (candidates.isEmpty()) {
            candidates = new java.util.ArrayList<>(phoneLookup.values());
        }

        // Step 2 — Build constrained prompt
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(request.getQuery(), candidates);

        // Step 3 — Call GLM-5.2
        String llmAnswer = nvidiaLlmClient.chat(systemPrompt, userPrompt);

        // Step 4 — Check for hallucinations
        HallucinationChecker.HallucinationResult check =
                hallucinationChecker.check(llmAnswer, candidates);

        // Step 5 — Build final response
        List<String> candidateNames = candidates.stream()
                .map(p -> String.valueOf(p.getOrDefault("name", p.getOrDefault("phoneName", "unknown"))))
                .toList();

        return new RecommendationResponse(
                llmAnswer,
                candidateNames,
                check.flaggedClaims(),
                check.hallucinationDetected(),
                modelName
        );
    }
}
