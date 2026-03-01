# Test Infrastructure — Personal Finance Tracker
**Author**: QA Automation Tester
**Date**: 2026-03-01
**Status**: Authoritative — full-stack-dev implements exactly this infrastructure before writing any tests

---

## 0. Prerequisites

Before any tests can be written, the following infrastructure files must exist. Create them in this order:

1. Add ArchUnit dependency to `application/build.gradle.kts`
2. Create `TestContainersConfig.java` (singleton container)
3. Create `application-test.yaml`
4. Create `acceptance/build.gradle.kts` (acceptance module test config)
5. Create test data builders
6. Create test fixtures

---

## 1. Dependency Updates

### `application/build.gradle.kts` — Add ArchUnit and tag-based task configuration

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.shan.cyber.tech"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // Core Web and JPA
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Security / Password Hashing (spring-security-crypto ONLY, not full Security)
    implementation("org.springframework.security:spring-security-crypto")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Code Generation
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Testing — Core
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Testing — Testcontainers
    testImplementation("org.testcontainers:postgresql:1.19.6")
    testImplementation("org.testcontainers:junit-jupiter:1.19.6")

    // Testing — RestAssured (also used in acceptance module)
    testImplementation("io.rest-assured:rest-assured:5.4.0")

    // Testing — ArchUnit for hexagonal boundary enforcement
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

// Domain unit tests + Architecture tests (fast, no Docker)
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration", "adapter")
    }
}

// Persistence adapter tests (Docker required)
tasks.register<Test>("adapterTest") {
    useJUnitPlatform {
        includeTags("adapter")
    }
    group = "verification"
    description = "Persistence adapter tests using Testcontainers"
    shouldRunAfter("test")
}
```

### `acceptance/build.gradle.kts`

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.shan.cyber.tech"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Include the application module's compiled classes and runtime
    testImplementation(project(":application"))

    // JUnit 5
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // RestAssured for HTTP-level assertions
    testImplementation("io.rest-assured:rest-assured:5.4.0")

    // Testcontainers
    testImplementation("org.testcontainers:postgresql:1.19.6")
    testImplementation("org.testcontainers:junit-jupiter:1.19.6")

    // JDBC for direct database verification queries
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly("org.postgresql:postgresql")
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    group = "verification"
    description = "Full HTTP round-trip integration tests (requires Docker)"
}
```

---

## 2. Testcontainers Singleton Container

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/config/TestContainersConfig.java`

The singleton pattern ensures that one PostgreSQL container is started once and reused across all test classes that need it. This dramatically reduces test suite duration.

```java
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
```

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/config/AbstractPersistenceTest.java`

```java
package com.shan.cyber.tech.financetracker.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for all persistence adapter tests.
 * Provides Testcontainers PostgreSQL via singleton pattern.
 * All subclasses run within a transaction rolled back after each test method.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yaml")
public abstract class AbstractPersistenceTest {

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainersConfig::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig::getUsername);
        registry.add("spring.datasource.password", TestContainersConfig::getPassword);
    }
}
```

---

## 3. Test Profile Configuration

**File**: `application/src/test/resources/application-test.yaml`

```yaml
spring:
  datasource:
    # Values injected dynamically from TestContainersConfig via @DynamicPropertySource
    # Defaults provided to avoid startup errors if container not yet running
    url: jdbc:postgresql://localhost:5432/personal-finance-tracker
    username: pft-app-user
    password: pft-app-user-secret
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate     # Never create/drop — Liquibase manages schema
    show-sql: true           # Helpful during test development; disable in CI if noisy
    properties:
      hibernate:
        format_sql: true
  liquibase:
    change-log: classpath:db.changelog/db.changelog-master.yaml
    default-schema: finance_tracker
    enabled: true

app:
  session:
    duration-days: 7
  cors:
    allowed-origins: "http://localhost:3000"

logging:
  level:
    com.shan.cyber.tech.financetracker: DEBUG
    org.springframework.security: DEBUG
```

