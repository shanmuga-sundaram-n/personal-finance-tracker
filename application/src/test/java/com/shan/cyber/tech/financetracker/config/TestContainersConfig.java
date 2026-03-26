package com.shan.cyber.tech.financetracker.config;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton Testcontainers PostgreSQL container.
 *
 * Usage: extend AbstractPersistenceTest or use @DynamicPropertySource
 * pointing to POSTGRES.getJdbcUrl(), getUsername(), getPassword().
 *
 * The container is started once for the entire JVM lifetime of the test run.
 * Testcontainers' Ryuk container handles cleanup after the JVM exits.
 */
public final class TestContainersConfig {

    // Package-visible; use through AbstractPersistenceTest
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15.2")
            .withDatabaseName("personal-finance-tracker")
            .withUsername("pft-app-user")
            .withPassword("pft-app-user-secret")
            // Tune for faster test execution
            .withCommand(
                "postgres",
                "-c", "fsync=off",
                "-c", "synchronous_commit=off",
                "-c", "full_page_writes=off"
            );
        POSTGRES.start();
    }

    private TestContainersConfig() {}

    public static String getJdbcUrl() { return POSTGRES.getJdbcUrl(); }
    public static String getUsername() { return POSTGRES.getUsername(); }
    public static String getPassword() { return POSTGRES.getPassword(); }
}
