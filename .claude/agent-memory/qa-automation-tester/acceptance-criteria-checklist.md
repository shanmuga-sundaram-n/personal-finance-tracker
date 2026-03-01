# Acceptance Criteria Checklist — Definition of Done
**Author**: QA Automation Tester
**Date**: 2026-03-01
**Status**: Authoritative — a feature is NOT done until every checkbox is ticked

---

## How to Use This Checklist

1. Before marking any feature as "Done" in the backlog, verify every checkbox below.
2. Checkboxes are organized by feature and layer.
3. "Domain unit test" = test in `application/src/test/java/.../domain/`
4. "Persistence adapter test" = `@DataJpaTest` + Testcontainers in `application/src/test/java/.../adapter/outbound/persistence/`
5. "Controller test" = `@WebMvcTest` + MockMvc in `application/src/test/java/.../adapter/inbound/web/`
6. "Integration test" = full HTTP round-trip in `acceptance/src/test/java/.../integration/`
7. "Architecture test" = ArchUnit in `application/src/test/java/.../architecture/`

---

## Feature: Shared Kernel and Foundation

```
Shared Kernel — Money Value Object
  [ ] Domain unit test: Money.add() with same currency produces correct BigDecimal sum
  [ ] Domain unit test: Money.add(0.1, 0.2) equals exactly 0.30, no floating-point error
  [ ] Domain unit test: Money.add() throws when currencies differ
  [ ] Domain unit test: Money.subtract() with same currency produces correct result
  [ ] Domain unit test: Money.subtract() throws when currencies differ
  [ ] Domain unit test: Money.isNegative() returns true for negative amounts
  [ ] Domain unit test: Money.isNegative() returns false for zero
  [ ] Domain unit test: Money.isPositive() returns true for positive amounts
  [ ] Domain unit test: Money.zero() creates zero amount with given currency
  [ ] Domain unit test: Money construction rejects null amount
  [ ] Domain unit test: Money construction rejects null or blank currency
  [ ] Domain unit test: Money stores 4 decimal places internally (1234.5678 preserved)

Shared Kernel — DateRange Value Object
  [ ] Domain unit test: DateRange rejects endDate before startDate
  [ ] Domain unit test: DateRange.contains() returns true when date is in range
  [ ] Domain unit test: DateRange.contains() returns false when date is outside range
  [ ] Domain unit test: DateRange.overlaps() correctly identifies overlapping ranges
  [ ] Domain unit test: DateRange with null endDate is treated as open-ended

Shared Kernel — Typed ID Value Objects (UserId, AccountId, etc.)
  [ ] Domain unit test: Each typed ID rejects null value
  [ ] Domain unit test: Each typed ID rejects zero or negative value

Architecture Boundaries
  [ ] Architecture test: domain packages import no Spring classes
  [ ] Architecture test: domain packages import no JPA classes
  [ ] Architecture test: domain packages import no Jackson classes
  [ ] Architecture test: domain packages import no Hibernate classes
  [ ] Architecture test: controllers do not call domain services directly
  [ ] Architecture test: account domain does not import transaction domain
  [ ] Architecture test: transaction domain does not import account domain
  [ ] Architecture test: budget domain does not import transaction domain
  [ ] Architecture test: domain services are not annotated @Service
  [ ] Architecture test: domain model classes are not annotated @Entity
```

---

## Feature: F-001 — User Registration and Authentication

