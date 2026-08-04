package com.phonegraph.orchestrator.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RecommendationRequest — what the Android app (or a test client) sends
 * when asking PhoneGraph for a phone recommendation.
 */
@Data
public class RecommendationRequest {

    @NotBlank
    private String query;          // e.g. "best camera phone under £400"

    private String brand;          // optional constraint
    private Integer maxPrice;      // optional constraint
    private int limit = 5;         // how many candidate phones to consider
}
