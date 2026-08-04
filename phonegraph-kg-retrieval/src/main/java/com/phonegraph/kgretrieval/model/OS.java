package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** OS — RUNS relationship target. e.g. Android 14, iOS 17, HarmonyOS 4. */
@Node("OS")
@Data
@NoArgsConstructor
public class OS {

    @Id
    private String name;

    private String developer;
}
