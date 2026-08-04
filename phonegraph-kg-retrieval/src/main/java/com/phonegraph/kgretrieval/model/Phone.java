package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Phone — the central node of the PhoneGraph knowledge graph.
 * All other entities (Brand, OS, Chipset, Display, Battery, Camera,
 * Feature, StorageVariant) connect to this node via typed relationships,
 * matching the hub-and-spoke schema in the PhoneGraph proposal (Figure 2).
 */
@Node("Phone")
@Data
@NoArgsConstructor
public class Phone {

    @Id
    private String name;

    private String yearReleased;
    private Integer price;
    private Integer weightG;
    private String brand;      // denormalised for quick filtering
    private String ram;
    private String storage;
    private String usbType;

    @Relationship(type = "MADE_BY", direction = Relationship.Direction.OUTGOING)
    private Brand brandNode;

    @Relationship(type = "RUNS", direction = Relationship.Direction.OUTGOING)
    private OS os;

    @Relationship(type = "HAS_CHIPSET", direction = Relationship.Direction.OUTGOING)
    private Chipset chipset;

    @Relationship(type = "HAS_DISPLAY", direction = Relationship.Direction.OUTGOING)
    private Display display;

    @Relationship(type = "HAS_BATTERY", direction = Relationship.Direction.OUTGOING)
    private Battery battery;

    @Relationship(type = "HAS_CAMERA", direction = Relationship.Direction.OUTGOING)
    private Camera camera;
}
