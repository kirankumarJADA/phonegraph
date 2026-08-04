package com.phonegraph.kgretrieval.repository.jpa;

import com.phonegraph.kgretrieval.model.PhoneEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PhoneEmbeddingRepository — runs pgvector similarity search.
 * This is the "semantic retrieval" half of PhoneGraph's hybrid retrieval.
 *
 * The <=> operator is pgvector's cosine distance operator: smaller
 * distance = more similar meaning. We convert it to a similarity score
 * (1 - distance) so higher = better, matching the convention used in
 * the Step 3 Python test script.
 *
 * NOTE: :vectorLiteral must be a pgvector-formatted string, e.g.
 * "[0.023,-0.451,0.892,...]" — built from the 384 floats returned by
 * the embedding microservice (see EmbeddingClient).
 */
public interface PhoneEmbeddingRepository extends JpaRepository<PhoneEmbedding, Long> {

    @Query(value = """
        SELECT * , (1 - (embedding <=> CAST(:vectorLiteral AS vector))) AS similarity
        FROM phone_embeddings
        ORDER BY embedding <=> CAST(:vectorLiteral AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<PhoneEmbedding> findSimilar(@Param("vectorLiteral") String vectorLiteral, @Param("limit") int limit);
}
