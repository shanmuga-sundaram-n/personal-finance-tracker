# Coordination Brief for QA Tester
**From**: personal-finance-analyst (Product Domain Owner)
**Date**: 2026-02-28
**Full domain reference**: `DOMAIN-OWNERSHIP.md` in this directory

This document defines every critical business rule that MUST have test coverage, all edge cases to validate, and data integrity rules that cannot be compromised. These are non-negotiable correctness requirements — financial software bugs have direct monetary consequences.

---

## 1. Critical Business Rules That MUST Be Tested

### Rule Set 1: Money Arithmetic Precision

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R1-001 | NEVER use float | Store amount = 0.1 + 0.2 | Must equal 0.30, not 0.30000000000000004 |
| R1-002 | NUMERIC(19,4) precision | Store amount = 1234567890.1234 | Retrieved value is exactly 1234567890.1234 |
| R1-003 | Rounding on display only | amount = 10.00005, display precision 2 | Display shows 10.00, stored value retains 10.00005 |

### Rule Set 2: Account Balance Mechanics

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R2-001 | Balance updates atomically | Add $100 EXPENSE to CHECKING with $50 balance | Balance becomes -$50 (allowed for CHECKING) |
| R2-002 | SAVINGS cannot go negative | Add $100 EXPENSE to SAVINGS with $50 balance | HTTP 422 "Insufficient balance", balance unchanged |
| R2-003 | CASH cannot go negative | Add $1 EXPENSE to CASH with $0 balance | HTTP 422, balance unchanged |
| R2-004 | CHECKING can go negative | Add $100 EXPENSE to CHECKING with $0 balance | HTTP 201, balance = -$100.00 |
| R2-005 | Balance on INCOME | Add $500 INCOME to CHECKING with $100 balance | Balance becomes $600.00 |
| R2-006 | Balance reflects all transactions | Initial balance $1000, add 3 expenses of $100 each | Balance = $700.00 |
| R2-007 | Balance recalcs on edit | Edit expense from $100 to $200, CHECKING balance was $400 | Balance becomes $300.00 |
| R2-008 | Balance recalcs on delete | Delete $100 expense, CHECKING balance was $400 | Balance becomes $500.00 |
| R2-009 | Credit card EXPENSE increases balance | Add $50 EXPENSE on CREDIT_CARD with $0 balance | Balance = $50.00 (money owed increases) |
| R2-010 | Credit card INCOME (payment) decreases balance | Add $50 INCOME on CREDIT_CARD with $100 balance | Balance = $50.00 (money owed decreases) |
| R2-011 | current_balance matches ledger sum | Independently query SUM of all transactions for account | Must equal account.current_balance exactly |

### Rule Set 3: Transfer Atomicity

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R3-001 | Two rows created | Create transfer $100 from CHECKING to SAVINGS | Exactly 2 transaction rows created |
| R3-002 | Transfer pair ID linked | Create transfer $100 | Both rows have matching transfer_pair_id pointing to each other |
| R3-003 | Both balances update | Transfer $100 from CHECKING($500) to SAVINGS($200) | CHECKING=$400, SAVINGS=$300 |
| R3-004 | SAVINGS overdraft on transfer | Transfer $100 from SAVINGS($50) to CHECKING | HTTP 422, both accounts unchanged |
| R3-005 | Same account rejected | Transfer from Account A to Account A | HTTP 422 "Source and destination must be different accounts" |
| R3-006 | Different user accounts rejected | Transfer from UserA's account to UserB's account | HTTP 422 or HTTP 404 |
| R3-007 | Delete one leg deletes both | Delete TRANSFER_OUT leg | Both rows deleted, both balances restored |
| R3-008 | Edit amount updates both legs | Edit transfer amount from $100 to $150 | Both transaction rows show $150, both balances recalculate |
| R3-009 | Transfer is atomic (failure scenario) | Simulate DB failure between the two insert statements | Zero or two rows created, never one. Both balances unchanged if zero |

