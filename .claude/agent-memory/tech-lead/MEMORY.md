# Tech Lead Memory — Personal Finance Tracker
**Last Updated**: 2026-03-01
**Model**: claude-sonnet-4-6

This file is loaded into every session. Keep it under 200 lines. Link to topic files for details.

---

## Project Snapshot

| Attribute | Value |
|---|---|
| Runtime | Java 17, Spring Boot 3.2.2 |
| DB | PostgreSQL 15.2, schema `finance_tracker`, DB `personal-finance-tracker` |
| Local port | 49883 -> 5432 (Docker Compose) |
| DB user | `pft-app-user` / `pft-app-user-secret` |
| Build | Gradle multi-module: `application` (runnable), `acceptance` (tests), `database` (placeholder) |
| Root package | `com.shan.cyber.tech` (main class) / `com.shan.cyber.tech.financetracker` (feature code) |

---

## Key Architecture Decisions (see `architecture-decisions.md` for full ADRs)

- **Architecture**: Hexagonal Architecture / Ports & Adapters (ADR-014). Supersedes pragmatic DDD (ADR-012).
  - Full spec: `hexagonal-architecture.md` (authoritative)
  - Full class tree: `updated-package-structure.md` (authoritative)
- **Package structure**: Strict hexagonal per bounded context (see `updated-package-structure.md`)
  - Bounded contexts: `identity/`, `account/`, `category/`, `transaction/`, `budget/`, `reporting/`, `shared/`
  - Each context: `domain/` + `adapter/inbound/web/` + `adapter/outbound/persistence/` + `adapter/outbound/event/` + `adapter/outbound/crosscontext/` + `config/`
- **Domain purity**: `domain/` subtrees have ZERO Spring, JPA, Jackson, Lombok imports. Enforced by ArchUnit.
- **Domain/JPA separation**: Separate domain class (pure Java) from JPA entity (`*JpaEntity`). Mapper (`*JpaMapper`) converts between them. No `@Entity` on domain classes ever. (ADR-015)
- **No @Service on domain**: Domain services are plain Java. Created as `@Bean` in `{Context}Config.java` only. (ADR-018)
- **Cross-context ACL**: No context imports another's domain classes. Cross-context via: (a) outbound port + adapter (synchronous), (b) domain events (async). See ADR-016.
- **Port naming**: Inbound: `{Action}{Entity}UseCase` / `Get{Entity}Query`. Outbound: `{Entity}PersistencePort`, `{Context}EventPublisherPort`. Services: `{Entity}CommandService`, `{Entity}QueryService`. (ADR-017)
- **Auth**: Custom `OncePerRequestFilter` (SessionAuthFilter) in `identity/adapter/inbound/web/` — NO Spring Security starter
- **Auth token**: Opaque UUID session token (NOT JWT) — supports web and mobile identically via Bearer header
- **Password hashing**: `spring-security-crypto` BCryptPasswordEncoder(strength=12)
- **Error handling**: `@RestControllerAdvice` GlobalExceptionHandler in `shared/adapter/inbound/web/`
- **Pagination**: `PageResponseDto<T>` record in `shared/adapter/inbound/web/dto/`
- **DTO mapping**: Manual — no MapStruct. Mappers in `adapter/outbound/persistence/` and `adapter/inbound/web/`
- **Migrations**: Single source in `application/src/main/resources/db.changelog/` only
- **Money**: `Money` Value Object in `shared/domain/model/Money.java` (amount + currency); JSON as string
- **Balance**: `Account.debit(Money)` / `Account.credit(Money)` enforce invariants; optimistic locking (`@Version`) on `AccountJpaEntity` — version carried as plain `Long` in domain `Account`
- **Transfer pair**: Two-phase insert with `DEFERRABLE INITIALLY DEFERRED` FK (ADR-011)
- **Domain Events**: Spring `ApplicationEventPublisher` via `SpringEventPublisherAdapter`; `@EventListener` in MVP; upgrade to `@TransactionalEventListener(AFTER_COMMIT)` in Phase 2
- **Multi-platform API**: Platform-agnostic REST. CORS for web only. See `multi-platform-api.md`
- **CORS**: Environment-configurable via `app.cors.allowed-origins` in application.yaml
- **Testing**: Unit (Mockito, zero Spring context for domain services) + Repository (@DataJpaTest + Testcontainers) + Integration (RestAssured + Testcontainers) + ArchUnit boundary enforcement

