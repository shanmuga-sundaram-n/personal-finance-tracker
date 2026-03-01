# Implementation Plan — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-03-01
**Status**: Authoritative — full-stack-dev implements from this document
**Architecture**: Hexagonal Architecture (Ports & Adapters) — see `hexagonal-architecture.md` and `updated-package-structure.md`

---

## Architectural Baseline

All feature work follows the hexagonal structure mandated by ADR-014 through ADR-018:

- `{context}/domain/` — pure Java, zero Spring/JPA/Jackson/Lombok imports
- `{context}/adapter/inbound/web/` — REST controllers, request/response DTOs, web mappers
- `{context}/adapter/outbound/persistence/` — JPA entities, Spring Data repositories, JPA mappers, persistence adapters
- `{context}/adapter/outbound/event/` — event publisher adapters
- `{context}/adapter/outbound/crosscontext/` — ACL adapters (providing context implements consuming context's port)
- `{context}/config/` — one `@Configuration` class per bounded context, creates domain service `@Bean`s

Domain services are plain Java; they are **never** annotated `@Service`. Spring wires them only through Config `@Bean` methods.

Root package for all feature code: `com.shan.cyber.tech.financetracker`

---

## Phase 0: Fix Existing Bugs

These bugs must be fixed before any new code is written. Fix them in order.

---

### BUG-001: Liquibase changelog path in application.yaml is wrong

**File**: `/Volumes/Learnings/urmail2ss-git/personal-finance-tracker/application/src/main/resources/application.yaml`

**Current value**:
```yaml
change-log: classpath:changelog/db.changelog-master.yaml
```

**Correct value**:
```yaml
change-log: classpath:db.changelog/db.changelog-master.yaml
```

**Reason**: The actual file is at `application/src/main/resources/db.changelog/db.changelog-master.yaml`.

---

### BUG-002: Liquibase default-schema name is wrong

**File**: `application/src/main/resources/application.yaml`

**Current value**:
```yaml
default-schema: personal_finance_tracker
```

**Correct value**:
```yaml
default-schema: finance_tracker
```

**Reason**: Migration `001_create_schema.yml` creates `finance_tracker`. These must match.

---

### BUG-003: users and sessions tables — INT primary key must be BIGINT

**File**: `application/src/main/resources/db.changelog/changes/002_create_user_and_session_tables.yml`

Replace ALL `INT` with `BIGINT` throughout (PKs, FK columns `user_id`, `created_by`, `updated_by`).

**Reason**: Domain spec requires BIGINT PKs. INT overflows at ~2 billion rows.

---

### BUG-004: users and sessions tables — TIMESTAMP must be TIMESTAMPTZ

**File**: `002_create_user_and_session_tables.yml`

Replace `created_at TIMESTAMP`, `updated_at TIMESTAMP`, `expires_at TIMESTAMP` with `TIMESTAMPTZ`.

**Reason**: `TIMESTAMP WITHOUT TIME ZONE` causes DST-related bugs.

---

### BUG-005: users table — missing required columns

Fixed via new changeset `003_alter_users_table.yml` (see Phase 2). Adds: `first_name`, `last_name`, `is_active`, `preferred_currency`.

---

### BUG-006: users table — email column too short

Fixed via `003_alter_users_table.yml`: `ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(254)`.

---

### BUG-007: Duplicate Liquibase migrations in database/ module

**Delete**:
- `database/src/main/resources/db/changelog/changes/001_create_schema.yml`
- `database/src/main/resources/db/changelog/changes/002_create_user_and_session_tables.yml`
- `database/src/main/resources/db/changelog/db.changelog-master.yaml`

---

### BUG-008: ManageExpense.java placeholder — delete it

**File**: `application/src/main/java/com/shan/cyber/tech/personal/finance/tracker/ManageExpense.java`

Delete the file.

---

### BUG-009: swagger-codegen-maven-plugin incorrectly added

**File**: `application/build.gradle.kts`

Remove: `implementation("io.swagger:swagger-codegen-maven-plugin:2.2.3")`

This is a Maven plugin, not a runtime library. Replaced by SpringDoc.

---

### BUG-010: Logging conflict — remove log4j2

**File**: `application/build.gradle.kts`

Remove: `implementation("org.springframework.boot:spring-boot-starter-log4j2")`

Keep Logback (default via spring-boot-starter-web transitive pull).

---

### BUG-011: PostgreSQL driver version is outdated

**File**: `application/build.gradle.kts`

Replace: `implementation("org.postgresql:postgresql:42.1.4")`
With: `runtimeOnly("org.postgresql:postgresql")`

Let Spring Boot BOM manage version (42.7.x).

---

## Phase 1: Foundation — Shared Infrastructure

Complete Phase 0 first. Then build the shared kernel and infrastructure that all contexts depend on.

All classes below live under `com.shan.cyber.tech.financetracker`.

---

### Task 1.1: Fix application.yaml

Replace the entire file with:

```yaml
spring:
  application:
    name: personal-finance-tracker
  datasource:
    url: jdbc:postgresql://localhost:49883/personal-finance-tracker
    username: pft-app-user
    password: pft-app-user-secret
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-timeout: 20000
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: finance_tracker
        format_sql: true
    show-sql: false
    open-in-view: false
  liquibase:
    change-log: classpath:db.changelog/db.changelog-master.yaml
    default-schema: finance_tracker

server:
  port: 8080
  error:
    include-message: never
    include-binding-errors: never

app:
  session:
    duration-days: 7
  auth:
    rate-limit:
      max-attempts: 5
      window-minutes: 5
  pagination:
    default-size: 30
    max-size: 100
  cors:
    allowed-origins:
      - "http://localhost:3000"
      - "http://localhost:5173"

logging:
  level:
    com.shan.cyber.tech.financetracker: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
```

---

### Task 1.2: Update application/build.gradle.kts

Replace with the exact content specified in `dependencies.md`. Also add:

```kotlin
testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
```

ArchUnit is required for the hexagonal boundary enforcement tests introduced in Phase 1.5.

---

### Task 1.3: Create Shared Kernel Domain Classes (pure Java — zero framework imports)

**Package**: `shared/domain/model/`

**`DomainException.java`** — `shared/domain/exception/DomainException.java`
```java
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
```

**`DomainEvent.java`** — `shared/domain/event/DomainEvent.java`
```java
public interface DomainEvent {}
```

**`EventPublisherPort.java`** — `shared/domain/port/outbound/EventPublisherPort.java`
```java
public interface EventPublisherPort {
    void publish(DomainEvent event);
}
```

**`Money.java`** — `shared/domain/model/Money.java`
```java
public final class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) { return new Money(amount, currency); }
    public static Money zero(String currency) { return new Money(BigDecimal.ZERO, currency); }

    public Money add(Money other) { assertSameCurrency(other); return new Money(this.amount.add(other.amount), this.currency); }
    public Money subtract(Money other) { assertSameCurrency(other); return new Money(this.amount.subtract(other.amount), this.currency); }
    public Money negate() { return new Money(this.amount.negate(), this.currency); }
    public boolean isNegative() { return this.amount.compareTo(BigDecimal.ZERO) < 0; }
    public boolean isPositive() { return this.amount.compareTo(BigDecimal.ZERO) > 0; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Cannot operate on different currencies: " + this.currency + " vs " + other.currency);
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Money m)) return false;
        return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
    }
    @Override public int hashCode() { return Objects.hash(amount.stripTrailingZeros(), currency); }
    @Override public String toString() { return amount.toPlainString() + " " + currency; }
}
```

**`DateRange.java`** — `shared/domain/model/DateRange.java`
```java
public record DateRange(LocalDate startDate, LocalDate endDate) {
    public DateRange {
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("endDate must not be before startDate");
    }
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }
    public boolean overlaps(DateRange other) {
        return !this.startDate.isAfter(other.endDate == null ? LocalDate.MAX : other.endDate)
            && !(endDate != null && endDate.isBefore(other.startDate));
    }
}
```

**`AuditInfo.java`** — `shared/domain/model/AuditInfo.java`
```java
public record AuditInfo(OffsetDateTime createdAt, OffsetDateTime updatedAt,
                         Long createdBy, Long updatedBy) {}
```

**Typed ID Value Objects** — one file each in `shared/domain/model/`:
```java
// Same compact record pattern for all five:
public record UserId(Long value) {
    public UserId { if (value == null || value <= 0) throw new IllegalArgumentException("UserId must be positive"); }
}
// AccountId.java, CategoryId.java, BudgetId.java, TransactionId.java — identical pattern
```

---

### Task 1.4: Create Shared Kernel Adapter Infrastructure

**`AuditableJpaEntity.java`** — `shared/adapter/outbound/persistence/AuditableJpaEntity.java`
```java
@MappedSuperclass
public abstract class AuditableJpaEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() { OffsetDateTime now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    // getters and setters
}
```

**`SpringEventPublisherAdapter.java`** — `shared/adapter/outbound/event/SpringEventPublisherAdapter.java`
```java
@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {
    private final ApplicationEventPublisher publisher;
    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    @Override public void publish(DomainEvent event) { publisher.publishEvent(event); }
}
```

**`SecurityContextHolder.java`** — `shared/adapter/inbound/web/SecurityContextHolder.java`
```java
public final class SecurityContextHolder {
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private SecurityContextHolder() {}
    public static void setCurrentUserId(Long userId) { CURRENT_USER_ID.set(userId); }
    public static Long getCurrentUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) throw new IllegalStateException("No authenticated user in current context");
        return userId;
    }
    public static void clear() { CURRENT_USER_ID.remove(); }
}
```

**`GlobalExceptionHandler.java`** — `shared/adapter/inbound/web/GlobalExceptionHandler.java`
- `@RestControllerAdvice`
- Handles: `DomainException` -> 422, `ResourceNotFoundException` (extends DomainException) -> 404, `DuplicateResourceException` -> 409, `InsufficientFundsException` -> 422, `ForbiddenOperationException` -> 403, `UnauthorizedException` -> 401, `MethodArgumentNotValidException` -> 422, `ConstraintViolationException` -> 422, `Exception` -> 500

**Shared DTOs** — `shared/adapter/inbound/web/dto/`:
- `ErrorResponseDto.java` — record: `int status, String error, String message, List<FieldErrorDto> errors, OffsetDateTime timestamp, String path`
- `FieldErrorDto.java` — record: `String field, String code, String message`
- `PageResponseDto.java` — record: `List<T> content, int page, int size, long totalElements, int totalPages` + static `from(Page<T>)` factory

---

### Task 1.5: Create Shared Config Classes

**`WebConfig.java`** — `shared/config/WebConfig.java`
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilter(SessionAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
            .allowedHeaders("Authorization","Content-Type","Accept","X-Requested-With")
            .exposedHeaders("Location")
            .allowCredentials(false)
            .maxAge(3600);
    }

    @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        resolver.setFallbackPageable(PageRequest.of(0, 30));
        resolvers.add(resolver);
    }
}
```

**`JacksonConfig.java`** — `shared/config/JacksonConfig.java`
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .modules(new JavaTimeModule())
            .serializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
```

