---
name: hierarchical-budget-plan-analysis
description: Domain analysis for Category-Hierarchical Budget Plan with Set Budget Dialog feature — acceptance criteria, data model changes, frequency multipliers, edge cases, and conflicts flagged for tech-lead
type: project
---

# Feature: Category-Hierarchical Budget Plan with Set Budget Dialog
**Analysed by**: personal-finance-analyst
**Date**: 2026-03-19
**Status**: Complete — ready for solution-planner and tech-lead

## Why
User requested a budget plan view matching a spreadsheet layout: income as flat rows, expenses grouped by parent category with bold headers and subtotals. Each leaf row has a "Set Budget" inline dialog with frequency selection (including BI_WEEKLY and SEMI_ANNUAL which are missing from the current BudgetPeriod enum).

## How to Apply
Use this file as the authoritative domain brief when the solution-planner coordinates the tech-lead and full-stack-dev for implementation. QA should use the acceptance criteria directly as the test specification basis.

## Blockers (Must Fix Before Implementation)
1. BudgetPeriod enum missing BI_WEEKLY and SEMI_ANNUAL
2. CategorySummary missing parentCategoryId and isLeaf fields
3. BudgetPlanView/BudgetPlanQueryService must restructure expense output from flat list to List<ExpenseCategoryGroup>
4. USD currency hardcode in BudgetPlanQueryService line 57 must be resolved
5. TRANSFER_OUT inflating expense actuals in sumExpenseAmount query

## Tech-Lead Decision Required
- period_type is final/immutable in Budget domain class. If user changes frequency in the dialog for an existing budget, two options exist:
  - Make period_type mutable in Budget.update() — simpler but weakens domain model
  - Deactivate existing budget and create a new one — correct domain approach, more complex frontend/backend coordination
- Which approach to take must be decided before implementation begins

## Key Domain Rules Applied
- Budgets on parent categories: BLOCKED. Backend guard in BudgetCommandService: check hasActiveChildren before allowing create.
- Category type TRANSFER: cannot be budgeted. Must not appear in plan view.
- One active budget per category per period type per user.
- Soft-delete only — removing a budget means is_active = false.
- All amounts: NUMERIC(19,4) / BigDecimal. No float.
- Currency: user.preferred_currency — never hardcoded.

## Frequency Multipliers (Exact BigDecimal)
Monthly multipliers (enteredAmount * multiplier = monthlyEquivalent):
- WEEKLY: 4.3333333333 (= 52/12)
- BI_WEEKLY: 2.1666666667 (= 26/12)
- MONTHLY: 1
- QUARTERLY: 0.3333333333 (= 1/3)
- SEMI_ANNUAL: 0.1666666667 (= 1/6)
- ANNUALLY: 0.0833333333 (= 1/12)

Yearly multipliers (enteredAmount * multiplier = yearlyEquivalent):
- WEEKLY: 52, BI_WEEKLY: 26, MONTHLY: 12, QUARTERLY: 4, SEMI_ANNUAL: 2, ANNUALLY: 1

Convention: 52 weeks/year (industry standard for budgeting). Not 365.25/7.
Scale: intermediate results retain 10dp; final stored/displayed values rounded to 4dp HALF_UP.

## Data Model Changes
- BudgetPeriod: add BI_WEEKLY (calculateEndDate = startDate.plusWeeks(2).minusDays(1)), SEMI_ANNUAL (startDate.plusMonths(6).minusDays(1))
- CategorySummary: add parentCategoryId (CategoryId, nullable), isLeaf (boolean)
- BudgetPlanCategoryRow: add parentCategoryId (Long, nullable), isLeaf (boolean), budgetPeriod (String, nullable), monthlyEquivalent (BigDecimal, nullable), yearlyEquivalent (BigDecimal, nullable)
- BudgetPlanView: expenseRows becomes List<ExpenseCategoryGroup> where each group = {parentCategoryId, parentCategoryName, parentMonthlyBudgetedTotal, parentActualTotal, List<BudgetPlanCategoryRow> childRows}

## Acceptance Criteria Count
29 numbered ACs across 6 groups:
- A: Plan view hierarchy rendering (AC-001 to AC-009)
- B: Dialog opening and pre-population (AC-010 to AC-014)
- C: Dialog user input and calculation (AC-015 to AC-019)
- D: Dialog save behaviour (AC-020 to AC-024)
- E: Parent category budget blocked (AC-025 to AC-027)
- F: Currency (AC-028 to AC-029)

## Notable Edge Cases
- Leaf with no parent: renders as standalone row (no group header required)
- Category deactivated after budget set: budget remains in DB; category row disappears from plan; budget is orphaned until category is reactivated
- Income parent category: same blocking rule as expense — no budget on parent if it has active children
- Race condition (two tabs): second CREATE returns 409; frontend shows "refresh to see current values"
- Currency change by user after budgets exist: amounts displayed in new currency label but not converted — documented MVP limitation
