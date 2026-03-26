# QA Automation Tester — Persistent Memory
**Last updated**: 2026-03-26
**Project**: Personal Finance Tracker

---

## Project at a Glance

Spring Boot 3.2.2, Java 17, PostgreSQL 15.2, Liquibase, Gradle multi-module (`application/`, `database/`, `acceptance/`). Strict hexagonal architecture (Ports and Adapters). Root package: `com.shan.cyber.tech.financetracker`. Main app class: `com.shan.cyber.tech.PersonalFinanceTracker`.

---

## Testing Stack

| Tool | Version | Purpose |
|---|---|---|
| JUnit 5 | BOM-managed via spring-boot-starter-test | All test layers |
| Mockito | BOM-managed via spring-boot-starter-test | Port mocks in domain tests |
| MockMvc | BOM-managed | Controller (@WebMvcTest) tests |
| Testcontainers (postgres) | 1.19.6 | Persistence adapter + integration tests |
| Testcontainers (junit-jupiter) | 1.19.6 | JUnit 5 integration |
| RestAssured | 5.4.0 | Integration test HTTP assertions |
| ArchUnit | 1.3.0 | Hexagonal boundary enforcement |
| AssertJ | BOM-managed | Fluent assertions |

---

## Five Testing Layers

| Layer | Framework | Module | Tag |
|---|---|---|---|
| Domain Unit | JUnit 5 + Mockito (no Spring) | `application/src/test/` | (none — default) |
| Persistence Adapter | @DataJpaTest + Testcontainers | `application/src/test/` | `@Tag("adapter")` |
| Controller (Web) | @WebMvcTest + MockMvc + @MockBean | `application/src/test/` | (none — default) |
| Integration | @SpringBootTest + RestAssured + Testcontainers | `acceptance/src/test/` | `@Tag("integration")` |
| Architecture | ArchUnit @AnalyzeClasses | `application/src/test/` | (none — always runs) |

---

## Gradle Tasks

```bash
./gradlew :application:test           # Domain + controller + architecture tests (fast, no Docker)
./gradlew :application:adapterTest    # Persistence adapter tests (Docker)
./gradlew :acceptance:integrationTest # Full HTTP integration tests (Docker)
```

---

## Key Architectural Constraints Affecting Tests

1. **Domain tests: ZERO Spring context**. If a test has `@SpringBootTest` or `@MockBean` inside `domain/`, it is wrong.
2. **Domain services are NOT `@Service`-annotated** — they are created as `@Bean` in Config classes. Tests instantiate them via constructor injection.
3. **No cross-context domain imports** — `AccountBalancePort` lives in `transaction/domain/port/outbound/` but is implemented by `AccountBalanceAdapter` in `account/adapter/outbound/crosscontext/`.
4. **Money is always BigDecimal** — use `compareTo()` not `equals()` in assertions. Helper: `assertMoneyEquals()` in `MoneyAssertions.java`.
5. **Balance = initial_balance + SUM(signed transaction amounts)** — every integration test modifying balances must verify this formula via a JDBC query.
6. **Transfer = 2 rows atomically** — both legs share `transfer_pair_id`, created in a single DB transaction.

---

## Test File Locations (Key Files)

| File | Purpose |
|---|---|
| `.../config/TestContainersConfig.java` | Singleton PostgreSQL container |
| `.../config/AbstractPersistenceTest.java` | Base class for all @DataJpaTest tests |
| `.../config/TestSecurityConfig.java` | No-op auth filter for @WebMvcTest (sets userId=1L) |
| `.../testdata/UserBuilder.java` | Domain User builder for unit tests |
| `.../testdata/AccountBuilder.java` | Domain Account builder with AccountTypes inner class |
| `.../testdata/MoneyFactory.java` | Money convenience factory (usd("100.00")) |
| `.../testdata/TransactionBuilder.java` | Domain Transaction builder |
| `.../assertion/MoneyAssertions.java` | assertMoneyEquals(), assertLedgerBalanceMatchesMaterializedBalance() |
| `acceptance/.../fixture/AbstractIntegrationTest.java` | Base class for all integration tests |
| `acceptance/.../fixture/ApiFixture.java` | Creates test data via real API (register, login, createAccount etc.) |
| `.../architecture/HexagonalArchitectureTest.java` | All ArchUnit rules |
| `application/src/test/resources/application-test.yaml` | Test profile config |

---

## Test Method Naming Convention

`should_{expectedResult}_when_{condition}`

Examples:
- `should_throw_InsufficientFundsException_when_savings_account_debited_below_zero`
- `should_return_201_and_location_header_when_account_created_successfully`
- `should_return_409_when_account_name_duplicated_for_same_user`

---

## Critical Business Rules Requiring Test Coverage

1. `Money.add(0.1, 0.2)` must equal exactly `0.30` (no float error) — R1-001
2. SAVINGS and CASH accounts: reject expense that would go negative — R2-002, R2-003
3. CHECKING can go negative — R2-004
4. Transfer creates exactly 2 rows atomically — R3-001
5. Transfer pair symmetry: same amount, TRANSFER_OUT + TRANSFER_IN, mutual pair IDs — R3-002
6. Budget spend includes child category transactions — R5-003
7. Budget does NOT block transactions — R5-004 (warning only)
8. Cross-user access returns 404, NOT 403 — R7-003
9. After logout, token is invalid — R7-009
10. Rate limiting: 6th failed login in 5 min returns 429 — R7-006
11. `account.current_balance == account.initial_balance + SUM(signed transaction amounts)` — R2-011