**`OpenApiConfig.java`** — `shared/config/OpenApiConfig.java`
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Personal Finance Tracker API").version("v1"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                    .bearerFormat("UUID").description("Session token from POST /api/v1/auth/login")));
    }
}
```

---

## Phase 1.5: Hexagonal Scaffold — Empty Package Structure + ArchUnit

**Do this AFTER Phase 1, BEFORE Phase 2.**

Creates the complete hexagonal package skeleton for all 6 bounded contexts so Phase 3+ feature work is written in the correct structure from the first line. Also installs ArchUnit enforcement.

---

### Task 1.5.1: Create ArchUnit Test Skeleton

**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/ArchitectureTest.java`

```java
@AnalyzeClasses(packages = "com.shan.cyber.tech.financetracker")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_import_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..", "lombok..");

    @ArchTest
    static final ArchRule controllers_only_call_inbound_ports =
        noClasses()
            .that().resideInAPackage("..adapter.inbound.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain.service..");

    @ArchTest
    static final ArchRule account_domain_not_import_transaction_domain =
        noClasses().that().resideInAPackage("..account.domain..")
            .should().dependOnClassesThat().resideInAPackage("..transaction.domain..");

    @ArchTest
    static final ArchRule transaction_domain_not_import_account_domain =
        noClasses().that().resideInAPackage("..transaction.domain..")
            .should().dependOnClassesThat().resideInAPackage("..account.domain..");

    // Add similar rules for all other context-pair combinations
}
```

---

### Task 1.5.2: Create Bounded Context Package Skeletons

For each of the 6 bounded contexts, create `package-info.java` placeholder files to make the package directories visible in the repo and establish the hexagonal structure before feature code is written.

Create empty `package-info.java` at the root of each sub-package:

```
identity/domain/model/
identity/domain/port/inbound/
identity/domain/port/outbound/
identity/domain/service/
identity/domain/event/
identity/domain/exception/
identity/adapter/inbound/web/
identity/adapter/outbound/persistence/
identity/adapter/outbound/security/
identity/config/

account/domain/model/
account/domain/port/inbound/
account/domain/port/outbound/
account/domain/service/
account/domain/event/
account/domain/exception/
account/adapter/inbound/web/
account/adapter/outbound/persistence/
account/adapter/outbound/event/
account/adapter/outbound/crosscontext/
account/config/

category/domain/model/
category/domain/port/inbound/
category/domain/port/outbound/
category/domain/service/
category/domain/event/
category/domain/exception/
category/adapter/inbound/web/
category/adapter/outbound/persistence/
category/adapter/outbound/event/
category/adapter/outbound/crosscontext/
category/config/

transaction/domain/model/
transaction/domain/port/inbound/
transaction/domain/port/outbound/
transaction/domain/service/
transaction/domain/event/
transaction/domain/exception/
transaction/adapter/inbound/web/
transaction/adapter/outbound/persistence/
transaction/adapter/outbound/event/
transaction/config/

budget/domain/model/
budget/domain/port/inbound/
budget/domain/port/outbound/
budget/domain/service/
budget/domain/event/
budget/domain/exception/
budget/adapter/inbound/web/
budget/adapter/outbound/persistence/
budget/adapter/outbound/event/
budget/config/

reporting/domain/port/inbound/
reporting/domain/port/outbound/
reporting/domain/service/
reporting/adapter/inbound/web/
reporting/adapter/outbound/persistence/
reporting/config/
```

---

## Phase 2: Database Schema

Write Liquibase changesets in order. Never edit previously committed changesets. Each new file is added to `db.changelog-master.yaml`.

Note on JPA entities: All JPA entities (`*JpaEntity`) live in `{context}/adapter/outbound/persistence/`, not in the domain model. The schema below defines the database tables; the Java entities that map to them are created in Phase 3+.

---

### Changeset 003: Alter users table

**File**: `application/src/main/resources/db.changelog/changes/003_alter_users_table.yml`

```yaml
databaseChangeLog:
  - changeSet:
      id: alter_users_table_fix_types_and_add_columns
      author: tech-lead
      changes:
        - sql:
            sql: |
              ALTER TABLE finance_tracker.users ALTER COLUMN id TYPE BIGINT;
              ALTER TABLE finance_tracker.users ALTER COLUMN created_by TYPE BIGINT;
              ALTER TABLE finance_tracker.users ALTER COLUMN updated_by TYPE BIGINT;
              ALTER TABLE finance_tracker.users ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
              ALTER TABLE finance_tracker.users ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
              ALTER TABLE finance_tracker.users ALTER COLUMN email TYPE VARCHAR(254);
              ALTER TABLE finance_tracker.users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
              ALTER TABLE finance_tracker.users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
              ALTER TABLE finance_tracker.users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
              ALTER TABLE finance_tracker.users ADD COLUMN IF NOT EXISTS preferred_currency CHAR(3) NOT NULL DEFAULT 'USD';
              ALTER TABLE finance_tracker.users ADD CONSTRAINT chk_users_username_format
                CHECK (username ~ '^[a-z0-9_]{3,50}$');

  - changeSet:
      id: alter_sessions_table_fix_types
      author: tech-lead
      changes:
        - sql:
            sql: |
              ALTER TABLE finance_tracker.sessions ALTER COLUMN id TYPE BIGINT;
              ALTER TABLE finance_tracker.sessions ALTER COLUMN user_id TYPE BIGINT;
              ALTER TABLE finance_tracker.sessions ALTER COLUMN created_by TYPE BIGINT;
              ALTER TABLE finance_tracker.sessions ALTER COLUMN updated_by TYPE BIGINT;
              ALTER TABLE finance_tracker.sessions ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'UTC';
              ALTER TABLE finance_tracker.sessions ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
              ALTER TABLE finance_tracker.sessions ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
```

