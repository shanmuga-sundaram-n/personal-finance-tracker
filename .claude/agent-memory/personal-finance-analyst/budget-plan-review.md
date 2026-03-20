---
name: budget-plan-review
description: Financial domain correctness review of the Monthly Budget Plan feature (BudgetPlanQueryService and related classes)
type: project
---

# Monthly Budget Plan — Financial Domain Review
**Reviewed by**: personal-finance-analyst
**Date**: 2026-03-19
**Files reviewed**:
- `BudgetPlanQueryService.java`
- `BudgetPlanCategoryRow.java`
- `BudgetPlanTotals.java`
- `BudgetPlanView.java`
- `SpendingQueryPort.java`
- `TransactionJpaRepository.java` (income/expense sum queries)
- `TransactionSpendingAdapter.java`
- `SpendingTotalsQueryService.java`
- `TransactionType.java` (enum)
- `BudgetQueryService.java` (for rollover comparison)

---

## Issues Found

### Issue 1 — CRITICAL: TRANSFER_IN Counted as Income in the Income Sum Query

**File**: `TransactionJpaRepository.java`, lines 89–97

**The problem**: The income sum query filters on `t.transactionType = 'INCOME'` only, which correctly excludes `TRANSFER_IN`. However, the domain's `TransactionType` enum defines `isCredit()` as returning `true` for both `INCOME` and `TRANSFER_IN`. The expense sum query (`sumExpenseAmount`) includes `TRANSFER_OUT` alongside `EXPENSE` (line 104). This means the two sides of the plan are treated asymmetrically: expenses include transfers-out, but income correctly excludes transfers-in.

**The deeper risk**: This asymmetry is by design for expenses (TRANSFER_OUT correctly reduces account balance and is treated as an outflow) but could mislead users in the budget plan if a user ever categorises a TRANSFER_IN transaction under an income category. The income query already correctly uses `= 'INCOME'` so TRANSFER_IN is excluded at the SQL level. This is the correct behaviour.

**Verdict**: The income query is CORRECT. TRANSFER_IN is excluded. No code change needed.

---

### Issue 2 — MAJOR: TRANSFER_OUT Included in Expense Actuals for Budget Plan Rows

**File**: `TransactionJpaRepository.java`, lines 99–111

**The problem**: `sumExpenseAmount` (used by `getSpentAmount`) includes `TRANSFER_OUT` in the `IN ('EXPENSE', 'TRANSFER_OUT')` clause. This means that when a user transfers money between their own accounts, the TRANSFER_OUT side inflates the actual spending figure for that category's budget plan row. Transfers between accounts are not true expenditure — they are balance movements. Including them overstates spending and makes every expense budget appear worse than it is.

**Why it matters**: A user who transfers $500 from checking to savings will see that $500 appear as "actual spending" against whichever category is attached to the TRANSFER_OUT transaction. The budget plan will show a false overspend.

**Recommended fix** — change the expense sum query:
```java
// In TransactionJpaRepository.java, replace sumExpenseAmount:
@Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM TransactionJpaEntity t
        WHERE t.userId = :userId
          AND t.categoryId = :categoryId
          AND t.transactionType = 'EXPENSE'
          AND t.transactionDate BETWEEN :fromDate AND :toDate
        """)
BigDecimal sumExpenseAmount(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);
```

**Caution**: This change also affects the existing `BudgetQueryService` which uses the same `getSpentAmount` port method. Verify that the existing budget-vs-spending view was intentionally including TRANSFER_OUT. If the dashboard reporting (`sumByCategory`) is also used to compute spending breakdowns, note that it also includes TRANSFER_OUT (line 134) and should be reviewed separately.

---

### Issue 3 — MAJOR: Totals Calculation — Asymmetry Between totalBudgeted and totalActual

**File**: `BudgetPlanQueryService.java`, lines 101–116

**The problem**: `computeTotals` applies a filter so that only rows where `hasBudget=true` contribute to `totalBudgeted`. But `totalActual` sums ALL rows (both budgeted and unbudgeted categories). This means:

- `totalVariance = totalBudgeted(budgeted categories only) - totalActual(ALL categories)`
- A user with $3,000 budgeted across 5 expense categories but $500 actual spending in a 6th unbudgeted category will see their total variance reduced by $500 without a corresponding budget figure to explain it.
- The `totalPercentUsed` becomes meaningless: `$3,500 actual / $3,000 budgeted = 116%` even though none of the overspend was in budgeted categories.

