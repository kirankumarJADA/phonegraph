package com.phonegraph.evaluation.service;

import com.phonegraph.evaluation.model.OrchestratorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * OrchestratorClient — calls the LLM Orchestrator's /recommend endpoint
 * (Step 5, port 8082) for each benchmark question.
 *
 * Uses a generous timeout (60s) since GLM-5.2 generation plus the full
 * retrieval pipeline can occasionally take several seconds per call,
 * and 250 sequential calls should not fail on transient slowness.
 */
@Component
public class OrchestratorClient {

    private final RestClient restClient;

    public OrchestratorClient(
            @Value("${phonegraph.orchestrator-service.url:http://localhost:8082}") String baseUrl) {

        ClientHttpRequestFactory factory = createRequestFactory();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);   // 15s to connect
        factory.setReadTimeout(60_000);      // 60s to wait for GLM-5.2's response
        return factory;
    }

    public OrchestratorResponse recommend(String query, int limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("limit", limit);

        return restClient.post()
                .uri("/api/orchestrator/recommend")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OrchestratorResponse.class);
    }
}

