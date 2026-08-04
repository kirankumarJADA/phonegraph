package com.phonegraph.evaluation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QuestionResult — the outcome of running ONE benchmark question through
 * PhoneGraph, with all metrics needed to compute the aggregate scores
 * for RQ1 (hallucination rate) and RQ2 (P@5, MRR, nDCG@10).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResult {
    private String questionId;
    private String category;
    private String question;
    private String groundTruth;
    private String answer;
    private List<String> candidatePhones;
    private List<String> relevantPhones;
    private boolean hallucinationDetected;
    private List<String> flaggedClaims;
    private long latencyMs;
    private double precisionAt5;
    private double reciprocalRank;
    private double ndcgAt10;
    private boolean error;
    private String errorMessage;
}