---

## Known Bugs to Fix Before Any Feature Work (see `implementation-plan.md` Phase 0)

1. **BUG-001**: `application.yaml` changelog path wrong (`changelog/` vs `db.changelog/`)
2. **BUG-002**: `application.yaml` default-schema `personal_finance_tracker` must be `finance_tracker`
3. **BUG-003**: `users` and `sessions` tables use `INT` PK — must be `BIGINT`
4. **BUG-004**: Timestamps use `TIMESTAMP` — must be `TIMESTAMPTZ`
5. **BUG-005**: `users` table missing `first_name`, `last_name`, `is_active`, `preferred_currency`
6. **BUG-006**: `users.email` is `VARCHAR(100)` — must be `VARCHAR(254)`
7. **BUG-007**: Duplicate Liquibase files in `database/` module — delete them
8. **BUG-008**: Delete `ManageExpense.java` placeholder
9. **BUG-009**: Remove `swagger-codegen-maven-plugin` from `implementation` scope
10. **BUG-010**: Remove `spring-boot-starter-log4j2` (conflicts with Logback)
11. **BUG-011**: Remove explicit `postgresql:42.1.4` version pin (2017 CVE-laden version)

---

## Migration Sequence (see `implementation-plan.md` Phase 2)

```
001_create_schema.yml                       (EXISTS)
002_create_user_and_session_tables.yml      (EXISTS — has bugs, fixed by 003)
003_alter_users_table.yml                   (NEW)
004_create_reference_tables.yml             (NEW)
005_create_accounts_table.yml               (NEW — includes `version` column for optimistic lock)
006_create_categories_table.yml             (NEW)
007_seed_system_categories.yml              (NEW)
008_create_transactions_table.yml           (NEW)
009_create_recurring_transactions_table.yml (NEW)
010_create_budgets_table.yml                (NEW)
```

---

## Non-Negotiable Domain Rules (from finance-analyst brief)

- Money = `BigDecimal`, `NUMERIC(19,4)`. Zero tolerance for float.
- All financial writes are atomic (`@Transactional`).
- `current_balance` is never set directly via API. `Account.debit()`/`credit()` are the only mutators.
- All list queries scoped to authenticated user — return 404 not 403 for other users' resources.
- Soft-delete only: `is_active = false` for User, Account, Category, Budget, RecurringTransaction.
- SAVINGS and CASH accounts cannot go below zero — `Account.debit()` throws `InsufficientFundsException`.
- TRANSFER creates two rows atomically; deleting either leg deletes both.

---

## Dependencies to Add/Remove

See `dependencies.md` for full list. Add `com.tngtech.archunit:archunit-junit5:1.2.1` to testImplementation.

Critical additions: `spring-boot-starter-validation`, `spring-security-crypto`, `springdoc-openapi-starter-webmvc-ui:2.3.0`, Testcontainers, ArchUnit.
Critical removals: `swagger-codegen-maven-plugin`, `spring-boot-starter-log4j2`, pinned `postgresql:42.1.4`.

---

## File Locations (Quick Reference)

| File | Path |
|---|---|
| Spring Boot main class | `application/src/main/java/com/shan/cyber/tech/PersonalFinanceTracker.java` |
| application.yaml | `application/src/main/resources/application.yaml` |
| Liquibase master | `application/src/main/resources/db.changelog/db.changelog-master.yaml` |
| Changesets | `application/src/main/resources/db.changelog/changes/` |
| Docker Compose | `localEnvironment/docker-compose.yaml` |
| Domain spec | `.claude/agent-memory/personal-finance-analyst/DOMAIN-OWNERSHIP.md` |
| Architecture decisions | `.claude/agent-memory/tech-lead/architecture-decisions.md` |
| Hexagonal architecture spec | `.claude/agent-memory/tech-lead/hexagonal-architecture.md` |
| Implementation plan | `.claude/agent-memory/tech-lead/implementation-plan.md` |
| Dependencies | `.claude/agent-memory/tech-lead/dependencies.md` |
| Code standards | `.claude/agent-memory/tech-lead/code-standards.md` |
| Multi-Platform API | `.claude/agent-memory/tech-lead/multi-platform-api.md` |
| Package Structure | `.claude/agent-memory/tech-lead/updated-package-structure.md` |
