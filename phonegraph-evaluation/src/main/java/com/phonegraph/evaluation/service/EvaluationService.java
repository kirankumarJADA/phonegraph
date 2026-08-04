package com.phonegraph.evaluation.service;

import com.phonegraph.evaluation.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EvaluationService — orchestrates a full benchmark run against
 * PhoneGraph (Phase A). For each of the 250 questions:
 *   1. Send it to the LLM Orchestrator, timing the request
 *   2. Compute P@5 / MRR / nDCG@10 against the benchmark's ground truth
 *   3. Record hallucination status (from the Orchestrator's own check)
 *   4. Collect everything into a QuestionResult
 *
 * Then aggregates all 250 results into an EvaluationReport — overall
 * and per-category — which directly answers RQ1 (hallucination rate)
 * and RQ2 (retrieval metrics) for the PhoneGraph system on its own.
 * Phase B will re-run this same logic against the 5 baselines for the
 * full comparison.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final BenchmarkLoader benchmarkLoader;
    private final BaselineClient baselineClient;
    private final OrchestratorClient orchestratorClient;
    private final MetricsCalculator metricsCalculator;

    public List<QuestionResult> runBaselineBenchmark(String baselineName, Integer start, Integer limit) throws Exception {
        BenchmarkFile benchmark = benchmarkLoader.load();
        List<BenchmarkQuestion> questions = benchmark.getQuestions();

        int startIdx = (start != null && start > 0) ? start : 0;
        int endIdx = questions.size();
        if (limit != null && limit > 0) {
            endIdx = Math.min(startIdx + limit, questions.size());
        }
        questions = questions.subList(startIdx, endIdx);

        System.out.println("BASELINE [" + baselineName + "]: running questions " + startIdx
                + " to " + (endIdx - 1) + " (" + questions.size() + " questions)");

        List<QuestionResult> results = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            BenchmarkQuestion q = questions.get(i);
            QuestionResult result = evaluateOneBaseline(q, baselineName);
            results.add(result);

            if ((i + 1) % 10 == 0) {
                System.out.println("  Progress: " + (i + 1) + "/" + questions.size());
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Baseline [" + baselineName + "] complete: " + results.size() + " questions.");
        return results;
    }

    private QuestionResult evaluateOneBaseline(BenchmarkQuestion q, String baselineName) {
        long start = System.currentTimeMillis();
        try {
            OrchestratorResponse response = baselineClient.callBaseline(baselineName, q.getQuestion());
            long latency = System.currentTimeMillis() - start;

            List<String> returned = response.getCandidatePhones() != null
                    ? response.getCandidatePhones() : Collections.emptyList();
            List<String> relevant = q.getRelevantPhones() != null
                    ? q.getRelevantPhones() : Collections.emptyList();

            double p5 = metricsCalculator.precisionAt5(returned, relevant);
            double rr = metricsCalculator.reciprocalRank(returned, relevant);
            double ndcg = metricsCalculator.ndcgAt10(returned, relevant);

            return new QuestionResult(
                    q.getId(), q.getCategory(), q.getQuestion(), q.getGroundTruth(),
                    response.getAnswer(), returned, relevant,
                    response.isHallucinationDetected(), response.getFlaggedClaims(),
                    latency, p5, rr, ndcg, false, null
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("Error evaluating question {} with baseline {}: {}", q.getId(), baselineName, e.getMessage());
            return new QuestionResult(
                    q.getId(), q.getCategory(), q.getQuestion(), q.getGroundTruth(),
                    null, Collections.emptyList(), q.getRelevantPhones(),
                    false, Collections.emptyList(),
                    latency, 0.0, 0.0, 0.0, true, e.getMessage()
            );
        }
    }

  public List<QuestionResult> runFullBenchmark(Integer start, Integer limit) throws Exception {
        BenchmarkFile benchmark = benchmarkLoader.load();
        List<BenchmarkQuestion> questions = benchmark.getQuestions();

        int startIdx = (start != null && start > 0) ? start : 0;
        int endIdx = questions.size();
        if (limit != null && limit > 0) {
            endIdx = Math.min(startIdx + limit, questions.size());
        }
        if (startIdx >= questions.size()) {
            throw new IllegalArgumentException("start=" + startIdx + " is beyond total question count (" + questions.size() + ")");
        }
        questions = questions.subList(startIdx, endIdx);

        System.out.println("BATCH MODE: running questions " + startIdx + " to " + (endIdx - 1)
                + " (" + questions.size() + " questions)");

        List<QuestionResult> results = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            BenchmarkQuestion q = questions.get(i);
            QuestionResult result = evaluateOne(q);
            results.add(result);

            if ((i + 1) % 10 == 0) {
                System.out.println("  Progress: " + (i + 1) + "/" + questions.size());
            }

            try {
                Thread.sleep(5000);  // 5s delay — ~4-5 rpm, safely under 40 rpm limit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Evaluation complete: " + results.size() + " questions processed.");
        return results;
    }

    private QuestionResult evaluateOne(BenchmarkQuestion q) {
        long start = System.currentTimeMillis();
        try {
            OrchestratorResponse response = orchestratorClient.recommend(q.getQuestion(), 10);
            long latency = System.currentTimeMillis() - start;

            List<String> returned = response.getCandidatePhones() != null
                    ? response.getCandidatePhones() : Collections.emptyList();
            List<String> relevant = q.getRelevantPhones() != null
                    ? q.getRelevantPhones() : Collections.emptyList();

            double p5 = metricsCalculator.precisionAt5(returned, relevant);
            double rr = metricsCalculator.reciprocalRank(returned, relevant);
            double ndcg = metricsCalculator.ndcgAt10(returned, relevant);

            return new QuestionResult(
                    q.getId(), q.getCategory(), q.getQuestion(), q.getGroundTruth(),
                    response.getAnswer(), returned, relevant,
                    response.isHallucinationDetected(), response.getFlaggedClaims(),
                    latency, p5, rr, ndcg, false, null
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("Error evaluating question {}: {}", q.getId(), e.getMessage());
            return new QuestionResult(
                    q.getId(), q.getCategory(), q.getQuestion(), q.getGroundTruth(),
                    null, Collections.emptyList(), q.getRelevantPhones(),
                    false, Collections.emptyList(),
                    latency, 0.0, 0.0, 0.0, true, e.getMessage()
            );
        }
    }

    public EvaluationReport aggregate(List<QuestionResult> results, String systemName) {
        List<QuestionResult> successful = results.stream().filter(r -> !r.isError()).toList();
        int errorCount = results.size() - successful.size();

        double hallucinationRate = rate(successful, QuestionResult::isHallucinationDetected);
        double meanP5 = mean(successful, QuestionResult::getPrecisionAt5);
        double meanRR = mean(successful, QuestionResult::getReciprocalRank);
        double meanNdcg = mean(successful, QuestionResult::getNdcgAt10);
        double medianLatency = median(successful, QuestionResult::getLatencyMs);
        double meanLatency = mean(successful, r -> (double) r.getLatencyMs());

        Map<String, EvaluationReport.CategoryStats> byCategory = new LinkedHashMap<>();
        Map<String, List<QuestionResult>> grouped = successful.stream()
                .collect(Collectors.groupingBy(QuestionResult::getCategory, LinkedHashMap::new, Collectors.toList()));

        for (var entry : grouped.entrySet()) {
            List<QuestionResult> catResults = entry.getValue();
            byCategory.put(entry.getKey(), new EvaluationReport.CategoryStats(
                    catResults.size(),
                    rate(catResults, QuestionResult::isHallucinationDetected),
                    mean(catResults, QuestionResult::getPrecisionAt5),
                    mean(catResults, QuestionResult::getReciprocalRank),
                    mean(catResults, QuestionResult::getNdcgAt10),
                    median(catResults, QuestionResult::getLatencyMs)
            ));
        }

        return new EvaluationReport(
                systemName, results.size(), successful.size(), errorCount,
                hallucinationRate, meanP5, meanRR, meanNdcg, medianLatency, meanLatency,
                byCategory
        );
    }

    private double rate(List<QuestionResult> list, java.util.function.Predicate<QuestionResult> predicate) {
        if (list.isEmpty()) return 0.0;
        return (double) list.stream().filter(predicate).count() / list.size();
    }

    private double mean(List<QuestionResult> list, java.util.function.ToDoubleFunction<QuestionResult> fn) {
        if (list.isEmpty()) return 0.0;
        return list.stream().mapToDouble(fn).average().orElse(0.0);
    }

    private double median(List<QuestionResult> list, java.util.function.ToDoubleFunction<QuestionResult> fn) {
        if (list.isEmpty()) return 0.0;
        double[] sorted = list.stream().mapToDouble(fn).sorted().toArray();
        int mid = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2.0 : sorted[mid];
    }
}