```
Domain Logic — IdentityCommandService
  [ ] Domain unit test: register() saves user with hashed (not plain) password
  [ ] Domain unit test: register() throws DuplicateUsernameException for existing username
  [ ] Domain unit test: register() throws DuplicateEmailException for existing email
  [ ] Domain unit test: register() publishes UserRegistered domain event
  [ ] Domain unit test: authenticate() returns LoginResult with token for valid credentials
  [ ] Domain unit test: authenticate() throws InvalidCredentialsException for wrong password
  [ ] Domain unit test: authenticate() throws InvalidCredentialsException when user not found
  [ ] Domain unit test: authenticate() throws InvalidCredentialsException for inactive user
  [ ] Domain unit test: authenticate() throws RateLimitExceededException when limit exceeded
  [ ] Domain unit test: authenticate() records failed attempt on wrong password
  [ ] Domain unit test: authenticate() resets rate limit on success
  [ ] Domain unit test: logout() calls deleteByToken on SessionPersistencePort

Persistence Adapter — SessionPersistenceAdapter
  [ ] Persistence adapter test: findValidSession() returns session for non-expired token
  [ ] Persistence adapter test: findValidSession() returns empty for expired token
  [ ] Persistence adapter test: findValidSession() returns empty for non-existent token
  [ ] Persistence adapter test: save() persists session and retrieves by token
  [ ] Persistence adapter test: deleteByToken() removes the session row
  [ ] Persistence adapter test: deleteExpiredBefore() removes only expired sessions

Persistence Adapter — UserPersistenceAdapter
  [ ] Persistence adapter test: save() persists user and findById() retrieves it
  [ ] Persistence adapter test: findByUsername() returns empty for non-existent username
  [ ] Persistence adapter test: existsByEmail() returns true for existing email
  [ ] Persistence adapter test: findByEmail() returns empty for inactive user

Controller — AuthController
  [ ] Controller test: POST /api/v1/auth/register returns 201 with UserProfileResponseDto
  [ ] Controller test: POST /api/v1/auth/register returns 201 with Location header
  [ ] Controller test: POST /api/v1/auth/register returns 422 for blank username
  [ ] Controller test: POST /api/v1/auth/register returns 422 for invalid email format
  [ ] Controller test: POST /api/v1/auth/register returns 422 for password under 8 chars
  [ ] Controller test: POST /api/v1/auth/register returns 422 with field-level errors array
  [ ] Controller test: POST /api/v1/auth/register returns 409 for duplicate username
  [ ] Controller test: POST /api/v1/auth/register returns 409 for duplicate email
  [ ] Controller test: POST /api/v1/auth/login returns 200 with token and expiresAt
  [ ] Controller test: POST /api/v1/auth/login returns 401 for wrong password
  [ ] Controller test: POST /api/v1/auth/login returns 429 when rate limit exceeded
  [ ] Controller test: POST /api/v1/auth/logout returns 204
  [ ] Controller test: GET /api/v1/auth/me returns 200 with user profile
  [ ] Controller test: GET /api/v1/auth/me response does not include password_hash

Controller — SessionAuthFilter
  [ ] Controller test: requests to /api/v1/auth/register pass through without token
  [ ] Controller test: requests to /api/v1/auth/login pass through without token
  [ ] Controller test: protected endpoint returns 401 with no Authorization header
  [ ] Controller test: protected endpoint returns 401 with invalid token format
  [ ] Controller test: protected endpoint returns 401 with expired token
  [ ] Controller test: valid token sets user ID in SecurityContextHolder
  [ ] Controller test: SecurityContextHolder is cleared after request completes

Integration Tests — Auth
  [ ] Integration test: full register -> login -> protected endpoint flow works
  [ ] Integration test: 409 returned for duplicate email on registration
  [ ] Integration test: 409 returned for duplicate username on registration
  [ ] Integration test: 401 returned for protected endpoint with no token
  [ ] Integration test: 401 returned for protected endpoint with expired token
  [ ] Integration test: 401 returned for wrong password (generic message, no field reveal)
  [ ] Integration test: 401 returned after logout using the same token
  [ ] Integration test: 429 returned after 6 failed login attempts from same IP
  [ ] Integration test: cross-user access returns 404 (not 403)
  [ ] Integration test: password_hash never appears in any API response
  [ ] Integration test: SQL injection in username returns 401, not 500
```

---

## Feature: F-002 — Account Management

