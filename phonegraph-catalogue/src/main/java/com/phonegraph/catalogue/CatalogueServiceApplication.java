package com.phonegraph.catalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhoneGraph Catalogue Service — exposes phone catalogue data via REST.
 * Reads from the existing phone_embeddings PostgreSQL table; does not
 * duplicate or redesign the knowledge graph. Part of the PhoneGraph
 * dissertation project (CMP7200, BCU).
 */
@SpringBootApplication
public class CatalogueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogueServiceApplication.class, args);
    }
}
