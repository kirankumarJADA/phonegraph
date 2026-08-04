package com.phonegraph.catalogue.repository;

import com.phonegraph.catalogue.model.CataloguePhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CataloguePhoneRepository extends JpaRepository<CataloguePhone, Long> {

    /**
     * Case-insensitive search across phone name and brand — backs
     * GET /phones/search?q=... . Kept as a simple LIKE query since this
     * service does not do semantic search (that stays in the KG service).
     */
    @Query("""
        SELECT p FROM CataloguePhone p
        WHERE LOWER(p.phoneName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    List<CataloguePhone> search(@Param("q") String query);
}
