# Changelog

All notable changes to this project will be documented in this file.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)

Versions map to logical delivery milestones (one per bounded context or major feature
wave), derived from the git history. Dates reflect the commit that completed the
milestone.

---

## [Unreleased]

### Added
- `ApplicationContextLoadTest` — `@SpringBootTest` context load test that catches Spring bean wiring
  bugs (duplicate beans, missing `@Bean` declarations, circular dependencies) before docker startup
- `.claude/hooks/verify-app-health.sh` — 5-layer application health feedback loop script; run after
  every pipeline track to verify compile → tests → frontend build → containers → HTTP smoke
- Tier 3 integration tests in `acceptance/` module (55 tests, RestAssured + Testcontainers covering
  Auth, Account, Transaction, Budget flows)
- OpenAPI `@Operation` annotations on all 8 controllers; `docs/openapi.yaml` (2 102 lines, 24 endpoints)
- Frontend accessibility hardening: WCAG 2.1 AA fixes (CategorySelect id/aria, CardTitle heading order);
  axe-core tests via vitest-axe (10 tests passing)

### Fixed
- `TransactionCommandService` and `TransactionApplicationService` both implemented `CreateTransferUseCase`
  and `DeleteTransactionUseCase` — duplicate Spring bean caused startup crash at `TransferController`
  injection point. Fixed by removing `implements` clause from domain service; only the application
  service in `config/` implements inbound port interfaces.
- `AccountNotFoundException` incorrectly extended `DomainException` (→ HTTP 422); changed to extend
  `ResourceNotFoundException` (→ HTTP 404) per cross-user isolation rule.
- `TransactionApplicationService` had `@Service` in `config/` package violating hexagonal architecture;
  replaced with explicit `@Bean` in `TransactionConfig`.

### Engineering
- `engineering-manager` agent is now mandatory entry point for ALL pipeline tracks (FEATURE, HOTFIX,
  CHORE, SPIKE, UI-ONLY); no track bypasses the orchestrator
- All 9 agent `MEMORY.md` files updated with health feedback loop responsibilities and `ApplicationContextLoadTest` reference
- `CLAUDE.md` enforcement block updated: health gate added as mandatory step for every track

### Planned
- Token refresh endpoint (`POST /api/v1/auth/refresh`) for mobile clients
- Device token registration for push notifications (`POST /api/v1/auth/devices`)
- Delta sync endpoint (`GET /api/v1/sync?since=...`) for offline-first mobile
- Redis-backed rate limiting (replace in-memory `ConcurrentHashMap` in `AuthService`)
- Scheduled ledger-reconciliation job for balance drift detection
- `@TransactionalEventListener(AFTER_COMMIT)` upgrade for all domain event listeners

---

## [0.8.0] - 2026-03-20 — CI/CD, Dependency Updates & Polish

### Added
- Production-ready GitHub Actions CI/CD pipeline with build, test, Docker image push,
  Trivy container security scan, and CodeQL static analysis jobs
- SARIF upload guard: Trivy scan job skips upload when no output file is produced,
  preventing false workflow failures
- `security-events: write` permission on Trivy scan job (required for SARIF upload)

### Changed
- Categories page UI revamped with improved layout and component hierarchy
- Pagination bug on Categories list fixed (off-by-one page index)

### Fixed
- `noUnusedParameters` TypeScript error in `BudgetPlanPage` (underscore-prefix
  convention applied to intentionally unused parameters)
- All ESLint errors that were blocking GitHub Actions CI runs
- Invalid GitHub Actions workflow: removed `env` context references from job name
  fields (not supported by the runner)
- Recharts 3.8 `Formatter` type breaking change — charting components updated to
  match new generic signature

### Security
- Bumped `github/codeql-action` from v3 to v4
- Bumped `docker/login-action` from v3 to v4
- Bumped `docker/build-push-action` from v5 to v7
- Bumped `actions/upload-artifact` from v4 to v7
- Bumped `gradle/actions` from v3 to v5
- Bumped `com.tngtech.archunit:archunit-junit5` to 1.4.1
- Bumped `io.spring.dependency-management` to 1.1.7
- Bumped `recharts` from 3.7.0 to 3.8.0
- Bumped `@types/node` to 25.5.0

---