**File**: `acceptance/src/test/resources/application-integration-test.yaml`

```yaml
# Used by @SpringBootTest integration tests in the acceptance module
spring:
  datasource:
    url: ${INTEGRATION_DB_URL:jdbc:postgresql://localhost:5432/personal-finance-tracker}
    username: ${INTEGRATION_DB_USER:pft-app-user}
    password: ${INTEGRATION_DB_PASS:pft-app-user-secret}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  liquibase:
    change-log: classpath:db.changelog/db.changelog-master.yaml
    default-schema: finance_tracker
    enabled: true

app:
  session:
    duration-days: 7
  cors:
    allowed-origins: "http://localhost:3000"

logging:
  level:
    com.shan.cyber.tech.financetracker: WARN
```

---

## 4. Test Security Configuration (Controller Layer)

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/config/TestSecurityConfig.java`

Used by `@WebMvcTest` controller tests to bypass `SessionAuthFilter` and inject a fixed user ID.

```java
package com.shan.cyber.tech.financetracker.config;

import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Test-only Spring configuration that replaces SessionAuthFilter with a stub
 * that always sets userId=1L for controller-layer tests.
 *
 * Import this in @WebMvcTest tests:
 * @WebMvcTest(AccountController.class)
 * @Import(TestSecurityConfig.class)
 */
@TestConfiguration
public class TestSecurityConfig {

    public static final Long TEST_USER_ID = 1L;

    @Bean
    public OncePerRequestFilter testAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {
                SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clear();
                }
            }
        };
    }
}
```

---

## 5. Test Data Builders

All builders use the fluent builder pattern. Builders are final classes with a static `aUser()`, `anAccount()` etc. factory method.

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/testdata/UserBuilder.java`

```java
package com.shan.cyber.tech.financetracker.testdata;

import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Builder for domain User objects used in unit tests.
 * Does NOT hit the database. For persistence tests, use TestEntityManager directly.
 */
public final class UserBuilder {

    private Long id = 1L;
    private String username = "testuser01";
    private String passwordHash = "$2a$12$hashedpassword";
    private String email = "testuser01@test.example.com";
    private String firstName = "Test";
    private String lastName = "User";
    private boolean isActive = true;
    private String preferredCurrency = "USD";

    private UserBuilder() {}

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public UserBuilder withId(Long id) { this.id = id; return this; }
    public UserBuilder withUsername(String username) { this.username = username; return this; }
    public UserBuilder withEmail(String email) { this.email = email; return this; }
    public UserBuilder withPasswordHash(String hash) { this.passwordHash = hash; return this; }
    public UserBuilder inactive() { this.isActive = false; return this; }
    public UserBuilder withCurrency(String currency) { this.preferredCurrency = currency; return this; }

    public User build() {
        return new User(
            new UserId(id),
            username,
            passwordHash,
            email,
            firstName,
            lastName,
            isActive,
            preferredCurrency,
            new AuditInfo(
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                id, id
            )
        );
    }
}
```

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/testdata/AccountBuilder.java`

```java
package com.shan.cyber.tech.financetracker.testdata;

import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import java.math.BigDecimal;

public final class AccountBuilder {

    private Long id = 1L;
    private Long userId = 1L;
    private AccountType accountType = AccountTypes.CHECKING;
    private String name = "Test Checking Account";
    private BigDecimal balance = new BigDecimal("1000.0000");
    private String currency = "USD";
    private boolean isActive = true;
    private boolean includeInNetWorth = true;
    private Long version = 0L;

    private AccountBuilder() {}

    public static AccountBuilder anAccount() { return new AccountBuilder(); }

    public AccountBuilder withId(Long id) { this.id = id; return this; }
    public AccountBuilder withUserId(Long userId) { this.userId = userId; return this; }
    public AccountBuilder ofType(AccountType type) { this.accountType = type; return this; }
    public AccountBuilder withName(String name) { this.name = name; return this; }
    public AccountBuilder withBalance(String amount) {
        this.balance = new BigDecimal(amount);
        return this;
    }
    public AccountBuilder inactive() { this.isActive = false; return this; }

