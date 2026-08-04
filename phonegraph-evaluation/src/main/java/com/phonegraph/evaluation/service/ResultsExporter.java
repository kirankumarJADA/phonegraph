package com.phonegraph.evaluation.service;

import com.phonegraph.evaluation.model.QuestionResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * ResultsExporter — writes the full per-question results to CSV, ready
 * to import into your dissertation's Evaluation chapter (tables,
 * charts, or further statistical analysis in R/Python for the
 * McNemar/Wilcoxon/bootstrap CI tests described in the proposal).
 */
@Component
public class ResultsExporter {

    public void exportToCsv(List<QuestionResult> results, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath);
             CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("question_id", "category", "question", "ground_truth",
                             "hallucination_detected", "flagged_claims_count",
                             "precision_at_5", "reciprocal_rank", "ndcg_at_10",
                             "latency_ms", "error", "error_message")
                     .build())) {

            for (QuestionResult r : results) {
                csv.printRecord(
                        r.getQuestionId(),
                        r.getCategory(),
                        r.getQuestion(),
                        r.getGroundTruth(),
                        r.isHallucinationDetected(),
                        r.getFlaggedClaims() != null ? r.getFlaggedClaims().size() : 0,
                        r.getPrecisionAt5(),
                        r.getReciprocalRank(),
                        r.getNdcgAt10(),
                        r.getLatencyMs(),
                        r.isError(),
                        r.getErrorMessage() != null ? r.getErrorMessage() : ""
                );
            }
        }
    }
}