---

### Changeset 004: Reference tables (account_types, category_types)

**File**: `application/src/main/resources/db.changelog/changes/004_create_reference_tables.yml`

```yaml
databaseChangeLog:
  - changeSet:
      id: create_account_types_table
      author: tech-lead
      changes:
        - sql:
            sql: |
              CREATE TABLE finance_tracker.account_types (
                id SMALLINT PRIMARY KEY,
                code VARCHAR(30) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                allows_negative_balance BOOLEAN NOT NULL,
                is_liability BOOLEAN NOT NULL,
                description TEXT
              );
              INSERT INTO finance_tracker.account_types (id, code, name, allows_negative_balance, is_liability) VALUES
                (1, 'CHECKING',       'Checking Account', true,  false),
                (2, 'SAVINGS',        'Savings Account',  false, false),
                (3, 'CREDIT_CARD',    'Credit Card',      true,  true),
                (4, 'INVESTMENT',     'Investment',       false, false),
                (5, 'LOAN',           'Loan',             true,  true),
                (6, 'CASH',           'Cash',             false, false),
                (7, 'DIGITAL_WALLET', 'Digital Wallet',   true,  false);

  - changeSet:
      id: create_category_types_table
      author: tech-lead
      changes:
        - sql:
            sql: |
              CREATE TABLE finance_tracker.category_types (
                id SMALLINT PRIMARY KEY,
                code VARCHAR(20) UNIQUE NOT NULL,
                name VARCHAR(50) NOT NULL
              );
              INSERT INTO finance_tracker.category_types (id, code, name) VALUES
                (1, 'INCOME',   'Income'),
                (2, 'EXPENSE',  'Expense'),
                (3, 'TRANSFER', 'Transfer');
```

---

### Changeset 005: accounts table

**File**: `application/src/main/resources/db.changelog/changes/005_create_accounts_table.yml`

```sql
CREATE SEQUENCE finance_tracker.accounts_id_seq START 1 INCREMENT BY 1;
CREATE TABLE finance_tracker.accounts (
  id BIGINT DEFAULT nextval('finance_tracker.accounts_id_seq') PRIMARY KEY,
  user_id BIGINT NOT NULL,
  account_type_id SMALLINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  initial_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  current_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  institution_name VARCHAR(100),
  account_number_last4 CHAR(4),
  is_active BOOLEAN NOT NULL DEFAULT true,
  include_in_net_worth BOOLEAN NOT NULL DEFAULT true,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES finance_tracker.users(id),
  CONSTRAINT fk_accounts_account_type FOREIGN KEY (account_type_id) REFERENCES finance_tracker.account_types(id),
  CONSTRAINT chk_accounts_currency CHECK (currency ~ '^[A-Z]{3}$')
);
CREATE UNIQUE INDEX idx_accounts_user_name_ci
  ON finance_tracker.accounts(user_id, LOWER(name)) WHERE is_active = true;
```

---

### Changeset 006: categories table

**File**: `application/src/main/resources/db.changelog/changes/006_create_categories_table.yml`

```sql
CREATE SEQUENCE finance_tracker.categories_id_seq START 1 INCREMENT BY 1;
CREATE TABLE finance_tracker.categories (
  id BIGINT DEFAULT nextval('finance_tracker.categories_id_seq') PRIMARY KEY,
  user_id BIGINT,
  category_type_id SMALLINT NOT NULL,
  parent_category_id BIGINT,
  name VARCHAR(100) NOT NULL,
  icon VARCHAR(50),
  color CHAR(7),
  is_system BOOLEAN NOT NULL DEFAULT false,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES finance_tracker.users(id),
  CONSTRAINT fk_categories_type FOREIGN KEY (category_type_id) REFERENCES finance_tracker.category_types(id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES finance_tracker.categories(id),
  CONSTRAINT chk_categories_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);
CREATE UNIQUE INDEX idx_categories_unique_name
  ON finance_tracker.categories(user_id, parent_category_id, LOWER(name)) WHERE is_active = true;
```

---

### Changeset 007: Seed system categories

**File**: `application/src/main/resources/db.changelog/changes/007_seed_system_categories.yml`

Insert system categories: `user_id = NULL`, `is_system = true`. Insert parents first, then children with `parent_category_id`.

Top-level EXPENSE parents: Housing, Transportation, Food, Utilities, Healthcare, Entertainment, Shopping, Personal Care, Education, Travel, Pets, Subscriptions, Insurance, Taxes, Fees & Charges, Miscellaneous.
Top-level INCOME parents: Salary, Freelance / Contract, Investment Income, Rental Income, Gifts Received, Government Benefits, Other Income.
Top-level TRANSFER parents: Account Transfer.

Include child categories under each parent (reference by subquery on parent name or explicit pre-seeded IDs).

---

### Changeset 008: transactions table

**File**: `application/src/main/resources/db.changelog/changes/008_create_transactions_table.yml`

```sql
CREATE SEQUENCE finance_tracker.transactions_id_seq START 1 INCREMENT BY 1;
CREATE TABLE finance_tracker.transactions (
  id BIGINT DEFAULT nextval('finance_tracker.transactions_id_seq') PRIMARY KEY,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  amount NUMERIC(19,4) NOT NULL,
  transaction_type VARCHAR(20) NOT NULL,
  transaction_date DATE NOT NULL,
  description VARCHAR(500),
  merchant_name VARCHAR(200),
  reference_number VARCHAR(100),
  is_recurring BOOLEAN NOT NULL DEFAULT false,
  recurring_transaction_id BIGINT,
  transfer_pair_id BIGINT,
  is_reconciled BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES finance_tracker.users(id),
  CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES finance_tracker.accounts(id),
  CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES finance_tracker.categories(id),
  CONSTRAINT fk_transactions_transfer_pair FOREIGN KEY (transfer_pair_id)
    REFERENCES finance_tracker.transactions(id) DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT chk_transactions_amount CHECK (amount > 0),
  CONSTRAINT chk_transactions_type CHECK (transaction_type IN ('INCOME','EXPENSE','TRANSFER_IN','TRANSFER_OUT'))
);
CREATE INDEX idx_transactions_user_date ON finance_tracker.transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_account ON finance_tracker.transactions(account_id);
CREATE INDEX idx_transactions_category ON finance_tracker.transactions(category_id);
CREATE INDEX idx_transactions_user_cat_date ON finance_tracker.transactions(user_id, category_id, transaction_date);
```

---

### Changeset 009: recurring_transactions table

**File**: `application/src/main/resources/db.changelog/changes/009_create_recurring_transactions_table.yml`

```sql
CREATE SEQUENCE finance_tracker.recurring_transactions_id_seq START 1 INCREMENT BY 1;
CREATE TABLE finance_tracker.recurring_transactions (
  id BIGINT DEFAULT nextval('finance_tracker.recurring_transactions_id_seq') PRIMARY KEY,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  amount NUMERIC(19,4) NOT NULL,
  transaction_type VARCHAR(20) NOT NULL,
  frequency VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  next_due_date DATE NOT NULL,
  description VARCHAR(500),
  merchant_name VARCHAR(200),
  is_active BOOLEAN NOT NULL DEFAULT true,
  auto_post BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_recur_user FOREIGN KEY (user_id) REFERENCES finance_tracker.users(id),
  CONSTRAINT fk_recur_account FOREIGN KEY (account_id) REFERENCES finance_tracker.accounts(id),
  CONSTRAINT fk_recur_category FOREIGN KEY (category_id) REFERENCES finance_tracker.categories(id),
  CONSTRAINT chk_recur_amount CHECK (amount > 0),
  CONSTRAINT chk_recur_type CHECK (transaction_type IN ('INCOME','EXPENSE')),
  CONSTRAINT chk_recur_frequency CHECK (frequency IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','ANNUALLY'))
);
ALTER TABLE finance_tracker.transactions
  ADD CONSTRAINT fk_transactions_recurring
  FOREIGN KEY (recurring_transaction_id) REFERENCES finance_tracker.recurring_transactions(id);
```

