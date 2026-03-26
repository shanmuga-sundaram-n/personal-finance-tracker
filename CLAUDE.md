# Personal Finance Tracker — Claude Instructions

## ⚠️ PIPELINE ENFORCEMENT — Read This First, Every Session

**These rules apply to EVERY request, EVERY session, with NO exceptions.**

### Classify before you act

**`engineering-manager` is the entry point for ALL tracks — no exceptions.**

| If the request is... | Track | First agent |
|---|---|---|
| New feature, new endpoint, domain change | **FEATURE** | `engineering-manager` |
| Production bug, startup failure, data issue | **HOTFIX** | `engineering-manager` |
| Refactor, rename, dependency upgrade | **CHORE** | `engineering-manager` |
| Research, investigation — doc output only | **SPIKE** | `engineering-manager` |
| Visual/copy change, no API change | **UI-ONLY** | `engineering-manager` |

### Non-negotiable gates

- **Gate 1D** — User approves Feature Brief before ANY code is written (FEATURE only)
- **Gate 3E** — `tech-lead` LGTM before PR merges (all tracks)
- **Gate 4C** — All tests pass before Phase 5 (all tracks)
- **Gate 6E** — Smoke test passes + user confirms before marking done (all tracks)

### Application Health Feedback Loop (mandatory after every track)

Run `.claude/hooks/verify-app-health.sh` at the end of every track. All 5 layers must pass:

| Layer | Checks | Skip when |
|---|---|---|
| 1 | `./gradlew :application:compileJava` — backend compiles | SPIKE, UI-ONLY |
| 2 | `./gradlew :application:test` — all tests + `ApplicationContextLoadTest` | SPIKE, UI-ONLY |
| 3 | `npm run build` — frontend builds clean | SPIKE |
| 4 | All 3 Docker containers running | SPIKE |
| 5 | Backend + frontend respond to HTTP | SPIKE |

Use `--quick` flag (layers 4+5 only) for UI-ONLY changes.

### What is NEVER allowed

- Implementing a FEATURE directly without going through `engineering-manager` first
- Skipping Phase 3 review streams (3A architecture, 3B security, 3C performance, 3D dependencies)
- Closing any track without running `.claude/hooks/verify-app-health.sh` and getting all layers green
- Marking a deploy done without layers 4 and 5 passing

---

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

### Track Selection (Phase 0 — always first)

**`engineering-manager` is the mandatory entry point for every track.**

| Track | When to use | Entry agent |
|---|---|---|
| **FEATURE** | New capability, domain change, new bounded context | `engineering-manager` |
| **HOTFIX** | Production bug, data integrity, security vulnerability | `engineering-manager` |
| **CHORE** | Dependency upgrade, refactor, no domain change | `engineering-manager` |
| **SPIKE** | Technical investigation, ADR research — output is a doc, never code | `engineering-manager` |
| **UI-ONLY** | Visual/copy change, no API contract change | `engineering-manager` |

### FEATURE Track — Full 7-Phase Pipeline

```
Phase 0   engineering-manager        → Intake & track selection

Phase 1   [PARALLEL]
          ├── personal-finance-analyst  → 1A: Domain rules, acceptance criteria, edge cases
          └── tech-lead                 → 1B: Ports, adapters, migration plan, complexity
          engineering-manager          → 1C: Convergence → Unified Feature Brief
          [GATE] Stakeholder approval  → 1D: Brief approved before implementation starts

Phase 2   devops-engineer + tech-lead → Branch & PR strategy
          [PARALLEL]
          ├── full-stack-dev           → 2A: Backend (domain → adapters → REST)
          └── ux-ui-designer           → 2B: Frontend design & component spec
          full-stack-dev               → 2C: Frontend implementation (after API contract stable)

Phase 3   [PARALLEL REVIEW STREAMS]
          ├── tech-lead                → 3A: Architecture & domain zone review
          ├── security-reviewer        → 3B: Auth, OWASP, financial data exposure
          ├── performance-reviewer     → 3C: N+1 queries, DB indexes, bundle size
          └── tech-lead                → 3D: Dependency review (new libraries, CVEs, licenses)
          tech-lead + full-stack-dev   → 3E: Consolidation, remediation, final LGTM
          [GATE] tech-lead LGTM        → PR ready to merge

Phase 4   [PARALLEL]
          ├── qa-automation-tester     → 4A: Unit + integration tests, coverage gates, migration validation
          └── ux-ui-designer           → 4B: Accessibility (WCAG 2.1 AA) + mobile review
          [GATE] QA gate               → 4C: All tests pass, all acceptance criteria verified

Phase 5   full-stack-dev + tech-lead  → API spec (OpenAPI), changelog, release notes, README

Phase 6   devops-engineer             → 6A: Pre-deploy checklist
          devops-engineer             → 6B: Deploy
          devops-engineer + qa        → 6C: Smoke test
          devops-engineer             → 6D: Monitoring & observability setup
          [GATE] Smoke test passes    → 6E: Stakeholder confirmation
```

