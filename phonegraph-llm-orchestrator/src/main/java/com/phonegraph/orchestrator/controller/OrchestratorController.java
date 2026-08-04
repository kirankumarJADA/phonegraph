package com.phonegraph.orchestrator.controller;

import com.phonegraph.orchestrator.model.RecommendationRequest;
import com.phonegraph.orchestrator.model.RecommendationResponse;
import com.phonegraph.orchestrator.service.OrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OrchestratorController — REST API for the LLM Orchestrator.
 *
 * This is the endpoint the Android app (or the Evaluation Service,
 * later) calls to get an actual AI-generated phone recommendation.
 */
@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {

    private final OrchestratorService orchestratorService;

    @GetMapping("/health")
    public String health() {
        return "LLM Orchestrator Service is running";
    }

    @PostMapping("/recommend")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        return orchestratorService.getRecommendation(request);
    }
}