---

### Changeset 010: budgets table

**File**: `application/src/main/resources/db.changelog/changes/010_create_budgets_table.yml`

```sql
CREATE SEQUENCE finance_tracker.budgets_id_seq START 1 INCREMENT BY 1;
CREATE TABLE finance_tracker.budgets (
  id BIGINT DEFAULT nextval('finance_tracker.budgets_id_seq') PRIMARY KEY,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  period_type VARCHAR(20) NOT NULL,
  amount NUMERIC(19,4) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  rollover_enabled BOOLEAN NOT NULL DEFAULT false,
  alert_threshold_pct SMALLINT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES finance_tracker.users(id),
  CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES finance_tracker.categories(id),
  CONSTRAINT chk_budgets_amount CHECK (amount > 0),
  CONSTRAINT chk_budgets_period CHECK (period_type IN ('WEEKLY','MONTHLY','QUARTERLY','ANNUALLY','CUSTOM')),
  CONSTRAINT chk_budgets_alert_pct CHECK (alert_threshold_pct IS NULL OR alert_threshold_pct BETWEEN 1 AND 100)
);
CREATE UNIQUE INDEX idx_budget_unique_active
  ON finance_tracker.budgets(user_id, category_id, period_type) WHERE is_active = true;
```

---

## Phase 3: F-001 — Identity (Authentication)

**Bounded context**: `identity/`

---

### Domain Layer (pure Java)

**Domain model** — `identity/domain/model/`:
- `User.java` — aggregate root. Fields: `UserId id`, `String username`, `String passwordHash`, `String email`, `String firstName`, `String lastName`, `boolean isActive`, `String preferredCurrency`, `AuditInfo auditInfo`. Methods: `activate()`, `deactivate()`, `updateProfile(firstName, lastName)`, `updatePreferredCurrency(String)`.
- `Session.java` — aggregate root. Fields: `Long id`, `UserId userId`, `String token`, `OffsetDateTime expiresAt`, `AuditInfo auditInfo`. Methods: `isExpired(OffsetDateTime now): boolean`, `isValid(OffsetDateTime now): boolean`.

**Inbound ports** — `identity/domain/port/inbound/`:
- `RegisterUserUseCase.java` — `UserId registerUser(RegisterUserCommand command)`
- `RegisterUserCommand.java` — record: `String username, String email, String rawPassword, String firstName, String lastName`
- `AuthenticateUserUseCase.java` — `LoginResult authenticate(AuthenticateUserCommand command)`
- `AuthenticateUserCommand.java` — record: `String username, String rawPassword, String clientIp`
- `LoginResult.java` — record: `String token, OffsetDateTime expiresAt`
- `LogoutUseCase.java` — `void logout(String token)`
- `GetCurrentUserQuery.java` — `UserProfile getCurrentUser(UserId userId)`
- `UserProfile.java` — record: `Long id, String username, String email, String firstName, String lastName, String preferredCurrency, OffsetDateTime createdAt`

**Outbound ports** — `identity/domain/port/outbound/`:
- `UserPersistencePort.java` — `findById, findByUsername, findByEmail, existsByUsername, existsByEmail, save`
- `SessionPersistencePort.java` — `findValidSession(token, now), save, deleteByToken, deleteExpiredBefore`
- `PasswordHasherPort.java` — `String hash(String rawPassword)`, `boolean matches(String rawPassword, String hashedPassword)`
- `LoginRateLimiterPort.java` — `isBlocked(clientIp), recordFailedAttempt(clientIp), resetAttempts(clientIp)`
- `IdentityEventPublisherPort.java` — extends `EventPublisherPort`

**Domain services** — `identity/domain/service/`:
- `IdentityCommandService.java` — implements `RegisterUserUseCase, AuthenticateUserUseCase, LogoutUseCase`. Constructor: `(UserPersistencePort, SessionPersistencePort, PasswordHasherPort, LoginRateLimiterPort, IdentityEventPublisherPort, int sessionDurationDays)`.
- `IdentityQueryService.java` — implements `GetCurrentUserQuery`. Constructor: `(UserPersistencePort)`.

**Domain events** — `identity/domain/event/`:
- `UserRegistered.java` — record implements DomainEvent: `UserId userId, String email`
- `UserDeactivated.java` — record implements DomainEvent: `UserId userId`

**Domain exceptions** — `identity/domain/exception/`:
- `UserNotFoundException.java` — extends DomainException, errorCode=`USER_NOT_FOUND`
- `DuplicateUsernameException.java` — errorCode=`DUPLICATE_USERNAME`
- `DuplicateEmailException.java` — errorCode=`DUPLICATE_EMAIL`
- `InvalidCredentialsException.java` — errorCode=`INVALID_CREDENTIALS`
- `RateLimitExceededException.java` — errorCode=`RATE_LIMIT_EXCEEDED`

---

### Adapter Layer

**Inbound — REST** — `identity/adapter/inbound/web/`:
- `AuthController.java` — `@RestController @RequestMapping("/api/v1/auth")`. Injects: `RegisterUserUseCase, AuthenticateUserUseCase, LogoutUseCase, GetCurrentUserQuery`.
  - `POST /register` -> 201 + `UserProfileResponseDto`
  - `POST /login` -> 200 + `LoginResponseDto`
  - `POST /logout` -> 204
  - `GET /me` -> 200 + `UserProfileResponseDto`
- `SessionAuthFilter.java` — extends `OncePerRequestFilter`. Reads `Authorization: Bearer {token}`. Calls `SessionPersistencePort.findValidSession`. Sets `SecurityContextHolder.setCurrentUserId`. Public paths: `/api/v1/auth/register`, `/api/v1/auth/login`.
- `RegisterRequestDto.java` — record: `@NotBlank @Size(min=3,max=50) @Pattern username, @Email @Size(max=254) email, @Size(min=8,max=72) password, @NotBlank firstName, @NotBlank lastName`
- `LoginRequestDto.java` — record: `@NotBlank username, @NotBlank password`
- `LoginResponseDto.java` — record: `String token, OffsetDateTime expiresAt`
- `UserProfileResponseDto.java` — record: `Long id, String username, String email, String firstName, String lastName, String preferredCurrency, OffsetDateTime createdAt`

**Outbound — Persistence** — `identity/adapter/outbound/persistence/`:
- `UserJpaEntity.java` — `@Entity @Table(schema="finance_tracker", name="users")`, extends `AuditableJpaEntity`. Fields use primitive Java types (Long, String, boolean).
- `SessionJpaEntity.java` — `@Entity @Table(schema="finance_tracker", name="sessions")`, extends `AuditableJpaEntity`.
- `UserJpaRepository.java` — extends `JpaRepository<UserJpaEntity, Long>`. Methods: `findByUsername, findByEmail, existsByUsername, existsByEmail`.
- `SessionJpaRepository.java` — extends `JpaRepository<SessionJpaEntity, Long>`. `@Query findValidSession(token, now)`, `deleteByToken`, `deleteByExpiresAtBefore`.
- `UserPersistenceAdapter.java` — `@Component implements UserPersistencePort`. Delegates to `UserJpaRepository + UserJpaMapper`.
- `SessionPersistenceAdapter.java` — `@Component implements SessionPersistencePort`. Delegates to `SessionJpaRepository + SessionJpaMapper`.
- `UserJpaMapper.java` — `@Component`. Maps `UserJpaEntity <-> User`.
- `SessionJpaMapper.java` — `@Component`. Maps `SessionJpaEntity <-> Session`.

**Outbound — Security** — `identity/adapter/outbound/security/`:
- `BcryptPasswordHasherAdapter.java` — `@Component implements PasswordHasherPort`. Uses `BCryptPasswordEncoder(strength=12)`.
- `InMemoryRateLimiterAdapter.java` — `@Component implements LoginRateLimiterPort`. Uses `ConcurrentHashMap<String, AttemptRecord>`. Sliding 5-minute window.

