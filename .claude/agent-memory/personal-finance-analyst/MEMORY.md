# Agent Memory — personal-finance-analyst
**Project**: Personal Finance Tracker
**Last Updated**: 2026-02-28

## Project Status
Early-stage Spring Boot monorepo. Implemented: users + sessions tables (Liquibase), 1 placeholder endpoint. All domain design work lives in the files below.

## Key File Locations
- Domain doc: `.claude/agent-memory/personal-finance-analyst/DOMAIN-OWNERSHIP.md`
- Tech lead brief: `.claude/agent-memory/personal-finance-analyst/brief-for-tech-lead.md`
- UX brief: `.claude/agent-memory/personal-finance-analyst/brief-for-ux-designer.md`
- QA brief: `.claude/agent-memory/personal-finance-analyst/brief-for-qa-tester.md`
- OpenAPI draft: `application/src/main/resources/personal-finance-tracker.yaml`
- DB draft SQL: `database/src/main/resources/data.sql`
- Liquibase master: `application/src/main/resources/db.changelog/db.changelog-master.yaml`
- Docker Compose: `localEnvironment/docker-compose.yaml` (port 49883->5432)

## Tech Stack
Spring Boot 3.2.2, Java 17, PostgreSQL 15.2, Liquibase, Gradle multi-module (application, database, acceptance)
DB: `personal-finance-tracker`, Schema: `finance_tracker`, User: `pft-app-user`

## Known Schema Issues (Must Fix)
1. application.yaml references `personal_finance_tracker` schema but migrations create `finance_tracker` — standardise on `finance_tracker`
2. Draft SQL uses `INT PRIMARY KEY` — must be `BIGINT`
3. Draft SQL uses `ON UPDATE CURRENT_TIMESTAMP` (MySQL syntax) — invalid in PostgreSQL; use triggers or application layer
4. OpenAPI spec uses `type: number` for money — must use string+format or pattern to preserve precision

## Core Domain Rules (Non-Negotiable)
- Money: always NUMERIC(19,4) in DB, BigDecimal in Java — never float/double
- current_balance: computed from initial_balance + ledger; never directly user-settable via API
- Amounts in DB: always stored positive; sign derived from transaction_type
- SAVINGS + CASH accounts: reject any transaction that would make balance < 0
- Transfer: exactly 2 atomic transaction rows, share transfer_pair_id
- Soft-delete only: is_active = false for User, Account, Category, Budget, RecurringTransaction
- All queries user-scoped: never return another user's data; return 404 not 403 for cross-user access

## Domain Entities (MVP)
User, Session, AccountType (ref), Account, CategoryType (ref), Category (2-level hierarchy), Transaction, RecurringTransaction (Phase 2 impl), Budget

## Feature Phases
- Phase 1 (MVP): F-001 Auth, F-002 Accounts, F-003 Categories, F-004 Transactions, F-005 Transfers, F-006 Dashboard, F-007 Budgets
- Phase 2: Recurring transactions, Budget rollover, Search/export, Reconciliation
- Phase 3: Multi-currency, Investments, Debt planner, Savings goals, Bank feeds

## Account Type Business Rules
- CHECKING, DIGITAL_WALLET: can go negative (overdraft allowed)
- SAVINGS, CASH: cannot go negative (API rejects)
- CREDIT_CARD, LOAN: liability accounts; EXPENSE increases balance (debt), INCOME decreases it
- INVESTMENT: Phase 2; no direct transaction entry in MVP

## Budget Spending Logic
Spent = SUM of EXPENSE transactions in period for budget's category AND all its child categories. Never stored — always computed at query time.
NOTE: The `sumExpenseAmount` query in `TransactionJpaRepository` incorrectly includes TRANSFER_OUT. Budget plan spending figures must filter to `transactionType = 'EXPENSE'` only.

## Budget Plan Review
Full findings at: `.claude/agent-memory/personal-finance-analyst/budget-plan-review.md`
Key outstanding issues (as of 2026-03-19): TRANSFER_OUT inflates expense actuals (Major), totals calculation asymmetry (Major), income variance sign inverted (Major), USD hardcoded as currency default (Minor), rollover not applied in plan view (Minor).

## API Conventions
- All endpoints under /api/v1/
- 201 + Location header on create
- 422 for validation/business rule errors
- 409 for duplicates
- Error body: { status, error, message, errors[], timestamp, path }
- Pagination: page/size/sort/direction query params; response wraps in content + metadata

## Hierarchical Budget Plan + Set Budget Dialog Feature (2026-03-19)
Full analysis at: `.claude/agent-memory/personal-finance-analyst/hierarchical-budget-plan-analysis.md`
Key decisions:
- BudgetPeriod needs BI_WEEKLY + SEMI_ANNUAL added (blocker)
- CategorySummary needs parentCategoryId + isLeaf fields (blocker)
- BudgetPlanView must change from flat expenseRows to List<ExpenseCategoryGroup> (blocker)
- Budgets on parent categories are BLOCKED — backend guard required in BudgetCommandService
- Parent subtotals = sum of children's monthlyEquivalents (BigDecimal, server-side)
- Frequency multipliers: WEEKLY=52/12, BI_WEEKLY=26/12, MONTHLY=1, QUARTERLY=1/3, SEMI_ANNUAL=1/6, ANNUALLY=1/12
- period_type is currently immutable in Budget domain — frequency change in dialog needs deactivate-and-recreate (tech-lead decision required)
- USD hardcode bug (budget-plan-review Issue 5) is promoted to MUST-FIX before this feature ships
