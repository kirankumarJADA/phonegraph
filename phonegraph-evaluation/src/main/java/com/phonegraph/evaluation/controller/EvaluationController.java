package com.phonegraph.evaluation.controller;

import com.phonegraph.evaluation.model.EvaluationReport;
import com.phonegraph.evaluation.model.QuestionResult;
import com.phonegraph.evaluation.service.EvaluationService;
import com.phonegraph.evaluation.service.ResultsExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final ResultsExporter resultsExporter;

    @Value("${phonegraph.results.output-dir:./results}")
    private String outputDir;

    @GetMapping("/health")
    public String health() {
        return "Evaluation Service is running";
    }

    @PostMapping("/run/baseline/{baselineName}")
    public EvaluationReport runBaseline(
            @PathVariable String baselineName,
            @RequestParam(required = false, defaultValue = "0") Integer start,
            @RequestParam(required = false) Integer limit) throws Exception {
        List<QuestionResult> results = evaluationService.runBaselineBenchmark(baselineName, start, limit);
        EvaluationReport report = evaluationService.aggregate(results, baselineName);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        String csvPath = outputDir + "/" + baselineName + "_results_" + timestamp
                + "_batch" + start + "-" + (start + results.size()) + ".csv";
        resultsExporter.exportToCsv(results, csvPath);

        System.out.println("Baseline [" + baselineName + "] results exported to: " + csvPath);
        return report;
    }

    @PostMapping("/run")
    public EvaluationReport runEvaluation(
            @RequestParam(required = false, defaultValue = "0") Integer start,
            @RequestParam(required = false) Integer limit) throws Exception {
        List<QuestionResult> results = evaluationService.runFullBenchmark(start, limit);
        EvaluationReport report = evaluationService.aggregate(results, "PhoneGraph");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        String csvPath = outputDir + "/phonegraph_results_" + timestamp
                + "_batch" + start + "-" + (start + results.size()) + ".csv";
        resultsExporter.exportToCsv(results, csvPath);

        System.out.println("Results exported to: " + csvPath);
        return report;
    }
}