```
Domain Logic — Account Aggregate
  [ ] Domain unit test: Account.debit() decreases balance for CHECKING
  [ ] Domain unit test: Account.debit() throws InsufficientFundsException for SAVINGS with insufficient balance
  [ ] Domain unit test: Account.debit() throws InsufficientFundsException for CASH at zero
  [ ] Domain unit test: Account.debit() allows negative balance for CHECKING
  [ ] Domain unit test: Account.credit() increases balance
  [ ] Domain unit test: Account.credit() decreases CREDIT_CARD balance (reduces debt)
  [ ] Domain unit test: Account.debit() increases CREDIT_CARD balance (increases debt)
  [ ] Domain unit test: Account.canDebit() returns false for SAVINGS when result would be negative
  [ ] Domain unit test: Account.canDebit() returns true for CHECKING regardless of result
  [ ] Domain unit test: Account.deactivate() sets isActive to false
  [ ] Domain unit test: Account.rename() updates the name
  [ ] Domain unit test: Account.isLiability() returns true for CREDIT_CARD and LOAN
  [ ] Domain unit test: Account.isLiability() returns false for CHECKING, SAVINGS, CASH

Domain Logic — AccountCommandService
  [ ] Domain unit test: createAccount() saves when valid (max < 20, unique name, valid type)
  [ ] Domain unit test: createAccount() throws MaxAccountsExceededException at limit 20
  [ ] Domain unit test: createAccount() throws DuplicateAccountNameException for duplicate name
  [ ] Domain unit test: createAccount() publishes AccountCreated event
  [ ] Domain unit test: applyDebit() calls Account.debit() and saves
  [ ] Domain unit test: applyDebit() throws AccountNotFoundException when account missing
  [ ] Domain unit test: applyCredit() calls Account.credit() and saves
  [ ] Domain unit test: deactivateAccount() calls Account.deactivate() and saves
  [ ] Domain unit test: deactivateAccount() throws AccountNotFoundException when not found

Persistence Adapter — AccountPersistenceAdapter
  [ ] Persistence adapter test: save() and findById() round-trip with all fields preserved
  [ ] Persistence adapter test: findById() with wrong userId returns empty
  [ ] Persistence adapter test: findActiveByOwner() returns only is_active=true accounts
  [ ] Persistence adapter test: countActiveByOwner() counts correctly
  [ ] Persistence adapter test: findByOwnerAndName() is case-insensitive
  [ ] Persistence adapter test: findByOwnerAndName() returns empty for different user's account
  [ ] Persistence adapter test: save() updates current_balance correctly

Controller — AccountController
  [ ] Controller test: POST /api/v1/accounts returns 201 with AccountResponseDto
  [ ] Controller test: POST /api/v1/accounts returns 201 with Location header
  [ ] Controller test: POST /api/v1/accounts returns 422 for blank name
  [ ] Controller test: POST /api/v1/accounts returns 422 for null accountTypeCode
  [ ] Controller test: POST /api/v1/accounts returns 422 for null initialBalance
  [ ] Controller test: POST /api/v1/accounts returns 422 for currency not 3 chars
  [ ] Controller test: POST /api/v1/accounts returns 409 for duplicate account name
  [ ] Controller test: POST /api/v1/accounts returns 422 when max 20 accounts exceeded
  [ ] Controller test: POST response includes computed currentBalance (not user-submitted)
  [ ] Controller test: GET /api/v1/accounts returns 200 with list of accounts
  [ ] Controller test: GET /api/v1/accounts/{id} returns 200 for valid id
  [ ] Controller test: GET /api/v1/accounts/{id} returns 404 when not found for user
  [ ] Controller test: PUT /api/v1/accounts/{id} returns 200 with updated account
  [ ] Controller test: DELETE /api/v1/accounts/{id} returns 204
  [ ] Controller test: GET /api/v1/accounts/net-worth returns 200 with totals

Integration Tests — Account
  [ ] Integration test: create account -> retrieve -> balance matches initialBalance
  [ ] Integration test: 409 returned for duplicate account name (case-insensitive)
  [ ] Integration test: 422 returned when creating 21st account
  [ ] Integration test: current_balance in API input is ignored; computed from initialBalance
  [ ] Integration test: deactivated account absent from GET /accounts list
  [ ] Integration test: net worth = (sum of asset balances) - (sum of liability balances)
  [ ] Integration test: 404 returned when user B accesses user A's account ID
  [ ] Integration test: account balance matches ledger sum (initial + all transactions)
  [ ] Integration test: SAVINGS account rejects expense that would go negative
  [ ] Integration test: CASH account rejects expense at zero balance
  [ ] Integration test: CHECKING account allows balance to go negative
  [ ] Integration test: balance correctly updated after transaction edit
  [ ] Integration test: balance correctly restored after transaction delete
```

