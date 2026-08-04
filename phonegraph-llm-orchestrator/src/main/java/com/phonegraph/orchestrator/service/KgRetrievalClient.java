package com.phonegraph.orchestrator.service;

import com.phonegraph.orchestrator.model.KgRetrievalResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * KgRetrievalClient — calls the KG & Retrieval Service (built in Step 4,
 * running on port 8081) to get candidate phones for a user query.
 *
 * This keeps the two microservices properly decoupled: the Orchestrator
 * never talks to Neo4j or pgvector directly — it only ever talks to the
 * KG & Retrieval Service's REST API, exactly as shown in the PhoneGraph
 * architecture diagram (Layer 3, four independent microservices).
 */
@Component
public class KgRetrievalClient {

    private final RestClient restClient;

    public KgRetrievalClient(
            @Value("${phonegraph.kg-retrieval-service.url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public KgRetrievalResult hybridSearch(String query, String brand, Integer maxPrice, int limit) {
        var uriBuilder = new StringBuilder("/api/retrieval/hybrid?query=" + encode(query));
        if (brand != null) uriBuilder.append("&brand=").append(encode(brand));
        if (maxPrice != null) uriBuilder.append("&maxPrice=").append(maxPrice);
        uriBuilder.append("&limit=").append(limit);

        return restClient.get()
                .uri(uriBuilder.toString())
                .retrieve()
                .body(KgRetrievalResult.class);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