### Rule Set 4: Transaction Validation

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R4-001 | Amount must be positive | POST transaction with amount = 0 | HTTP 422 "Amount must be greater than zero" |
| R4-002 | Amount must be positive | POST transaction with amount = -50 | HTTP 422 |
| R4-003 | Future date limit | POST transaction with date = today + 31 days | HTTP 422 "Date cannot be more than 30 days in the future" |
| R4-004 | Past date limit | POST transaction with date = 11 years ago | HTTP 422 "Date cannot be more than 10 years in the past" |
| R4-005 | Valid date range | POST transaction with date = 29 days in future | HTTP 201 (success) |
| R4-006 | Valid date range | POST transaction with date = today | HTTP 201 (success) |
| R4-007 | Valid date range | POST transaction with date = 9 years ago | HTTP 201 (success) |
| R4-008 | Inactive account rejected | POST transaction on deactivated account | HTTP 422 "Cannot add transactions to an inactive account" |
| R4-009 | Category type must match | POST EXPENSE transaction with INCOME category | HTTP 422 "Category type does not match transaction type" |
| R4-010 | User ownership | POST transaction with another user's account_id | HTTP 404 |
| R4-011 | Amount precision | POST amount = 10.12345 (5 decimal places) | Accepted and stored, display rounds to $10.12 |

### Rule Set 5: Budget Constraints

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R5-001 | One budget per category per period | Create two MONTHLY budgets for Groceries | Second one returns HTTP 409 |
| R5-002 | TRANSFER category cannot be budgeted | Create budget for "Account Transfer" category | HTTP 422 |
| R5-003 | Spent includes child categories | Budget on "Food", transaction on "Groceries" (child of Food) | Spent amount includes Groceries transactions |
| R5-004 | Budget does NOT block transactions | Add $200 EXPENSE when budget is $100 and already $90 spent | HTTP 201 (transaction succeeds), budget shows OVER_BUDGET |
| R5-005 | Budget status ON_TRACK | 50% of budget used | status = "ON_TRACK" |
| R5-006 | Budget status WARNING | 80% of budget used | status = "WARNING" |
| R5-007 | Budget status OVER_BUDGET | 100% or more of budget used | status = "OVER_BUDGET" |
| R5-008 | CUSTOM period requires end_date | Create CUSTOM budget without end_date | HTTP 422 |
| R5-009 | Budget amount must be positive | Create budget with amount = 0 | HTTP 422 |
| R5-010 | Alert threshold range | Create budget with alert_threshold_pct = 0 | HTTP 422 |
| R5-011 | Alert threshold range | Create budget with alert_threshold_pct = 101 | HTTP 422 |
| R5-012 | Alert threshold valid | Create budget with alert_threshold_pct = 75 | HTTP 201 |

### Rule Set 6: Category Hierarchy

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R6-001 | Max 2 levels | Create a child category under an existing child category | HTTP 422 "Category hierarchy cannot exceed 2 levels" |
| R6-002 | Type consistency | Create EXPENSE child under INCOME parent | HTTP 422 "Child category type must match parent" |
| R6-003 | Cannot delete with transactions | Delete category that has 1+ transactions | HTTP 409 "Category has associated transactions" |
| R6-004 | Cannot delete system category | DELETE /categories/1 (system category) | HTTP 403 |
| R6-005 | Cannot edit system category | PUT /categories/1 (system category) | HTTP 403 |
| R6-006 | Unique name per level per user | Create two categories named "Food" with same parent for same user | HTTP 409 |
| R6-007 | Case-insensitive unique check | Create category "food" when "Food" exists at same level | HTTP 409 |
| R6-008 | Different users can share names | Two different users each create "Groceries" | Both succeed: HTTP 201 |

### Rule Set 7: Authentication & Authorization

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R7-001 | No token = 401 | GET /api/v1/accounts without Authorization header | HTTP 401 |
| R7-002 | Expired token = 401 | GET /api/v1/accounts with expired session token | HTTP 401 |
| R7-003 | Cross-user isolation | User A requests User B's account by ID | HTTP 404 (not 403, not 200) |
| R7-004 | Cross-user transactions | User A requests User B's transaction by ID | HTTP 404 |
| R7-005 | Wrong credentials | POST /auth/login with wrong password | HTTP 401, generic message |
| R7-006 | Rate limiting | 6 failed login attempts in 5 min from same IP | 6th attempt returns HTTP 429 |
| R7-007 | Duplicate registration | Register with existing email | HTTP 409 |
| R7-008 | Duplicate registration | Register with existing username | HTTP 409 |
| R7-009 | Logout invalidates token | Logout then use same token on any endpoint | HTTP 401 |
| R7-010 | SQL injection | POST /auth/login with username = "admin' OR '1'='1" | HTTP 401 (no SQL injection) |

