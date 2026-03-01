# Architecture Decision Records — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: Authoritative — full-stack-dev builds from this document

---

## ADR-001: Package Structure — Domain-First

### Context
The current codebase has `com.shan.cyber.tech.personal.finance.tracker` (deep, non-standard) and the `@SpringBootApplication` root is `com.shan.cyber.tech`. Adding features with a layer-first structure (controller/, service/, repository/) causes every layer to import from all domains simultaneously, making it harder to reason about boundaries.

### Decision
Use **domain-first** package structure under the application module root `com.shan.cyber.tech.financetracker`:

```
com.shan.cyber.tech.financetracker
├── auth/
│   ├── controller/    AuthController.java
│   ├── service/       AuthService.java, SessionService.java
│   ├── repository/    UserRepository.java, SessionRepository.java
│   ├── entity/        User.java, Session.java
│   ├── dto/           RegisterRequest.java, LoginRequest.java, LoginResponse.java, UserProfileResponse.java
│   └── filter/        SessionAuthFilter.java
├── account/
│   ├── controller/    AccountController.java
│   ├── service/       AccountService.java
│   ├── repository/    AccountRepository.java, AccountTypeRepository.java
│   ├── entity/        Account.java, AccountType.java
│   └── dto/           CreateAccountRequest.java, AccountResponse.java, NetWorthResponse.java
├── category/
│   ├── controller/    CategoryController.java
│   ├── service/       CategoryService.java
│   ├── repository/    CategoryRepository.java, CategoryTypeRepository.java
│   ├── entity/        Category.java, CategoryType.java
│   └── dto/           CreateCategoryRequest.java, CategoryResponse.java
├── transaction/
│   ├── controller/    TransactionController.java, TransferController.java
│   ├── service/       TransactionService.java, TransferService.java, BalanceService.java
│   ├── repository/    TransactionRepository.java
│   ├── entity/        Transaction.java
│   └── dto/           CreateTransactionRequest.java, CreateTransferRequest.java, TransactionResponse.java, TransactionFilter.java
├── budget/
│   ├── controller/    BudgetController.java
│   ├── service/       BudgetService.java
│   ├── repository/    BudgetRepository.java
│   ├── entity/        Budget.java
│   └── dto/           CreateBudgetRequest.java, BudgetResponse.java, BudgetSummaryResponse.java
├── dashboard/
│   ├── controller/    DashboardController.java
│   ├── service/       DashboardService.java
│   └── dto/           DashboardSummaryResponse.java
└── common/
    ├── entity/        BaseAuditEntity.java
    ├── dto/           PageResponse.java, ErrorResponse.java, FieldError.java
    ├── exception/     GlobalExceptionHandler.java, ResourceNotFoundException.java,
    │                  DuplicateResourceException.java, BusinessRuleException.java,
    │                  InsufficientBalanceException.java, ForbiddenOperationException.java
    ├── security/      SecurityContextHelper.java (holds current user from ThreadLocal)
    └── validation/    CurrencyCodeValidator.java, HexColorValidator.java
```

**Rationale**: Each domain package is self-contained. You can understand Account without reading Transaction. Cross-domain dependencies are explicit imports.

### Consequences
- The placeholder class `com.shan.cyber.tech.personal.finance.tracker.ManageExpense` is deleted.
- `@SpringBootApplication` in `PersonalFinanceTracker.java` stays at `com.shan.cyber.tech` — Spring Boot component scan covers `com.shan.cyber.tech.financetracker` automatically.
- The `src/main/java/com/shan/cyber/tech/Main.java` stubs in `database/` and `acceptance/` modules are left alone (they are non-functional placeholders).

---

## ADR-002: API Versioning — URI Path Prefix v1

### Context
We need versioning from day one because a mobile/web client will be built against this API. Changing response shapes without a version contract breaks clients.

### Decision
All API endpoints are prefixed `/api/v1/`. No header-based versioning. When v2 endpoints are needed, add a `v2` prefix alongside `v1`; never mutate `v1` contracts.

**Implementation**: Annotate every controller with `@RequestMapping("/api/v1/...")`.

### Consequences
- Simple to implement, simple to understand, simple to route.
- v2 can coexist with v1 when the time comes.
- No Spring versioning framework needed; plain `@RequestMapping`.

---

## ADR-003: Authentication — Custom Session Filter (No Spring Security)

