package com.phonegraph.orchestrator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Component
public class NvidiaLlmClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 300_000; // 5 minutes

    public NvidiaLlmClient(
            @Value("${phonegraph.nvidia.api-key}") String apiKey,
            @Value("${phonegraph.nvidia.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${phonegraph.nvidia.model:z-ai/glm-5.2}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        String requestJson = buildRequestJson(systemPrompt, userPrompt);

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                String response = callNvidiaApi(requestJson);
                return extractContentFromResponse(response);

            } catch (IOException e) {
                if (attempt < MAX_RETRIES) {
                    long backoffMs = 5000L * attempt;
                    System.out.println("NVIDIA call failed (" + e.getMessage()
                            + ") — retrying in " + backoffMs + "ms (attempt " + attempt + "/" + MAX_RETRIES + ")");
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw new RuntimeException("NVIDIA API failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }
            }
        }
    }

    private String extractContentFromResponse(String response) throws IOException {
        // Use JsonNode for robust parsing — avoids LinkedHashMap cast failures
        // when GLM-5.2 returns integer values in unexpected fields
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.size() == 0) {
            throw new IOException("No choices in NVIDIA response: " + response);
        }
        com.fasterxml.jackson.databind.JsonNode message = choices.get(0).get("message");
        if (message == null) {
            throw new IOException("No message in NVIDIA response: " + response);
        }
        com.fasterxml.jackson.databind.JsonNode content = message.get("content");
        if (content == null) {
            throw new IOException("No content in NVIDIA response: " + response);
        }
        return content.asText();
    }

    private String callNvidiaApi(String requestJson) throws IOException {
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(TIMEOUT_MS); // applied directly on the connection

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        try (java.util.Scanner scanner = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8)) {
            String body = scanner.useDelimiter("\\A").next();
            if (status >= 500) {
                throw new IOException("NVIDIA API returned HTTP " + status + ": " + body);
            }
            return body;
        }
    }

    private String buildRequestJson(String systemPrompt, String userPrompt) {
        String safeSystem = systemPrompt.replace("\"", "\\\"").replace("\n", "\\n");
        String safeUser   = userPrompt.replace("\"", "\\\"").replace("\n", "\\n");
        return "{"
                + "\"model\":\"" + model + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + safeSystem + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + safeUser + "\"}"
                + "],"
                + "\"temperature\":0.2,"
                + "\"max_tokens\":400"
                + "}";
    }

    // ================================================================
    // STRUCTURED / FUNCTION-CALL GENERATION (proposal Section 3.4)
    // Forces the model to call a named function whose schema constrains
    // which phone names it may return (via a JSON-schema enum built from
    // the actual candidate list) — rather than relying on prompt wording
    // alone. This is additive: it does not change chat(), buildRequestJson(),
    // or extractContentFromResponse() above, so every existing evaluation
    // endpoint keeps behaving exactly as before.
    // ================================================================

    public String chatWithForcedFunction(String systemPrompt, String userPrompt,
                                          String functionName, String functionDescription,
                                          com.fasterxml.jackson.databind.JsonNode parametersSchema) {
        String requestJson = buildFunctionCallRequestJson(
                systemPrompt, userPrompt, functionName, functionDescription, parametersSchema);

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                String response = callNvidiaApi(requestJson);
                return extractToolCallArguments(response);
            } catch (IOException e) {
                if (attempt < MAX_RETRIES) {
                    long backoffMs = 5000L * attempt;
                    System.out.println("NVIDIA function-call failed (" + e.getMessage()
                            + ") — retrying in " + backoffMs + "ms (attempt " + attempt + "/" + MAX_RETRIES + ")");
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw new RuntimeException("NVIDIA function-call API failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }
            }
        }
    }

    private String buildFunctionCallRequestJson(String systemPrompt, String userPrompt,
                                                 String functionName, String functionDescription,
                                                 com.fasterxml.jackson.databind.JsonNode parametersSchema) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1);
        root.put("max_tokens", 600);

        com.fasterxml.jackson.databind.node.ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        com.fasterxml.jackson.databind.node.ObjectNode function = mapper.createObjectNode();
        function.put("name", functionName);
        function.put("description", functionDescription);
        function.set("parameters", parametersSchema);

        com.fasterxml.jackson.databind.node.ObjectNode tool = mapper.createObjectNode();
        tool.put("type", "function");
        tool.set("function", function);

        root.putArray("tools").add(tool);

        com.fasterxml.jackson.databind.node.ObjectNode toolChoice = mapper.createObjectNode();
        toolChoice.put("type", "function");
        toolChoice.putObject("function").put("name", functionName);
        root.set("tool_choice", toolChoice);

        return root.toString();
    }

    private String extractToolCallArguments(String response) throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.size() == 0) {
            throw new IOException("No choices in NVIDIA response: " + response);
        }
        com.fasterxml.jackson.databind.JsonNode message = choices.get(0).get("message");
        if (message == null) {
            throw new IOException("No message in NVIDIA response: " + response);
        }
        com.fasterxml.jackson.databind.JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls == null || !toolCalls.isArray() || toolCalls.size() == 0) {
            com.fasterxml.jackson.databind.JsonNode content = message.get("content");
            throw new IOException("No tool_calls in NVIDIA response (model/endpoint may not support forced function calling); content was: "
                    + (content != null ? content.asText() : "null"));
        }
        com.fasterxml.jackson.databind.JsonNode function = toolCalls.get(0).get("function");
        if (function == null || function.get("arguments") == null) {
            throw new IOException("Malformed tool_call in NVIDIA response: " + response);
        }
        return function.get("arguments").asText();
    }
}