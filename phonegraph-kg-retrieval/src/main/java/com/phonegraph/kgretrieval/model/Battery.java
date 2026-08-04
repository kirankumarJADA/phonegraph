package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Battery — HAS_BATTERY relationship target. One per phone. */
@Node("Battery")
@Data
@NoArgsConstructor
public class Battery {

    @Id
    @GeneratedValue
    private Long id;

    private String phoneId;
    private Integer capacityMah;
    private String wiredChargingW;
    private String wirelessCharging;
}