**Outbound — Event** — `identity/adapter/outbound/event/`:
- `IdentityEventPublisherAdapter.java` — `@Component implements IdentityEventPublisherPort`. Delegates to `SpringEventPublisherAdapter`.

---

### Config — `identity/config/IdentityConfig.java`

```java
@Configuration
public class IdentityConfig {
    @Value("${app.session.duration-days}") private int sessionDurationDays;

    @Bean
    public IdentityCommandService identityCommandService(
            UserPersistencePort userPersistencePort,
            SessionPersistencePort sessionPersistencePort,
            PasswordHasherPort passwordHasherPort,
            LoginRateLimiterPort loginRateLimiterPort,
            IdentityEventPublisherPort identityEventPublisherPort) {
        return new IdentityCommandService(userPersistencePort, sessionPersistencePort,
            passwordHasherPort, loginRateLimiterPort, identityEventPublisherPort, sessionDurationDays);
    }

    @Bean
    public IdentityQueryService identityQueryService(UserPersistencePort userPersistencePort) {
        return new IdentityQueryService(userPersistencePort);
    }
}
```

---

### Tests

- `IdentityCommandServiceTest.java` — unit test. No Spring context. Mocks: `UserPersistencePort, SessionPersistencePort, PasswordHasherPort, LoginRateLimiterPort, IdentityEventPublisherPort`. Tests: duplicate username, duplicate email, rate limit enforcement, invalid credentials, valid login creates session, logout deletes session.
- `UserPersistenceAdapterTest.java` — `@DataJpaTest` + Testcontainers. Tests: `findValidSession` with expired token returns empty, with valid token returns session.
- `AuthIntegrationTest.java` — `acceptance/` module. Full HTTP round-trip: register -> login -> GET /me -> logout.

---

## Phase 4: F-002 — Account Management

**Bounded context**: `account/`

---

### Domain Layer (pure Java)

**Domain model** — `account/domain/model/`:
- `Account.java` — aggregate root. Fields: `AccountId id`, `UserId ownerId`, `AccountType accountType`, `String name`, `Money currentBalance`, `Money initialBalance`, `String institutionName`, `String accountNumberLast4`, `boolean isActive`, `boolean includeInNetWorth`, `Long version`. Methods: `debit(Money amount)` throws `InsufficientFundsException` if type disallows negative and result < 0; `credit(Money amount)`; `rename(String)`; `deactivate()`; `canDebit(Money): boolean`; `isLiability(): boolean`.
- `AccountType.java` — value object. Fields: `Short id`, `String code`, `String name`, `boolean allowsNegativeBalance`, `boolean isLiability`.
- `NetWorth.java` — value object. Fields: `Money totalAssets`, `Money totalLiabilities`. Methods: `netWorth(): Money`.

**Inbound ports** — `account/domain/port/inbound/`:
- `CreateAccountUseCase.java`, `CreateAccountCommand.java` (record: `UserId ownerId, String name, String accountTypeCode, Money initialBalance, String institutionName, String accountNumberLast4`)
- `UpdateAccountUseCase.java`, `UpdateAccountCommand.java` (record: `AccountId, UserId ownerId, String name, String institutionName`)
- `DeactivateAccountUseCase.java` — `void deactivateAccount(AccountId, UserId requestingUser)`
- `ApplyBalanceDeltaUseCase.java` — `applyDebit(AccountId, UserId, Money)`, `applyCredit(AccountId, UserId, Money)`, `reverseDebit`, `reverseCredit`, `canDebit(AccountId, UserId, Money): boolean`
- `GetAccountsQuery.java` — `getAccountsByOwner(UserId)`, `getAccountById(AccountId, UserId)`, `getNetWorth(UserId)`
- `AccountView.java` — record (full view fields)
- `NetWorthView.java` — record: `Money totalAssets, Money totalLiabilities, Money netWorth`

**Outbound ports** — `account/domain/port/outbound/`:
- `AccountPersistencePort.java` — `findById(AccountId, UserId)`, `findActiveByOwner(UserId)`, `countActiveByOwner(UserId)`, `findByOwnerAndName(UserId, String)`, `save(Account): Account`
- `AccountTypePersistencePort.java` — `findByCode(String): Optional<AccountType>`, `findAll(): List<AccountType>`
- `AccountEventPublisherPort.java` — extends `EventPublisherPort`

**Domain services** — `account/domain/service/`:
- `AccountCommandService.java` — implements `CreateAccountUseCase, UpdateAccountUseCase, DeactivateAccountUseCase, ApplyBalanceDeltaUseCase`. Constructor: `(AccountPersistencePort, AccountTypePersistencePort, AccountEventPublisherPort)`. Rules enforced: max 20 active accounts, unique name (case-insensitive), valid account type code.
- `AccountQueryService.java` — implements `GetAccountsQuery`. Constructor: `(AccountPersistencePort)`.

**Domain events** — `account/domain/event/`:
- `AccountCreated.java` — record: `AccountId, UserId, String name`
- `AccountDeactivated.java` — record: `AccountId, UserId`
- `AccountDebited.java` — record: `AccountId, Money amount, Money newBalance`
- `AccountCredited.java` — record: `AccountId, Money amount, Money newBalance`

**Domain exceptions** — `account/domain/exception/`:
- `AccountNotFoundException.java` — errorCode=`ACCOUNT_NOT_FOUND`
- `InsufficientFundsException.java` — errorCode=`INSUFFICIENT_FUNDS`, fields: `AccountId, Money resultingBalance`
- `MaxAccountsExceededException.java` — errorCode=`MAX_ACCOUNTS_EXCEEDED`
- `DuplicateAccountNameException.java` — errorCode=`DUPLICATE_ACCOUNT_NAME`

---

### Adapter Layer

**Inbound — REST** — `account/adapter/inbound/web/`:
- `AccountController.java` — `@RestController @RequestMapping("/api/v1/accounts")`. Injects: `CreateAccountUseCase, UpdateAccountUseCase, DeactivateAccountUseCase, GetAccountsQuery`.
  - `GET /` -> 200 + `List<AccountResponseDto>`
  - `POST /` -> 201 + `AccountResponseDto` + `Location` header
  - `GET /{id}` -> 200 + `AccountResponseDto`
  - `PUT /{id}` -> 200 + `AccountResponseDto`
  - `DELETE /{id}` -> 204
  - `GET /net-worth` -> 200 + `NetWorthResponseDto`
- `CreateAccountRequestDto.java` — record with Bean Validation
- `UpdateAccountRequestDto.java` — record
- `AccountResponseDto.java` — record (all money fields use `@JsonSerialize(using=ToStringSerializer.class)`)
- `NetWorthResponseDto.java` — record
- `AccountRequestMapper.java` — static methods: `toCommand(CreateAccountRequestDto, UserId)`, `toResponseDto(AccountView)`

**Outbound — Persistence** — `account/adapter/outbound/persistence/`:
- `AccountJpaEntity.java` — `@Entity @Table(schema="finance_tracker", name="accounts")`, extends `AuditableJpaEntity`. `@Version Long version`. Fields: `Long id, Long userId, AccountTypeJpaEntity accountType (@ManyToOne lazy), String name, BigDecimal currentBalance, BigDecimal initialBalance, String currency, String institutionName, String accountNumberLast4, boolean isActive, boolean includeInNetWorth`. Protected no-arg constructor.
- `AccountTypeJpaEntity.java` — `@Entity @Table(name="account_types")`. No `@GeneratedValue`.
- `AccountJpaRepository.java` — extends `JpaRepository<AccountJpaEntity, Long>`. Methods: `findByUserIdAndIsActiveTrue`, `findByIdAndUserIdAndIsActiveTrue`, `countByUserIdAndIsActiveTrue`, `@Query findByUserIdAndNameIgnoreCase`.
- `AccountTypeJpaRepository.java` — extends `JpaRepository<AccountTypeJpaEntity, Short>`. `findByCode(String)`.
- `AccountPersistenceAdapter.java` — `@Component implements AccountPersistencePort`.
- `AccountTypePersistenceAdapter.java` — `@Component implements AccountTypePersistencePort`.
- `AccountJpaMapper.java` — `@Component`. `toDomain(AccountJpaEntity): Account`, `toJpaEntity(Account): AccountJpaEntity`.
- `AccountTypeJpaMapper.java` — maps `AccountTypeJpaEntity -> AccountType`.

