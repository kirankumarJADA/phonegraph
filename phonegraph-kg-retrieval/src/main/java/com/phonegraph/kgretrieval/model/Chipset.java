package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Chipset — HAS_CHIPSET relationship target. e.g. Snapdragon 8 Gen 3. */
@Node("Chipset")
@Data
@NoArgsConstructor
public class Chipset {

    @Id
    private String name;

    private String manufacturer;
}