---

## Feature: F-003 — Category Management

```
Domain Logic — CategoryCommandService
  [ ] Domain unit test: createCategory() saves valid top-level user category
  [ ] Domain unit test: createCategory() saves valid child category with same type as parent
  [ ] Domain unit test: createCategory() throws HierarchyDepthExceededException for 3rd level
  [ ] Domain unit test: createCategory() throws CategoryTypeMismatchException when child type differs from parent
  [ ] Domain unit test: createCategory() throws DuplicateCategoryException for duplicate name at same level per user
  [ ] Domain unit test: updateCategory() updates name, icon, color
  [ ] Domain unit test: updateCategory() throws SystemCategoryModificationException for system category
  [ ] Domain unit test: deactivateCategory() throws CategoryHasTransactionsException when category has transactions
  [ ] Domain unit test: deactivateCategory() throws SystemCategoryModificationException for system category
  [ ] Domain unit test: deactivateCategory() succeeds when category has no transactions

Persistence Adapter — CategoryPersistenceAdapter
  [ ] Persistence adapter test: findVisibleToUser() returns both system categories and user's own
  [ ] Persistence adapter test: findVisibleToUser() does not return other users' custom categories
  [ ] Persistence adapter test: findByOwnerAndParentAndName() is case-insensitive
  [ ] Persistence adapter test: findCategoryAndDescendantIds() returns parent and all children
  [ ] Persistence adapter test: hasTransactions() returns true when transactions reference category
  [ ] Persistence adapter test: hasTransactions() returns false when no transactions reference category

Controller — CategoryController
  [ ] Controller test: POST /api/v1/categories returns 201 with location header
  [ ] Controller test: POST /api/v1/categories returns 422 for blank name
  [ ] Controller test: POST /api/v1/categories returns 422 for null categoryTypeCode
  [ ] Controller test: POST /api/v1/categories returns 422 for 3rd-level hierarchy attempt
  [ ] Controller test: POST /api/v1/categories returns 422 when child type mismatches parent
  [ ] Controller test: POST /api/v1/categories returns 409 for duplicate name at same level
  [ ] Controller test: GET /api/v1/categories returns system and user categories combined
  [ ] Controller test: PUT /api/v1/categories/{id} returns 403 for system category
  [ ] Controller test: PUT /api/v1/categories/{id} returns 200 for user category
  [ ] Controller test: DELETE /api/v1/categories/{id} returns 403 for system category
  [ ] Controller test: DELETE /api/v1/categories/{id} returns 409 when category has transactions
  [ ] Controller test: DELETE /api/v1/categories/{id} returns 204 for deactivatable category

Integration Tests — Category
  [ ] Integration test: new user sees all pre-seeded system categories
  [ ] Integration test: create custom category -> visible in categories list
  [ ] Integration test: 422 returned when creating 3-level category hierarchy
  [ ] Integration test: 422 returned when child category type differs from parent type
  [ ] Integration test: 409 returned for duplicate category name at same level (case-insensitive)
  [ ] Integration test: 201 returned when two different users create category with same name
  [ ] Integration test: 403 returned when deleting system category
  [ ] Integration test: 403 returned when editing system category
  [ ] Integration test: 409 returned when deleting category that has transactions
  [ ] Integration test: deactivated category absent from new transaction category dropdown
```

---

## Feature: F-004 — Transaction Entry (Income and Expense)

