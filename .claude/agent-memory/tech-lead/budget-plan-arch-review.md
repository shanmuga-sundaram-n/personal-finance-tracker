# Architectural Review: Monthly Budget Plan Feature
**Date**: 2026-03-19
**Reviewer**: Tech Lead
**Scope**: `budget/` domain + `transaction/` additions for GET /api/v1/budgets/plan

---

## Summary

The feature is largely well-structured and follows hexagonal conventions correctly in most respects. Domain purity is clean. Cross-context communication is routed correctly through ports. However, there are three issues that require immediate action before this merges: a Critical query bug that will silently return empty data for most users, a Critical missing port method in SpendingQueryPort, and a Critical test gap on the new endpoint. Several Major issues require follow-up work.

---

## Violations

### CRITICAL — V-001: BudgetJpaRepository date range query uses exact equality, not overlap
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/outbound/persistence/BudgetJpaRepository.java`, line 20–21

```java
@Query("SELECT b FROM BudgetJpaEntity b WHERE b.userId = :userId AND b.isActive = true
        AND b.startDate = :startDate AND b.endDate = :endDate")
```

The query uses `startDate = :startDate AND endDate = :endDate` — exact equality on both ends. This means the plan endpoint only returns budgets whose exact start and end date match the query parameters to the day. Any budget with a different start/end date (e.g., a monthly budget that started on the 1st when the user queries for a partial range) will be silently excluded. The correct semantics for "give me budgets active within a date range" requires an overlap check:

```
b.startDate <= :endDate AND b.endDate >= :startDate
```

Without this fix, a user with 10 MONTHLY budgets will see zero rows returned by `findActiveByUserAndDateRange` unless they pass the exact start/end date of a budget record. This is not detectable via unit tests against a mock — it requires a repository integration test. This bug must be fixed before the feature ships.

**Action**: Fix the JPQL query in `BudgetJpaRepository` line 20.

---

### CRITICAL — V-002: SpendingQueryPort missing batch method — N+1 is not just performance, it's a design contract gap
**Files**:
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/port/outbound/SpendingQueryPort.java`
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/service/BudgetPlanQueryService.java`, lines 56–61

`BudgetPlanQueryService.getBudgetPlan()` calls `spendingQueryPort.getSpentAmount()` and `getIncomeAmount()` in a per-category loop. For a user with 20 categories, that is 40 database round-trips per page load. Each call executes a separate JPQL aggregate query against `TransactionJpaRepository`.

This is a Critical issue for two reasons:
1. **Performance**: 40 queries per request is unacceptable at any realistic user scale. The data needed (sum by category, in a date range) is trivially expressible as a single GROUP BY query.
2. **Port contract is wrong**: `SpendingQueryPort` exposes a per-category API, which forces any implementation to either fan out (N queries) or implement in-memory aggregation of a full dataset. Neither is acceptable. The port should expose a batch method.

The `TransactionJpaRepository` already has `sumByCategory` (line 126–142) which returns a list of category totals in one query. The plumbing to expose this through the transaction domain and into the budget context via a batch port method is the right fix, not calling individual totals per category.

**Required design**:
- Add `Map<CategoryId, BigDecimal> getSpentAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to)` to `SpendingQueryPort`.
- Add the equivalent for income.
- Implement in `TransactionSpendingAdapter` using `GetSpendingTotalsQuery` (or add a batch method to `GetSpendingTotalsQuery`).
- Rewrite `BudgetPlanQueryService.getBudgetPlan()` to call the batch port once, then look up by `CategoryId` from the map.

This is flagged Critical because the per-category loop will execute even in development where it is invisible, and will cause visible latency and DB connection pressure in production as the user's category list grows.

---

### CRITICAL — V-003: No test coverage for GET /api/v1/budgets/plan
**File**: `application/src/test/java/com/shan/cyber/tech/financetracker/budget/adapter/inbound/web/BudgetControllerTest.java`

`GetBudgetPlanQuery` is mocked as `@MockBean` (line 45) and the controller constructor wires it correctly (lines 50, 60), but there is **zero test** exercising the `GET /api/v1/budgets/plan` endpoint. The test file covers `list`, `create`, `getById`, `update`, and `delete` — not `plan`.

Missing test scenarios:
1. Happy path: returns 200 with correct JSON shape (startDate, endDate, currency, incomeRows, expenseRows, incomeTotals, expenseTotals).
2. Date validation: what happens with `startDate` after `endDate` (should this be 400 or silently handled?).
3. Missing required params: `startDate` or `endDate` absent — should be 400.
4. Empty plan: user has no budgets in range — should return 200 with empty rows and zero totals, not 404/500.

This must be added before merging. The mock infrastructure is already present; only the test methods are missing.

---

### MAJOR — V-004: `CategoryNameAdapter` imports `CategoryView` from category context inbound port
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/outbound/CategoryNameAdapter.java`, line 5

