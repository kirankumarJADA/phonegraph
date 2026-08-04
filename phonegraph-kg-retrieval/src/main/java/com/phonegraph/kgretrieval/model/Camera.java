package com.phonegraph.kgretrieval.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Camera — HAS_CAMERA relationship target. One per phone. Includes DXOMark score. */
@Node("Camera")
@Data
@NoArgsConstructor
public class Camera {

    @Id
    @GeneratedValue
    private Long id;

    private String phoneId;
    private String mainCamera;
    private String frontCamera;
    private String dxomarkScore;
}