**Outbound — Event** — `account/adapter/outbound/event/`:
- `AccountEventPublisherAdapter.java` — `@Component implements AccountEventPublisherPort`. Delegates to `SpringEventPublisherAdapter`.

**Outbound — Cross-context** — `account/adapter/outbound/crosscontext/`:
- `AccountBalanceAdapter.java` — `@Component implements transaction/domain/port/outbound/AccountBalancePort`. Calls `ApplyBalanceDeltaUseCase`. This adapter lives in the account package but implements a port defined in the transaction context.

---

### Config — `account/config/AccountConfig.java`

```java
@Configuration
public class AccountConfig {
    @Bean
    public AccountCommandService accountCommandService(
            AccountPersistencePort accountPersistencePort,
            AccountTypePersistencePort accountTypePersistencePort,
            AccountEventPublisherPort accountEventPublisherPort) {
        return new AccountCommandService(accountPersistencePort, accountTypePersistencePort, accountEventPublisherPort);
    }

    @Bean
    public AccountQueryService accountQueryService(AccountPersistencePort accountPersistencePort) {
        return new AccountQueryService(accountPersistencePort);
    }
}
```

---

### Tests

- `AccountCommandServiceTest.java` — unit. Mocks all ports. Tests: max 20 limit, duplicate name, valid type required, `Account.debit()` throws InsufficientFundsException for SAVINGS with negative result.
- `AccountPersistenceAdapterTest.java` — `@DataJpaTest` + Testcontainers.
- `AccountIntegrationTest.java` — `acceptance/` module. Create -> list -> get -> update -> delete -> verify inactive.

---

## Phase 5: F-003 — Category Management

**Bounded context**: `category/`

---

### Domain Layer (pure Java)

**Domain model** — `category/domain/model/`:
- `Category.java` — aggregate root. Fields: `CategoryId id`, `UserId ownerId (nullable — null = system)`, `CategoryType categoryType`, `CategoryId parentCategoryId (nullable)`, `String name`, `String icon`, `String color`, `boolean isSystem`, `boolean isActive`, `AuditInfo auditInfo`. Methods: `updateDetails(name, icon, color)`, `deactivate()`.
- `CategoryType.java` — value object. Fields: `Short id`, `String code (INCOME/EXPENSE/TRANSFER)`, `String name`.

**Inbound ports** — `category/domain/port/inbound/`:
- `CreateCategoryUseCase.java`, `CreateCategoryCommand.java` (record: `UserId ownerId, String name, Short categoryTypeId, CategoryId parentCategoryId, String icon, String color`)
- `UpdateCategoryUseCase.java`, `UpdateCategoryCommand.java` (record: `CategoryId, UserId ownerId, String name, String icon, String color`)
- `DeactivateCategoryUseCase.java` — `void deactivateCategory(CategoryId, UserId requestingUser)`
- `GetCategoriesQuery.java` — `getCategoriesVisibleToUser(UserId)`, `getCategoryById(CategoryId)`, `getCategoryAndDescendantIds(CategoryId): List<CategoryId>`
- `CategoryView.java` — record: `id, name, categoryTypeCode, categoryTypeName, parentCategoryId, icon, color, isSystem, isActive`

**Outbound ports** — `category/domain/port/outbound/`:
- `CategoryPersistencePort.java` — `findById, findVisibleToUser, findByOwnerAndParentAndName, findCategoryAndDescendantIds, hasTransactions, save`
- `CategoryEventPublisherPort.java` — extends `EventPublisherPort`

**Domain services** — `category/domain/service/`:
- `CategoryCommandService.java` — implements `CreateCategoryUseCase, UpdateCategoryUseCase, DeactivateCategoryUseCase`. Rules: max 2 hierarchy levels, child type must match parent, no TRANSFER type for user-created, no modify/delete on isSystem, hasTransactions check before deactivate.
- `CategoryQueryService.java` — implements `GetCategoriesQuery`.

**Domain events** — `category/domain/event/`:
- `CategoryDeactivated.java` — record: `CategoryId`

**Domain exceptions** — `category/domain/exception/`:
- `CategoryNotFoundException.java`, `SystemCategoryModificationException.java` (SYSTEM_CATEGORY_IMMUTABLE), `CategoryHasTransactionsException.java` (CATEGORY_HAS_TRANSACTIONS), `HierarchyDepthExceededException.java` (HIERARCHY_DEPTH_EXCEEDED), `CategoryTypeMismatchException.java` (CATEGORY_TYPE_MISMATCH)

---

### Adapter Layer

**Inbound — REST** — `category/adapter/inbound/web/`:
- `CategoryController.java` — `GET /, POST /, GET /{id}, PUT /{id}, DELETE /{id}`
- `CreateCategoryRequestDto.java`, `UpdateCategoryRequestDto.java`, `CategoryResponseDto.java`, `CategoryRequestMapper.java`

**Outbound — Persistence** — `category/adapter/outbound/persistence/`:
- `CategoryJpaEntity.java` — extends `AuditableJpaEntity`. `@ManyToOne categoryType (lazy)`. `@Column parentCategoryId (Long — no FK navigation, just the raw ID)`.
- `CategoryTypeJpaEntity.java` — no `@GeneratedValue`.
- `CategoryJpaRepository.java` — `@Query findAllVisibleToUser(Long userId)`, `@Query findByUserIdAndParentAndNameIgnoreCase`, `@Query findCategoryAndDescendantIds`, `@Query hasTransactions`.
- `CategoryPersistenceAdapter.java`, `CategoryJpaMapper.java`, `CategoryTypeJpaMapper.java`

**Outbound — Event** — `category/adapter/outbound/event/`:
- `CategoryEventPublisherAdapter.java` — `@Component implements CategoryEventPublisherPort`.

**Outbound — Cross-context** — `category/adapter/outbound/crosscontext/`:
- `CategoryValidationAdapter.java` — `@Component implements transaction/domain/port/outbound/CategoryValidationPort`. Calls `GetCategoriesQuery`.
- `CategoryQueryAdapter.java` — `@Component implements budget/domain/port/outbound/CategoryQueryPort`. Calls `GetCategoriesQuery`.

---

### Config — `category/config/CategoryConfig.java`

Creates `@Bean CategoryCommandService(CategoryPersistencePort, CategoryEventPublisherPort)` and `@Bean CategoryQueryService(CategoryPersistencePort)`.

---

### Tests

- `CategoryCommandServiceTest.java` — unit. Tests: hierarchy depth violation, type mismatch, system category immutability, hasTransactions blocking deactivate.
- `CategoryPersistenceAdapterTest.java` — `@DataJpaTest` + Testcontainers. Tests: `findAllVisibleToUser` includes system categories, `findCategoryAndDescendantIds` returns correct IDs.

---

## Phase 6: F-004 + F-005 — Transaction Management and Transfers

**Bounded context**: `transaction/`

---

### Domain Layer (pure Java)

**Domain model** — `transaction/domain/model/`:
- `Transaction.java` — aggregate root. Fields: `TransactionId id`, `UserId userId`, `AccountId accountId`, `CategoryId categoryId`, `Money amount`, `TransactionType transactionType`, `LocalDate transactionDate`, `String description`, `String merchantName`, `String referenceNumber`, `boolean isRecurring`, `Long recurringTransactionId`, `TransactionId transferPairId`, `boolean isReconciled`, `AuditInfo auditInfo`. Methods: `updateDetails`, `changeAccount`, `linkToPair`, `markReconciled`. Invariant: amount > 0.
- `RecurringTransaction.java` — aggregate root. Fields include `Frequency frequency`. Methods: `advanceNextDueDate()`, `deactivate()`, `isDue(LocalDate today)`.
- `TransactionType.java` — enum: `INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT`. Methods: `isDebit()`, `isCredit()`, `isTransfer()`.
- `Frequency.java` — enum: `DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY`. Method: `nextDate(LocalDate from): LocalDate`.