**Recommended fix**: Make the totals calculation consistent. Two valid approaches:

**Option A (recommended) — Budgeted-only totals**: Filter BOTH `totalBudgeted` and `totalActual` to rows where `hasBudget=true`. Show unbudgeted actuals in a separate summary field if desired.
```java
private BudgetPlanTotals computeTotals(List<BudgetPlanCategoryRow> rows) {
    List<BudgetPlanCategoryRow> budgetedRows = rows.stream()
            .filter(BudgetPlanCategoryRow::hasBudget)
            .toList();
    BigDecimal totalBudgeted = budgetedRows.stream()
            .map(BudgetPlanCategoryRow::budgetedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalActual = budgetedRows.stream()
            .map(BudgetPlanCategoryRow::actualAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalVariance = totalBudgeted.subtract(totalActual);
    double totalPercentUsed = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
            ? totalActual.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudgeted, 2, RoundingMode.HALF_UP)
                    .doubleValue()
            : 0.0;
    return new BudgetPlanTotals(totalBudgeted, totalActual, totalVariance, totalPercentUsed);
}
```

**Option B — All-category totals**: Sum ALL rows for both `totalBudgeted` and `totalActual`. Unbudgeted categories contribute 0 to budgeted total and their actual to actual total. This is a valid "full picture" view but must be clearly communicated to the user in the UI.

---

### Issue 4 — MAJOR: Variance Semantics Are Inverted for Income

**File**: `BudgetPlanQueryService.java`, line 82

**The problem**: Both income and expense rows use the same formula: `variance = budgetedAmount - actual`.

- For **expenses**: `variance = budget - spent`. Positive = under budget (good). This is correct.
- For **income**: `variance = budget - actual`. Positive means you earned LESS than expected (bad). Negative means you earned MORE than expected (good). The sign convention is backwards from what a user expects.

In personal finance, income variance should be reported as `actual - budget` so that a positive number means "I earned more than planned" (favourable). Using the same `budget - actual` formula for income produces a number that reads backwards and will confuse users or require the UI to compensate — which is a domain-layer responsibility leak.

**Recommended fix** — pass the row type into `buildRow` and compute variance accordingly:
```java
// In buildRow():
BigDecimal variance = isIncome
        ? actual.subtract(budgetedAmount)   // positive = earned more than planned (good)
        : budgetedAmount.subtract(actual);  // positive = spent less than planned (good)
```

And update `BudgetPlanCategoryRow` variance field name to `varianceAmount` (already done) but ensure the API response documents that the sign convention differs between income and expense rows.

Also update `computeTotals` to accept an `isIncome` flag and apply the same directional fix to `totalVariance`.

---

### Issue 5 — MINOR: Currency Defaults to "USD" When No Budgets Exist

**File**: `BudgetPlanQueryService.java`, lines 44–47

**The problem**: When a user has not yet set any budgets, `activeBudgets` is empty and the currency defaults to the hardcoded string `"USD"`. This is wrong for non-USD users.

**Recommended fix**: Inject the user's `preferred_currency` from the User domain context and use it as the default. The `GetBudgetPlanQuery` use case should accept the user's preferred currency as an input parameter or the service should call a `UserPreferencePort` to look it up.

```java
// Preferred: pass preferred currency into the query
String currency = activeBudgets.stream()
        .findFirst()
        .map(b -> b.getAmount().currency())
        .orElse(preferredCurrency);  // resolved from user profile, not hardcoded
```

---

### Issue 6 — MINOR: percentUsed Returns 0.0 When budgetedAmount = 0

**File**: `BudgetPlanQueryService.java`, lines 83–87

**The behaviour**: When a row has no budget (`budgetedAmount = 0` because no budget is set), `percentUsed` is 0.0 even if `actualAmount > 0`. This suppresses meaningful information.

**Financial assessment**: Returning 0.0 is defensively correct in that it avoids a divide-by-zero. However, it is misleading for unbudgeted rows that have actual spending — the user cannot tell from `percentUsed = 0` whether they have no spending or simply no budget. A `null` value (or `Double` instead of `double`) would be semantically more accurate: "percentage is undefined when no budget exists." The UI can then render "N/A" rather than "0%".

