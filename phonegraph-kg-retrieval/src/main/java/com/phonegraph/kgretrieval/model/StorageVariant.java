package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** StorageVariant — HAS_VARIANT relationship target. RAM/storage/price combos. */
@Node("StorageVariant")
@Data
@NoArgsConstructor
public class StorageVariant {

    @Id
    @GeneratedValue
    private Long id;

    private String phoneId;
    private String ramGb;
    private String storageGb;
    private Integer price;
}