```
Domain Logic — TransactionCommandService
  [ ] Domain unit test: createTransaction() succeeds with valid inputs
  [ ] Domain unit test: createTransaction() calls applyDebit on balance port for EXPENSE
  [ ] Domain unit test: createTransaction() calls applyCredit on balance port for INCOME
  [ ] Domain unit test: createTransaction() throws for date 31 days in future
  [ ] Domain unit test: createTransaction() accepts date exactly 30 days in future
  [ ] Domain unit test: createTransaction() throws for date more than 10 years in past
  [ ] Domain unit test: createTransaction() accepts date exactly 10 years in past
  [ ] Domain unit test: createTransaction() throws when category type mismatches transaction type
  [ ] Domain unit test: createTransaction() throws when category not visible to user
  [ ] Domain unit test: createTransaction() publishes TransactionCreated event
  [ ] Domain unit test: updateTransaction() reverses old balance and applies new on amount change
  [ ] Domain unit test: updateTransaction() handles account change (reverse on old, apply on new)
  [ ] Domain unit test: updateTransaction() publishes TransactionAmountChanged event
  [ ] Domain unit test: deleteTransaction() calls appropriate reverse on balance port
  [ ] Domain unit test: deleteTransaction() throws when trying to delete a transfer leg

Persistence Adapter — TransactionPersistenceAdapter
  [ ] Persistence adapter test: save() and findByIdAndOwner() round-trip
  [ ] Persistence adapter test: findByIdAndOwner() returns empty for wrong user
  [ ] Persistence adapter test: findPage() filters by date range correctly
  [ ] Persistence adapter test: findPage() filters by account id
  [ ] Persistence adapter test: findPage() filters by category id
  [ ] Persistence adapter test: findPage() filters by transaction type
  [ ] Persistence adapter test: findPage() filters by min and max amount
  [ ] Persistence adapter test: findPage() returns most recent first by default
  [ ] Persistence adapter test: findPage() respects page and size parameters
  [ ] Persistence adapter test: updateTransferPairId() correctly sets the pair link

Controller — TransactionController
  [ ] Controller test: POST /api/v1/transactions returns 201 with TransactionResponseDto
  [ ] Controller test: POST /api/v1/transactions returns 201 with Location header
  [ ] Controller test: POST /api/v1/transactions returns 422 for amount = 0
  [ ] Controller test: POST /api/v1/transactions returns 422 for negative amount
  [ ] Controller test: POST /api/v1/transactions returns 422 for null accountId
  [ ] Controller test: POST /api/v1/transactions returns 422 for null categoryId
  [ ] Controller test: POST /api/v1/transactions returns 422 for null transactionDate
  [ ] Controller test: POST response includes accountName and categoryName fields
  [ ] Controller test: GET /api/v1/transactions returns paginated response with correct shape
  [ ] Controller test: DELETE /api/v1/transactions/{id} returns 422 for transfer leg
  [ ] Controller test: DELETE /api/v1/transactions/{id} returns 204 for regular transaction

Integration Tests — Transaction
  [ ] Integration test: create expense -> account balance decreases
  [ ] Integration test: create income -> account balance increases
  [ ] Integration test: 422 for amount = 0
  [ ] Integration test: 422 for negative amount
  [ ] Integration test: 422 for transaction date 31 days in future
  [ ] Integration test: 422 for transaction date 10 years + 1 day in past
  [ ] Integration test: 201 for transaction date exactly 30 days in future
  [ ] Integration test: 201 for transaction date exactly 10 years in past
  [ ] Integration test: 201 for transaction date = today
  [ ] Integration test: 422 when SAVINGS account expense would go negative
  [ ] Integration test: 422 when CASH account at zero gets expense
  [ ] Integration test: 422 when account is inactive
  [ ] Integration test: 422 when INCOME category used with EXPENSE transaction type
  [ ] Integration test: 404 when account belongs to another user
  [ ] Integration test: amount stored with 4 decimal precision
  [ ] Integration test: balance correctly updated after amount edit
  [ ] Integration test: balance correctly restored after transaction delete
  [ ] Integration test: paginated list returns correct metadata (page, size, totalElements, totalPages)
  [ ] Integration test: empty content returned when page number exceeds available data
  [ ] Integration test: filter by date range returns only matching transactions
  [ ] Integration test: filter by account returns only that account's transactions
  [ ] Integration test: filter by category returns only that category's transactions
  [ ] Integration test: current_balance matches ledger sum after every create/edit/delete
```

