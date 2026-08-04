package com.phonegraph.evaluation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OrchestratorResponse — mirrors RecommendationResponse from the
 * LLM Orchestrator service (Step 5).
 */
@Data
@NoArgsConstructor
public class OrchestratorResponse {
    private String answer;
    private List<String> candidatePhones;
    private List<String> flaggedClaims;
    private boolean hallucinationDetected;
    private String modelUsed;
}
