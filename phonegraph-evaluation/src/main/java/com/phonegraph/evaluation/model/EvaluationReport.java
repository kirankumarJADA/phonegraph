package com.phonegraph.evaluation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * EvaluationReport — the final aggregated output: overall metrics plus
 * a per-category breakdown, matching the structure your dissertation's
 * Evaluation chapter (RQ1/RQ2) will report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {
    private String systemName;
    private int totalQuestions;
    private int successfulRuns;
    private int errorRuns;

    private double hallucinationRate;
    private double meanPrecisionAt5;
    private double meanReciprocalRank;
    private double meanNdcgAt10;
    private double medianLatencyMs;
    private double meanLatencyMs;

    private Map<String, CategoryStats> byCategory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private int count;
        private double hallucinationRate;
        private double meanPrecisionAt5;
        private double meanReciprocalRank;
        private double meanNdcgAt10;
        private double medianLatencyMs;
    }
}