**Inbound ports** — `transaction/domain/port/inbound/`:
- `CreateTransactionUseCase.java`, `CreateTransactionCommand.java`
- `UpdateTransactionUseCase.java`, `UpdateTransactionCommand.java`
- `DeleteTransactionUseCase.java` — `void deleteTransaction(TransactionId, UserId)`
- `CreateTransferUseCase.java`, `CreateTransferCommand.java`, `TransferResult.java` (record: `TransactionId outId, TransactionId inId`)
- `UpdateTransferUseCase.java`, `UpdateTransferCommand.java`
- `DeleteTransferUseCase.java` — `void deleteTransfer(TransactionId eitherLeg, UserId)`
- `GetTransactionsQuery.java` — `getTransactions(UserId, TransactionFilter, Pageable)`, `getTransactionById(TransactionId, UserId)`
- `TransactionFilter.java` — record: `startDate, endDate, accountId, categoryId, transactionType, minAmount, maxAmount`
- `TransactionView.java` — record (full view including accountName, categoryName, categoryTypeCode)

**Outbound ports** — `transaction/domain/port/outbound/`:
- `TransactionPersistencePort.java` — `findByIdAndOwner, findPage, save, saveAndFlush, delete, updateTransferPairId`
- `RecurringTransactionPersistencePort.java` — `findById, findActiveByOwner, findDueBy, save`
- `TransactionEventPublisherPort.java` — extends `EventPublisherPort`
- `AccountBalancePort.java` — cross-context. `applyDebit(AccountId, UserId, Money)`, `applyCredit`, `reverseDebit`, `reverseCredit`, `canDebit(AccountId, UserId, Money): boolean`
- `CategoryValidationPort.java` — cross-context. `categoryExistsAndVisibleToUser(CategoryId, UserId): boolean`, `getCategoryTypeCode(CategoryId): String`

**Domain services** — `transaction/domain/service/`:
- `TransactionCommandService.java` — implements `CreateTransactionUseCase, UpdateTransactionUseCase, DeleteTransactionUseCase`. Constructor: `(TransactionPersistencePort, AccountBalancePort, CategoryValidationPort, TransactionEventPublisherPort)`. Rules: date range validation (max 10 years past, 30 days future), category type check, balance check, blocks delete of transfer legs.
- `TransferCommandService.java` — implements `CreateTransferUseCase, UpdateTransferUseCase, DeleteTransferUseCase`. Two-phase insert for transfer creation (saveAndFlush for TRANSFER_OUT, insert TRANSFER_IN with pairId, update TRANSFER_OUT pairId).
- `TransactionQueryService.java` — implements `GetTransactionsQuery`.

**Domain events** — `transaction/domain/event/`:
- `TransactionCreated.java`, `TransactionDeleted.java`, `TransactionAmountChanged.java`, `TransferCreated.java`, `RecurringTransactionDue.java`

**Domain exceptions** — `transaction/domain/exception/`:
- `TransactionNotFoundException.java`, `InvalidTransactionDateException.java` (INVALID_TRANSACTION_DATE), `TransferDeleteViaTransactionException.java` (USE_TRANSFER_DELETE), `SameAccountTransferException.java` (SAME_ACCOUNT_TRANSFER)

---

### Adapter Layer

**Inbound — REST** — `transaction/adapter/inbound/web/`:
- `TransactionController.java` — `GET /, POST /, GET /{id}, PUT /{id}, DELETE /{id}` at `/api/v1/transactions`
- `TransferController.java` — `POST /, GET /{id}, PUT /{id}, DELETE /{id}` at `/api/v1/transfers`
- `CreateTransactionRequestDto.java`, `UpdateTransactionRequestDto.java`, `CreateTransferRequestDto.java`, `UpdateTransferRequestDto.java`
- `TransactionResponseDto.java`, `TransferResponseDto.java`
- `TransactionRequestMapper.java`

**Outbound — Persistence** — `transaction/adapter/outbound/persistence/`:
- `TransactionJpaEntity.java` — extends `AuditableJpaEntity`. Stores `transactionType` as `VARCHAR`.
- `RecurringTransactionJpaEntity.java` — extends `AuditableJpaEntity`.
- `TransactionJpaRepository.java` — extends `JpaRepository + JpaSpecificationExecutor<TransactionJpaEntity>`.
- `RecurringTransactionJpaRepository.java`
- `TransactionPersistenceAdapter.java`, `RecurringTransactionPersistenceAdapter.java`
- `TransactionJpaMapper.java`, `RecurringTransactionJpaMapper.java`
- `TransactionSpecification.java` — `Specification<TransactionJpaEntity>` for filter predicates.

**Outbound — Event** — `transaction/adapter/outbound/event/`:
- `TransactionEventPublisherAdapter.java` — `@Component implements TransactionEventPublisherPort`.

---

### Config — `transaction/config/TransactionConfig.java`

```java
@Configuration
public class TransactionConfig {
    @Bean
    public TransactionCommandService transactionCommandService(
            TransactionPersistencePort transactionPersistencePort,
            AccountBalancePort accountBalancePort,
            CategoryValidationPort categoryValidationPort,
            TransactionEventPublisherPort eventPublisher) {
        return new TransactionCommandService(transactionPersistencePort, accountBalancePort,
            categoryValidationPort, eventPublisher);
    }

    @Bean
    public TransferCommandService transferCommandService(
            TransactionPersistencePort transactionPersistencePort,
            AccountBalancePort accountBalancePort,
            TransactionEventPublisherPort eventPublisher) {
        return new TransferCommandService(transactionPersistencePort, accountBalancePort, eventPublisher);
    }

    @Bean
    public TransactionQueryService transactionQueryService(TransactionPersistencePort transactionPersistencePort) {
        return new TransactionQueryService(transactionPersistencePort);
    }
}
```

Note: `AccountBalancePort` is satisfied by `AccountBalanceAdapter` (`@Component` in `account/adapter/outbound/crosscontext/`). Spring injects it by type.

---

### Tests

- `TransactionCommandServiceTest.java` — unit. Tests: date range validation, category type check, balance check via `AccountBalancePort`, transfer leg delete blocked.
- `TransferCommandServiceTest.java` — unit. Tests: same-account transfer rejected, two-phase insert sequence, both balance deltas applied.
- `TransactionPersistenceAdapterTest.java` — `@DataJpaTest` + Testcontainers. Tests filter combinations via `TransactionSpecification`.
- `TransactionIntegrationTest.java` — full HTTP round-trip, including transfer creation and deletion.

---

## Phase 7: F-006 — Reporting / Dashboard

**Bounded context**: `reporting/` — read-only CQRS. No domain service command methods. No write ports.

---

### Domain Layer (pure Java)

**Inbound ports** — `reporting/domain/port/inbound/`:
- `GetDashboardQuery.java` — `DashboardView getDashboard(UserId userId)`
- `DashboardView.java` — record: `NetWorthView netWorth, Money currentMonthIncome, Money currentMonthExpense, Money netCashFlow, List<AccountBalanceView> accountBalances, List<TopCategoryView> topExpenseCategories`
- `NetWorthView.java`, `MonthlyFlowView.java`, `AccountBalanceView.java`, `TopCategoryView.java` — records

**Outbound ports** — `reporting/domain/port/outbound/`:
- `DashboardReadPort.java` — `getNetWorth(UserId)`, `getMonthlyFlow(UserId, YearMonth)`, `getTopExpenseCategories(UserId, YearMonth, int limit)`, `getAccountsWithBalances(UserId)`

**Domain service** — `reporting/domain/service/`:
- `DashboardQueryService.java` — implements `GetDashboardQuery`. Constructor: `(DashboardReadPort)`. Assembles `DashboardView` from read port results.

---

### Adapter Layer

**Inbound — REST** — `reporting/adapter/inbound/web/`:
- `DashboardController.java` — `GET /api/v1/dashboard/summary -> 200 + DashboardResponseDto`. Injects `GetDashboardQuery`.
- `DashboardResponseDto.java` — record (full dashboard shape with nested DTOs)

**Outbound — Persistence** — `reporting/adapter/outbound/persistence/`:
- `DashboardReadAdapter.java` — `@Component implements DashboardReadPort`. Uses `@PersistenceContext EntityManager` and native SQL. All queries scoped to `userId`. NOT a `JpaRepository`.

---

### Config — `reporting/config/ReportingConfig.java`

