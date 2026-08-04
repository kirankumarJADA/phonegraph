package com.phonegraph.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BenchmarkQuestion — mirrors one entry in benchmark_250_questions.json
 * (Step 6). Loaded at startup and iterated over during evaluation.
 */
@Data
@NoArgsConstructor
public class BenchmarkQuestion {

    private String id;
    private String category;
    private String question;

    @JsonProperty("ground_truth")
    private String groundTruth;

    @JsonProperty("relevant_phones")
    private List<String> relevantPhones;

    private String difficulty;
}