### Context
Spring Security's default machinery (UsernamePasswordAuthenticationFilter, HttpSession, CSRF) is designed for form-based and OAuth flows. Our model is a simple opaque session token stored in the database's `sessions` table. Wiring Spring Security around this requires more configuration than writing a clean custom filter.

### Decision
Implement a **custom `OncePerRequestFilter`** (`SessionAuthFilter`) that:
1. Reads `Authorization: Bearer {token}` from every request.
2. Queries `sessions` WHERE `token = :token AND expires_at > NOW()`.
3. On hit: sets a `UserContext` into a ThreadLocal via `SecurityContextHelper`.
4. On miss: writes HTTP 401 and short-circuits.

**Public endpoints** (no filter): `POST /api/v1/auth/register`, `POST /api/v1/auth/login`.

The filter is registered via `FilterRegistrationBean` in a `WebConfig` class. Spring Security is **not added** as a dependency.

### Consequences
- No Spring Security dependency — avoids CSRF config, auto-configuration conflicts, and the extensive setup for a non-standard auth model.
- The filter is simple, testable in isolation.
- Downside: no built-in protection for CORS, CSRF. CORS must be configured via `WebMvcConfigurer`. CSRF is irrelevant for a token-based API.
- Rate limiting for login (F-001) is implemented inside `AuthService` using an in-memory `ConcurrentHashMap<String, AttemptRecord>` keyed by IP. This is MVP-sufficient; replace with Redis-backed rate limiting in Phase 2.

---

## ADR-004: Error Handling — Global Exception Handler

### Context
Controllers must not contain try/catch blocks for business exceptions. A consistent error response format is required by the domain spec (Section 4.2).

### Decision
A single `@RestControllerAdvice` class `GlobalExceptionHandler` in `common/exception/` handles:

