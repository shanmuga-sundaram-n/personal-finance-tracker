# Feature Brief: Budget Copy from Previous Month
**Date**: 2026-03-26  **Status**: COMPLETE (All phases passed)  **Complexity**: S (Small)
**Stakeholder Decisions (2026-03-26)**:
1. Scope: Current month only
2. Conflict behavior: Show confirmation dialog when current-month budgets will be overwritten. User confirms -> overwrite. User cancels -> abort. No conflicts -> copy directly without dialog.
3. Edge cases: All covered per acceptance criteria below.

## Summary
Add a "Copy from Previous Month" action to the budget plan page that copies all budget entries from the previous calendar month into the current month. The button is only available when viewing the current (active) month. When current-month budgets already exist, a confirmation dialog warns the user and allows overwrite. The API accepts an `overwriteExisting` flag in the request body.

## Domain Rules
- One active budget per category per period type (existing invariant -- copy respects this via upsert/overwrite semantics)
- TRANSFER categories cannot be budgeted (skip during copy)
- Parent categories cannot have budgets (skip during copy)
- Budget amount must be positive (inherited from source -- always valid)
- Money = BigDecimal, NUMERIC(19,4), string in JSON
- Soft-delete only (is_active = false)
- All queries scoped to authenticated user
- Spent amount is computed from transactions -- copied/overwritten budgets retain their current-month spent calculation

## Acceptance Criteria
1. **Given** the user is viewing the current month's budget plan, **When** budgets exist for the previous month, **Then** a "Copy from Previous Month" button is visible and enabled.
2. **Given** the user clicks "Copy from Previous Month" AND the current month has NO existing budgets, **When** the previous month has N active budgets, **Then** N new budget entries are created for the current month with the same category, period type, amount, rollover setting, and alert threshold as the source budgets. No dialog is shown -- copy proceeds directly.
3. **Given** the user clicks "Copy from Previous Month" AND the current month has some existing budgets, **Then** a confirmation dialog is shown listing the count of budgets that will be overwritten.
4. **Given** the user confirms the overwrite dialog, **When** `overwriteExisting: true` is sent, **Then** existing current-month budgets matching previous-month categories are updated with the previous month's amounts. New budgets are created for categories not yet in the current month.
5. **Given** the user cancels the overwrite dialog, **Then** the copy operation is aborted, no API call is made.
6. **Given** the user is viewing a past or future month (not the current month), **Then** the "Copy from Previous Month" button is NOT visible.
7. **Given** the previous month has no budgets, **When** the user clicks "Copy from Previous Month", **Then** a toast shows "No budgets to copy" and no dialog or API call is made (frontend preflight check).
8. **Given** a previous-month budget references a now-inactive category, **Then** that budget is skipped during copy (not copied), counted in `skippedCount`.
9. **Given** a previous-month budget references a TRANSFER category (defensive), **Then** that budget is skipped during copy.
10. **Given** a previous-month budget references a parent category (defensive), **Then** that budget is skipped during copy.
11. **Given** it is January, **When** the user clicks copy, **Then** the system correctly resolves the previous month as December of the previous year.
12. **Given** the user has no valid session token, **When** the API is called, **Then** the response is 401 Unauthorized.
13. **Given** the target month is NOT the current month, **When** the API is called, **Then** the response is 422 with error code `TARGET_MONTH_NOT_CURRENT`.

## Architecture

### Bounded Contexts Affected
- **Budget** (primary) -- new inbound port, domain service method, REST endpoint, frontend button + confirmation dialog
- **Category** (read-only, via existing `CategoryNameQueryPort`) -- validates source categories are active/valid

### New Ports (Inbound)

```java
// budget/domain/port/inbound/CopyBudgetsFromPreviousMonthUseCase.java
public interface CopyBudgetsFromPreviousMonthUseCase {
    CopyBudgetsResult copyFromPreviousMonth(CopyBudgetsFromPreviousMonthCommand command);
}

// budget/domain/port/inbound/CopyBudgetsFromPreviousMonthCommand.java
public record CopyBudgetsFromPreviousMonthCommand(
    UserId userId,
    int targetYear,
    int targetMonth,       // 1-12
    boolean overwriteExisting
) {}

// budget/domain/port/inbound/CopyBudgetsResult.java
public record CopyBudgetsResult(
    int copiedCount,
    int skippedCount,
    int conflictCount,
    int overwrittenCount
) {}
```

### New Ports (Outbound)
None. Existing `BudgetPersistencePort` and `CategoryNameQueryPort` are sufficient.

### Domain Services
- `BudgetCommandService.copyFromPreviousMonth(CopyBudgetsFromPreviousMonthCommand)` -- new method.
  - Constructor signature unchanged (already has `persistencePort`, `eventPublisherPort`, `categoryNameQueryPort`).
  - Logic:
    1. Validate targetMonth is current month (else throw BusinessRuleException TARGET_MONTH_NOT_CURRENT)
    2. Compute previous month (handle January -> December year-1)
    3. Fetch previous month budgets (bulk query)
    4. Fetch current month budgets (bulk query)
    5. Fetch all categories visible to user (single query)
    6. Build Set<CategoryId> of current-month budget categories
    7. For each previous-month budget:
       a. Skip if category is inactive, TRANSFER, or parent → increment skippedCount
       b. If category already has current-month budget:
          - If overwriteExisting=false: count in conflictCount (do NOT create or update)
          - If overwriteExisting=true: update existing budget's amount, rollover, alertThreshold → increment overwrittenCount
       c. Else: create new budget via Budget.create() → increment copiedCount
    8. Return CopyBudgetsResult(copiedCount, skippedCount, conflictCount, overwrittenCount)

