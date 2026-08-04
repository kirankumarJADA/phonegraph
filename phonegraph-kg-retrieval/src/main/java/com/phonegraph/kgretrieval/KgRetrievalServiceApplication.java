package com.phonegraph.kgretrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
        JpaRepositoriesAutoConfiguration.class,
        Neo4jRepositoriesAutoConfiguration.class
})
public class KgRetrievalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KgRetrievalServiceApplication.class, args);
    }
}