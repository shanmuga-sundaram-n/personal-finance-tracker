package com.shan.cyber.tech.financetracker.config;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for all persistence adapter tests.
 * Provides Testcontainers PostgreSQL via singleton pattern.
 * All subclasses run within a transaction rolled back after each test method.
 *
 * LiquibaseAutoConfiguration is explicitly imported because @DataJpaTest
 * excludes it by default. This ensures Liquibase migrations run against the
 * Testcontainers PostgreSQL before any test executes, creating the full schema
 * (including seeded reference data: account_types, category_types, system categories).
 *
 * The datasource URL/credentials are overridden via @DynamicPropertySource
 * with the actual Testcontainers-assigned host:port before Liquibase runs.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LiquibaseAutoConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractPersistenceTest {

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainersConfig::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig::getUsername);
        registry.add("spring.datasource.password", TestContainersConfig::getPassword);
    }
}
