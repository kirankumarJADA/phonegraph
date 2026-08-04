package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Feature — SUPPORTS_FEATURE relationship target. e.g. NFC, 5G, USB-C. */
@Node("Feature")
@Data
@NoArgsConstructor
public class Feature {

    @Id
    private String name;

    private String category;
}