| Exception Class | HTTP Status | Error Code |
|---|---|---|
| `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | 422 | `VALIDATION_ERROR` |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `DuplicateResourceException` | 409 | `CONFLICT` |
| `BusinessRuleException` | 422 | Error code from exception |
| `InsufficientBalanceException` | 422 | `INSUFFICIENT_BALANCE` |
| `ForbiddenOperationException` | 403 | `FORBIDDEN` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

All handlers return the `ErrorResponse` shape defined in code-standards.md.

### Consequences
- Controllers are clean: call service, return result, no error handling code.
- Adding a new exception type = one handler in one class.
- Logs the 500 handler at ERROR level; all others at DEBUG or WARN.

---

## ADR-005: Pagination — Spring Data Pageable with Custom Wrapper

### Context
Domain spec (Section 4.3) defines a specific response envelope: `{ content, page, size, totalElements, totalPages }`. Spring Data's `Page<T>` contains all this but its default JSON serialization includes extra fields.

### Decision
Use `Pageable` from Spring Data as the service/repository parameter. Wrap results in a custom `PageResponse<T>` DTO that maps exactly to the specified envelope.

```java
// common/dto/PageResponse.java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
```

Default page=0, size=30, max size=100 enforced in controller via `@PageableDefault` and custom `PageableHandlerMethodArgumentResolverCustomizer`.

### Consequences
- Clean, predictable JSON shape.
- No accidental leakage of Spring internals into the API contract.

---

## ADR-006: DTO Mapping — Manual Mapping in Service Layer (No MapStruct)

### Context
MapStruct is excellent for large entities with many fields. It adds an annotation processor dependency and generated code that requires inspection when debugging. For this project's entity count (8 main entities), the mapping code is not voluminous enough to justify the added dependency chain.

### Decision
All entity-to-DTO and DTO-to-entity mapping is done **manually in the service layer** using constructor calls or static factory methods on DTOs. Java 17 records are used for all DTOs.

Pattern:
```java
// In AccountService.java
private AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getName(),
        account.getCurrentBalance(),
        account.getCurrency(),
        account.getAccountType().getCode(),
        ...
    );
}
```

### Consequences
- Zero additional dependencies.
- Full IDE navigation from DTO field to entity field.
- Mapping errors are compile-time (records), not runtime annotation processing surprises.
- Downside: more boilerplate per entity. Acceptable at this scale.

---

## ADR-007: Database Migration Strategy — Liquibase, application module only

### Context
There are currently **two sets** of Liquibase migration files: one under `application/src/main/resources/db.changelog/` and one under `database/src/main/resources/db/changelog/`. The `application.yaml` Liquibase config points to `classpath:changelog/db.changelog-master.yaml` — note the path discrepancy from the actual file location (`db.changelog/`). The `database` module has Spring Boot wired but has no datasource config — it is non-functional.

### Decision
1. **Single migration source of truth**: `application/src/main/resources/db.changelog/`. The `database/` module migrations are **deleted** (they duplicate `application/`).
2. **Fix `application.yaml`**: change `change-log` path to `classpath:db.changelog/db.changelog-master.yaml` to match actual file location.
3. **Fix schema name**: change `default-schema` from `personal_finance_tracker` to `finance_tracker` in `application.yaml`.
4. **All new changesets go into** `application/src/main/resources/db.changelog/changes/` following the `NNN_description.yml` naming scheme.
5. All changesets run inside the `finance_tracker` schema. The SET search_path is handled by the `defaultSchemaName` in application.yaml.

### Consequences
- One place to look for migrations: no confusion between `application/` and `database/` modules.
- The `database/` module is retained as a Gradle module (for future acceptance test database setup helpers) but its duplicate Liquibase files are removed.

---

## ADR-008: Testing Strategy — Three Tiers

### Context
The project has `application/` (main code), `acceptance/` (separate Gradle module, currently empty), and `database/` (non-functional). Testing must be practical for a small team.

### Decision

**Tier 1: Unit Tests** (in `application/src/test/java/`)
- Scope: service layer logic, balance calculation, validation rules
- Framework: JUnit 5 + Mockito
- Mock: all repositories, external services
- Naming: `{ClassName}Test.java` (e.g., `TransactionServiceTest.java`)
- Target: 80% line coverage on service layer

**Tier 2: Repository/Slice Tests** (in `application/src/test/java/`)
- Scope: `@DataJpaTest` for complex JPQL queries (budget spend calculation, transaction filters)
- Framework: JUnit 5 + Spring Data JPA test slice + H2 in-memory OR Testcontainers Postgres
- **Decision**: Use **Testcontainers with postgres:15.2** for repository tests to match production behaviour (H2 diverges on PostgreSQL-specific syntax like TIMESTAMPTZ, partial indexes)
- Naming: `{ClassName}RepositoryTest.java`

**Tier 3: Integration Tests** (in `acceptance/` module)
- Scope: full HTTP round-trips (controller → service → DB)
- Framework: JUnit 5 + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured + Testcontainers Postgres
- Naming: `{Feature}IntegrationTest.java` (e.g., `AuthIntegrationTest.java`)
- Run separately from unit tests; tagged with `@Tag("integration")`

**Test execution**:
- `./gradlew test` runs Unit + Repository tests only
- `./gradlew integrationTest` runs Tier 3 (requires Docker)

### Consequences
- Fast feedback loop: unit tests run in seconds.
- Repository tests catch SQL/JPQL issues early.
- Integration tests give confidence before deployment.
- Testcontainers is required for Tier 2 and 3 — Docker must be available in CI.

---

## ADR-009: Money — BigDecimal Everywhere, Jackson Serialization as String

### Context
Domain rule 3.1: all monetary values as NUMERIC(19,4). The draft OpenAPI spec uses `type: number` which JavaScript parses as a float — precision is lost for amounts with more than 15 significant digits. Even at 2dp, `0.1 + 0.2 != 0.3` in JavaScript.

### Decision
- **Java entity fields**: `BigDecimal` annotated `@Column(precision = 19, scale = 4)`
- **JSON serialization**: Configure Jackson to serialize `BigDecimal` as a **string** by adding `WRITE_BIGDECIMAL_AS_PLAIN` and using `@JsonSerialize(using = ToStringSerializer.class)` on all money DTO fields
- **API request bodies**: `BigDecimal` fields in request DTOs, validated with `@Positive` and `@DecimalMin("0.0001")`
- **Response format**: All money values in JSON are strings: `"amount": "1250.0000"`

### Consequences
- No floating-point precision loss in the JSON transport layer.
- Clients must parse money strings (not use JSON number directly). This is standard in fintech APIs (Stripe, PayPal do this).
- The OpenAPI spec must be updated: money fields become `type: string, format: decimal, example: "1250.0000"`.

---

## ADR-010: Balance Recalculation — Optimistic In-Memory Update (Not Full Ledger Replay)

### Context
Domain rule 3.2: `current_balance = initial_balance + SUM(signed amounts)`. Option A is to replay the full ledger on every transaction write. Option B is to maintain the materialized `current_balance` column and adjust it by the delta on each write.

### Decision
Use **Option B: delta adjustment** for MVP, as specified in domain brief section 3.1.

- On INSERT: `current_balance += signedDelta`
- On UPDATE (amount change): `current_balance -= oldSignedDelta; current_balance += newSignedDelta`
- On DELETE: `current_balance -= signedDelta`

A `BalanceService` class owns this logic. All mutations happen within the same `@Transactional` boundary as the transaction row write.

A `@Scheduled` reconciliation job (Phase 2) will periodically recompute balances from the ledger and flag discrepancies. In MVP, the domain brief explicitly states the ledger is the source of truth on inconsistency.

### Consequences
- O(1) balance update cost per transaction write.
- Risk: accumulated drift if bugs exist. Mitigated by ledger-based reconciliation in Phase 2.
- The `@Version` (optimistic locking) annotation is placed on `Account.java` to prevent concurrent balance update races.

---

## ADR-012: DDD Architecture — Pragmatic Domain-Driven Design in a Spring Boot Monolith

### Context
The PM requires DDD compliance. The original domain-first package structure (`auth/`, `account/`, etc.) gives domain isolation at the package level but has an anemic domain model: all business logic lives in service classes, entities are pure data bags, no Value Objects exist, and there are no domain events. See `ddd-architecture.md` for full assessment.

### Decision
Adopt **pragmatic DDD** within the existing monolith, not a full hexagonal or clean architecture rewrite. The following DDD patterns are applied:

1. **Bounded Contexts**: Identity (auth), Account Management, Categorization, Transaction Management, Budget Management, Reporting/Dashboard, Shared Kernel. Each context gets its own top-level package: `identity/`, `account/`, `category/`, `transaction/`, `budget/`, `reporting/`, `shared/`.

2. **DDD Layers per context**: Each bounded context follows `domain/`, `application/`, `infrastructure/` sub-packages. See `updated-package-structure.md` for the exact class tree.

3. **Rich Domain Model**: Business rules move from services into entity methods:
   - `Account.debit(Money)` enforces the SAVINGS/CASH cannot-go-negative invariant
   - `Account.credit(Money)` updates balance
   - `Budget.evaluateStatus(Money spent)` computes ON_TRACK/WARNING/OVER_BUDGET
   - `Transaction` validates date range invariants in its constructor

4. **Value Objects**: `Money` (amount + currency), `DateRange`, `AuditInfo`, typed ID wrappers (`UserId`, `AccountId`, etc.) introduced in `shared/domain/model/`. Money replaces bare `BigDecimal + String currency` pairs on all entities.

5. **Domain Events**: Published via Spring `ApplicationEventPublisher`. Events are plain Java records in `{context}/domain/event/`. For MVP, listeners use `@EventListener` (synchronous, same transaction). Phase 2 upgrades to `@TransactionalEventListener(phase = AFTER_COMMIT)` for notification/cache work.

6. **Application Layer as Use-Case Orchestrator**: Service methods are split into `CommandHandler` (write) and `QueryHandler` (read) classes — one class per use case. The old monolithic `AccountService.java` becomes e.g. `CreateAccountCommandHandler`, `ListAccountsQueryHandler`, etc.

7. **Repository Interfaces in Domain Layer**: Plain Java interfaces with no Spring annotations. Spring Data JPA implementations live in `infrastructure/persistence/` and are named `{Entity}RepositoryImpl`.

8. **Pragmatic simplification for MVP**: The full-stack dev may keep JPA annotations (`@Entity`, `@Column`, etc.) on the domain model classes for the initial pass. The correct separation (domain class + `JpaEntity` class + mapper) is deferred to Phase 2 but package locations must be established from the start.

### Consequences
- Domain logic is testable without Spring context — unit tests require no Mockito repository mocks for domain invariant tests.
- Cross-context coupling is explicit via domain events and typed service calls, not hidden imports.
- More classes for the same feature (CommandHandler + QueryHandler instead of one ServiceMethod) — acceptable at this scale for the clarity gained.
- `BalanceService` is renamed `BalanceDomainService` and moves from `transaction/service/` to `account/domain/service/` — it is an Account domain concern, not a Transaction concern.
- The `common/` package is replaced by `shared/`. No other context imports from `shared/domain/` except to use Value Objects. All contexts import from `shared/infrastructure/` for common web/persistence infrastructure.

---

## ADR-013: Multi-Platform API — Platform-Agnostic REST with Bearer Token Auth

### Context
The API must serve a React SPA web app now, and a React Native mobile app in Phase 2. A 3rd party API consumer is possible in Phase 3.

### Decision
The REST API is **platform-agnostic from day one**. No server-side branching based on client type. Key decisions:

1. **Auth remains Bearer token (opaque session token), NOT JWT**. Bearer header works identically for browsers, mobile OS HTTP clients, and 3rd party tools. Rationale: opaque tokens support immediate revocation (logout deletes the row), reveal no user data, and are simpler than JWT (no signing key management, no clock skew). See `multi-platform-api.md` Section 2.

2. **No HATEOAS**. The `_links` overhead is not justified — mobile and web apps both have hard-coded route knowledge. Response shape stays flat. See `multi-platform-api.md` Section 3.2.

3. **CORS configured for web SPA only**. CORS is browser-only. Mobile apps do not send CORS preflight. Config: `allowedOrigins` is environment-variable-driven (`app.cors.allowed-origins` in `application.yaml`). `allowCredentials(false)` because we use Authorization header, not cookies. See `multi-platform-api.md` Section 5.

4. **Phase 2 mobile extensions** (not in MVP):
   - `POST /api/v1/auth/refresh` — token refresh before expiry
   - `POST /api/v1/auth/devices` — FCM/APNs device token registration
   - `GET /api/v1/sync?since=...` — delta sync for offline-first
   - Idempotency-Key header on all POST endpoints
   - `ShallowEtagHeaderFilter` for cache validation on list endpoints
   - Per-platform rate limiting via Bucket4j + Redis

5. **OpenAPI (SpringDoc) disabled in production** (`springdoc.swagger-ui.enabled: false` and `springdoc.api-docs.enabled: false` in `application-prod.yaml`). The YAML spec is committed at `docs/openapi.yaml` and kept in sync manually.

6. **Breaking change policy**: New v2 prefix `/api/v2/` alongside existing v1. v1 maintained for minimum 6 months after v2 launch. `Deprecation` + `Sunset` headers added to deprecated v1 endpoints.

### Consequences
- No code changes needed to existing endpoint logic when mobile app is added — only auth/device registration and notification endpoints are new.
- CORS config must be updated in `application-prod.yaml` when the production web URL is known.
- SpringDoc `OpenApiConfig` bean added to `shared/infrastructure/config/`.
- Security headers (`X-Content-Type-Options`, `Cache-Control: no-store`) added as a response filter in `WebConfig`.

---

## ADR-014: Hexagonal Architecture (Ports & Adapters) — Strict Boundary Enforcement

**Status**: SUPERSEDES ADR-012. Authoritative from 2026-02-28 onward.
**Triggered by**: PM mandate for strict Ports & Adapters.
**Reference**: `hexagonal-architecture.md` (full spec), `updated-package-structure.md` (full class tree).

### Context

ADR-012 adopted "pragmatic DDD" which permitted JPA annotations (`@Entity`, `@Column`) directly on domain classes as an initial shortcut. This couples the domain model to a specific ORM, making the domain impossible to test without a JPA context and making the domain change for two unrelated reasons: (a) business rule changes and (b) schema changes. The PM subsequently mandated strict Ports & Adapters (Hexagonal Architecture) to enforce the domain-purity constraint.

### Decision

The application follows strict Hexagonal Architecture with three inviolable zones:

1. **Domain zone** — pure Java. Zero Spring, JPA, Jackson, Lombok, or Hibernate imports. The entire `{context}/domain/` subtree compiles with only the JDK on the classpath. This is enforced by code review AND ArchUnit tests.

2. **Adapter zone** — `{context}/adapter/inbound/web/` (REST) and `{context}/adapter/outbound/persistence/` (JPA) and `{context}/adapter/outbound/event/` (Spring events) and `{context}/adapter/outbound/crosscontext/` (ACL adapters). Spring and JPA annotations are expected here.

3. **Config zone** — `{context}/config/{Context}Config.java`. One `@Configuration` class per bounded context. Creates domain service beans via `@Bean` factory methods with constructor injection. Domain services are NOT annotated with `@Service` or `@Component`.

The port model:
- **Inbound ports** (`domain/port/inbound/`) — interfaces the domain EXPOSES. Naming: `{Action}{Entity}UseCase` (commands), `Get{Entity}Query` (reads).
- **Outbound ports** (`domain/port/outbound/`) — interfaces the domain NEEDS. Naming: `{Entity}PersistencePort`, `{Context}EventPublisherPort`.
- Ports are implemented by adapters, never by domain classes.

### Consequences

- Significantly more classes per feature (domain model + JPA entity + mapper + persistence adapter + event adapter + config), but each class has a single, clear responsibility.
- Domain services can be unit-tested with Mockito mocks of port interfaces — zero Spring context startup required.
- ArchUnit tests in CI enforce the boundary. A Spring import in a `domain/` class fails the build.
- The JPA entity (`AccountJpaEntity`) and domain model (`Account`) are separate classes, linked by a mapper (`AccountJpaMapper`). See ADR-015.
- Cross-context communication follows ADR-016 (no direct domain imports across contexts).

---

## ADR-015: Domain Model / JPA Entity Separation

**Status**: Active. Follows from ADR-014.

### Context

Allowing `@Entity` on a domain class introduces two problems: (a) JPA requires a no-arg constructor, which can bypass domain invariant enforcement since JPA can create instances in an invalid state; (b) the same class changes for both business rule changes and schema changes — separate axes of change that should be independently deployable.

### Decision

Every persistent domain aggregate has two separate Java classes:

1. **Domain class** (`{context}/domain/model/{Entity}.java`) — pure Java. Contains business methods, enforces invariants, carries typed IDs and Value Objects (Money, etc.). No JPA annotations. No Lombok annotations. Use explicit getters.

2. **JPA Entity** (`{context}/adapter/outbound/persistence/{Entity}JpaEntity.java`) — carries all JPA annotations (`@Entity`, `@Table`, `@Column`, `@Version`, `@ManyToOne`, `@PrePersist`, etc.). Extends `AuditableJpaEntity` (shared MappedSuperclass). Has a protected no-arg constructor for JPA. Uses primitive/String/BigDecimal fields (not Value Objects).

3. **Mapper** (`{context}/adapter/outbound/persistence/{Entity}JpaMapper.java`) — `@Component`. Two methods: `toDomain(JpaEntity): DomainClass` and `toJpaEntity(DomainClass): JpaEntity`. No framework for mapping — explicit constructor calls only.

### Consequences

- Schema changes affect only the JPA entity and mapper. Domain class is untouched.
- Domain invariants are safe: JPA cannot construct a domain object directly.
- The `@Version` field for optimistic locking is carried as a plain `Long version` field in the domain class (value-only, no JPA semantics), then mapped to `@Version Long version` in the JPA entity. The persistence adapter reads the version from the saved JPA entity and returns a refreshed domain object with the updated version.
- Lombok is permitted on JPA entities (for getters/setters) but forbidden on domain classes.
- No MapStruct — all mapping is manual (see ADR-006).

---

## ADR-016: Anti-Corruption Layer Between Bounded Contexts

**Status**: Active. Follows from ADR-014.

### Context

In a monolith, it is tempting for one bounded context to directly import and use domain classes from another context. This creates implicit coupling: a change in the `Account` domain class can break the `Transaction` context compile without any indication that a cross-context boundary was violated.

### Decision

No bounded context's `domain/` package may import any class from another bounded context's `domain/` package. The only permitted cross-context reference is a **primitive ID** (e.g., `Long` or a typed ID VO from `shared/domain/model/`).

Cross-context communication uses two patterns:

**Pattern A — Synchronous direct call via cross-context outbound port** (for operations that must be atomic with the caller's transaction):

- The consuming context defines an outbound port interface in its own `domain/port/outbound/` package.
- The providing context implements the adapter in its own `adapter/outbound/crosscontext/` package.
- The adapter calls the providing context's inbound port (use case interface), not its domain service directly.
- Wiring happens in the providing context's `Config` class.

Examples:
- `transaction/domain/port/outbound/AccountBalancePort` — implemented by `account/adapter/outbound/crosscontext/AccountBalanceAdapter`
- `transaction/domain/port/outbound/CategoryValidationPort` — implemented by `category/adapter/outbound/crosscontext/CategoryValidationAdapter`
- `budget/domain/port/outbound/CategoryQueryPort` — implemented by `category/adapter/outbound/crosscontext/CategoryQueryAdapter`

**Pattern B — Domain events via EventPublisherPort** (for eventually-consistent side effects):

- Domain service publishes a domain event via `{Context}EventPublisherPort.publish(DomainEvent)`.
- `SpringEventPublisherAdapter` (in `shared/adapter/outbound/event/`) delegates to Spring `ApplicationEventPublisher`.
- Any context can listen via `@EventListener` (MVP) or `@TransactionalEventListener(AFTER_COMMIT)` (Phase 2).
- In MVP, balance updates use Pattern A (synchronous) for atomicity. Pattern B is used only for non-critical side effects (cache invalidation, audit).

### ACL Summary

| Source Context | Target Context | Port (in source) | Adapter (in target) | Pattern |
|---|---|---|---|---|
| Transaction | Account | `AccountBalancePort` | `AccountBalanceAdapter` | A (sync) |
| Transaction | Category | `CategoryValidationPort` | `CategoryValidationAdapter` | A (sync) |
| Budget | Category | `CategoryQueryPort` | `CategoryQueryAdapter` | A (sync) |
| Any context | broadcast | `{Context}EventPublisherPort` | `SpringEventPublisherAdapter` | B (event) |

### Consequences

- Adding a new cross-context dependency requires explicitly defining a port interface and an adapter — the coupling is visible and reviewable.
- ArchUnit rule: no class in `..account..` may depend on a class in `..transaction.domain..` (and vice versa for all context pairs).
- The `shared/domain/model/` typed IDs (UserId, AccountId, etc.) are the only shared domain types. All other cross-context references use these IDs as opaque primitives.

---

## ADR-017: Port and Service Naming Conventions

**Status**: Active. Follows from ADR-014.

### Context

With inbound ports, outbound ports, domain services, JPA entities, mappers, and adapters all coexisting, consistent naming is critical for immediate comprehension of a class's role without opening it.

### Decision

The following naming conventions are mandatory across all bounded contexts:

| Role | Naming Pattern | Example |
|---|---|---|
| Inbound port (command) | `{Action}{Entity}UseCase` | `CreateAccountUseCase`, `DeactivateAccountUseCase` |
| Inbound port (query) | `Get{Entity}Query` | `GetAccountsQuery`, `GetCurrentUserQuery` |
| Domain command (input record) | `{Action}{Entity}Command` | `CreateAccountCommand`, `UpdateTransactionCommand` |
| Domain view (query output record) | `{Entity}View` | `AccountView`, `TransactionView` |
| Outbound persistence port | `{Entity}PersistencePort` | `AccountPersistencePort`, `TransactionPersistencePort` |
| Outbound event port | `{Context}EventPublisherPort` | `AccountEventPublisherPort`, `IdentityEventPublisherPort` |
| Cross-context outbound port | `{TargetCapability}Port` | `AccountBalancePort`, `CategoryValidationPort` |
| Domain service (commands) | `{Entity}CommandService` | `AccountCommandService`, `TransactionCommandService` |
| Domain service (queries) | `{Entity}QueryService` | `AccountQueryService`, `TransactionQueryService` |
| Domain aggregate | Plain noun | `Account`, `Transaction`, `Budget` |
| Domain event | Past-tense verb phrase, implements DomainEvent | `AccountCreated`, `TransactionDeleted` |
| Domain exception | `{Reason}Exception` extends DomainException | `InsufficientFundsException`, `DuplicateBudgetException` |
| REST controller | `{Entity}Controller` | `AccountController`, `AuthController` |
| Request DTO | `{Action}{Entity}RequestDto` | `CreateAccountRequestDto` |
| Response DTO | `{Entity}ResponseDto` | `AccountResponseDto`, `LoginResponseDto` |
| Web mapper | `{Entity}RequestMapper` | `AccountRequestMapper` |
| JPA entity | `{Entity}JpaEntity` | `AccountJpaEntity`, `UserJpaEntity` |
| JPA repository | `{Entity}JpaRepository` | `AccountJpaRepository` |
| Persistence adapter | `{Entity}PersistenceAdapter` | `AccountPersistenceAdapter` |
| JPA mapper | `{Entity}JpaMapper` | `AccountJpaMapper` |
| Event publisher adapter | `{Context}EventPublisherAdapter` | `AccountEventPublisherAdapter` |
| Cross-context adapter | `{PortName}Adapter` | `AccountBalanceAdapter`, `CategoryValidationAdapter` |
| Config class | `{Context}Config` | `AccountConfig`, `IdentityConfig` |

### Consequences

- Any developer can determine a class's architectural role from its name alone, without opening the file.
- ArchUnit rules can be written using name patterns to enforce location — e.g., classes ending in `UseCase` must reside in `domain.port.inbound`.

---

## ADR-018: Adapter Wiring Strategy — Config-Class-Only Bean Creation for Domain Services

**Status**: Active. Follows from ADR-014.

### Context

Spring's standard approach is to annotate service classes with `@Service` and repository implementations with `@Repository`, then use `@Autowired` (or constructor injection picked up by Spring) to wire them together. However, placing `@Service` on a domain service class would require importing `org.springframework.stereotype.Service` into the domain package — a direct violation of the domain-purity constraint in ADR-014.

### Decision

Domain services are **never** annotated with `@Service`, `@Component`, or any other Spring stereotype annotation. They are plain Java classes.

Each bounded context has exactly **one** `@Configuration` class at `{context}/config/{Context}Config.java`. This class uses `@Bean` factory methods to:

1. Instantiate the domain service with constructor injection.
2. Accept outbound adapters as parameters (Spring injects them because they are `@Component`-annotated adapters).
3. Return the domain service as a Spring-managed bean.

Example for the Account context:

```java
// account/config/AccountConfig.java
@Configuration
public class AccountConfig {

