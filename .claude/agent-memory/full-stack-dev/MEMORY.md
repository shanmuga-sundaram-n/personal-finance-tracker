# Full-Stack Dev Memory — Personal Finance Tracker
**Last Updated**: 2026-03-19

This file is loaded into every session. Keep it under 200 lines. Link to topic files for details.

---

## Project Snapshot

| Attribute | Value |
|---|---|
| Backend | Java 17, Spring Boot 3.2.2, Gradle multi-module |
| Frontend | React + TypeScript, Vite, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL 15.2, schema `finance_tracker` |
| Root package | `com.shan.cyber.tech.financetracker` |
| API prefix | `/api/v1/` |
| Auth | Opaque UUID session token, `Authorization: Bearer {token}` header |

---

## Architecture (Read Before Writing Any Code)

This project uses **Strict Hexagonal Architecture** (ADR-014). Violating these rules fails the ArchUnit build:

1. **Domain zone is pure Java** — zero Spring, JPA, Jackson, Lombok in any `{context}/domain/` package.
2. **Never `@Service` on domain classes** — wire via `@Bean` in `{Context}Config.java` only.
3. **Domain model ≠ JPA entity** — every aggregate has a separate `*JpaEntity` + `*JpaMapper`.
4. **No cross-context domain imports** — use outbound ports (sync) or domain events (async). Only `shared/domain/model/` typed IDs cross boundaries.

Full architecture spec: `.claude/agent-memory/tech-lead/architecture-decisions.md`
Full package tree: `.claude/agent-memory/tech-lead/updated-package-structure.md`

---

## Bounded Contexts

`identity/` | `account/` | `category/` | `transaction/` | `budget/` | `reporting/` | `shared/`

Each context structure:
```
{context}/
├── domain/model/ service/ port/inbound/ port/outbound/ event/ exception/
├── adapter/inbound/web/         (controllers, request/response DTOs, mappers)
├── adapter/outbound/persistence/ (JPA entities, JPA repositories, persistence adapters, mappers)
├── adapter/outbound/event/      (Spring event publisher adapter)
├── adapter/outbound/crosscontext/ (ACL adapters for consuming other contexts)
└── config/{Context}Config.java  (Spring @Bean wiring only)
```

---

## Naming Conventions (ADR-017)

| Role | Pattern | Example |
|---|---|---|
| Inbound port (command) | `{Action}{Entity}UseCase` | `CreateAccountUseCase` |
| Inbound port (query) | `Get{Entity}Query` | `GetAccountsQuery` |
| Domain service (write) | `{Entity}CommandService` | `AccountCommandService` |
| Domain service (read) | `{Entity}QueryService` | `AccountQueryService` |
| JPA entity | `{Entity}JpaEntity` | `AccountJpaEntity` |
| Persistence adapter | `{Entity}PersistenceAdapter` | `AccountPersistenceAdapter` |
| Context config | `{Context}Config` | `AccountConfig` |
| Request DTO | `{Action}{Entity}RequestDto` | `CreateAccountRequestDto` |
| Response DTO | `{Entity}ResponseDto` | `AccountResponseDto` |

---

## Money Rules (ADR-009)

- Java fields: `BigDecimal` with `@Column(precision=19, scale=4)`
- JSON: serialize as string (`"amount": "1250.0000"`) — use `@JsonSerialize(using=ToStringSerializer.class)`
- DB: `NUMERIC(19,4)` — never `FLOAT` or `DOUBLE`
- `Money` Value Object: `shared/domain/model/Money.java` (amount + currency)

---

## Key File Locations

| File | Path |
|---|---|
| Spring Boot main | `application/src/main/java/com/shan/cyber/tech/PersonalFinanceTracker.java` |
| application.yaml | `application/src/main/resources/application.yaml` |
| Liquibase master | `application/src/main/resources/db.changelog/db.changelog-master.yaml` |
| DB changesets | `application/src/main/resources/db.changelog/changes/NNN_description.yml` |
| Frontend entry | `frontend/src/main.tsx` |
| Frontend components | `frontend/src/components/` |
| Frontend pages | `frontend/src/pages/` |
| Frontend API client | `frontend/src/` (check for `api/` or `services/` directory) |

---

## Testing Strategy (ADR-008)

- **Unit**: `application/src/test/java/` — JUnit 5 + Mockito, no Spring context for domain services
- **Repository**: `@DataJpaTest` + Testcontainers postgres:15.2 (NOT H2)
- **Integration**: `acceptance/` module — `@SpringBootTest` + RestAssured + Testcontainers
- `./gradlew test` → unit + repository; `./gradlew integrationTest` → integration (needs Docker)

---

## Known Bugs to Fix (Phase 0)

See `.claude/agent-memory/tech-lead/MEMORY.md` for BUG-001 through BUG-011 before starting any feature work.