### Rule Set 8: Account Management

| Test ID | Rule | Test Case | Expected Result |
|---|---|---|---|
| R8-001 | Duplicate account name per user | Create two accounts named "My Checking" | HTTP 409 |
| R8-002 | Case-insensitive account name | Create "my checking" when "My Checking" exists | HTTP 409 |
| R8-003 | Maximum 20 accounts | Create 21st account | HTTP 422 "Maximum number of active accounts reached" |
| R8-004 | Cannot set current_balance via API | POST /accounts with current_balance: 999 in body | current_balance ignored; computed from initial_balance |
| R8-005 | initial_balance sets starting balance | Create account with initial_balance: 500 | account.current_balance = 500.00 |
| R8-006 | Deactivated account not in list | Deactivate account, then GET /accounts | Deactivated account absent from response |
| R8-007 | Net worth calculation | 2 ASSET accounts ($1000, $500), 1 LIABILITY account ($200 owed) | Net worth = $1300 |

---

## 2. Edge Cases to Test

### 2.1 Zero and Boundary Values

| Scenario | Expected |
|---|---|
| Amount = 0.0001 (smallest valid amount) | Accepted |
| Amount = 0.00001 (beyond precision) | Stored as 0.0000 or rejected (clarify with domain owner) |
| Amount = 9999999999999.9999 (near max NUMERIC(19,4)) | Accepted if fits in NUMERIC(19,4) |
| initial_balance = 0 | Accepted (brand new empty account) |
| initial_balance = negative number | Accepted for CREDIT_CARD and LOAN (they start with debt), rejected for asset types |
| Budget amount = 0.0001 | Accepted |
| Username = "abc" (min 3 chars) | Accepted |
| Username = "ab" (2 chars) | HTTP 422 |
| Username = exactly 50 chars | Accepted |
| Username = 51 chars | HTTP 422 |
| Description = 500 characters (max) | Accepted |
| Description = 501 characters | HTTP 422 |

### 2.2 Date Edge Cases

| Scenario | Expected |
|---|---|
| Transaction date = today | Accepted |
| Transaction date = exactly 10 years ago today | Accepted |
| Transaction date = 10 years ago minus 1 day | Rejected (HTTP 422) |
| Transaction date = exactly 30 days from today | Accepted |
| Transaction date = 31 days from today | Rejected (HTTP 422) |
| Budget start_date > end_date (CUSTOM) | HTTP 422 |
| Recurring template end_date < start_date | HTTP 422 |
| Transaction date = Feb 29 on non-leap year | HTTP 422 (invalid date) |
| Transaction date = Feb 29 on leap year (2024) | Accepted |

### 2.3 Concurrent Operations

| Scenario | Expected |
|---|---|
| Two simultaneous EXPENSE transactions on SAVINGS ($100 balance), each for $60 | Only one succeeds; second returns HTTP 422. Balance remains >= 0 |
| Two simultaneous account name create requests with same name | One succeeds (HTTP 201), one fails (HTTP 409) |
| Two simultaneous budget creates for same category+period | One succeeds, one fails (HTTP 409) |

### 2.4 Cascade and Referential Integrity

| Scenario | Expected |
|---|---|
| Deactivate account that has transactions | Succeeds; transactions are retained |
| Deactivate account that has recurring templates | Succeeds; templates are deactivated too |
| Delete user (if supported) | All their accounts, transactions, budgets, categories cascade appropriately |
| Transfer: source account deactivated after transfer creation | Existing transfer pair still visible in transaction history |

### 2.5 Pagination Edge Cases

| Scenario | Expected |
|---|---|
| GET /transactions?page=0&size=30 with 0 transactions | HTTP 200, content: [], totalElements: 0 |
| GET /transactions?page=999 (beyond available pages) | HTTP 200, content: [], page: 999, totalElements: N |
| GET /transactions?size=101 (exceeds max) | HTTP 422 or clamped to 100 |
| GET /transactions?size=0 | HTTP 422 |

---

## 3. Data Integrity Rules (Database-Level Verification)

These should be verified by integration tests that query the database directly after API calls:

1. **Balance ledger consistency**: After every transaction insert/update/delete, verify:
   `account.current_balance == account.initial_balance + SUM(signed transaction amounts)`

