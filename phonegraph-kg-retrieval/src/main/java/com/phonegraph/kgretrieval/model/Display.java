package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Display — HAS_DISPLAY relationship target. One per phone. */
@Node("Display")
@Data
@NoArgsConstructor
public class Display {

    @Id
    @GeneratedValue
    private Long id;

    private String phoneId;
    private Double sizeInches;
    private String type;
    private String resolution;
}