## [0.7.0] - 2026-03-19 — Agent System & MVP Feature Gaps

### Added
- Claude multi-agent system under `.claude/agents/` with roles: `solution-planner`,
  `personal-finance-analyst`, `tech-lead`, `full-stack-dev`, `ux-ui-designer`,
  `qa-automation-tester`, `devops-engineer`
- Agent memory directory at `.claude/agent-memory/` with per-agent knowledge files
  (ADRs, domain ownership, design system, testing strategy, package structure)
- Feature delivery pipeline documentation enforcing the mandatory 7-step agent flow

### Fixed
- Multiple MVP feature gaps identified during agent architecture review resolved:
  - Budget upsert-by-category endpoint wired correctly end-to-end
  - `BudgetApplicationService` pattern corrected — `BudgetCommandService` injected as
    `@Bean`, never constructed inline
  - `BudgetPlanQueryService` pure-Java grouping logic for expense categories by parent
  - Parent category guard and TRANSFER category guard enforced at domain service level

---

## [0.6.0] - 2026-03-14 — Reporting, Dashboard & Preferred Currency

### Added
- Dashboard summary endpoint (`GET /api/v1/dashboard`) aggregating net worth, monthly
  income/expense totals, and top spending categories
- Reporting context (`reporting/`) with spending-by-category and cash-flow endpoints:
  - `GET /api/v1/reports/spending-by-category`
  - `GET /api/v1/reports/cash-flow`
- Interactive dashboard charts in the React frontend (Recharts): net worth trend,
  monthly cash flow bar chart, spending breakdown donut chart
- Preferred currency setting per user: `PATCH /api/v1/users/me/preferred-currency`
- App-wide currency propagation — all money display in the frontend respects the
  authenticated user's preferred currency
- Comprehensive test suite: 147 tests across unit (Mockito), repository
  (`@DataJpaTest` + Testcontainers PostgreSQL 15.2), and ArchUnit boundary checks

### Changed
- Frontend style upgrade: improved typography, spacing, and color tokens aligned with
  Tailwind CSS design system
- Graphs and visualisation components integrated into existing pages (Accounts,
  Transactions, Budget)

### Fixed
- DB migration 011: `preferred_currency` column type changed from `CHAR(3)` to
  `VARCHAR(3)` for JPA/Hibernate compatibility
- DB migration 012: `currency` and `account_number_last4` on `accounts` table changed
  from `CHAR` to `VARCHAR` for JPA/Hibernate compatibility

---

## [0.5.0] - 2026-03-13 — Budget Management

### Added
- Budget bounded context (`budget/`) — full hexagonal implementation:
  - `POST /api/v1/budgets/upsert-by-category` — create or update budget per category
    and period type; soft-deactivates conflicting active budget when period type changes
  - `GET /api/v1/budgets` — list active budgets for authenticated user with spent
    amount and percent-used
  - `GET /api/v1/budgets/plan` — grouped budget plan view: expense categories grouped
    by parent, income categories flat, ungrouped leaves rendered as solo groups
  - `DELETE /api/v1/budgets/{id}` — soft-delete (sets `is_active = false`)
- `budgets` table: `NUMERIC(19,4)` amount, `currency VARCHAR(3)`, `period_type`,
  `rollover_enabled`, `alert_threshold_pct`, soft-delete via `is_active`
- Unique partial index `idx_budget_unique_active` on `(user_id, category_id, period_type)`
  `WHERE is_active = true` — prevents duplicate active budgets
- Composite index `idx_budgets_user_active_dates` on `(user_id, start_date, end_date)`
  `WHERE is_active = true` — supports budget plan date-range queries
- `currency` column added to `budgets` table (migration 013) for `Money` VO consistency
- Budget period multipliers (`BudgetPeriod.toMonthlyMultiplier()`) using pure
  `BigDecimal` arithmetic — no float/double
- Cross-context port `CategoryQueryPort` (budget → category) via `CategoryQueryAdapter`
  implementing the ACL pattern from ADR-016
- `BudgetApplicationService` in `budget/config/` as `@Service @Transactional` wrapper
  delegating to the pure-Java `BudgetCommandService`
- Budget UI: budget list with progress bars, budget plan grouped view, create/edit
  modal with period type selector