Creates `@Bean DashboardQueryService(DashboardReadPort)`.

---

### Tests

- `DashboardQueryServiceTest.java` — unit. Mocks `DashboardReadPort`. Tests assembly of `DashboardView`.
- `DashboardReadAdapterTest.java` — `@DataJpaTest` + Testcontainers. Tests each native SQL query in isolation.

---

## Phase 8: F-007 — Budget Management

**Bounded context**: `budget/`

---

### Domain Layer (pure Java)

**Domain model** — `budget/domain/model/`:
- `Budget.java` — aggregate root. Fields: `BudgetId id`, `UserId userId`, `CategoryId categoryId`, `BudgetPeriodType periodType`, `Money limit`, `LocalDate startDate`, `LocalDate endDate`, `boolean rolloverEnabled`, `Integer alertThresholdPct`, `boolean isActive`, `AuditInfo auditInfo`. Methods: `evaluateStatus(Money spent): BudgetStatus`, `isThresholdBreached(Money spent): boolean`, `computePeriodRange(LocalDate today): DateRange`, `updateLimit(Money)`, `updateAlertThreshold(Integer)`, `deactivate()`.
- `BudgetPeriodType.java` — enum: `WEEKLY, MONTHLY, QUARTERLY, ANNUALLY, CUSTOM`. Method: `computeRange(LocalDate start, LocalDate today): DateRange`.
- `BudgetStatus.java` — enum: `ON_TRACK (<75%), WARNING (75-99%), OVER_BUDGET (>=100%)`. Static: `from(BigDecimal percentageUsed): BudgetStatus`.

**Inbound ports** — `budget/domain/port/inbound/`:
- `CreateBudgetUseCase.java`, `CreateBudgetCommand.java` (record: `UserId, CategoryId, BudgetPeriodType, Money limit, LocalDate startDate, LocalDate endDate, boolean rolloverEnabled, Integer alertThresholdPct`)
- `UpdateBudgetUseCase.java`, `UpdateBudgetCommand.java` (record: `BudgetId, UserId ownerId, Money newLimit, Integer newAlertThresholdPct`)
- `DeactivateBudgetUseCase.java`
- `GetBudgetsQuery.java` — `getActiveBudgets(UserId)`, `getBudgetSummary(BudgetId, UserId)`
- `BudgetView.java`, `BudgetSummaryView.java` (record: `BudgetView budget, Money spent, Money remaining, BigDecimal percentageUsed, BudgetStatus status, LocalDate periodStart, LocalDate periodEnd`)

**Outbound ports** — `budget/domain/port/outbound/`:
- `BudgetPersistencePort.java` — `findById(BudgetId, UserId)`, `findActiveByOwner(UserId)`, `findActiveByOwnerAndCategoryAndPeriod(UserId, CategoryId, BudgetPeriodType)`, `save(Budget): Budget`
- `BudgetSpendCalculationPort.java` — `calculateSpend(UserId, List<CategoryId>, DateRange): Money`
- `BudgetEventPublisherPort.java` — extends `EventPublisherPort`
- `CategoryQueryPort.java` — cross-context. `getCategoryTypeCode(CategoryId): Optional<String>`, `getCategoryAndDescendantIds(CategoryId): List<CategoryId>`

**Domain services** — `budget/domain/service/`:
- `BudgetCommandService.java` — implements `CreateBudgetUseCase, UpdateBudgetUseCase, DeactivateBudgetUseCase`. Rules: no TRANSFER category, CUSTOM period requires endDate, no duplicate active budget per category+period.
- `BudgetQueryService.java` — implements `GetBudgetsQuery`. `getBudgetSummary` computes period range, queries spend via `BudgetSpendCalculationPort`, calls `Budget.evaluateStatus()`, publishes `BudgetThresholdExceeded` if `isThresholdBreached`.

**Domain events** — `budget/domain/event/`:
- `BudgetThresholdExceeded.java` — record: `BudgetId, UserId, BudgetStatus, Money spent, Money limit`

**Domain exceptions** — `budget/domain/exception/`:
- `BudgetNotFoundException.java`, `DuplicateBudgetException.java` (DUPLICATE_BUDGET), `InvalidBudgetCategoryException.java` (TRANSFER_CATEGORY_BUDGET)

---

### Adapter Layer

**Inbound — REST** — `budget/adapter/inbound/web/`:
- `BudgetController.java` — `GET /, POST /, GET /{id}, PUT /{id}, DELETE /{id}, GET /{id}/summary` at `/api/v1/budgets`
- `CreateBudgetRequestDto.java`, `UpdateBudgetRequestDto.java`, `BudgetResponseDto.java`, `BudgetSummaryResponseDto.java`, `BudgetRequestMapper.java`

**Outbound — Persistence** — `budget/adapter/outbound/persistence/`:
- `BudgetJpaEntity.java` — extends `AuditableJpaEntity`. Stores `periodType` as `VARCHAR`, `amount` as `BigDecimal`.
- `BudgetJpaRepository.java` — `findByUserIdAndIsActiveTrue`, `findByUserIdAndCategoryIdAndPeriodTypeAndIsActiveTrue`.
- `BudgetPersistenceAdapter.java`, `BudgetJpaMapper.java`
- `BudgetSpendCalculationAdapter.java` — `@Component implements BudgetSpendCalculationPort`. Uses `@PersistenceContext EntityManager`, native SQL: `SELECT SUM(t.amount) FROM transactions t WHERE t.user_id=:userId AND t.category_id IN :categoryIds AND t.transaction_type='EXPENSE' AND t.transaction_date BETWEEN :start AND :end`.

**Outbound — Event** — `budget/adapter/outbound/event/`:
- `BudgetEventPublisherAdapter.java` — `@Component implements BudgetEventPublisherPort`.

---

### Config — `budget/config/BudgetConfig.java`

```java
@Configuration
public class BudgetConfig {
    @Bean
    public BudgetCommandService budgetCommandService(
            BudgetPersistencePort budgetPersistencePort,
            CategoryQueryPort categoryQueryPort,
            BudgetEventPublisherPort budgetEventPublisherPort) {
        return new BudgetCommandService(budgetPersistencePort, categoryQueryPort, budgetEventPublisherPort);
    }

    @Bean
    public BudgetQueryService budgetQueryService(
            BudgetPersistencePort budgetPersistencePort,
            BudgetSpendCalculationPort budgetSpendCalculationPort,
            CategoryQueryPort categoryQueryPort,
            BudgetEventPublisherPort budgetEventPublisherPort) {
        return new BudgetQueryService(budgetPersistencePort, budgetSpendCalculationPort,
            categoryQueryPort, budgetEventPublisherPort);
    }
}
```

Note: `CategoryQueryPort` is satisfied by `CategoryQueryAdapter` (`@Component` in `category/adapter/outbound/crosscontext/`).

---

### Tests

- `BudgetCommandServiceTest.java` — unit. Tests: TRANSFER category rejected, CUSTOM without endDate rejected, duplicate active budget rejected.
- `BudgetQueryServiceTest.java` — unit. Tests: `getBudgetSummary` with mocked `BudgetSpendCalculationPort`, correct `BudgetStatus` computation, `BudgetThresholdExceeded` published when breached.
- `BudgetSpendCalculationAdapterTest.java` — `@DataJpaTest` + Testcontainers. Tests actual SQL aggregation with seed transactions.

---

## Phase 9: Quality and Hardening

- Verify all ArchUnit rules pass: `./gradlew test` must include `ArchitectureTest` run.
- Write any missing unit tests to reach 80% service-layer coverage.
- Write `@DataJpaTest` repository tests for: `SessionJpaRepository.findValidSession` (expired/valid), `CategoryJpaRepository.findAllVisibleToUser` (system + own), `CategoryJpaRepository.findCategoryAndDescendantIds`.
- Write full integration tests in `acceptance/` module: `AuthIntegrationTest`, `AccountIntegrationTest`, `TransactionIntegrationTest`, `BudgetIntegrationTest`.
- Confirm `springdoc.swagger-ui.enabled: false` and `springdoc.api-docs.enabled: false` in `application-prod.yaml`.
- Add `Deprecation` and `Sunset` response headers to any endpoints marked for future removal.
