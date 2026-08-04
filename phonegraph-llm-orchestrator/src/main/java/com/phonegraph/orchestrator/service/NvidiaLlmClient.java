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
}