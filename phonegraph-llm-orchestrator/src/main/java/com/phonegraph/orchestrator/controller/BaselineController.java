package com.phonegraph.orchestrator.controller;

import com.phonegraph.orchestrator.model.RecommendationRequest;
import com.phonegraph.orchestrator.model.RecommendationResponse;
import com.phonegraph.orchestrator.service.BaselineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baseline")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    @PostMapping("/zero-shot")
    public RecommendationResponse zeroShot(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.zeroShot(request.getQuery());
    }

    @PostMapping("/bm25")
    public RecommendationResponse bm25(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.bm25(request.getQuery());
    }

    @PostMapping("/content-based")
    public RecommendationResponse contentBased(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.contentBased(request.getQuery());
    }

    @PostMapping("/vanilla-rag")
    public RecommendationResponse vanillaRag(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.vanillaRag(request.getQuery());
    }

    @PostMapping("/rag-fusion")
    public RecommendationResponse ragFusion(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.ragFusion(request.getQuery());
    }

    @PostMapping("/ablation-vector-only")
    public RecommendationResponse ablationVectorOnly(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.ablationVectorOnly(request.getQuery());
    }

    @PostMapping("/ablation-graph-only")
    public RecommendationResponse ablationGraphOnly(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.ablationGraphOnly(request.getQuery());
    }

    @PostMapping("/ablation-hybrid-no-constraints")
    public RecommendationResponse ablationHybridNoConstraints(@Valid @RequestBody RecommendationRequest request) {
        return baselineService.ablationHybridNoConstraints(request.getQuery());
    }
}
