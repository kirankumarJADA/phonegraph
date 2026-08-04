package com.phonegraph.orchestrator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * RecommendationResponse — what gets returned to the user after the
 * full pipeline runs: retrieval → LLM generation → hallucination check.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private String answer;                 // the final, checked LLM response
    private List<String> candidatePhones;  // phones retrieved from KG & Retrieval Service
    private List<String> flaggedClaims;    // any claims that failed the KG whitelist check
    private boolean hallucinationDetected;  // true if any claim was NOT in the KG
    private String modelUsed;              // e.g. "glm-5.2"
}
