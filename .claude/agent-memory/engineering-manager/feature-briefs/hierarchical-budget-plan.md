# Feature Brief: Category-Hierarchical Budget Plan with Set Budget Dialog
**Date**: 2026-03-19 | **Status**: IN PROGRESS | **Phase**: 2 | **Complexity**: M

## Decision Log
- Frequency change on existing budget: **Option A — deactivate + recreate** (period type is fundamental, not updatable in place)

## Domain Rules
- Leaf-only budgeting: budgets only on leaf categories (no children). Parent → HTTP 422 PARENT_CATEGORY_BUDGET_NOT_ALLOWED
- Frequency multipliers (BigDecimal only): Weekly=13/3, Bi-weekly=13/6, Monthly=1, Quarterly=1/3, Semi-Annual=1/6, Yearly=1/12
- Yearly = source_amount × original_multiplier (never monthly×12 — avoids rounding drift)
- Parent subtotals = sum of children's monthlyEquivalent (server-side BigDecimal)
- TRANSFER categories excluded from both sections
- One active budget per category — upsert semantics
- CUSTOM frequency excluded from dialog

## Architecture

### Backend — CREATE
- budget/domain/port/inbound/BudgetPlanCategoryGroup.java
- budget/domain/port/inbound/UpsertBudgetByCategoryUseCase.java
- budget/domain/port/inbound/UpsertBudgetByCategoryCommand.java

### Backend — MODIFY
- budget/domain/model/BudgetPeriod.java              (add BI_WEEKLY, SEMI_ANNUAL, toMonthlyMultiplier, update calculateEndDate)
- budget/domain/port/outbound/CategorySummary.java   (add parentCategoryId)
- budget/domain/port/inbound/BudgetPlanCategoryRow.java (add frequency, monthlyAmount, yearlyAmount)
- budget/domain/port/inbound/BudgetPlanView.java     (replace expenseRows with expenseGroups)
- budget/adapter/outbound/CategoryNameAdapter.java   (pass parentCategoryId)
- budget/domain/service/BudgetPlanQueryService.java  (grouping algorithm + monthly/yearly)
- budget/domain/service/BudgetQueryService.java      (switch cases for new periods)
- budget/domain/service/BudgetCommandService.java    (upsert logic: deactivate+recreate on period change)
- budget/adapter/inbound/web/BudgetController.java   (upsert endpoint + updated plan DTO mapper)

### Frontend — CREATE
- frontend/src/components/budgets/SetBudgetDialog.tsx

### Frontend — MODIFY
- frontend/src/types/budget.types.ts
- frontend/src/api/budgets.api.ts
- frontend/src/pages/budgets/BudgetPlanPage.tsx
- frontend/src/pages/budgets/CreateBudgetPage.tsx

## Key API
- POST /api/v1/budgets/upsert-by-category
  Body: { categoryId, periodType, amount, currency, startDate, endDate }
  Behavior: if active budget exists for (userId, categoryId) in date range AND period changed → deactivate old, create new; if only amount changed → update in place; if none exists → create

## Implementation Checklist
- [ ] BudgetPeriod enum changes
- [ ] CategorySummary + CategoryNameAdapter
- [ ] BudgetPlanCategoryRow + BudgetPlanCategoryGroup + BudgetPlanView
- [ ] BudgetPlanQueryService grouping algorithm
- [ ] BudgetQueryService switch cases
- [ ] UpsertBudgetByCategoryUseCase + Command
- [ ] BudgetCommandService upsert implementation
- [ ] BudgetController upsert endpoint + plan DTO mapper update
- [ ] budget.types.ts + budgets.api.ts
- [ ] SetBudgetDialog.tsx
- [ ] BudgetPlanPage.tsx grouped rendering
- [ ] CreateBudgetPage.tsx Zod schema update
- [ ] Unit tests: BudgetPlanQueryService (grouping)
- [ ] Unit tests: BudgetCommandService (upsert, deactivate+recreate)
- [ ] Controller tests: upsert endpoint
- [ ] UX review
- [ ] QA sign-off
- [ ] Tech-lead sign-off
