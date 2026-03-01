# Testing Strategy — Personal Finance Tracker
**Author**: QA Automation Tester
**Date**: 2026-03-01
**Status**: Authoritative — full-stack-dev writes tests from this document

---

## 0. Strategy Overview

The Personal Finance Tracker uses strict hexagonal architecture (Ports and Adapters). This is a first-class advantage for testing: the domain layer is pure Java, so every business rule can be unit-tested with zero framework overhead. The testing pyramid is shaped accordingly:

```
          [Integration Tests]           <- Tier 3: acceptance/ module, full HTTP round-trips
         [Adapter Unit Tests]           <- Tier 2: MockMvc + @DataJpaTest, per-layer
        [Domain Unit Tests]             <- Tier 1: pure JUnit 5, no Spring, fastest
       [Architecture Tests]             <- Enforced at CI, runs with unit tests
```

**Non-negotiable rules:**
- Domain tests MUST run without Spring context. If a test uses `@SpringBootTest` or `@MockBean` in a domain test, it is wrong.
- All monetary assertions use `BigDecimal.compareTo()`, never `.equals()` across scale differences.
- Every test that verifies balance correctness must also independently verify via the ledger formula: `current_balance == initial_balance + SUM(signed amounts)`.
- Tests are deterministic. No `Thread.sleep()`. No `LocalDate.now()` hardcoded without a fixed clock.

---

## 1. Testing Layers

### Layer 1: Domain Unit Tests

**What is tested**: Pure domain logic inside `{context}/domain/model/`, `{context}/domain/service/`, and `shared/domain/model/`. This includes aggregate root business methods, value object invariants, domain service orchestration logic, and domain event creation.

**What is mocked**: All outbound ports (persistence, event publishers). The domain service is instantiated directly via constructor injection with Mockito mocks for ports.

**What is NOT mocked**: The domain model classes themselves. You always instantiate real `Account`, `Money`, `Budget`, etc.

**Framework**: JUnit 5 + Mockito (via `@ExtendWith(MockitoExtension.class)`). No `@SpringBootTest`.

**Test file locations**:
```
application/src/test/java/com/shan/cyber/tech/financetracker/
  shared/domain/model/MoneyTest.java
  shared/domain/model/DateRangeTest.java
  identity/domain/service/IdentityCommandServiceTest.java
  account/domain/model/AccountTest.java
  account/domain/service/AccountCommandServiceTest.java
  category/domain/service/CategoryCommandServiceTest.java
  transaction/domain/service/TransactionCommandServiceTest.java
  transaction/domain/service/TransferCommandServiceTest.java
  budget/domain/model/BudgetTest.java
  budget/domain/model/BudgetPeriodTypeTest.java
  budget/domain/service/BudgetCommandServiceTest.java
```

**Naming convention**: `should_{expectedBehaviour}_when_{condition}`

Examples:
```
should_throw_InsufficientFundsException_when_savings_account_debited_below_zero
should_increase_currentBalance_when_account_credited
should_return_WARNING_status_when_budget_80_percent_used
should_reject_createAccount_when_user_has_20_active_accounts
```

**Test data strategy**: Use builder-style factory methods inside each test class's `@BeforeEach` or as static helper methods. Do NOT use shared test fixtures for unit tests — each test constructs exactly what it needs, no more.

Example domain object construction:
```java
private Account checkingAccount(Money balance) {
    return new Account(
        new AccountId(1L),
        new UserId(10L),
        checkingAccountType(),      // AccountType with allowsNegativeBalance=true
        "Test Checking",
        balance,
        balance,                    // initialBalance = currentBalance for simplicity
        true,
        true,
        0L
    );
}

private AccountType checkingAccountType() {
    return new AccountType((short)1, "CHECKING", "Checking Account", true, false);
}
```

**Coverage target**: 90% line coverage on domain model methods. 85% line coverage on domain service methods. Every `throws` declaration must have a test for the throw path.

---

### Layer 2: Port Contract Tests (Persistence Adapter Tests)

**What is tested**: Outbound persistence adapters — that the JPA adapter implementation correctly satisfies the port interface contract. This includes: correct SQL execution, correct mapping between domain objects and JPA entities, correct query semantics (e.g., case-insensitive name lookup, only-active filters, pagination).

**What is mocked**: Nothing. These tests use a real Testcontainers PostgreSQL 15.2 instance with Liquibase migrations applied.