---

## Feature: F-005 — Transfer Between Accounts

```
Domain Logic — TransferCommandService
  [ ] Domain unit test: createTransfer() produces exactly 2 Transaction domain objects
  [ ] Domain unit test: createTransfer() checks canDebit before executing
  [ ] Domain unit test: createTransfer() throws InsufficientFundsException for SAVINGS source
  [ ] Domain unit test: createTransfer() throws SameAccountTransferException for same account
  [ ] Domain unit test: createTransfer() links both legs with same transfer_pair_id
  [ ] Domain unit test: createTransfer() publishes TransferCreated event
  [ ] Domain unit test: deleteTransfer() deletes both legs and reverses both balances
  [ ] Domain unit test: updateTransfer() updates amount on both legs
  [ ] Domain unit test: updateTransfer() recalculates both account balances on amount change

Controller — TransferController
  [ ] Controller test: POST /api/v1/transfers returns 201 with both leg IDs
  [ ] Controller test: POST /api/v1/transfers returns 422 for same from and to account
  [ ] Controller test: POST /api/v1/transfers returns 422 for amount = 0
  [ ] Controller test: POST /api/v1/transfers returns 422 for null fromAccountId
  [ ] Controller test: POST /api/v1/transfers returns 422 for null toAccountId
  [ ] Controller test: DELETE /api/v1/transfers/{id} returns 204
  [ ] Controller test: PUT /api/v1/transfers/{id} returns 200 with updated amounts

Integration Tests — Transfer
  [ ] Integration test: create transfer -> exactly 2 transaction rows in database
  [ ] Integration test: both legs share same transfer_pair_id and reference each other
  [ ] Integration test: source account balance decreases and destination increases
  [ ] Integration test: 422 when SAVINGS source account would go negative
  [ ] Integration test: 422 when from and to account are the same
  [ ] Integration test: 422 or 404 when destination account belongs to a different user
  [ ] Integration test: deleting TRANSFER_OUT leg also deletes TRANSFER_IN leg
  [ ] Integration test: deleting TRANSFER_IN leg also deletes TRANSFER_OUT leg
  [ ] Integration test: both account balances restored after transfer delete
  [ ] Integration test: editing transfer amount updates both legs and both balances
  [ ] Integration test: transfer pair symmetry verified in database (same amount, opposite types)
  [ ] Integration test: transfer does not affect budget spend calculation
```

---

## Feature: F-006 — Dashboard and Net Worth

```
Persistence Adapter — DashboardReadAdapter
  [ ] Persistence adapter test: getNetWorth() sums assets correctly
  [ ] Persistence adapter test: getNetWorth() sums liabilities correctly
  [ ] Persistence adapter test: getNetWorth() computes net worth as assets minus liabilities
  [ ] Persistence adapter test: getMonthlyFlow() returns correct income for current month
  [ ] Persistence adapter test: getMonthlyFlow() returns correct expense for current month
  [ ] Persistence adapter test: getTopExpenseCategories() returns max 5 categories
  [ ] Persistence adapter test: getTopExpenseCategories() is scoped to current month
  [ ] Persistence adapter test: all queries are scoped to the requesting user only
  [ ] Persistence adapter test: getAccountsWithBalances() sorts by account type then name

Controller — DashboardController
  [ ] Controller test: GET /api/v1/dashboard/summary returns 200
  [ ] Controller test: response includes totalAssets, totalLiabilities, netWorth
  [ ] Controller test: response includes currentMonthIncome, currentMonthExpenses, netCashFlow
  [ ] Controller test: response includes topExpenseCategories array
  [ ] Controller test: response includes accountBalances array
  [ ] Controller test: 401 returned without auth token

Integration Tests — Dashboard
  [ ] Integration test: net worth correct with mixed asset and liability accounts
  [ ] Integration test: current month income total includes only INCOME type transactions
  [ ] Integration test: current month expense total includes only EXPENSE type transactions
  [ ] Integration test: net cash flow = income - expense
  [ ] Integration test: top 5 expense categories for current month are correct
  [ ] Integration test: dashboard returns zeroes for new user with no transactions
  [ ] Integration test: dashboard data is scoped to authenticated user only
  [ ] Integration test: accounts list sorted by type then name
```

