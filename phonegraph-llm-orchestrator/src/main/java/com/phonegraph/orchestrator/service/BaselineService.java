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

    // ================================================================
    // CONSTRAINED GENERATION WITH CORRECTION LOOP (proposal Section 3.4,
    // second strategy: "post-generation validation that triggers
    // correction for any out-of-set token").
    //
    // Uses PhoneGraph's own hybrid retrieval + strict prompt to generate
    // an initial answer, exactly as PhoneGraph does today. If the SHARED
    // HallucinationChecker flags anything, this re-prompts the model ONCE
    // with the specific flagged claims and asks it to correct them, then
    // re-checks the corrected answer. This is additive — PhoneGraph's own
    // endpoint and all already-evaluated conditions are untouched; this is
    // a new, separately-testable path.
    // ================================================================

    public RecommendationResponse constrainedWithCorrection(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = buildFusedCandidates(retrieval);

        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()),
                    "constrained-with-correction");
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(query, candidates);
        String initialAnswer = llmClient.chat(systemPrompt, userPrompt);
        var initialCheck = hallucinationChecker.check(initialAnswer, candidates);

        if (!initialCheck.hallucinationDetected()) {
            // Nothing to correct — behaves identically to runConstrained().
            return buildResponse(initialAnswer, extractNames(candidates), initialCheck,
                    "constrained-with-correction");
        }

        // Build a correction prompt naming the specific flagged claims and
        // re-send it. This is the actual "triggers correction" mechanism —
        // not just a stricter initial prompt, a real second pass conditioned
        // on what was wrong with the first answer.
        String flaggedList = String.join(", ", initialCheck.flaggedClaims());
        String correctionPrompt = userPrompt
                + "\n\nYour previous answer incorrectly mentioned: " + flaggedList + ". "
                + "These are NOT in the candidate list above. Rewrite your answer using ONLY "
                + "the phones and facts given — remove or replace anything not in that data.";

        String correctedAnswer = llmClient.chat(systemPrompt, correctionPrompt);
        var correctedCheck = hallucinationChecker.check(correctedAnswer, candidates);

        String label = correctedCheck.hallucinationDetected()
                ? "constrained-with-correction-still-flagged-after-retry"
                : "constrained-with-correction-fixed";

        return buildResponse(correctedAnswer, extractNames(candidates), correctedCheck, label);
    }

    // ================================================================
    // STRUCTURED / FUNCTION-CALL GENERATION (proposal Section 3.4)
    // Uses the SAME hybrid retrieval as PhoneGraph itself (buildFusedCandidates),
    // but instead of asking the LLM to write the final answer as free text,
    // it is forced to call a function whose schema constrains the phone
    // selection to an ENUM built from the actual candidate names — the
    // model structurally cannot name a phone that isn't in that list.
    //
    // The numeric/spec values in the final answer are then filled in by
    // CODE from the retrieved KG data, not generated by the LLM at all —
    // this is the "fills slots directly from KG values" behaviour the
    // proposal describes, distinct from the prompt-only constrained
    // generation used for PhoneGraph's main evaluation and the ablation
    // study above.
    // ================================================================

    public RecommendationResponse structuredFunctionCalling(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = buildFusedCandidates(retrieval);

        if (candidates.isEmpty()) {
            return buildResponse("No phones found.", Collections.emptyList(),
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()),
                    "structured-function-calling");
        }

        List<String> candidateNames = extractNames(candidates);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // Build the JSON-schema "parameters" object for the forced function call.
        // selected_phones is an array whose items MUST come from an enum of the
        // real candidate names — this is the structural constraint, not a prompt.
        com.fasterxml.jackson.databind.node.ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        com.fasterxml.jackson.databind.node.ObjectNode properties = schema.putObject("properties");

        com.fasterxml.jackson.databind.node.ObjectNode selectedPhones = properties.putObject("selected_phones");
        selectedPhones.put("type", "array");
        selectedPhones.put("description", "Names of the recommended phone(s), chosen ONLY from the candidate list.");
        com.fasterxml.jackson.databind.node.ObjectNode itemsSchema = selectedPhones.putObject("items");
        itemsSchema.put("type", "string");
        com.fasterxml.jackson.databind.node.ArrayNode enumArray = itemsSchema.putArray("enum");
        for (String name : candidateNames) {
            enumArray.add(name);
        }

        com.fasterxml.jackson.databind.node.ObjectNode reasoning = properties.putObject("reasoning");
        reasoning.put("type", "string");
        reasoning.put("description", "Brief explanation of why these phones fit the query. "
                + "Do NOT restate exact numeric specs here — those are added separately from verified data.");

        com.fasterxml.jackson.databind.node.ArrayNode required = schema.putArray("required");
        required.add("selected_phones");
        required.add("reasoning");

        String userPrompt = "User question: " + query
                + "\n\nCandidate phones (choose only from these):\n" + formatCandidates(candidates);

        String argumentsJson;
        try {
            argumentsJson = llmClient.chatWithForcedFunction(
                    "You are a smartphone recommendation assistant. Call the recommend_phones function with your selection.",
                    userPrompt,
                    "recommend_phones",
                    "Select recommended phones from the candidate list and explain why",
                    schema
            );
        } catch (Exception e) {
            return buildResponse("Structured generation failed: " + e.getMessage(),
                    candidateNames,
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()),
                    "structured-function-calling");
        }

        List<String> selectedNames = new ArrayList<>();
        String reasoningText = "";
        try {
            com.fasterxml.jackson.databind.JsonNode args = mapper.readTree(argumentsJson);
            if (args.get("selected_phones") != null) {
                for (com.fasterxml.jackson.databind.JsonNode n : args.get("selected_phones")) {
                    selectedNames.add(n.asText());
                }
            }
            if (args.get("reasoning") != null) {
                reasoningText = args.get("reasoning").asText();
            }
        } catch (Exception e) {
            return buildResponse("Could not parse structured response: " + argumentsJson,
                    candidateNames,
                    new HallucinationChecker.HallucinationResult(false, Collections.emptyList()),
                    "structured-function-calling");
        }

        // Defensive re-check: keep only names that are ACTUALLY in the candidate
        // set, even though the enum constraint above should already guarantee
        // this on the API side.
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> p : candidates) {
            String name = String.valueOf(p.getOrDefault("name", p.getOrDefault("phoneName", "unknown")));
            byName.put(name, p);
        }
        List<String> validSelected = new ArrayList<>();
        for (String name : selectedNames) {
            if (byName.containsKey(name)) validSelected.add(name);
        }

        // Build the final answer PROGRAMMATICALLY — specs come from the
        // retrieved KG data via code, not from LLM generation.
        StringBuilder answer = new StringBuilder();
        if (!reasoningText.isBlank()) {
            answer.append(reasoningText).append("\n\n");
        }
        int rank = 1;
        for (String name : validSelected) {
            Map<String, Object> phone = byName.get(name);
            answer.append(rank++).append(". ").append(name);
            Object price = phone.get("price");
            if (price != null) answer.append(" — $").append(price);
            Object ram = phone.get("ram");
            if (ram != null) answer.append(", ").append(ram).append("GB RAM");
            Object battery = phone.get("battery");
            if (battery instanceof Map) {
                Object cap = ((Map<?, ?>) battery).get("capacityMah");
                if (cap != null) answer.append(", ").append(cap).append("mAh battery");
            } else if (phone.get("capacityMah") != null) {
                answer.append(", ").append(phone.get("capacityMah")).append("mAh battery");
            }
            answer.append("\n\n");
        }

        var hallCheck = hallucinationChecker.check(answer.toString(), candidates);
        return buildResponse(answer.toString(), validSelected.isEmpty() ? candidateNames : validSelected,
                hallCheck, "structured-function-calling");
    }

    static List<Map<String, Object>> buildFusedCandidates(KgRetrievalResult retrieval) {
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

    static String formatCandidates(List<Map<String, Object>> candidates) {
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

    static List<String> extractNames(List<Map<String, Object>> candidates) {
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