2. **Transfer pair symmetry**: Every row with `transfer_pair_id = X` must have a counterpart row where `transfer_pair_id` points back and:
   - Same user_id
   - Same amount
   - One is TRANSFER_OUT, the other is TRANSFER_IN

3. **Category type consistency**: No transaction of type INCOME should have a category with category_type_code = 'EXPENSE', and vice versa.

4. **User data isolation**: A query of all transactions/accounts/budgets filtered by user_id A must never return rows where the associated resource belongs to user_id B.

5. **Soft-delete completeness**: Deactivated accounts (is_active=false) must not appear in any API list response.

6. **No orphan transactions**: Every transaction.account_id must reference an existing account. Every transaction.category_id must reference an existing category. (FK constraints enforce this at DB level, but verify via API that orphan creation is impossible.)

7. **Budget uniqueness**: Query `SELECT COUNT(*) FROM budgets WHERE user_id = :uid AND category_id = :cid AND period_type = :pt AND is_active = true` — must always be <= 1.

---

## 4. API Contract Tests

### 4.1 Response Shape Validation (Must Include)

**Account response must include**:
- id, name, account_type (object with code and name), current_balance, initial_balance, currency, institution_name, account_number_last4 (masked), is_active, created_at

**Transaction response must include**:
- id, account (id + name), category (id + name + type), amount, transaction_type, transaction_date, description, merchant_name, is_reconciled, created_at

**Budget response must include**:
- id, category (id + name), period_type, amount, start_date, spent_this_period, remaining, percentage_used, budget_status (ON_TRACK/WARNING/OVER_BUDGET), is_active

**Dashboard response must include**:
- total_assets, total_liabilities, net_worth, current_month_income, current_month_expenses, current_month_net_cash_flow, top_5_expense_categories[], recent_transactions[]

### 4.2 Error Response Shape Validation

All 4xx/5xx responses must follow:
```json
{
  "status": <integer>,
  "error": "<ERROR_CODE_STRING>",
  "message": "<human readable>",
  "errors": [ { "field": "<name>", "code": "<CODE>", "message": "<msg>" } ],
  "timestamp": "<ISO8601>",
  "path": "<request path>"
}
```

- `errors` array may be empty for non-validation errors
- `timestamp` must be a valid ISO 8601 datetime with timezone
- `status` must match the HTTP status code

### 4.3 Location Header on 201 Responses

Every POST that creates a resource and returns HTTP 201 must include:
`Location: /api/v1/{resource}/{id}`

---

## 5. Security Tests

| Test | Verification |
|---|---|
| SQL injection in all string inputs | Verify parameterized queries prevent injection |
| XSS in description/merchant_name | Stored value must be returned as plain text (not rendered as HTML) |
| Password not exposed | GET /api/v1/auth/me must not include password_hash |
| Token not in response body | Login response includes token, but no other endpoint should echo it back |
| IDOR on account | Enumerate account IDs from 1-100 as User B; must get 404 for all User A accounts |
| IDOR on transactions | Same pattern |
| Large payload | POST transaction with 100KB description | HTTP 413 or clamped to max length |

---

## 6. Test Data Patterns

### Minimal viable user for testing:
```json
{
  "username": "testuser01",
  "email": "testuser01@test.example.com",
  "password": "TestPass123!",
  "firstName": "Test",
  "lastName": "User"
}
```

### Standard test accounts to create:
1. CHECKING, initial_balance: 5000.00 (primary)
2. SAVINGS, initial_balance: 10000.00
3. CREDIT_CARD, initial_balance: 2500.00 (owed)
4. CASH, initial_balance: 200.00

### Standard test categories (use system categories):
- EXPENSE: Groceries (child of Food), Dining Out (child of Food), Rent (child of Housing)
- INCOME: Salary

### Precision test amounts:
- Use $1234.56, $0.01, $9999.99 to verify rounding behaviour

---

## 7. Regression Tests After Every Feature Change

These tests must remain green regardless of what feature is being developed:

1. User can register, login, and reach dashboard
2. Account balance = initial_balance + sum of transactions (ledger check)
3. Transfer creates exactly 2 rows and updates both balances
4. SAVINGS account rejects transaction that would cause negative balance
5. User B cannot read User A's data (one specific cross-user test per resource type)
6. Budget spent calculation includes child category transactions
7. Duplicate account name for same user is rejected
8. Amount = 0 is always rejected

These 8 tests form the smoke test suite. Run them on every build.