    public Account build() {
        Money money = Money.of(balance, currency);
        return new Account(
            new AccountId(id),
            new UserId(userId),
            accountType,
            name,
            money,   // currentBalance
            money,   // initialBalance
            isActive,
            includeInNetWorth,
            version
        );
    }

    /**
     * Pre-configured account types for use in tests.
     * Mirrors the pre-seeded AccountType reference data.
     */
    public static class AccountTypes {
        public static final AccountType CHECKING =
            new AccountType((short)1, "CHECKING", "Checking", true, false);
        public static final AccountType SAVINGS =
            new AccountType((short)2, "SAVINGS", "Savings", false, false);
        public static final AccountType CREDIT_CARD =
            new AccountType((short)3, "CREDIT_CARD", "Credit Card", true, true);
        public static final AccountType CASH =
            new AccountType((short)6, "CASH", "Cash", false, false);
        public static final AccountType LOAN =
            new AccountType((short)5, "LOAN", "Loan", true, true);
        public static final AccountType DIGITAL_WALLET =
            new AccountType((short)7, "DIGITAL_WALLET", "Digital Wallet", true, false);
    }
}
```

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/testdata/MoneyFactory.java`

```java
package com.shan.cyber.tech.financetracker.testdata;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import java.math.BigDecimal;

/**
 * Convenience factory for creating Money values in tests.
 * Uses USD by default (matches preferred currency in test user fixtures).
 */
public final class MoneyFactory {

    private MoneyFactory() {}

    public static Money usd(String amount) {
        return Money.of(new BigDecimal(amount), "USD");
    }

    public static Money usd(double amount) {
        return Money.of(new BigDecimal(String.valueOf(amount)), "USD");
    }

    public static Money zero() {
        return Money.zero("USD");
    }
}
```

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/testdata/TransactionBuilder.java`

```java
package com.shan.cyber.tech.financetracker.testdata;

import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.shared.domain.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class TransactionBuilder {

    private Long id = 1L;
    private Long userId = 1L;
    private Long accountId = 1L;
    private Long categoryId = 1L;
    private BigDecimal amount = new BigDecimal("100.0000");
    private TransactionType type = TransactionType.EXPENSE;
    private LocalDate date = LocalDate.now();
    private String description = null;
    private String merchantName = null;

    private TransactionBuilder() {}

    public static TransactionBuilder aTransaction() { return new TransactionBuilder(); }

    public TransactionBuilder withId(Long id) { this.id = id; return this; }
    public TransactionBuilder withUserId(Long uid) { this.userId = uid; return this; }
    public TransactionBuilder withAccountId(Long aid) { this.accountId = aid; return this; }
    public TransactionBuilder withCategoryId(Long cid) { this.categoryId = cid; return this; }
    public TransactionBuilder withAmount(String amount) {
        this.amount = new BigDecimal(amount);
        return this;
    }
    public TransactionBuilder ofType(TransactionType type) { this.type = type; return this; }
    public TransactionBuilder onDate(LocalDate date) { this.date = date; return this; }
    public TransactionBuilder withDescription(String desc) { this.description = desc; return this; }

    public Transaction build() {
        return new Transaction(
            new TransactionId(id),
            new UserId(userId),
            new AccountId(accountId),
            new CategoryId(categoryId),
            Money.of(amount, "USD"),
            type,
            date,
            description,
            merchantName,
            null,   // referenceNumber
            false,  // isRecurring
            null,   // recurringTransactionId
            null,   // transferPairId
            false,  // isReconciled
            null    // auditInfo
        );
    }
}
```

---

## 6. Integration Test Fixtures

These fixtures call the REAL API to create prerequisite state. They live in the acceptance module.

**File**: `acceptance/src/test/java/com/shan/cyber/tech/financetracker/fixture/ApiFixture.java`

```java
package com.shan.cyber.tech.financetracker.fixture;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Central fixture class for integration tests.
 * Provides fluent helpers to register users, login, create accounts,
 * transactions etc. using the real API.
 *
 * Usage:
 *   ApiFixture fixture = new ApiFixture(port);
 *   String token = fixture.registerAndLogin();
 *   Long accountId = fixture.createCheckingAccount(token, "5000.00");
 */
