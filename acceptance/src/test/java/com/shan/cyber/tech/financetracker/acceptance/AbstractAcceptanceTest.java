package com.shan.cyber.tech.financetracker.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Abstract base class for all acceptance tests.
 *
 * Spins up a singleton PostgreSQL 15.2 Testcontainer, starts the full Spring Boot
 * application context on a random port, and configures RestAssured.
 *
 * Subclasses call {@link #registerAndLogin(String)} in their own {@code @BeforeAll}
 * to obtain a session token scoped to that test class.
 */
@SpringBootTest(
    classes = com.shan.cyber.tech.PersonalFinanceTracker.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractAcceptanceTest {

    // Singleton container — shared across all acceptance test classes in the JVM
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15.2")
            .withDatabaseName("personal-finance-tracker")
            .withUsername("pft-app-user")
            .withPassword("pft-app-user-secret")
            // Run Liquibase migrations inside the finance_tracker schema
            .withInitScript(null);

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Liquibase will create the finance_tracker schema via the master changelog
        registry.add("spring.liquibase.default-schema", () -> "finance_tracker");
        registry.add("spring.liquibase.liquibase-schema", () -> "public");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "finance_tracker");
    }

    /**
     * Wires RestAssured to the random port chosen by Spring Boot.
     * Called once per JVM via the static initialiser on the shared Testcontainer.
     */
    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    private int serverPort;

    @BeforeAll
    void configureRestAssured() {
        RestAssured.port = serverPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // -------------------------------------------------------------------------
    // Helper methods shared by all flow test classes
    // -------------------------------------------------------------------------

    /**
     * Registers a new user and immediately logs in, returning the opaque session token.
     *
     * @param uniqueSuffix short suffix appended to username/email to guarantee uniqueness
     * @return Bearer token string (UUID)
     */
    protected String registerAndLogin(String uniqueSuffix) {
        String username = "testuser_" + uniqueSuffix;
        String email = "testuser_" + uniqueSuffix + "@acceptance.test";
        String password = "Str0ng!Pass";

        // Register
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", username,
                "email", email,
                "password", password,
                "firstName", "Test",
                "lastName", "User"
            ))
        .when()
            .post("/api/v1/auth/register")
        .then()
            .statusCode(201);

        // Login and extract token
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", password))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
    }

    /**
     * Returns a pre-configured RestAssured {@link RequestSpecification} that includes
     * the {@code Authorization: Bearer {token}} header.
     */
    protected RequestSpecification authed(String token) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token);
    }

    /**
     * Creates a checking account for the given user and returns its ID.
     */
    protected long createCheckingAccount(String token, String nameSuffix) {
        return authed(token)
            .body(Map.of(
                "name", "Checking " + nameSuffix,
                "accountTypeCode", "CHECKING",
                "initialBalance", "1000.00",
                "currency", "USD",
                "institutionName", "Test Bank",
                "accountNumberLast4", "1234"
            ))
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
    }

    /**
     * Creates a custom EXPENSE category for the given user and returns its ID.
     */
    protected long createExpenseCategory(String token, String nameSuffix) {
        return authed(token)
            .body(Map.of(
                "name", "Groceries " + nameSuffix,
                "categoryTypeCode", "EXPENSE",
                "icon", "shopping-cart",
                "color", "#FF5733"
            ))
        .when()
            .post("/api/v1/categories")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
    }

    /**
     * Creates a custom INCOME category for the given user and returns its ID.
     */
    protected long createIncomeCategory(String token, String nameSuffix) {
        return authed(token)
            .body(Map.of(
                "name", "Salary " + nameSuffix,
                "categoryTypeCode", "INCOME",
                "icon", "briefcase",
                "color", "#33FF57"
            ))
        .when()
            .post("/api/v1/categories")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
    }

    /**
     * Produces a short random suffix suitable for unique resource names within a test run.
     */
    protected static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
