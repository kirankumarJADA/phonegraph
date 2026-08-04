package com.phonegraph.evaluation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BenchmarkFile {

    @JsonProperty("benchmark_name")
    private String benchmarkName;

    @JsonProperty("total_questions")
    private int totalQuestions;

    private Map<String, Integer> categories;

    private List<BenchmarkQuestion> questions;
}
