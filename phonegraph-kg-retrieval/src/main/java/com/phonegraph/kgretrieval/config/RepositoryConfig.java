package com.phonegraph.kgretrieval.config;

import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableNeo4jRepositories(
        basePackages = "com.phonegraph.kgretrieval.repository.neo4j",
        transactionManagerRef = "neo4jTransactionManager"
)
@EnableJpaRepositories(
        basePackages = "com.phonegraph.kgretrieval.repository.jpa",
        transactionManagerRef = "transactionManager"
)
@EntityScan(basePackages = "com.phonegraph.kgretrieval.model")
public class RepositoryConfig {

    @Bean
    public Neo4jTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}