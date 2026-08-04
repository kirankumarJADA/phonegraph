package com.phonegraph.kgretrieval.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EmbeddingClient — calls the Python embedding microservice
 * (embedding-service/embedding_service.py) to convert a user's query
 * text into a 384-number vector, using the same all-MiniLM-L6-v2 model
 * that embedded all 2,000 phones in Step 3.
 */
@Component
public class EmbeddingClient {

    private final RestClient restClient;

    public EmbeddingClient(@Value("${phonegraph.embedding-service.url:http://localhost:5001}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        Map<String, Object> response = restClient.post()
                .uri("/embed")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(Map.class);

        return (List<Double>) response.get("embedding");
    }

    /** Converts a Java list of doubles into pgvector's literal string format: [0.1,0.2,...] */
    public String toVectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }
}