public class ApiFixture {

    private final int port;

    public ApiFixture(int port) {
        this.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ---- Auth ----

    public String registerAndLogin() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "testuser_" + suffix;
        String email = "test_" + suffix + "@test.example.com";
        String password = "TestPass123!";

        given().port(port).contentType("application/json")
            .body("""
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Test",
                  "lastName": "User"
                }
                """.formatted(username, email, password))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201);

        return login(username, password);
    }

    public String login(String username, String password) {
        return given().port(port).contentType("application/json")
            .body("""
                {"username": "%s", "password": "%s"}
                """.formatted(username, password))
            .when().post("/api/v1/auth/login")
            .then().statusCode(200)
            .extract().path("token");
    }

    // ---- Accounts ----

    public Long createCheckingAccount(String token, String initialBalance) {
        return createAccount(token, "My Checking", "CHECKING", initialBalance);
    }

    public Long createSavingsAccount(String token, String initialBalance) {
        return createAccount(token, "My Savings", "SAVINGS", initialBalance);
    }

    public Long createCreditCardAccount(String token, String initialBalance) {
        return createAccount(token, "My Credit Card", "CREDIT_CARD", initialBalance);
    }

    public Long createCashAccount(String token, String initialBalance) {
        return createAccount(token, "My Cash", "CASH", initialBalance);
    }

    public Long createAccount(String token, String name, String typeCode, String initialBalance) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().port(port)
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                {
                  "name": "%s_%s",
                  "accountTypeCode": "%s",
                  "initialBalance": "%s",
                  "currency": "USD"
                }
                """.formatted(name, suffix, typeCode, initialBalance))
            .when().post("/api/v1/accounts")
            .then().statusCode(201)
            .extract().path("id");
    }

    // ---- Transactions ----

    /**
     * Creates an EXPENSE transaction using a system category.
     * The system category ID for "Groceries" is known from Liquibase seed data.
     * Adjust the hardcoded category ID if the seed changes.
     */
    public Long createExpenseTransaction(String token, Long accountId, String amount) {
        // Category ID for "Groceries" from seed data — adjust per Liquibase changeset
        return createTransaction(token, accountId, GROCERIES_CATEGORY_ID, amount,
            "EXPENSE", "2026-03-01");
    }

    public Long createIncomeTransaction(String token, Long accountId, String amount) {
        return createTransaction(token, accountId, SALARY_CATEGORY_ID, amount,
            "INCOME", "2026-03-01");
    }

    private static final Long GROCERIES_CATEGORY_ID = 4L;   // adjust per seed
    private static final Long SALARY_CATEGORY_ID = 1L;      // adjust per seed

    public Long createTransaction(String token, Long accountId, Long categoryId,
                                   String amount, String transactionType, String date) {
        return given().port(port)
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                {
                  "accountId": %d,
                  "categoryId": %d,
                  "amount": "%s",
                  "transactionType": "%s",
                  "transactionDate": "%s"
                }
                """.formatted(accountId, categoryId, amount, transactionType, date))
            .when().post("/api/v1/transactions")
            .then().statusCode(201)
            .extract().path("id");
    }

    // ---- Requests with auth ----

    public RequestSpecification withAuth(String token) {
        return given().port(port)
            .header("Authorization", "Bearer " + token)
            .contentType("application/json");
    }

    public RequestSpecification withoutAuth() {
        return given().port(port).contentType("application/json");
    }
}
```

**File**: `acceptance/src/test/java/com/shan/cyber/tech/financetracker/fixture/AbstractIntegrationTest.java`

```java
package com.shan.cyber.tech.financetracker.fixture;

