package com.phonegraph.kgretrieval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — confirms the Spring application context loads with all
 * beans wired correctly (Neo4j repository, JPA repository, embedding
 * client, retrieval service, REST controller).
 *
 * Requires Neo4j and PostgreSQL to be running (docker-compose up -d
 * in both phonegraph-neo4j/ and phonegraph-pgvector/ folders).
 */
@SpringBootTest
class KgRetrievalServiceApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, all beans (repositories, services,
        // controllers) were created successfully and both database
        // connections were established without errors.
    }
}