**Recommended fix** (low priority — UI can work around it, but cleaner domain model):
- Change `percentUsed` field in `BudgetPlanCategoryRow` from `double` to `Double` (nullable).
- Return `null` when `budgetedAmount == 0`, `0.0` only when budget is set but actual is 0.

---

### Issue 7 — MINOR: Rollover Not Applied to Budget Plan

**File**: `BudgetPlanQueryService.java`

**The behaviour**: `BudgetPlanQueryService` uses `budget.getAmount().amount()` directly as `budgetedAmount`. `BudgetQueryService` adds rollover to compute `effectiveBudget`. The two views therefore show different budgeted figures for the same budget when rollover is enabled.

**Financial assessment**: This is a design gap, not a calculation error. It may be intentional (the plan shows the base budget, the detail view shows effective budget). However, a user who relies on rollover to manage spending across periods will see inconsistent numbers depending on which view they use. This erodes trust in the tool.

**Recommended fix**: Either:
1. Apply the same rollover logic from `BudgetQueryService` to `BudgetPlanQueryService` for non-CUSTOM budgets (preferred for correctness), or
2. Add a `rolloverAmount` column to `BudgetPlanCategoryRow` and show both base and effective budget in the plan, making the difference explicit to the user.

---

## What Is Correct

1. **Income sign storage**: Amounts are stored as positive values in the DB. The income sum query (`sumIncomeAmountByCategoryAndDateRange`) correctly filters `transactionType = 'INCOME'`, which means TRANSFER_IN is excluded. This is financially sound.

2. **Expense sign storage**: Amounts in the DB are always positive. `getSpentAmount` returns a positive figure representing money out. The `variance = budget - actual` formula for expenses correctly produces a positive number meaning "under budget."

3. **Category type separation**: Income categories and expense categories are filtered via `typeCode` before building rows. This prevents an expense category from appearing in the income section and vice versa. The data model supports this correctly.

4. **Port/adapter layering**: The `SpendingQueryPort` outbound port is correctly defined in the budget domain. The `TransactionSpendingAdapter` implements it by delegating to `GetSpendingTotalsQuery` in the transaction domain — this respects the cross-context communication rule (no direct domain imports across contexts).

5. **BigDecimal arithmetic**: All calculations use `BigDecimal` with `RoundingMode.HALF_UP` and scale 2. No floating-point precision issues in the core calculations. The `double percentUsed` field is acceptable for display purposes.

6. **Null safety on budget**: `budget != null ? budget.getAmount().amount() : BigDecimal.ZERO` correctly handles the unbudgeted case without NPE.

7. **Hexagonal architecture compliance**: `BudgetPlanQueryService` is pure Java (no Spring/JPA imports). It is wired via `@Bean` in `BudgetConfig`. Domain records (`BudgetPlanCategoryRow`, `BudgetPlanTotals`, `BudgetPlanView`) are clean Java records. Architecture rules are respected.

---

## Priority Fix Summary

| # | Issue | Severity | File | Action |
|---|---|---|---|---|
| 2 | TRANSFER_OUT inflates expense actuals | Major | `TransactionJpaRepository.java` | Change `IN ('EXPENSE','TRANSFER_OUT')` to `= 'EXPENSE'` in `sumExpenseAmount` |
| 3 | totalActual includes unbudgeted rows; totalBudgeted does not | Major | `BudgetPlanQueryService.java` | Filter both budgeted+actual totals to `hasBudget=true` rows (Option A) |
| 4 | Income variance sign is inverted | Major | `BudgetPlanQueryService.java` | Use `actual - budgeted` for income rows |
| 5 | Currency defaults to hardcoded "USD" | Minor | `BudgetPlanQueryService.java` | Source default from user preferred_currency |
| 6 | percentUsed = 0 when no budget (ambiguous) | Minor | `BudgetPlanCategoryRow.java` | Change `double` to nullable `Double`, return null when no budget |
| 7 | Rollover not applied in plan view | Minor | `BudgetPlanQueryService.java` | Document as intentional or port rollover logic from `BudgetQueryService` |

**Why:** These are financial correctness issues that directly affect what numbers users see and act on. Issue 4 (variance sign inversion for income) and Issue 3 (totals asymmetry) will cause the most visible user confusion.