### DB Migration
None. No schema changes.

### REST API

```
POST /api/v1/budgets/copy-from-previous-month
Authorization: Bearer {token}

Request body:
{
  "targetYear": 2026,
  "targetMonth": 3,        // 1-12
  "overwriteExisting": true
}

Response: 200 OK
{
  "copiedCount": 5,
  "skippedCount": 2,
  "conflictCount": 3,
  "overwrittenCount": 3
}

Flow:
- First call: `overwriteExisting: false` → returns copiedCount (new budgets created), skippedCount (inactive/TRANSFER/parent), conflictCount (existing budgets that would be overwritten), overwrittenCount=0.
  - If conflictCount > 0, frontend shows confirmation dialog.
  - If conflictCount = 0, copy is complete (all new budgets created).
- Second call: `overwriteExisting: true` → overwrites conflicts, returns copiedCount (new), skippedCount, conflictCount=0, overwrittenCount (updated).
```

Request DTO: `CopyBudgetsFromPreviousMonthRequestDto` (Java record, `@NotNull Integer targetYear`, `@NotNull Integer targetMonth`, `@NotNull Boolean overwriteExisting`)
Response DTO: `CopyBudgetsResultDto` (Java record, `int copiedCount`, `int skippedCount`, `int conflictCount`, `int overwrittenCount`)

### Transaction Boundaries
- `BudgetApplicationService` (config layer, `@Service @Transactional`) implements `CopyBudgetsFromPreviousMonthUseCase`. All copies happen atomically in one transaction.

### N+1 Risks & Mitigation
- **Risk**: Per-budget `findActiveByUserAndCategoryAndPeriod()` call = N+1 queries.
- **Mitigation**: Fetch ALL active budgets for both previous month and current month in two bulk queries. Build an in-memory `Set<CategoryId>` for existing current-month budgets. Check membership in O(1). Total: 2 SELECT + 1 SELECT (categories) + N INSERT/UPDATE.

## Implementation Order
```
1. CopyBudgetsFromPreviousMonthCommand (record) -- with overwriteExisting field
2. CopyBudgetsResult (record) -- with overwrittenCount field
3. CopyBudgetsFromPreviousMonthUseCase (interface)
4. BudgetCommandService.copyFromPreviousMonth() (domain logic with overwrite support)
5. BudgetApplicationService (add implements + delegation)
6. CopyBudgetsFromPreviousMonthRequestDto (web DTO)
7. CopyBudgetsResultDto (web DTO)
8. BudgetController.copyFromPreviousMonth() (REST endpoint)
9. Frontend: budgets.api.ts (new API function + preflight query)
10. Frontend: BudgetPlanPage.tsx (add button + confirmation dialog + toast handling)
```

## Implementation Checklist
- [ ] Domain: `CopyBudgetsFromPreviousMonthCommand` record (with overwriteExisting)
- [ ] Domain: `CopyBudgetsResult` record (with overwrittenCount)
- [ ] Domain: `CopyBudgetsFromPreviousMonthUseCase` interface
- [ ] Domain: `BudgetCommandService.copyFromPreviousMonth()` method (with overwrite logic)
- [ ] Config: `BudgetApplicationService` implements new use case
- [ ] Config: `BudgetConfig` -- verify @Bean unchanged (no new constructor deps)
- [ ] Web: `CopyBudgetsFromPreviousMonthRequestDto` record (with overwriteExisting)
- [ ] Web: `CopyBudgetsResultDto` record (with overwrittenCount)
- [ ] Web: `BudgetController.copyFromPreviousMonth()` endpoint
- [ ] Frontend: `budgets.api.ts` -- add `copyBudgetsFromPreviousMonth()` function
- [ ] Frontend: `BudgetPlanPage.tsx` -- add "Copy from Previous Month" button (current month only)
- [ ] Frontend: Confirmation dialog when current-month budgets exist (show count to be overwritten)
- [ ] Frontend: "No budgets to copy" toast when previous month is empty
- [ ] Unit tests: `BudgetCommandService` -- copy happy path, overwrite existing, skip inactive categories, skip TRANSFER, skip parent, empty previous month, January edge case
- [ ] Integration tests: `BudgetController` -- MockMvc for POST endpoint, 200/401/422 cases
- [ ] Accessibility review
- [ ] Security review
- [ ] Performance review
- [ ] Dependency review
- [ ] API spec updated (OpenAPI)
- [ ] Changelog entry added

## Open Questions / Conflicts
None. Domain analyst and tech-lead are aligned. Stakeholder has confirmed overwrite behavior.

## Security Considerations
- Endpoint requires valid session token (existing `SessionAuthFilter`).
- Only copies budgets belonging to the authenticated user (userId from SecurityContextHolder).
- Target month validation prevents abuse (must be current month -- cannot copy into arbitrary past/future months).
- Returns 404 for other users' resources (standard pattern).

## Performance Considerations
- 2 bulk SELECT queries (previous month + current month budgets) + 1 SELECT (categories) + N INSERT/UPDATE statements.
- N is bounded: maximum = number of leaf expense/income categories (typically 30-50 for a power user).
- No index changes needed -- existing indexes on `(user_id, is_active, start_date, end_date)` serve the queries.