### HOTFIX Track — Accelerated (entry: `engineering-manager`)
```
engineering-manager → classifies as HOTFIX, then orchestrates:
tech-lead           → Diagnose root cause
full-stack-dev      → Minimal targeted fix (no refactoring, no scope creep)
tech-lead           → Architecture review (3A only)
qa                  → Targeted test reproducing the bug + full regression run
devops              → Accelerated deploy + smoke test + incident documentation
[GATE]              → Stakeholder confirms bug resolved
```

### CHORE Track — Abbreviated (entry: `engineering-manager`)
```
engineering-manager → classifies as CHORE, then orchestrates:
full-stack-dev      → Implementation
tech-lead           → Architecture check (3A) + Dependency review (3D)
qa                  → Full regression run (no new tests required)
devops              → Deploy + smoke test (verify backend starts)
[GATE]              → Stakeholder confirms working
```

### SPIKE Track — Investigation Only (entry: `engineering-manager`)
```
engineering-manager → classifies as SPIKE, then orchestrates:
tech-lead           → Research options, author ADR
Output: ADR document only — no code, no tests, no deploy
```

### UI-ONLY Track (entry: `engineering-manager`)
```
engineering-manager → classifies as UI-ONLY, then orchestrates:
ux-ui-designer      → Design review & component spec
full-stack-dev      → Frontend implementation
tech-lead           → Confirm no API contract changes
ux-ui-designer      → WCAG 2.1 AA accessibility + mobile review
devops              → Deploy
[GATE]              → Stakeholder confirms
```

## Agent Routing Guide

| Agent | Pipeline Role | Responsibility |
|---|---|---|
| `engineering-manager` | Phase 0, 1C, 1D | Orchestrates discovery, owns Feature Brief, tracks delivery |
| `personal-finance-analyst` | Phase 1A | Domain rules, financial correctness, acceptance criteria |
| `tech-lead` | Phase 1B, 3A, 3D, 3E, 6E sign-off | Architecture decisions, ADRs, code review, final sign-off |
| `full-stack-dev` | Phase 2A, 2C, 3E, 5 | Backend + frontend implementation |
| `ux-ui-designer` | Phase 2B, 4B | Design system, mobile-first, accessibility |
| `qa-automation-tester` | Phase 4A, 6C | Unit, integration, accessibility tests, coverage gates |
| `devops-engineer` | Phase 2 (branch), 6A–6D | Docker, CI/CD, deployment, monitoring |
| `security-reviewer` | Phase 3B | OWASP, auth flows, financial data exposure |
| `performance-reviewer` | Phase 3C | N+1 queries, DB indexes, frontend bundle size |

## Deep Reference (Agent Memory)

| Topic | File |
|---|---|
| Architecture decisions (ADRs) | `.claude/agent-memory/tech-lead/architecture-decisions.md` |
| Hexagonal architecture spec | `.claude/agent-memory/tech-lead/hexagonal-architecture.md` |
| Full package structure | `.claude/agent-memory/tech-lead/updated-package-structure.md` |
| Domain ownership & financial rules | `.claude/agent-memory/personal-finance-analyst/DOMAIN-OWNERSHIP.md` |
| Design system & components | `.claude/agent-memory/ux-ui-designer/design-system.md` |
| Testing strategy & infrastructure | `.claude/agent-memory/qa-automation-tester/testing-strategy.md` |
| Known bugs (Phase 0) | `.claude/agent-memory/tech-lead/MEMORY.md` (BUG-001 through BUG-012) |
| Agent pipeline doc | `docs/agent-pipeline.md` |
| Pipeline visual (interactive) | `docs/engineering-pipeline.html` |
