# Feature Brief: Budget Suite (Quick Budget, Budget by Month, Comparison View, Daily Spending Tracker)
**Date**: 2026-03-19 | **Status**: BACKLOG — awaiting prioritisation | **Phase**: 2

## Open Decisions (unresolved — must be answered before implementation)
1. **Budget source toggle persistence** — localStorage (MVP recommendation) or backend user preference?
2. **Totals row scope** — budgeted rows only (recommendation) or all categories with activity?
3. **Daily Spending Tracker scope** — EXPENSE only (recommendation) or INCOME + EXPENSE?

## Summary
Four features modelled after a spreadsheet-based budgeting workflow (Excel template). Features 1–3 extend the existing Budget domain. Feature 4 adds a new UI view over existing transactions. New DB table required only for Feature 2 (monthly_budgets).

## Feature 1 — Quick Budget (S)
- Amount + frequency per category; auto-calculates monthly/yearly equivalents
- Multipliers: Weekly ×52/12, Bi-weekly ×26/12, Monthly ×1, Quarterly ×4/12, Yearly ×1/12
- Yearly derived from source amount (not monthly×12) to prevent rounding drift
- Schema: add BI_WEEKLY to BudgetPeriod + toMonthlyMultiplier()
- New endpoint: GET /api/v1/budgets/quick-summary

## Feature 2 — Budget by Month (M)
- Per-category budget for each calendar month independently
- New table: monthly_budgets(user_id, category_id, budget_year, budget_month 1-12, amount NUMERIC(19,4), currency)
- Unique on (user_id, category_id, budget_year, budget_month)
- rollover_enabled always false for monthly budgets
- New ports: SetMonthlyBudgetUseCase, GetMonthlyBudgetQuery, MonthlyBudgetPersistencePort
- Endpoints: GET/PUT /api/v1/budgets/monthly, PUT/DELETE /api/v1/budgets/monthly/{categoryId}/{month}

## Feature 3 — Comparison View enhancement (S)
- Period selector: Current Month / Prior Month / Jan–Dec / Full Year
- Budget source: QUICK or MONTHLY (resolved server-side from URL param)
- New enums: BudgetComparisonPeriod, BudgetSource
- New endpoint: GET /api/v1/budgets/plan/period?period=CURRENT_MONTH&year=2026&source=QUICK
- Frontend: PeriodSelector + BudgetSourceToggle on existing BudgetPlanPage

## Feature 4 — Daily Spending Tracker (S-M)
- Transactions grouped by day for a selected month
- No new DB table — reads from existing transactions
- New port: GetDailySpendingQuery
- New endpoint: GET /api/v1/transactions/daily?year=&month=&categoryId=
- Frontend: DailySpendingPage with month navigator + accordion day groups

## Implementation Order (when prioritised)
1. BI_WEEKLY to BudgetPeriod + multiplier
2. Feature 4 backend (no DB change — quickest win)
3. Feature 1 backend (quick summary endpoint)
4. Feature 2 DB migration + domain + endpoints (parallel)
5. Feature 3 — extend BudgetPlanQueryService + new endpoint
6. All frontend pages

## Full Analysis
- Domain brief: personal-finance-analyst (session 2026-03-19)
- Architecture brief: tech-lead (session 2026-03-19)