import com.shan.cyber.tech.PersonalFinanceTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests in the acceptance module.
 *
 * - Starts the full Spring Boot application against a singleton PostgreSQL container.
 * - Each test class gets a fresh user via @BeforeEach.
 * - Provides ApiFixture for creating test data via the real API.
 *
 * Subclasses must be annotated with @Tag("integration") (enforced here).
 */
@Tag("integration")
@SpringBootTest(
    classes = PersonalFinanceTracker.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15.2")
        .withDatabaseName("personal-finance-tracker")
        .withUsername("pft-app-user")
        .withPassword("pft-app-user-secret");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    protected ApiFixture fixture;
    protected String authToken;

    /**
     * Creates a fresh user and logs in before each test.
     * Subclasses can call super.setUp() and then create additional state.
     */
    @BeforeEach
    void setUp() {
        fixture = new ApiFixture(port);
        authToken = fixture.registerAndLogin();
    }
}
```

---

## 7. Custom Assertions

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/assertion/MoneyAssertions.java`

```java
package com.shan.cyber.tech.financetracker.assertion;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Domain-specific assertion helpers for Money comparisons.
 * Always uses BigDecimal.compareTo() to avoid scale-difference false failures.
 */
public final class MoneyAssertions {

    private MoneyAssertions() {}

    /**
     * Asserts that two Money values represent the same amount in the same currency.
     * Uses compareTo() to ignore trailing zeros (100.00 == 100.0000).
     */
    public static void assertMoneyEquals(Money actual, Money expected) {
        assertThat(actual.currency())
            .as("Currency should match")
            .isEqualTo(expected.currency());
        assertThat(actual.amount().compareTo(expected.amount()))
            .as("Amount should be equal: expected %s but was %s",
                expected.amount().toPlainString(), actual.amount().toPlainString())
            .isZero();
    }

    /**
     * Asserts that two BigDecimal values are equal ignoring scale.
     */
    public static void assertAmountEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual.compareTo(expected))
            .as("Amount should be equal: expected %s but was %s",
                expected.toPlainString(), actual.toPlainString())
            .isZero();
    }

    /**
     * Asserts that an account's materialized current_balance matches its ledger sum.
     * Uses a JdbcTemplate query against the live database.
     * Only usable in integration and persistence adapter tests.
     */
    public static void assertLedgerBalanceMatchesMaterializedBalance(
            org.springframework.jdbc.core.JdbcTemplate jdbc, Long accountId) {

        BigDecimal ledgerBalance = jdbc.queryForObject(
            "SELECT a.initial_balance + COALESCE(SUM(CASE " +
            "  WHEN t.transaction_type IN ('INCOME','TRANSFER_IN') THEN t.amount " +
            "  WHEN t.transaction_type IN ('EXPENSE','TRANSFER_OUT') THEN -t.amount " +
            "  END), 0) " +
            "FROM finance_tracker.accounts a " +
            "LEFT JOIN finance_tracker.transactions t ON t.account_id = a.id " +
            "WHERE a.id = ? GROUP BY a.initial_balance",
            BigDecimal.class, accountId
        );

        BigDecimal materializedBalance = jdbc.queryForObject(
            "SELECT current_balance FROM finance_tracker.accounts WHERE id = ?",
            BigDecimal.class, accountId
        );

        assertThat(ledgerBalance)
            .as("Ledger-derived balance should match materialized current_balance for account " + accountId)
            .isNotNull();
        assertThat(materializedBalance)
            .as("Materialized current_balance should not be null for account " + accountId)
            .isNotNull();
        assertThat(ledgerBalance.compareTo(materializedBalance))
            .as("Ledger balance (%s) should match materialized balance (%s) for account %d",
                ledgerBalance, materializedBalance, accountId)
            .isZero();
    }

    /**
     * Asserts that a transfer pair is symmetric: same amount, opposite types, mutual references.
     */
    public static void assertTransferPairIsSymmetric(
            org.springframework.jdbc.core.JdbcTemplate jdbc, Long outLegId) {

        var legs = jdbc.queryForList(
            "SELECT id, transaction_type, amount, transfer_pair_id " +
            "FROM finance_tracker.transactions " +
            "WHERE id = ? OR transfer_pair_id = ?",
            outLegId, outLegId
        );

        assertThat(legs)
            .as("Transfer must produce exactly 2 transaction rows")
            .hasSize(2);

        BigDecimal amount1 = (BigDecimal) legs.get(0).get("amount");
        BigDecimal amount2 = (BigDecimal) legs.get(1).get("amount");
        assertThat(amount1.compareTo(amount2))
            .as("Both transfer legs must have the same amount")
            .isZero();

        String type1 = (String) legs.get(0).get("transaction_type");
        String type2 = (String) legs.get(1).get("transaction_type");
        assertThat(java.util.Set.of(type1, type2))
            .as("Transfer legs must be TRANSFER_OUT and TRANSFER_IN")
            .containsExactlyInAnyOrder("TRANSFER_OUT", "TRANSFER_IN");
    }
}
```

