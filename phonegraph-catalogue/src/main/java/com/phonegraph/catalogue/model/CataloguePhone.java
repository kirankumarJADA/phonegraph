package com.phonegraph.catalogue.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CataloguePhone — read model over the EXISTING phone_embeddings table
 * (created by setup_pgvector.py, already populated with ~2,000 phones).
 *
 * This deliberately does NOT create a new table or duplicate phone data —
 * it reuses the same PostgreSQL table the KG & Retrieval Service already
 * reads from (see PhoneEmbedding.java in phonegraph-kg-retrieval), just
 * exposed here as a plain catalogue REST API. The `embedding` vector
 * column is intentionally not mapped since this service does no
 * similarity search — that stays the KG service's job.
 */
@Entity
@Table(name = "phone_embeddings")
@Data
@NoArgsConstructor
public class CataloguePhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_name")
    private String phoneName;

    private String brand;
    private String year;
    private String os;
    private String chipset;
    private Integer price;
    private String ram;
    private String storage;
    private String display;
    private String battery;
    private String camera;
    private String nfc;

    @Column(name = "five_g")
    private String fiveG;

    private String description;
}