**Framework**: JUnit 5 + `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers.

**Test file locations**:
```
application/src/test/java/com/shan/cyber/tech/financetracker/
  identity/adapter/outbound/persistence/UserPersistenceAdapterTest.java
  identity/adapter/outbound/persistence/SessionPersistenceAdapterTest.java
  account/adapter/outbound/persistence/AccountPersistenceAdapterTest.java
  category/adapter/outbound/persistence/CategoryPersistenceAdapterTest.java
  transaction/adapter/outbound/persistence/TransactionPersistenceAdapterTest.java
  budget/adapter/outbound/persistence/BudgetPersistenceAdapterTest.java
  budget/adapter/outbound/persistence/BudgetSpendCalculationAdapterTest.java
  reporting/adapter/outbound/persistence/DashboardReadAdapterTest.java
```

**Naming convention**: Same as domain tests: `should_{expectedBehaviour}_when_{condition}`

**Test data strategy**: Use SQL `@Sql` scripts or programmatic entity insertion via `TestEntityManager` to set up state. Each test method cleans up via `@Transactional` rollback (default for `@DataJpaTest`).

**Key invariants to verify for every persistence adapter**:
- `findById` with wrong userId returns empty (multi-tenant isolation)
- `findActiveByOwner` never returns inactive records
- Case-insensitive uniqueness queries behave correctly under PostgreSQL collation
- `save()` and then `findById()` returns a domain object with all fields correctly mapped

**Coverage target**: 80% of query methods in each persistence adapter must have a dedicated test.

---

### Layer 3: Adapter Unit Tests (Controller Tests)

**What is tested**: Inbound REST adapters (`{context}/adapter/inbound/web/{Context}Controller.java`) in isolation. Tests verify: correct HTTP method routing, request deserialization, Bean Validation triggering, domain command construction, HTTP status codes, response body shape, Location header on 201 responses, and correct delegation to inbound ports.

**What is mocked**: All inbound ports (use cases and queries) are Mockito mocks. The Spring context is loaded as a web slice only (`@WebMvcTest`).

**Framework**: JUnit 5 + `@WebMvcTest` + MockMvc + Mockito (`@MockBean`).

**Test file locations**:
```
application/src/test/java/com/shan/cyber/tech/financetracker/
  identity/adapter/inbound/web/AuthControllerTest.java
  identity/adapter/inbound/web/SessionAuthFilterTest.java
  account/adapter/inbound/web/AccountControllerTest.java
  category/adapter/inbound/web/CategoryControllerTest.java
  transaction/adapter/inbound/web/TransactionControllerTest.java
  transaction/adapter/inbound/web/TransferControllerTest.java
  budget/adapter/inbound/web/BudgetControllerTest.java
  reporting/adapter/inbound/web/DashboardControllerTest.java
  shared/adapter/inbound/web/GlobalExceptionHandlerTest.java
```

**Naming convention**: Same pattern.

**Test data strategy**: Use static factory methods that return pre-filled request JSON strings or DTOs. Use `objectMapper.writeValueAsString()` for request bodies.

**SessionAuthFilter handling**: In controller tests, either:
1. Configure the `@WebMvcTest` to exclude `SessionAuthFilter` and manually set `SecurityContextHolder.setCurrentUserId(1L)` in test setup, OR
2. Use a `@TestConfiguration` that provides a mock `SessionPersistencePort` returning a valid session.

The recommended approach is option 1: annotate controller tests with:
```java
@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
```
where `TestSecurityConfig` registers a no-op filter that sets a fixed user ID.

**Coverage target**: 100% of controller endpoints have at least one happy-path test and one validation-error test. Every 4xx scenario documented in the domain spec has a controller test.

---

### Layer 4: Integration Tests (Full HTTP Round-Trips)

**What is tested**: The entire system from HTTP request to database and back. No mocks. Tests verify that all layers wire together correctly, that Liquibase migrations produce the expected schema, that cross-context adapters work, and that business rules are enforced end-to-end.

**What is mocked**: Nothing. Real PostgreSQL 15.2 via Testcontainers, real Spring Boot application context.

**Framework**: JUnit 5 + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured + Testcontainers. Tagged `@Tag("integration")`.

**Test file locations** (acceptance module):
```
acceptance/src/test/java/com/shan/cyber/tech/financetracker/
  smoke/ApplicationStartupTest.java
  smoke/LiquibaseSchemaTest.java
  integration/auth/AuthIntegrationTest.java
  integration/account/AccountIntegrationTest.java
  integration/account/AccountBalanceIntegrationTest.java
  integration/category/CategoryIntegrationTest.java
  integration/transaction/TransactionIntegrationTest.java
  integration/transaction/TransactionFilterIntegrationTest.java
  integration/transaction/TransferIntegrationTest.java
  integration/budget/BudgetIntegrationTest.java
  integration/budget/BudgetSpendCalculationIntegrationTest.java
  integration/reporting/DashboardIntegrationTest.java
  regression/RegressionSmokeTest.java
  security/AuthorizationIntegrationTest.java
  security/SqlInjectionTest.java