---

## Feature: F-007 — Budget Creation and Tracking

```
Domain Logic — Budget Aggregate
  [ ] Domain unit test: evaluateStatus() returns ON_TRACK when spending < 75%
  [ ] Domain unit test: evaluateStatus() returns WARNING when 75% <= spending < 100%
  [ ] Domain unit test: evaluateStatus() returns OVER_BUDGET when spending >= 100%
  [ ] Domain unit test: isThresholdBreached() returns true when spending >= alert threshold
  [ ] Domain unit test: computePeriodRange() returns correct range for MONTHLY
  [ ] Domain unit test: computePeriodRange() returns correct range for WEEKLY
  [ ] Domain unit test: computePeriodRange() returns correct range for QUARTERLY (Q1 and Q4)
  [ ] Domain unit test: computePeriodRange() returns correct range for ANNUALLY
  [ ] Domain unit test: computePeriodRange() uses startDate and endDate directly for CUSTOM

Domain Logic — BudgetPeriodType
  [ ] Domain unit test: MONTHLY range starts on 1st, ends on last day of month
  [ ] Domain unit test: MONTHLY range handles February in leap year correctly
  [ ] Domain unit test: MONTHLY range handles February in non-leap year correctly
  [ ] Domain unit test: QUARTERLY range Q1 = Jan 1 to Mar 31
  [ ] Domain unit test: QUARTERLY range Q4 = Oct 1 to Dec 31

Domain Logic — BudgetCommandService
  [ ] Domain unit test: createBudget() saves when valid inputs
  [ ] Domain unit test: createBudget() throws InvalidBudgetCategoryException for TRANSFER category
  [ ] Domain unit test: createBudget() throws DuplicateBudgetException for duplicate active budget
  [ ] Domain unit test: createBudget() throws when CUSTOM period missing end_date
  [ ] Domain unit test: createBudget() publishes budget created event
  [ ] Domain unit test: updateBudget() updates limit and alert threshold
  [ ] Domain unit test: deactivateBudget() deactivates budget

Persistence Adapter — BudgetSpendCalculationAdapter
  [ ] Persistence adapter test: spend includes only EXPENSE transactions
  [ ] Persistence adapter test: spend includes transactions in child categories of budgeted category
  [ ] Persistence adapter test: spend excludes INCOME transactions
  [ ] Persistence adapter test: spend excludes TRANSFER transactions
  [ ] Persistence adapter test: spend only counts transactions within the date range
  [ ] Persistence adapter test: spend returns zero for period with no transactions
  [ ] Persistence adapter test: spend excludes transactions on inactive accounts

Controller — BudgetController
  [ ] Controller test: POST /api/v1/budgets returns 201 with location header
  [ ] Controller test: POST /api/v1/budgets returns 422 for amount = 0
  [ ] Controller test: POST /api/v1/budgets returns 422 for CUSTOM period without end_date
  [ ] Controller test: POST /api/v1/budgets returns 422 for alert_threshold_pct = 0
  [ ] Controller test: POST /api/v1/budgets returns 422 for alert_threshold_pct = 101
  [ ] Controller test: POST /api/v1/budgets returns 201 for alert_threshold_pct = 75
  [ ] Controller test: POST /api/v1/budgets returns 422 for TRANSFER category
  [ ] Controller test: POST /api/v1/budgets returns 409 for duplicate active budget
  [ ] Controller test: GET /api/v1/budgets returns list of active budgets
  [ ] Controller test: GET /api/v1/budgets/{id}/summary returns budget_status, spent, remaining, percentage_used

Integration Tests — Budget
  [ ] Integration test: create monthly budget -> retrieve summary with status, spent, remaining
  [ ] Integration test: 409 for second active budget for same category and period
  [ ] Integration test: 422 for TRANSFER category budget
  [ ] Integration test: 422 for CUSTOM budget without end_date
  [ ] Integration test: 422 for budget amount = 0
  [ ] Integration test: 422 for alert_threshold_pct = 0
  [ ] Integration test: 422 for alert_threshold_pct = 101
  [ ] Integration test: 201 for alert_threshold_pct = 100
  [ ] Integration test: transaction succeeds even when budget is exceeded (no blocking)
  [ ] Integration test: OVER_BUDGET status shown after budget is exceeded
  [ ] Integration test: ON_TRACK status when spending below 75%
  [ ] Integration test: WARNING status when spending between 75% and 99%
  [ ] Integration test: deactivate budget -> absent from active list
  [ ] Integration test: edit budget amount -> reflected in next summary call
  [ ] Integration test: child category transactions counted in parent category budget spend
  [ ] Integration test: transactions outside budget period not counted in spend
```