---

## 8. RestAssured Base Configuration

**File**: `acceptance/src/test/java/com/shan/cyber/tech/financetracker/fixture/RestAssuredConfig.java`

```java
package com.shan.cyber.tech.financetracker.fixture;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.path.json.config.JsonPathConfig;

/**
 * Static RestAssured configuration applied once at integration test startup.
 * Call RestAssuredConfig.configure() in AbstractIntegrationTest's @BeforeAll or @BeforeEach.
 */
public final class RestAssuredConfig {

    private RestAssuredConfig() {}

    public static void configure(int port) {
        RestAssured.port = port;
        RestAssured.config = RestAssured.config()
            .jsonConfig(JsonConfig.jsonConfig()
                // Use BigDecimal for number parsing to avoid floating-point issues
                .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
```

### Standard response validation snippets for integration tests

```java
// Verify paginated response shape
.then()
    .statusCode(200)
    .body("content", notNullValue())
    .body("page", equalTo(0))
    .body("size", equalTo(30))
    .body("totalElements", greaterThanOrEqualTo(0))
    .body("totalPages", greaterThanOrEqualTo(0));

// Verify error response shape
.then()
    .statusCode(422)
    .body("status", equalTo(422))
    .body("error", notNullValue())
    .body("message", notNullValue())
    .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z"))
    .body("path", notNullValue());

// Verify 201 + Location header
.then()
    .statusCode(201)
    .header("Location", matchesPattern("/api/v1/.*"));

// Extract token from login
String token = given()...
    .when().post("/api/v1/auth/login")
    .then().statusCode(200)
    .extract().path("token");

// Money amount assertion (BigDecimal from JSON, no floating-point)
BigDecimal balance = given()...
    .when().get("/api/v1/accounts/1")
    .then()
    .extract().path("currentBalance");
assertThat(balance.compareTo(new BigDecimal("1000.00"))).isZero();
```

---