```java
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
```

This import is at the **adapter layer**, not the domain layer — so it does not violate domain purity. The adapter is permitted to call across contexts. However, the dependency direction is worth noting: `CategoryNameAdapter` (in `budget/adapter/outbound/`) directly depends on `GetCategoriesQuery` (in `category/domain/port/inbound/`). This means the `budget` adapter layer has a compile-time dependency on the `category` domain's inbound port interface.

This is the intended cross-context ACL pattern for the adapter zone (the adapter calls the other context's inbound port, not its domain model directly — `CategoryView` is a DTO-like record, not a domain class). The boundary is correct: no `category/domain/model/` types enter the `budget` domain.

**However**: the adapter also creates `CategoryId` from `cv.id()` (line 29), which is fine since `CategoryId` lives in `shared/domain/model/`. The mapping from `CategoryView` to `CategorySummary` happens inside the adapter, which is correct.

**Verdict**: The pattern is architecturally correct. The concern is that `GetCategoriesQuery` is an inbound port — it was designed as the entry point for the category REST layer, not as an ACL interface for cross-context reads. The cleaner approach (low priority) is to define a dedicated cross-context port in `category/adapter/outbound/crosscontext/` and expose a narrower query — but at this scale and team size, calling `GetCategoriesQuery` directly from the budget adapter is an acceptable pragmatic choice. Document this as a known shortcut.

---

### MAJOR — V-005: `CategorySummary` placement — belongs in `budget/domain/port/outbound/` but the `typeCode` string is stringly typed
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/port/outbound/CategorySummary.java`

The placement of `CategorySummary` in `budget/domain/port/outbound/` is **correct** per the architecture rules. It is the data contract between the budget domain and its outbound cross-context port (`CategoryNameQueryPort`). It does not belong in `shared/domain/model/` — that package is reserved for typed IDs, VOs, and domain events. `CategorySummary` is an ACL transfer object specific to the budget context's needs.

The structural issue is: `typeCode` is a raw `String` compared with `"INCOME"` and `"EXPENSE"` literals in `BudgetPlanQueryService` lines 50 and 53:

```java
.filter(c -> "INCOME".equals(c.typeCode()))
.filter(c -> "EXPENSE".equals(c.typeCode()))
```

If the category type code ever changes, or a new type is added (e.g., `"INVESTMENT"`), these comparisons will silently fail. The `typeCode` field on `CategorySummary` should be typed as an enum or at minimum compared against a shared constant. Since `CategorySummary` lives in the budget domain and must not import from category, the enum would need to be defined in `budget/domain/` or `shared/domain/model/` (only if it becomes a genuine shared concern). For now, extract the string literals to named constants in `BudgetPlanQueryService`.

---

### MAJOR — V-006: `@Transactional(readOnly=true)` not applied on read path — transaction-demarcation gap
**Files**:
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/service/BudgetPlanQueryService.java`
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/outbound/persistence/BudgetPersistenceAdapter.java`
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/outbound/CategoryNameAdapter.java`
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/outbound/TransactionSpendingAdapter.java`

The domain service is correctly free of Spring annotations. Transactional boundaries must be applied at the adapter layer. Neither `BudgetPersistenceAdapter` nor `CategoryNameAdapter` nor `TransactionSpendingAdapter` carries `@Transactional(readOnly=true)` on their query methods.

For the budget plan query, this means:
1. Each of the (currently) N+1 calls to the persistence adapters opens and closes a separate Hibernate session. With no shared transaction, the JPA persistence context is re-created per call, defeating first-level cache.
2. Without `readOnly=true`, Hibernate's dirty-checking mechanism is not disabled — adding overhead on each query session.

The correct fix: annotate the `BudgetPlanQueryService`'s public method with `@Transactional(readOnly=true)` — but since domain is pure Java, this goes on the **adapter that drives the domain service**, not the service itself. In this architecture, the `BudgetController` calls through ports; the transaction should be opened by a Spring-managed proxy wrapping the domain service. The cleanest approach is to annotate the `@Bean` factory method return type's interface method or, more practically, add a thin Spring-managed facade. Alternatively, annotate each persistence adapter method individually with `@Transactional(readOnly=true)`. This is a **documented pattern gap** in the architecture that should be resolved in the config layer.

---

### MINOR — V-007: Mapping methods in controller should be in a dedicated mapper class
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/budget/adapter/inbound/web/BudgetController.java`, lines 138–170

`BudgetController` contains three private mapping methods: `mapToPlanDto`, `mapRowDto`, `mapTotalsDto`. This follows the same pattern as the existing `toResponseDto` method (line 176), so it is consistent with the pre-existing style. However, it means the controller is doing two jobs: HTTP request/response handling and DTO transformation.

At the current scale this is fine — the mapping is simple and co-locating it in the controller avoids creating a mapper class that is only used in one place. Flag for extraction when either: (a) the controller grows beyond ~200 lines, or (b) the same mapping logic is needed in another controller or a test fixture.

**Action**: No immediate change required. Create a `BudgetPlanMapper` class in `budget/adapter/inbound/web/` if/when the controller grows.

---

### MINOR — V-008: `currency` fallback to hard-coded `"USD"` in `BudgetPlanQueryService`
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/service/BudgetPlanQueryService.java`, lines 44–47

```java
String currency = activeBudgets.stream()
        .findFirst()
        .map(b -> b.getAmount().currency())
        .orElse("USD");
```

If a user has no active budgets in the given date range, currency defaults to `"USD"`. This is incorrect for non-USD users. The currency should come from the user's `preferred_currency` setting, not a budget record (which may be absent), and certainly not a hard-coded fallback. There is a `preferred_currency` column on the `users` table.

This is Minor because the behavior only manifests when no budgets exist in range (the empty-state view). But it will frustrate non-USD users. The `GetBudgetPlanQuery` interface should accept or the `UserId` should allow resolving the user's preferred currency from a user outbound port.

---

### MINOR — V-009: `double` used for `percentUsed` — precision risk for financial display
**Files**:
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/port/inbound/BudgetPlanCategoryRow.java`, line 12
- `application/src/main/java/com/shan/cyber/tech/financetracker/budget/domain/port/inbound/BudgetPlanTotals.java`, line 9

`percentUsed` and `totalPercentUsed` are declared as `double`. While percentage-of-budget is a display value and not a monetary amount, using `double` introduces floating-point imprecision that will manifest as `40.000000000000004` in JSON responses. The `BigDecimal.doubleValue()` call in `BudgetPlanQueryService` lines 86 and 113 loses the precision that was maintained through the `divide(..., 2, HALF_UP)` call immediately before it. Declare as `BigDecimal` throughout, or keep as `BigDecimal` in the domain and convert to `double`/`String` only in the DTO layer.

---

### MINOR — V-010: `TransactionPersistencePort` imports `TransactionView` (inbound port type) into outbound port
**File**: `application/src/main/java/com/shan/cyber/tech/financetracker/transaction/domain/port/outbound/TransactionPersistencePort.java`, lines 7–9

```java
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;
```

`TransactionView` and `CategorySpending` are inbound port types used as return types on the outbound `TransactionPersistencePort`. This is a pre-existing issue (not introduced by this feature) but surfaced during review. The outbound persistence port should not depend on inbound port types — it leaks presentation concerns into the storage contract. These types should live in `transaction/domain/model/` or in a dedicated `transaction/domain/port/outbound/` data record package. Pre-existing tech debt — flag for cleanup in a dedicated refactor, not this PR.

---

## Approved Patterns

**AP-1: Domain purity — fully clean.** `BudgetPlanQueryService`, all inbound port records (`BudgetPlanView`, `BudgetPlanCategoryRow`, `BudgetPlanTotals`), and all outbound port interfaces (`SpendingQueryPort`, `CategoryNameQueryPort`, `CategorySummary`) contain zero Spring, JPA, Jackson, or Lombok imports. This is exactly correct.

**AP-2: No `@Service` on domain.** `BudgetPlanQueryService` and `SpendingTotalsQueryService` are both wired exclusively via `@Bean` factory methods in their respective `Config` classes (`BudgetConfig`, `TransactionConfig`). The pattern is applied consistently across both contexts.

**AP-3: Cross-context boundary respected at the adapter layer.** `CategoryNameAdapter` imports `GetCategoriesQuery` (category inbound port) and translates `CategoryView` → `CategorySummary` within the adapter. The budget domain only ever sees `CategorySummary`, which is defined in the budget context. No `category/domain/model/` type crosses the boundary. `TransactionSpendingAdapter` similarly imports `GetSpendingTotalsQuery` (transaction inbound port) and exposes `BigDecimal` — the budget domain is insulated from transaction domain types entirely.

**AP-4: Port naming conventions correct.** `GetBudgetPlanQuery` (inbound, query), `SpendingQueryPort` (outbound), `CategoryNameQueryPort` (outbound), `BudgetPersistencePort` (outbound) — all follow the established naming conventions.

**AP-5: `CategorySummary` correctly scoped to budget context.** Placed in `budget/domain/port/outbound/` — not leaked into `shared/domain/model/`. The record carries only what the budget domain needs: `CategoryId` (typed, from shared), `name` (String), `typeCode` (String).

**AP-6: JPA entity separation correct in budget persistence.** `BudgetPersistenceAdapter` maps through `BudgetJpaMapper`. No `@Entity` types surface to the domain layer.

**AP-7: Controller endpoint correctly structured.** `GET /api/v1/budgets/plan` with `@RequestParam` date params, `@DateTimeFormat(iso = ISO.DATE)` annotation, and `ResponseEntity` return is the correct Spring MVC pattern. Auth is resolved via `SecurityContextHolder.getCurrentUserId()` consistent with all other endpoints.

**AP-8: `SpendingTotalsQueryService` wired correctly in `TransactionConfig`.** The new domain service is added as a `@Bean` alongside the existing transaction services. No `@Service` annotation used.

---

## Action Items

| # | Severity | File | Action |
|---|---|---|---|
| A-001 | Critical | `BudgetJpaRepository.java` line 20 | Fix date-overlap query: replace `= :startDate AND endDate = :endDate` with `<= :endDate AND b.endDate >= :startDate` |
| A-002 | Critical | `SpendingQueryPort.java`, `GetSpendingTotalsQuery.java`, `TransactionSpendingAdapter.java`, `BudgetPlanQueryService.java` | Add batch sum methods; rewrite per-category loop to use single batch call |
| A-003 | Critical | `BudgetControllerTest.java` | Add tests: happy path, missing params (400), empty plan (200 with zero totals) |
| A-004 | Major | `BudgetPlanQueryService.java` lines 50, 53 | Extract `"INCOME"` / `"EXPENSE"` string literals to named constants |
| A-005 | Major | `BudgetPersistenceAdapter.java`, `CategoryNameAdapter.java`, `TransactionSpendingAdapter.java` | Add `@Transactional(readOnly=true)` to all read methods on persistence/cross-context adapters |
| A-006 | Minor | `BudgetPlanQueryService.java` line 47 | Replace `"USD"` fallback with user preferred currency from a `UserPreferencePort` |
| A-007 | Minor | `BudgetPlanCategoryRow.java` line 12, `BudgetPlanTotals.java` line 9 | Change `double percentUsed` to `BigDecimal`; remove `.doubleValue()` call in service |
| A-008 | Minor | `BudgetController.java` lines 138–170 | Defer mapper extraction until controller exceeds ~200 lines |
| A-009 | Pre-existing | `TransactionPersistencePort.java` lines 7–9 | Log as tech debt: move `TransactionView`/`CategorySpending` out of inbound port package; tackle in standalone refactor |

---

## Merge Readiness

**Do not merge until A-001, A-002, and A-003 are resolved.** A-001 is a silent correctness bug that will affect all users. A-002 is a design contract that will compound with every new category a user adds. A-003 means a regression in this endpoint will not be caught by the test suite.

A-004 through A-008 can ship as follow-up issues — they do not cause incorrect behavior in the happy path.
