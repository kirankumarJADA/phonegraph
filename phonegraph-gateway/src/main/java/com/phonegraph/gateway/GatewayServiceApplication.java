package com.phonegraph.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhoneGraph API Gateway — routes requests to the existing microservices.
 * Contains NO business logic; it only forwards. Part of the PhoneGraph
 * dissertation project (CMP7200, BCU).
 */
@SpringBootApplication
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