### Fixed
- DB migration 014: added `idx_budgets_user_active_dates` partial index (was missing
  from initial `010_create_budgets_table.yml`)

---

## [0.4.0] - 2026-03-13 — Transaction Management & Recurring Transactions

### Added
- Transaction bounded context (`transaction/`) — full hexagonal implementation:
  - `POST /api/v1/transactions` — create INCOME, EXPENSE, or TRANSFER transaction
  - `GET /api/v1/transactions` — paginated list with filters (account, category, type,
    date range, search term)
  - `GET /api/v1/transactions/{id}` — get single transaction (404 for other users')
  - `PATCH /api/v1/transactions/{id}` — update amount, category, date, description
  - `DELETE /api/v1/transactions/{id}` — soft-delete; TRANSFER deletes both legs
- `transactions` table: `NUMERIC(19,4)` amount, `DEFERRABLE INITIALLY DEFERRED` FK
  for `transfer_pair_id` (ADR-011 two-phase insert pattern)
- Indexes: `idx_transactions_user_date`, `idx_transactions_account`,
  `idx_transactions_category`, `idx_transactions_user_cat_date`
- Recurring transaction support (`recurring_transactions` table):
  - `POST /api/v1/recurring-transactions`
  - `GET /api/v1/recurring-transactions`
  - `PATCH /api/v1/recurring-transactions/{id}`
  - `DELETE /api/v1/recurring-transactions/{id}` — soft-delete
  - Frequency enum: `DAILY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `QUARTERLY`, `ANNUALLY`
- Cross-context ports: `AccountBalancePort` (transaction → account) for atomic balance
  updates; `CategoryValidationPort` (transaction → category) for category existence
  checks — both ACL adapters per ADR-016
- `Account.debit(Money)` / `Account.credit(Money)` domain methods enforce balance
  invariants; `InsufficientFundsException` thrown for SAVINGS/CASH accounts going below zero
- TRANSFER creates two rows atomically in one `@Transactional` boundary; delete of
  either leg removes both
- Transaction list and detail UI in the React frontend with filters sidebar and
  paginated table

---

## [0.3.0] - 2026-03-13 — Category Management

### Added
- Category bounded context (`category/`) — full hexagonal implementation:
  - `POST /api/v1/categories` — create user-owned category (INCOME, EXPENSE, TRANSFER)
  - `GET /api/v1/categories` — list all categories (system + user-owned), paginated
  - `GET /api/v1/categories/{id}` — get single category
  - `PATCH /api/v1/categories/{id}` — update name, icon, color (user-owned only)
  - `DELETE /api/v1/categories/{id}` — soft-delete user-owned categories only
- `categories` table with self-referencing `parent_category_id` FK for two-level
  hierarchy (parent → child subcategory)
- Unique partial index on `(COALESCE(user_id, 0), COALESCE(parent_category_id, 0), LOWER(name))`
  `WHERE is_active = true` — prevents duplicate category names per user/level
- `category_types` reference table seeded with `INCOME`, `EXPENSE`, `TRANSFER`
- System categories seeded (migration 007): 16 EXPENSE parent categories with
  25 child subcategories, 7 INCOME parent categories, 1 TRANSFER category —
  all with icon codes (lucide-react compatible)
- Categories management page in React frontend with search, type filter, and
  create/edit/delete modal

---

## [0.2.0] - 2026-03-04 — Account Management & React Frontend Scaffold

### Added
- Account bounded context (`account/`) — full hexagonal implementation:
  - `POST /api/v1/accounts` — create account (CHECKING, SAVINGS, CREDIT_CARD,
    INVESTMENT, LOAN, CASH, DIGITAL_WALLET)
  - `GET /api/v1/accounts` — list active accounts for authenticated user
  - `GET /api/v1/accounts/{id}` — get single account (404 for other users')
  - `PATCH /api/v1/accounts/{id}` — update name, institution, account number last 4
  - `DELETE /api/v1/accounts/{id}` — soft-delete (sets `is_active = false`)
  - `GET /api/v1/accounts/net-worth` — sum of all account balances (assets minus liabilities)
- `accounts` table: `NUMERIC(19,4)` balance columns, optimistic locking via `version`
  column (`@Version` in JPA entity, plain `Long` in domain class)
- `account_types` reference table seeded with 7 account types; `is_liability` and
  `allows_negative_balance` flags drive domain invariants
- Case-insensitive unique index on account name per user (`idx_accounts_user_name_ci`)
- React + TypeScript frontend scaffolded with Vite, Tailwind CSS, and shadcn/ui
- Accounts management page: list, create/edit modal, delete confirmation
- Net worth summary card on the main dashboard
- Sidebar navigation linking all implemented sections

### Fixed
- Backend bugs in account creation and balance update identified and resolved during
  frontend integration

---

## [0.1.0] - 2026-03-01 — Identity Context & Hexagonal Architecture Foundation

### Added
- Hexagonal architecture foundation (ADR-014) across all bounded contexts:
  `identity/`, `account/`, `category/`, `transaction/`, `budget/`, `reporting/`,
  `shared/`
- Strict domain-purity enforcement: ArchUnit tests verify zero Spring/JPA/Jackson
  imports in any `{context}/domain/` package
- `shared/` kernel: `Money` VO, `DateRange` VO, `AuditInfo` VO, typed-ID records
  (`UserId`, `AccountId`, `CategoryId`, `BudgetId`, `TransactionId`), `DomainEvent`
  marker interface
- `GlobalExceptionHandler` (`@RestControllerAdvice`) in `shared/adapter/inbound/web/`
  mapping domain exceptions to HTTP status codes (ADR-004)
- `PageResponseDto<T>` pagination wrapper matching the specified API envelope shape
- Identity bounded context (`identity/`) — full hexagonal implementation:
  - `POST /api/v1/auth/register` — user registration with BCrypt password hashing
    (`spring-security-crypto`, strength 12)
  - `POST /api/v1/auth/login` — returns opaque UUID session token; in-memory rate
    limiting keyed by client IP (5 attempts / 15 minutes)
  - `POST /api/v1/auth/logout` — invalidates session row
  - `GET /api/v1/users/me` — authenticated user profile
  - `PATCH /api/v1/users/me` — update first name, last name, email
- Custom `SessionAuthFilter` (`OncePerRequestFilter`) validating `Authorization: Bearer
  {token}` against `sessions` table; no Spring Security dependency (ADR-003)
- `sessions` table: UUID token, `expires_at TIMESTAMPTZ`, user FK with `ON DELETE CASCADE`
- Liquibase migration 003 (`alter_users_table`): adds `first_name`, `last_name`,
  `is_active`, `preferred_currency` columns; fixes PK/FK column types to `BIGINT`
  and timestamps to `TIMESTAMPTZ`
- Liquibase migration 004 (`create_reference_tables`): `account_types` and
  `category_types` seeded reference tables

### Changed
- Gradle multi-module build finalised: `application/` (runnable Spring Boot),
  `acceptance/` (integration tests — empty at this milestone), `database/`
  (placeholder — duplicate Liquibase files removed)
- Liquibase single source of truth established in `application/src/main/resources/
  db.changelog/` (ADR-007); `application.yaml` changelog path and `default-schema`
  corrected (BUG-001, BUG-002)
- Removed `swagger-codegen-maven-plugin` from `implementation` scope (BUG-009)
- Removed `spring-boot-starter-log4j2` conflicting with Logback (BUG-010)
- Removed pinned `postgresql:42.1.4` (2017-era CVE-laden driver) in favour of
  Spring Boot managed version (BUG-011)

---

## [0.0.1] - 2024-06-20 — Initial Project Skeleton

### Added
- Gradle multi-module project skeleton (`application/`, `acceptance/`, `database/`)
- Spring Boot 3.2.2 application entry point at
  `com.shan.cyber.tech.PersonalFinanceTracker`
- Initial Liquibase master changelog with two changesets:
  - `001_create_schema.yml` — creates `finance_tracker` PostgreSQL schema
  - `002_create_user_and_session_tables.yml` — creates `users` and `sessions` tables
    with sequence-generated `BIGINT` primary keys
- Basic project structure and Liquibase folder layout

---

[Unreleased]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.8.0...HEAD
[0.8.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/compare/v0.0.1...v0.1.0
[0.0.1]: https://github.com/shanmuga-sundaram-n/personal-finance-tracker/releases/tag/v0.0.1