    @Bean
    public AccountCommandService accountCommandService(
            AccountPersistencePort accountPersistencePort,
            AccountTypePersistencePort accountTypePersistencePort,
            AccountEventPublisherPort accountEventPublisherPort) {
        return new AccountCommandService(
            accountPersistencePort, accountTypePersistencePort, accountEventPublisherPort);
    }

    @Bean
    public AccountQueryService accountQueryService(
            AccountPersistencePort accountPersistencePort) {
        return new AccountQueryService(accountPersistencePort);
    }
}
```

The `AccountPersistenceAdapter` is a `@Component` that Spring finds via component scan. It implements `AccountPersistencePort`. Spring injects it as the `AccountPersistencePort` parameter in the `@Bean` method above. The domain service never knows it is Spring-managed.

**Cross-context adapter wiring**: Cross-context adapters (e.g., `AccountBalanceAdapter`) are `@Component`-annotated and live in the providing context's adapter package. The consuming context's `Config` class receives the cross-context port implementation as a parameter because Spring resolves it by type from the providing context's `@Component`.

### Consequences

- Domain services have zero Spring imports. They are instantiated like ordinary Java objects in tests: `new AccountCommandService(mockPort, mockEventPublisher)`.
- All wiring is explicit and visible in the Config class — no hidden `@Autowired` magic inside domain classes.
- One Config class per context means there is exactly one place to look to understand how a context's services are assembled.
- The full-stack dev must never add `@Service`, `@Component`, or `@Autowired` to any class under `{context}/domain/`.

---

## ADR-011: Transfer Pair ID — Two-Phase Insert

### Context
`transfer_pair_id` is a self-referencing FK on the `transactions` table. Both legs reference each other. We cannot set the FK before both rows exist.

### Decision
Two-phase approach within one database transaction:
1. Insert TRANSFER_OUT row with `transfer_pair_id = NULL` → get generated ID (let's call it `outId`)
2. Insert TRANSFER_IN row with `transfer_pair_id = outId` → get generated ID (`inId`)
3. `UPDATE transactions SET transfer_pair_id = inId WHERE id = outId`
4. Commit

The FK is `DEFERRABLE INITIALLY DEFERRED` in the Liquibase changeset so the constraint is only checked at commit time. This avoids the need for a separate `transfer_groups` table.

### Consequences
- Clean two-row model that matches the domain exactly.
- Requires `DEFERRABLE` FK in Postgres (supported natively).
- Delete of either leg: service layer fetches the paired ID and deletes both in one transaction.
