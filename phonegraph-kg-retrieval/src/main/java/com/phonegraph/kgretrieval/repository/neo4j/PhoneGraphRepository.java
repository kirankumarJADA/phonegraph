package com.phonegraph.kgretrieval.repository.neo4j;

import com.phonegraph.kgretrieval.model.Phone;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhoneGraphRepository extends Neo4jRepository<Phone, String> {

    @Query("""
        MATCH (p:Phone)-[:MADE_BY]->(b:Brand {name: $brand})
        OPTIONAL MATCH (p)-[r1:RUNS]->(o:OS)
        OPTIONAL MATCH (p)-[r2:HAS_CHIPSET]->(c:Chipset)
        OPTIONAL MATCH (p)-[r3:HAS_DISPLAY]->(d:Display)
        OPTIONAL MATCH (p)-[r4:HAS_BATTERY]->(bat:Battery)
        OPTIONAL MATCH (p)-[r5:HAS_CAMERA]->(cam:Camera)
        RETURN p, b, r1, o, r2, c, r3, d, r4, bat, r5, cam
        ORDER BY p.price ASC
        """)
    List<Phone> findByBrand(@Param("brand") String brand);

    @Query("""
        MATCH (p:Phone)-[:SUPPORTS_FEATURE]->(f:Feature {name: "5G"})
        WHERE toInteger(p.price) <= $maxPrice
        MATCH (p)-[:MADE_BY]->(b:Brand)
        OPTIONAL MATCH (p)-[r1:RUNS]->(o:OS)
        OPTIONAL MATCH (p)-[r2:HAS_CHIPSET]->(c:Chipset)
        OPTIONAL MATCH (p)-[r3:HAS_DISPLAY]->(d:Display)
        OPTIONAL MATCH (p)-[r4:HAS_BATTERY]->(bat:Battery)
        OPTIONAL MATCH (p)-[r5:HAS_CAMERA]->(cam:Camera)
        RETURN p, b, r1, o, r2, c, r3, d, r4, bat, r5, cam
        ORDER BY p.price ASC
        LIMIT $limit
        """)
    List<Phone> findByMaxPriceWith5G(@Param("maxPrice") int maxPrice, @Param("limit") int limit);

    @Query("""
        MATCH (p:Phone)-[:HAS_CHIPSET]->(c:Chipset {name: $chipset})
        MATCH (p)-[:MADE_BY]->(b:Brand)
        OPTIONAL MATCH (p)-[r1:RUNS]->(o:OS)
        OPTIONAL MATCH (p)-[r3:HAS_DISPLAY]->(d:Display)
        OPTIONAL MATCH (p)-[r4:HAS_BATTERY]->(bat:Battery)
        OPTIONAL MATCH (p)-[r5:HAS_CAMERA]->(cam:Camera)
        RETURN p, b, r1, o, c, r3, d, r4, bat, r5, cam
        """)
    List<Phone> findByChipset(@Param("chipset") String chipset);

    @Query("""
        MATCH (p:Phone)-[:SUPPORTS_FEATURE]->(f:Feature {name: $feature})
        MATCH (p)-[:MADE_BY]->(b:Brand)
        OPTIONAL MATCH (p)-[r1:RUNS]->(o:OS)
        OPTIONAL MATCH (p)-[r2:HAS_CHIPSET]->(c:Chipset)
        OPTIONAL MATCH (p)-[r3:HAS_DISPLAY]->(d:Display)
        OPTIONAL MATCH (p)-[r4:HAS_BATTERY]->(bat:Battery)
        OPTIONAL MATCH (p)-[r5:HAS_CAMERA]->(cam:Camera)
        RETURN p, b, r1, o, r2, c, r3, d, r4, bat, r5, cam
        ORDER BY p.price ASC
        LIMIT $limit
        """)
    List<Phone> findByFeature(@Param("feature") String feature, @Param("limit") int limit);

    @Query("""
        MATCH (p:Phone)-[:MADE_BY]->(b:Brand)
        MATCH (p)-[:SUPPORTS_FEATURE]->(f:Feature {name: $feature})
        WHERE ($brand IS NULL OR b.name = $brand)
          AND toInteger(p.price) <= $maxPrice
        OPTIONAL MATCH (p)-[r1:RUNS]->(o:OS)
        OPTIONAL MATCH (p)-[r2:HAS_CHIPSET]->(c:Chipset)
        OPTIONAL MATCH (p)-[r3:HAS_DISPLAY]->(d:Display)
        OPTIONAL MATCH (p)-[r4:HAS_BATTERY]->(bat:Battery)
        OPTIONAL MATCH (p)-[r5:HAS_CAMERA]->(cam:Camera)
        RETURN p, b, r1, o, r2, c, r3, d, r4, bat, r5, cam
        ORDER BY p.price ASC
        LIMIT $limit
        """)
    List<Phone> findByConstraints(
            @Param("brand") String brand,
            @Param("maxPrice") int maxPrice,
            @Param("feature") String feature,
            @Param("limit") int limit
    );

    @Query("""
        MATCH (p:Phone)-[:MADE_BY]->(b:Brand)
        RETURN b.name AS brand, count(p) AS total
        ORDER BY total DESC
        """)
    List<BrandCount> countPhonesByBrand();

    interface BrandCount {
        String getBrand();
        Long getTotal();
    }
}