---

## Decisions Made

- Use `@Tag("adapter")` and `@Tag("integration")` to separate test tiers for Gradle tasks.
- Integration tests each create a fresh user via `ApiFixture.registerAndLogin()` in `@BeforeEach` for isolation.
- Category seed IDs in `ApiFixture` must be updated after the Liquibase category seed changeset is written.
- `Clock` is injected into all domain services to enable deterministic date-based tests.
- `@WebMvcTest` controller tests use `TestSecurityConfig` which sets userId=1L globally via a no-op filter.
- AssertJ `BigDecimal` assertions always use `compareTo()` via `assertMoneyEquals()`, never direct `.isEqualTo()`.

---

## Budget Domain Model Notes (Category-Hierarchical Budget Plan)

- `BudgetPlanView.expenseGroups()` returns `List<BudgetPlanCategoryGroup>` (NOT `expenseRows()`). The old `expenseRows()` field no longer exists — flatten via `.stream().flatMap(g -> g.rows().stream())` in tests.
- `CategorySummary` is a 4-param record: `(CategoryId id, String name, String typeCode, Long parentCategoryId)`. Pass `null` for `parentCategoryId` for flat (ungrouped) categories.
- `BudgetPlanCategoryRow` has 11 fields — new fields `frequency`, `monthlyAmount`, `yearlyAmount` added. Always supply all 11.
- `BudgetCommandService` takes 3 constructor args: `(BudgetPersistencePort, BudgetEventPublisherPort, CategoryNameQueryPort)`.
- `POST /api/v1/budgets/upsert-by-category` — invalid enum string in `periodType` causes `IllegalArgumentException` → 500 (not 400), because `UpsertBudgetByCategoryRequestDto.periodType` is a plain `String`, not a `BudgetPeriod`. Validated at `BudgetPeriod.valueOf()` call in controller. Future fix: add `@Pattern` or deserialize directly to `BudgetPeriod`.
- JSON response for budget plan uses field name `expenseGroups` (not `expenseRows`). Controller tests must assert against `$.expenseGroups`.

## Pre-existing Test Compile Failures (as of 2026-03-20)

The test suite has 2 compile errors that block `./gradlew :application:test`. They are unrelated to the category bug fix:

1. **`AccountCommandServiceTest`** — constructor call `new AccountCommandService(port1, port2, port3)` is missing the 4th arg `AccountTransactionCountPort`. Production service was updated to require it but the test was not updated.
2. **`BudgetControllerTest` line ~385** — `new BudgetPlanCategoryGroup(null, name, rows, monthlyTotal, yearlyTotal)` uses 5 args but the record now requires 7: `(Long parentCategoryId, String parentCategoryName, List<rows>, BigDecimal groupMonthlyTotal, BigDecimal groupYearlyTotal, BigDecimal groupActualTotal, boolean alertTriggered)`.

Both are pre-existing and must be fixed before any `./gradlew :application:test` run will succeed.

---

## Application Health Feedback Loop (your gate: Layer 2)

QA is responsible for Layer 2 of the health feedback loop — all tests must pass including `ApplicationContextLoadTest`.

```bash
./gradlew :application:test --no-daemon    # must pass before any track is done
```

**`ApplicationContextLoadTest`** lives at:
`application/src/test/java/com/shan/cyber/tech/financetracker/ApplicationContextLoadTest.java`

It is a `@SpringBootTest` that just loads the Spring context. It catches:
- Duplicate bean definitions (two classes implementing the same inbound port interface)
- Missing `@Bean` declarations
- Circular dependencies
- Misconfigured `@Configuration` classes

This test MUST be included in every test run. If it fails, stop and fix before proceeding.

**Lesson (2026-03-26)**: `TransactionCommandService` and `TransactionApplicationService` both implemented
`CreateTransferUseCase` + `DeleteTransactionUseCase`. Spring couldn't resolve which to inject into
`TransferController` → startup crash. `ApplicationContextLoadTest` would have caught this at Layer 2
before docker. Now mandatory on all tracks.

## Pipeline Entry
All tracks enter via `engineering-manager`. qa-automation-tester is spawned at Phase 4A (FEATURE)
or equivalent QA phase in other tracks.

## Note on SecurityContextHolder in @WebMvcTest

The `TestSecurityConfig` class referenced in memory does NOT exist in `application/src/test/` — instead, controller tests directly call `SecurityContextHolder.setCurrentUserId(1L)` in `@BeforeEach` and `SecurityContextHolder.clear()` in `@AfterEach`. The `SecurityContextHolder` is a custom static holder, not Spring Security.

---

## Documents Written (This Session)

- `testing-strategy.md` — Full 5-layer testing strategy with mocking decisions and coverage targets
- `test-cases-by-phase.md` — Exact test class and method names for every feature, organized by development phase
- `test-infrastructure.md` — All infrastructure code: Testcontainers, builders, fixtures, RestAssured config, ArchUnit rules, CI pipeline
- `acceptance-criteria-checklist.md` — Feature-by-feature definition of done with checkbox for every test
- `MEMORY.md` — This file
