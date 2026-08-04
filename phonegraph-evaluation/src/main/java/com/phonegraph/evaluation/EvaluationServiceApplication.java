package com.phonegraph.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhoneGraph — Evaluation Service
 * =================================
 * The 4th and final microservice in the PhoneGraph architecture.
 *
 * Responsibilities (Phase A — this build):
 *  - Load the 250-question benchmark (Step 6)
 *  - Send each question to the LLM Orchestrator (Step 5) for PhoneGraph's
 *    full pipeline answer
 *  - Score each answer: hallucination detected (from Orchestrator),
 *    retrieval quality (P@5, MRR, nDCG@10 using candidatePhones vs the
 *    benchmark's ground-truth relevant_phones), and latency
 *  - Aggregate results into per-category and overall statistics
 *  - Export a CSV + JSON report for the dissertation Evaluation chapter
 *
 * Phase B (later): extend to also run the 5 baselines (B1-B5) through
 * the same 250 questions for the full RQ1/RQ2 comparison.
 */
@SpringBootApplication
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