```

**Naming convention**: Same pattern. Integration test class names end in `IntegrationTest`.

**Test data strategy**: Use `TestUserFixture` and `TestAccountFixture` helper classes (see `test-infrastructure.md`) that call the real API to create prerequisite data in each test. Each test class uses `@BeforeEach` to register and log in a fresh user, ensuring test isolation.

**State isolation strategy**: Each integration test class creates its own unique user (e.g., `user_auth_it_001@test.example.com`). Tests within a class share the same user session but create resources with unique names. The Testcontainers PostgreSQL is a singleton container shared across all integration tests (see `test-infrastructure.md`).

**Coverage target**: Every API endpoint has at least one integration test covering the happy path. Every cross-context interaction has an integration test.

---

### Layer 5: Architecture Tests

**What is tested**: Hexagonal boundary constraints enforced by ArchUnit at compile time.

**Framework**: ArchUnit 1.3.0 (add to dependencies — see `test-infrastructure.md`).

**Test file location**:
```
application/src/test/java/com/shan/cyber/tech/financetracker/architecture/HexagonalArchitectureTest.java
```

**Rules to enforce**:

```java
// Rule 1: No Spring/JPA/Jackson imports in any domain package
noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "org.springframework..",
        "jakarta.persistence..",
        "com.fasterxml.jackson..",
        "org.hibernate.."
    )

// Rule 2: No cross-context domain imports
// e.g., account domain cannot import transaction domain
noClasses()
    .that().resideInAPackage("..account.domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..transaction.domain..")

noClasses()
    .that().resideInAPackage("..account.domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..category.domain..")

// (replicate for each pair of bounded contexts)

// Rule 3: Controllers only call inbound ports, never domain services directly
noClasses()
    .that().resideInAPackage("..adapter.inbound.web..")
    .should().dependOnClassesThat()
    .resideInAPackage("..domain.service..")

// Rule 4: Domain services must not be annotated with @Service or @Component
noClasses()
    .that().resideInAPackage("..domain.service..")
    .should().beAnnotatedWith(org.springframework.stereotype.Service.class)

// Rule 5: Domain model classes must not be annotated with @Entity
noClasses()
    .that().resideInAPackage("..domain.model..")
    .should().beAnnotatedWith(jakarta.persistence.Entity.class)
```

**Coverage target**: All five rules must pass on every CI build. Zero exceptions permitted.

---

## 2. Test Naming Conventions (Canonical Reference)

### Test class names
| Layer | Pattern | Example |
|---|---|---|
| Domain model test | `{DomainClass}Test` | `AccountTest`, `MoneyTest`, `BudgetTest` |
| Domain service test | `{ServiceClass}Test` | `AccountCommandServiceTest` |
| Persistence adapter test | `{AdapterClass}Test` | `AccountPersistenceAdapterTest` |
| Controller test | `{ControllerClass}Test` | `AccountControllerTest` |
| Integration test | `{Feature}IntegrationTest` | `AccountBalanceIntegrationTest` |
| Architecture test | `{ArchConcern}Test` | `HexagonalArchitectureTest` |

### Test method names
Pattern: `should_{expectedResult}_when_{condition}`

Examples:
```
should_throw_InsufficientFundsException_when_savings_account_debited_below_zero
should_return_201_and_location_header_when_account_created_successfully
should_return_409_when_account_name_duplicated_for_same_user
should_update_both_account_balances_when_transfer_created
should_not_import_spring_annotations_in_domain_model
```

---

## 3. Test Execution Model

### Gradle task separation

```kotlin
// Unit tests + Architecture tests (fast, no Docker)
// Command: ./gradlew :application:test
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

// Persistence adapter tests (requires Docker)
// Command: ./gradlew :application:adapterTest
tasks.register<Test>("adapterTest") {
    useJUnitPlatform {
        includeTags("adapter")
    }
    group = "verification"
}