---

## Global API Contract Requirements (All Features)

```
HTTP Status Codes
  [ ] Integration test: all successful GETs return 200
  [ ] Integration test: all successful POSTs return 201 with Location header
  [ ] Integration test: all successful PUTs and PATCHes return 200
  [ ] Integration test: all successful DELETEs and deactivations return 204
  [ ] Integration test: all validation errors return 422 with errors array
  [ ] Integration test: all duplicate/unique conflicts return 409
  [ ] Integration test: all not-found responses return 404
  [ ] Integration test: all unauthorized responses return 401
  [ ] Integration test: all forbidden responses return 403

Error Response Shape (all 4xx responses)
  [ ] Integration test: error response includes "status" integer matching HTTP code
  [ ] Integration test: error response includes "error" string with error code
  [ ] Integration test: error response includes "message" human-readable string
  [ ] Integration test: error response includes "timestamp" as valid ISO 8601 with timezone
  [ ] Integration test: error response includes "path" matching request path
  [ ] Integration test: 422 validation errors include "errors" array with field, code, message
  [ ] Integration test: non-validation errors omit or empty "errors" array

Money Serialization
  [ ] Integration test: all money fields in responses are strings, not numbers
  [ ] Integration test: money strings have 4 decimal places (e.g., "1000.0000")
  [ ] Integration test: 0.1 + 0.2 stored and retrieved as exactly 0.3000, not 0.3000000000000004

Pagination Shape
  [ ] Integration test: paginated responses include content, page, size, totalElements, totalPages
  [ ] Integration test: page is 0-indexed
  [ ] Integration test: default size is 30
  [ ] Integration test: maximum size is 100

Security — Cross-User Data Isolation
  [ ] Integration test: User B cannot read User A's account (returns 404, not 200 or 403)
  [ ] Integration test: User B cannot read User A's transactions (returns 404)
  [ ] Integration test: User B cannot read User A's categories (returns 404 for custom categories)
  [ ] Integration test: User B cannot read User A's budgets (returns 404)
  [ ] Integration test: Enumerating IDs 1-100 as User B yields no User A resources
```

---

## Regression Smoke Tests (Must Stay Green Always)

```
  [ ] Smoke test: user registers, logs in, and receives a non-blank dashboard response
  [ ] Smoke test: account.current_balance equals initial_balance + SUM of signed transactions (verified via DB)
  [ ] Smoke test: transfer creates exactly 2 rows and both account balances update correctly
  [ ] Smoke test: SAVINGS account rejects expense that causes negative balance
  [ ] Smoke test: User B reading User A's account returns 404
  [ ] Smoke test: User B reading User A's transaction returns 404
  [ ] Smoke test: User B reading User A's budget returns 404
  [ ] Smoke test: child category transactions are included in parent budget spend
  [ ] Smoke test: duplicate account name for same user returns 409
  [ ] Smoke test: transaction amount = 0 returns 422 for any account type
```
