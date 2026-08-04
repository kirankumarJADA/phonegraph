package com.phonegraph.orchestrator.service;

import com.phonegraph.orchestrator.model.KgRetrievalResult;
import com.phonegraph.orchestrator.model.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BaselineService {

    private final NvidiaLlmClient llmClient;
    private final KgRetrievalClient kgRetrievalClient;
    private final HallucinationChecker hallucinationChecker;
    private final PromptBuilder promptBuilder;

    public RecommendationResponse zeroShot(String query) {
        String systemPrompt = "You are a smartphone recommendation assistant. "
                + "Answer the user's question about phones based on your knowledge. "
                + "Be concise and factual.";
        String answer = llmClient.chat(systemPrompt, query);
        var hallCheck = hallucinationChecker.check(answer, Collections.emptyList());
        return buildResponse(answer, Collections.emptyList(), hallCheck, "zero-shot");
    }

    public RecommendationResponse bm25(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = retrieval.getGraphMatches() != null
                ? retrieval.getGraphMatches() : Collections.emptyList();
        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()), "bm25");
        }
        String context = formatCandidates(candidates);
        String systemPrompt = "You are a smartphone assistant. Use ONLY the phone data below to answer.\n\nPHONE DATA:\n" + context;
        String answer = llmClient.chat(systemPrompt, query);
        var hallCheck = hallucinationChecker.check(answer, candidates);
        return buildResponse(answer, extractNames(candidates), hallCheck, "bm25");
    }

    public RecommendationResponse contentBased(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = retrieval.getGraphMatches() != null
                ? retrieval.getGraphMatches() : Collections.emptyList();
        StringBuilder answer = new StringBuilder("Based on attribute matching:\n");
        int rank = 1;
        for (Map<String, Object> phone : candidates) {
            String name = String.valueOf(phone.getOrDefault("name", "Unknown"));
            Object price = phone.get("price");
            Object ram = phone.get("ram");
            Object storage = phone.get("storage");
            answer.append(rank++).append(". ").append(name);
            if (price != null) answer.append(" - $").append(price);
            if (ram != null) answer.append(", ").append(ram).append("GB RAM");
            if (storage != null) answer.append(", ").append(storage).append("GB storage");
            answer.append("\n");
            if (rank > 5) break;
        }
        return buildResponse(answer.toString(), extractNames(candidates),
                new HallucinationChecker.HallucinationResult(false, Collections.emptyList()), "content-based");
    }

    public RecommendationResponse vanillaRag(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = retrieval.getSemanticMatches() != null
                ? retrieval.getSemanticMatches() : Collections.emptyList();
        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()), "vanilla-rag");
        }
        String context = formatCandidates(candidates);
        String systemPrompt = "You are a smartphone assistant. Use ONLY the phone data below to answer.\n\nPHONE DATA:\n" + context;
        String answer = llmClient.chat(systemPrompt, query);
        var hallCheck = hallucinationChecker.check(answer, candidates);
        return buildResponse(answer, extractNames(candidates), hallCheck, "vanilla-rag");
    }

    public RecommendationResponse ragFusion(String query) {
        String variationPrompt = "Generate 3 different search queries that could help answer this phone question. "
                + "Return ONLY the 3 queries, one per line, no numbering or explanation.\n\nQuestion: " + query;
        String variations;
        try {
            variations = llmClient.chat("You generate search queries. Return only the queries, one per line.", variationPrompt);
        } catch (Exception e) {
            return vanillaRag(query);
        }
        Set<String> seenNames = new LinkedHashSet<>();
        List<Map<String, Object>> allCandidates = new ArrayList<>();
        addSemanticResults(query, seenNames, allCandidates);
        for (String variation : variations.split("\n")) {
            variation = variation.trim();
            if (!variation.isEmpty() && variation.length() > 5) {
                addSemanticResults(variation, seenNames, allCandidates);
            }
        }
        if (allCandidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()), "rag-fusion");
        }
        List<Map<String, Object>> topCandidates = allCandidates.stream().limit(10).collect(Collectors.toList());
        String context = formatCandidates(topCandidates);
        String systemPrompt = "You are a smartphone assistant. Use ONLY the phone data below to answer.\n\nPHONE DATA:\n" + context;
        String answer = llmClient.chat(systemPrompt, query);
        var hallCheck = hallucinationChecker.check(answer, topCandidates);
        return buildResponse(answer, extractNames(topCandidates), hallCheck, "rag-fusion");
    }

    // ================================================================
    // ABLATION STUDY CONDITIONS (proposal Section 3.5)
    // hybrid-with-constraints = PhoneGraph itself (already evaluated).
    // These 3 isolate the other corners of the design space.
    // ================================================================

    public RecommendationResponse ablationVectorOnly(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = retrieval.getSemanticMatches() != null
                ? retrieval.getSemanticMatches() : Collections.emptyList();
        return runConstrained(query, candidates, "ablation-vector-only");
    }

    public RecommendationResponse ablationGraphOnly(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = retrieval.getGraphMatches() != null
                ? retrieval.getGraphMatches() : Collections.emptyList();
        return runConstrained(query, candidates, "ablation-graph-only");
    }

    public RecommendationResponse ablationHybridNoConstraints(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = buildFusedCandidates(retrieval);

        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()),
                    "ablation-hybrid-no-constraints");
        }

        String context = formatCandidates(candidates);
        String loosePrompt = "You are a smartphone assistant. Here is some phone data that may help:\n\n"
                + context + "\nAnswer the user's question as helpfully as you can.";
        String answer = llmClient.chat(loosePrompt, query);
        var hallCheck = hallucinationChecker.check(answer, candidates);
        return buildResponse(answer, extractNames(candidates), hallCheck, "ablation-hybrid-no-constraints");
    }

    private RecommendationResponse runConstrained(String query, List<Map<String, Object>> candidates, String label) {
        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()), label);
        }
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(query, candidates);
        String answer = llmClient.chat(systemPrompt, userPrompt);
        var hallCheck = hallucinationChecker.check(answer, candidates);
        return buildResponse(answer, extractNames(candidates), hallCheck, label);
    }

    private List<Map<String, Object>> buildFusedCandidates(KgRetrievalResult retrieval) {
        List<String> rankedNames = retrieval.getFusedRanking() != null
                ? retrieval.getFusedRanking() : List.of();
        Map<String, Map<String, Object>> phoneLookup = new LinkedHashMap<>();
        if (retrieval.getGraphMatches() != null) {
            for (Map<String, Object> p : retrieval.getGraphMatches()) {
                String name = String.valueOf(p.getOrDefault("name", p.getOrDefault("phoneName", "unknown")));
                phoneLookup.putIfAbsent(name, p);
            }
        }
        if (retrieval.getSemanticMatches() != null) {
            for (Map<String, Object> p : retrieval.getSemanticMatches()) {
                String name = String.valueOf(p.getOrDefault("phoneName", p.getOrDefault("name", "unknown")));
                phoneLookup.putIfAbsent(name, p);
            }
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (String name : rankedNames) {
            Map<String, Object> phone = phoneLookup.get(name);
            if (phone != null) candidates.add(phone);
        }
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(phoneLookup.values());
        }
        return candidates;
    }

    private void addSemanticResults(String query, Set<String> seenNames, List<Map<String, Object>> allCandidates) {
        try {
            KgRetrievalResult result = kgRetrievalClient.hybridSearch(query, null, null, 5);
            if (result.getSemanticMatches() != null) {
                for (Map<String, Object> phone : result.getSemanticMatches()) {
                    String name = String.valueOf(phone.getOrDefault("phoneName", phone.getOrDefault("name", "unknown")));
                    if (seenNames.add(name)) { allCandidates.add(phone); }
                }
            }
        } catch (Exception e) { /* skip */ }
    }

    private String formatCandidates(List<Map<String, Object>> candidates) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> phone : candidates) {
            String name = String.valueOf(phone.getOrDefault("name", phone.getOrDefault("phoneName", "Unknown")));
            sb.append("- ").append(name);
            for (String key : new String[]{"brand", "price", "ram", "storage", "yearReleased", "mainCamera", "os"}) {
                Object val = phone.get(key);
                if (val != null) sb.append(", ").append(key).append(": ").append(val);
            }
            Object battery = phone.get("battery");
            if (battery instanceof Map) {
                Object cap = ((Map<?, ?>) battery).get("capacityMah");
                if (cap != null) sb.append(", battery: ").append(cap).append("mAh");
            }
            Object desc = phone.get("description");
            if (desc != null) sb.append(" | ").append(desc);
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<String> extractNames(List<Map<String, Object>> candidates) {
        return candidates.stream()
                .map(p -> String.valueOf(p.getOrDefault("name", p.getOrDefault("phoneName", "Unknown"))))
                .collect(Collectors.toList());
    }

    private RecommendationResponse buildResponse(String answer, List<String> candidatePhones,
                                                  HallucinationChecker.HallucinationResult hallCheck, String modelLabel) {
        RecommendationResponse resp = new RecommendationResponse();
        resp.setAnswer(answer);
        resp.setCandidatePhones(candidatePhones);
        resp.setHallucinationDetected(hallCheck.hallucinationDetected());
        resp.setFlaggedClaims(hallCheck.flaggedClaims());
        resp.setModelUsed(modelLabel);
        return resp;
    }
}