// Full integration tests (requires Docker)
// Command: ./gradlew :acceptance:integrationTest
// Defined in acceptance/build.gradle.kts
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    group = "verification"
    description = "Full HTTP round-trip integration tests"
}
```

### CI pipeline execution order
1. `./gradlew :application:test` — domain unit tests + architecture tests (no Docker required)
2. `./gradlew :application:adapterTest` — persistence adapter tests (Docker required)
3. `./gradlew :acceptance:integrationTest` — full integration tests (Docker required)

Steps 2 and 3 run in the same CI stage since both require Docker. Step 1 can fail fast independently.

---

## 4. What is Mocked vs. Real — Per Layer Summary

| Test Layer | Domain Classes | Spring Context | PostgreSQL | External Ports |
|---|---|---|---|---|
| Domain unit tests | REAL | NONE | NONE | MOCKED (Mockito) |
| Persistence adapter tests | REAL | JPA slice only | REAL (Testcontainers) | N/A |
| Controller (web) tests | MOCKED (UseCase returns) | Web slice only | NONE | MOCKED (Mockito) |
| Integration tests | REAL | FULL | REAL (Testcontainers) | REAL |
| Architecture tests | Class metadata only | NONE | NONE | N/A |

---

## 5. Data Integrity Verification Strategy

These database-level checks are run only in integration tests, by querying the database directly after API operations:

### Balance Ledger Consistency
After every transaction create/update/delete:
```java
BigDecimal ledgerBalance = jdbcTemplate.queryForObject(
    "SELECT a.initial_balance + COALESCE(SUM(CASE " +
    "  WHEN t.transaction_type IN ('INCOME','TRANSFER_IN') THEN t.amount " +
    "  WHEN t.transaction_type IN ('EXPENSE','TRANSFER_OUT') THEN -t.amount " +
    "  END), 0) " +
    "FROM finance_tracker.accounts a " +
    "LEFT JOIN finance_tracker.transactions t ON t.account_id = a.id " +
    "WHERE a.id = ? GROUP BY a.initial_balance",
    BigDecimal.class, accountId
);
BigDecimal materializedBalance = jdbcTemplate.queryForObject(
    "SELECT current_balance FROM finance_tracker.accounts WHERE id = ?",
    BigDecimal.class, accountId
);
assertThat(ledgerBalance.compareTo(materializedBalance)).isZero();
```

### Transfer Pair Symmetry
After every transfer create:
```java
// Both rows exist and are linked
List<Map<String, Object>> legs = jdbcTemplate.queryForList(
    "SELECT id, transaction_type, amount, transfer_pair_id " +
    "FROM finance_tracker.transactions WHERE transfer_pair_id = ? OR id = ?",
    pairId, pairId
);
assertThat(legs).hasSize(2);
// Verify same amount, opposite types, mutual references
```

### User Data Isolation
In every integration test class involving multi-user scenarios:
```java
// Query DB directly to verify no cross-user data leakage
long count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM finance_tracker.accounts WHERE user_id = ? AND id = ?",
    Long.class, otherUserId, accountIdBelongingToCurrentUser
);
assertThat(count).isZero();
```

---

## 6. Money Assertion Standards

Never use `assertEquals` or `isEqualTo` on BigDecimal directly in financial assertions (scale differences cause false failures). Always use:

```java
// Correct
assertThat(actual.amount().compareTo(new BigDecimal("100.0000"))).isZero();

// OR use the custom assertion (defined in test-infrastructure.md)
assertMoneyEquals(actual, Money.of(new BigDecimal("100.00"), "USD"));

// Incorrect — will fail if scales differ (100.00 != 100.0000)
assertThat(actual.amount()).isEqualTo(new BigDecimal("100.00"));
```

---

## 7. Fixed Clock Strategy

Domain services that call `LocalDate.now()` or `OffsetDateTime.now()` must accept a `Clock` parameter (injected via constructor in production, overridden in tests).

Pattern for tests:
```java
// In test setup
Clock fixedClock = Clock.fixed(
    Instant.parse("2026-03-01T12:00:00Z"),
    ZoneOffset.UTC
);

// Domain service receives fixed clock
service = new TransactionCommandService(
    transactionPort, accountBalancePort, categoryValidationPort,
    eventPublisher, fixedClock
);
```

This ensures date-range tests (10 years past, 30 days future) are deterministic regardless of when the test runs.

---

## 8. Test Tagging Reference

| Tag | Applied To | Excluded From |
|---|---|---|
| `@Tag("integration")` | All tests in `acceptance/` module | `./gradlew :application:test` |
| `@Tag("adapter")` | All `@DataJpaTest` adapter tests | `./gradlew :application:test` (fast tier) |
| `@Tag("architecture")` | `HexagonalArchitectureTest` | Nothing — always included |
| `@Tag("smoke")` | Regression smoke tests | Nothing — always included |
| `@Tag("security")` | IDOR, injection, auth tests | Nothing — always included |

---

## 9. Test Independence Rules

1. Each integration test class must be able to run standalone. If test B depends on state created by test A, it is wrong.
2. Each test method within a class creates its own named resources (use unique suffixes: `UUID.randomUUID().toString().substring(0,8)` appended to resource names).
3. `@BeforeEach` at the integration test level: register a new user + login + capture the session token. This is the ONLY shared state within a test class.
4. Never assert on IDs from previous test runs — IDs are sequences and non-deterministic across runs.
5. All `@DataJpaTest` persistence tests run within a transaction that is rolled back after each test method (Spring default behaviour).
