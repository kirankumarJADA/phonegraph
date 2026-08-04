package com.phonegraph.kgretrieval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PhoneEmbedding — maps to the `phone_embeddings` table in pgvector
 * (created by setup_pgvector.py in Step 3).
 *
 * The `embedding` column itself (a vector(384)) is NOT mapped here as a
 * normal JPA field — pgvector similarity search is done via a native
 * SQL query in PhoneEmbeddingRepository, since Spring Data JPA does not
 * natively understand the `vector` type or the `<=>` distance operator.
 */
@Entity
@Table(name = "phone_embeddings")
@Data
@NoArgsConstructor
public class PhoneEmbedding {

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
