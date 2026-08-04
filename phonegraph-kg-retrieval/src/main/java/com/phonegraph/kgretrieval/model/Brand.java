package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Brand — MADE_BY relationship target. e.g. Samsung, Apple, Xiaomi. */
@Node("Brand")
@Data
@NoArgsConstructor
public class Brand {

    @Id
    private String name;

    private String country;
    private String founded;
}
