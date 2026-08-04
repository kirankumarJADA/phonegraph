package com.phonegraph.evaluation.service;

import com.phonegraph.evaluation.model.OrchestratorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class BaselineClient {

    private final RestClient restClient;

    public BaselineClient(
            @Value("${phonegraph.orchestrator-service.url:http://localhost:8082}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(120_000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public OrchestratorResponse callBaseline(String baselineName, String query) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("limit", 10);

        return restClient.post()
                .uri("/api/baseline/" + baselineName)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OrchestratorResponse.class);
    }
}
