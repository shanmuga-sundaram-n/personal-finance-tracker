package com.shan.cyber.tech.financetracker;

import com.shan.cyber.tech.financetracker.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Layer 3 — Spring context load test.
 *
 * Verifies the entire Spring application context starts without errors.
 * Catches bean wiring ambiguity, missing @Bean declarations, circular
 * dependencies, and misconfigured @Configuration classes before they
 * reach Docker or production.
 *
 * This test MUST pass at the end of every pipeline track (FEATURE, HOTFIX,
 * CHORE, UI-ONLY). It is the cheapest gate before a docker compose up.
 */
@SpringBootTest
class ApplicationContextLoadTest {

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainersConfig::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig::getUsername);
        registry.add("spring.datasource.password", TestContainersConfig::getPassword);
    }

    @Test
    void contextLoads() {
        // If Spring context fails to start, this test fails with a clear error
        // showing exactly which bean or configuration caused the failure.
    }
}