## 9. ArchUnit Test Setup

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/architecture/HexagonalArchitectureTest.java`

```java
package com.shan.cyber.tech.financetracker.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.shan.cyber.tech.financetracker",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    private static final String DOMAIN_PACKAGES =
        "com.shan.cyber.tech.financetracker..domain..";

    @ArchTest
    static final ArchRule domain_must_not_import_spring =
        noClasses()
            .that().resideInAPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_must_not_import_jpa =
        noClasses()
            .that().resideInAPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domain_must_not_import_jackson =
        noClasses()
            .that().resideInAPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule domain_must_not_import_hibernate =
        noClasses()
            .that().resideInAPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.hibernate..");

    @ArchTest
    static final ArchRule controllers_must_not_call_domain_services_directly =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker..adapter.inbound.web..")
            .should().dependOnClassesThat()
            .resideInAPackage(
                "com.shan.cyber.tech.financetracker..domain.service..");

    @ArchTest
    static final ArchRule account_domain_must_not_import_transaction_domain =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker.account.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage(
                "com.shan.cyber.tech.financetracker.transaction.domain..");

    @ArchTest
    static final ArchRule transaction_domain_must_not_import_account_domain =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker.transaction.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage(
                "com.shan.cyber.tech.financetracker.account.domain..");

    @ArchTest
    static final ArchRule budget_domain_must_not_import_transaction_domain =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker.budget.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage(
                "com.shan.cyber.tech.financetracker.transaction.domain..");

    @ArchTest
    static final ArchRule domain_services_must_not_use_spring_service_annotation =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker..domain.service..")
            .should().beAnnotatedWith(
                org.springframework.stereotype.Service.class);

    @ArchTest
    static final ArchRule domain_models_must_not_use_entity_annotation =
        noClasses()
            .that().resideInAPackage(
                "com.shan.cyber.tech.financetracker..domain.model..")
            .should().beAnnotatedWith(jakarta.persistence.Entity.class);
}
```

---

## 10. CI Pipeline Configuration

### Gradle command sequence for CI

```bash
# Stage 1: Fast feedback (no Docker, < 30 seconds)
./gradlew :application:test

# Stage 2: Adapter + Integration tests (Docker required, ~ 2-5 minutes)
./gradlew :application:adapterTest :acceptance:integrationTest

# (Optional) Full verification
./gradlew check
```

### GitHub Actions workflow snippet

```yaml
name: CI

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run unit and architecture tests
        run: ./gradlew :application:test
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: unit-test-results
          path: application/build/reports/tests/test/

  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run adapter and integration tests
        run: ./gradlew :application:adapterTest :acceptance:integrationTest
      - name: Upload integration test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: integration-test-results
          path: acceptance/build/reports/tests/integrationTest/
```

---

## 11. Seed Data Category IDs Reference

These are the expected IDs from Liquibase seed data. Tests that reference system categories by ID must use these values. Update this table when a new Liquibase changeset changes seed data:

| Category | Type | Expected ID | Notes |
|---|---|---|---|
| Salary | INCOME | TBD from seed | System category |
| Groceries | EXPENSE | TBD from seed | Child of Food |
| Food | EXPENSE | TBD from seed | Parent category |
| Housing | EXPENSE | TBD from seed | Parent category |
| Rent | EXPENSE | TBD from seed | Child of Housing |
| Account Transfer | TRANSFER | TBD from seed | System, cannot be budgeted |

**Note to dev**: After implementing the Liquibase category seed changeset, query the database to get actual IDs and update `ApiFixture.GROCERIES_CATEGORY_ID` and `ApiFixture.SALARY_CATEGORY_ID` constants.

---

## 12. Fixed Clock Configuration

Domain services that depend on `LocalDate.now()` or `Instant.now()` must be clock-injectable. Add a `Clock` parameter to each domain service constructor and expose it as a `@Value`-injected `@Bean` in Config:

```java
// In IdentityConfig.java
@Bean
public IdentityCommandService identityCommandService(
        UserPersistencePort userPort,
        SessionPersistencePort sessionPort,
        PasswordHasherPort passwordHasher,
        LoginRateLimiterPort rateLimiter,
        IdentityEventPublisherPort eventPublisher,
        @Value("${app.session.duration-days}") int sessionDurationDays) {
    return new IdentityCommandService(
        userPort, sessionPort, passwordHasher, rateLimiter, eventPublisher,
        sessionDurationDays,
        Clock.systemUTC()   // overridable in tests
    );
}
```

In domain service unit tests:
```java
Clock fixedClock = Clock.fixed(
    Instant.parse("2026-03-01T12:00:00Z"),
    ZoneOffset.UTC
);
service = new TransactionCommandService(txPort, balancePort, categoryPort, eventPublisher, fixedClock);
```
