# Personal Finance Tracker — Claude Instructions

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.2, Gradle multi-module |
| Frontend | React + TypeScript, Vite, Tailwind CSS, shadcn/ui — in `frontend/` |
| Database | PostgreSQL 15.2, schema `finance_tracker`, DB `personal-finance-tracker` |
| Infrastructure | Docker Compose, Nginx, GitHub Actions CI/CD |
| Gradle modules | `application/` (runnable), `acceptance/` (integration tests), `database/` (placeholder) |

## Architecture Rules (Non-Negotiable)

- **Hexagonal Architecture** (ADR-014): Strict Ports & Adapters. Every bounded context has `domain/` + `adapter/inbound/web/` + `adapter/outbound/persistence/` + `adapter/outbound/event/` + `adapter/outbound/crosscontext/` + `config/`.
- **Domain zone is pure Java**: Zero Spring/JPA/Jackson/Lombok imports in any `{context}/domain/` package. Enforced by ArchUnit — violations fail the build.
- **No `@Service` on domain classes**: Domain services wired via `@Bean` factory methods in `{Context}Config.java` only.
- **Separate domain class from JPA entity**: `*JpaEntity` lives in `adapter/outbound/persistence/`. `*JpaMapper` converts between them. Never put `@Entity` on domain models.
- **No cross-context domain imports**: Contexts communicate via outbound ports (sync) or domain events (async). Only typed IDs from `shared/domain/model/` cross boundaries.
  - `shared/domain/model/` contains ONLY: `Money` VO, `DateRange` VO, `UserId`/`AccountId`/`CategoryId`/`BudgetId` typed-ID records, `AuditInfo` VO, `DomainEvent` marker interface. Nothing else belongs here.
- **Money = BigDecimal**: `NUMERIC(19,4)` in DB, serialized as string in JSON. Never use float/double for money.
- **All financial writes are `@Transactional`**.
- **Soft-delete only**: `is_active = false`. No hard deletes for User, Account, Category, Budget, RecurringTransaction.
- **All list queries scoped to authenticated user**: Return 404 (not 403) for another user's resources.

## Key Conventions

- Root package: `com.shan.cyber.tech.financetracker`
- API prefix: `/api/v1/`
- DTOs: Java records, named `*RequestDto` / `*ResponseDto`
- Migrations: `application/src/main/resources/db.changelog/changes/NNN_description.yml` only — single source of truth
- Auth: opaque UUID session token via `Authorization: Bearer {token}` header. No Spring Security. No JWT.
- Port naming: inbound = `{Action}{Entity}UseCase` / `Get{Entity}Query`; outbound = `{Entity}PersistencePort`

## Bounded Contexts

`identity/` | `account/` | `category/` | `transaction/` | `budget/` | `reporting/` | `shared/`

## Build & Run

```bash
make start                       # Start full Docker Compose stack
make stop                        # Stop stack
./gradlew test                   # Unit + repository tests (no Docker needed)
./gradlew integrationTest        # Integration tests (requires Docker)
cd frontend && npm run dev       # Frontend dev server (Vite)
cd frontend && npm run build     # Production build
```

## Key File Locations

| File | Path |
|---|---|
| Spring Boot main | `application/src/main/java/com/shan/cyber/tech/PersonalFinanceTracker.java` |
| application.yaml | `application/src/main/resources/application.yaml` |
| Liquibase master | `application/src/main/resources/db.changelog/db.changelog-master.yaml` |
| Docker Compose (local) | `localEnvironment/docker-compose.yaml` |
| Docker Compose (CI/prod) | `docker-compose.yml` |
| Frontend entry | `frontend/src/main.tsx` |
| Nginx config | `frontend/nginx.conf` |

## Feature Delivery Pipeline (Mandatory — Every Feature, Every Time)

**This pipeline is non-negotiable. Never skip or reorder steps. Never implement directly without planning first.**

```
1. solution-planner        → coordinates analyst + tech-lead, produces unified Feature Brief
      ├── personal-finance-analyst  → domain rules, acceptance criteria, edge cases
      └── tech-lead                 → architecture, ports/adapters, implementation order

2. full-stack-dev          → implements backend + frontend from the Feature Brief

3. ux-ui-designer          → reviews all UI, ensures mobile-first, applies design system

4. qa-automation-tester    → writes + runs unit tests, integration tests, accessibility checks

5. tech-lead               → final architecture sign-off, confirms no rule violations
```

**Trigger rules:**
- Any new feature or significant change → start at step 1 (`solution-planner`)
- UI-only change → start at step 3 (`ux-ui-designer`)
- Bug fix → `tech-lead` to diagnose, `full-stack-dev` to fix, `qa-automation-tester` to verify
- Infrastructure change → `devops-engineer` directly

## Agent Routing Guide

| Agent | Responsibility |
|---|---|
| `solution-planner` | Orchestrates analyst + tech-lead → unified Feature Brief |
| `personal-finance-analyst` | Domain rules, financial correctness, acceptance criteria |
| `tech-lead` | Architecture decisions, code review, design patterns, final sign-off |
| `full-stack-dev` | Implementation spanning frontend + backend |
| `ux-ui-designer` | All UI/UX — mobile-first, design system, accessibility |
| `qa-automation-tester` | Unit, integration, and accessibility tests — always runs after implementation |
| `devops-engineer` | Docker, CI/CD, Makefile, Gradle, infrastructure |

## Deep Reference (Agent Memory)

| Topic | File |
|---|---|
| Architecture decisions (ADRs) | `.claude/agent-memory/tech-lead/architecture-decisions.md` |
| Hexagonal architecture spec | `.claude/agent-memory/tech-lead/hexagonal-architecture.md` |
| Full package structure | `.claude/agent-memory/tech-lead/updated-package-structure.md` |
| Domain ownership & financial rules | `.claude/agent-memory/personal-finance-analyst/DOMAIN-OWNERSHIP.md` |
| Design system & components | `.claude/agent-memory/ux-ui-designer/design-system.md` |
| Testing strategy & infrastructure | `.claude/agent-memory/qa-automation-tester/testing-strategy.md` |
| Known bugs (Phase 0) | `.claude/agent-memory/tech-lead/MEMORY.md` (BUG-001 through BUG-011) |
