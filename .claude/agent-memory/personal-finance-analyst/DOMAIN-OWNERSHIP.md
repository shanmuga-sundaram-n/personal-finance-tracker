# Personal Finance Tracker — Product Domain Document
**Author**: personal-finance-analyst (Product Domain Owner)
**Date**: 2026-02-28
**Status**: Authoritative — all other agents build from this document

---

## 0. Project Context

| Attribute | Value |
|---|---|
| Framework | Spring Boot 3.2.2, Java 17 |
| Database | PostgreSQL 15.2 |
| Migrations | Liquibase (Gradle plugin 2.2.2) |
| Build | Gradle (multi-module: application, database, acceptance) |
| Schema | `finance_tracker` (in DB `personal-finance-tracker`) |
| Local DB Port | 49883 mapped to 5432 |
| Auth Model | Session token (token stored in `sessions` table) |

The codebase is early-stage. Implemented: `users` table, `sessions` table, one placeholder REST endpoint (`GET /expense/list`). The draft `data.sql` and OpenAPI spec define the intended model. This document formalises and extends that intent.

---

## 1. Domain Model

### 1.1 Entity Catalogue

#### Entity: User
The human who owns and manages their financial data. All data in the system is scoped to a User.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | Auto-generated via sequence |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Lowercase, alphanumeric + underscore only |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt hash, never stored in plain text |
| email | VARCHAR(254) | UNIQUE, NOT NULL | RFC 5321 max length, validated format |
| first_name | VARCHAR(100) | NOT NULL | Display name |
| last_name | VARCHAR(100) | NOT NULL | Display name |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Soft-delete flag |
| preferred_currency | CHAR(3) | NOT NULL, DEFAULT 'USD' | ISO 4217 currency code |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | Self-referencing audit |
| updated_by | BIGINT | FK -> users(id) | Self-referencing audit |

Business rules:
- Username must be 3-50 characters, letters/numbers/underscores only
- Email must be valid format and globally unique
- A User cannot be hard-deleted if they have Accounts; use is_active = false
- preferred_currency defaults to USD; MVP supports single currency per user

---

#### Entity: Session
Represents an authenticated session. Token-based, stateless on the client.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NOT NULL, ON DELETE CASCADE | |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Cryptographically random (UUID v4 or JWT) |
| expires_at | TIMESTAMPTZ | NOT NULL | Must be in the future at creation |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- A session is invalid if expires_at < NOW()
- A user may have multiple concurrent sessions (multi-device)
- Session duration: 7 days (refresh on activity); configurable
- Expired sessions must be purged periodically (scheduled job)

---

#### Entity: AccountType (Reference Data)
Enumerated list of financial account types. System-managed, not user-editable.

| Value | Behavior Description |
|---|---|
| CHECKING | Debit-based; balance can go negative (overdraft) |
| SAVINGS | Balance cannot go below zero in normal flow |
| CREDIT_CARD | Balance represents debt owed; positive balance = amount owed |
| INVESTMENT | Read-only balance updates (Phase 2); no direct transaction entry |
| LOAN | Liability; positive balance = principal outstanding |
| CASH | Physical cash held by user; cannot go negative |
| DIGITAL_WALLET | e.g., PayPal, Venmo; behaves like CHECKING |

Database: A lookup/reference table, pre-seeded by Liquibase.

| Field | Type | Constraints |
|---|---|---|
| id | SMALLINT | PK |
| code | VARCHAR(30) | UNIQUE, NOT NULL |
| name | VARCHAR(100) | NOT NULL |
| allows_negative_balance | BOOLEAN | NOT NULL |
| is_liability | BOOLEAN | NOT NULL |
| description | TEXT | NULLABLE |

---

#### Entity: Account
A financial account owned by a User. The primary container for money.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NOT NULL | Account belongs to exactly one User |
| account_type_id | SMALLINT | FK -> account_type(id), NOT NULL | |
| name | VARCHAR(100) | NOT NULL | User-defined label (e.g., "Chase Checking") |
| initial_balance | NUMERIC(19,4) | NOT NULL, DEFAULT 0 | Balance at account creation (historical seed) |
| current_balance | NUMERIC(19,4) | NOT NULL | Computed from initial_balance + all transactions |
| currency | CHAR(3) | NOT NULL, DEFAULT 'USD' | ISO 4217; MVP = must match user.preferred_currency |
| institution_name | VARCHAR(100) | NULLABLE | Bank/institution name |
| account_number_last4 | CHAR(4) | NULLABLE | Last 4 digits, for display only |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Soft-delete |
| include_in_net_worth | BOOLEAN | NOT NULL, DEFAULT true | Liabilities set to false by default |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- current_balance is ALWAYS derived: initial_balance + SUM(signed transaction amounts). It is never directly written by an API call.
- For LIABILITY accounts (CREDIT_CARD, LOAN): a positive current_balance means money is OWED. Spending increases balance; payments decrease it.
- For ASSET accounts (CHECKING, SAVINGS, CASH, DIGITAL_WALLET): spending decreases balance; income increases it.
- SAVINGS and CASH cannot have current_balance < 0 (API must reject transaction if it would violate this).
- A user may have at most 20 active accounts (MVP limit).
- Account name must be unique per user (case-insensitive).

---

#### Entity: CategoryType (Reference Data)
The transaction nature that a category belongs to.

| Code | Description |
|---|---|
| INCOME | Money coming in to the user |
| EXPENSE | Money going out of the user |
| TRANSFER | Money moving between user's own accounts |

Database: Pre-seeded reference table.

| Field | Type | Constraints |
|---|---|---|
| id | SMALLINT | PK |
| code | VARCHAR(20) | UNIQUE, NOT NULL |
| name | VARCHAR(50) | NOT NULL |

---

#### Entity: Category
Labels for classifying transactions. System provides defaults; users can add custom categories.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NULLABLE | NULL = system category (shared for all users) |
| category_type_id | SMALLINT | FK -> category_type(id), NOT NULL | INCOME, EXPENSE, or TRANSFER |
| parent_category_id | BIGINT | FK -> categories(id), NULLABLE | For subcategories (max 2 levels deep) |
| name | VARCHAR(100) | NOT NULL | |
| icon | VARCHAR(50) | NULLABLE | Icon identifier for UI |
| color | CHAR(7) | NULLABLE | Hex color code, e.g. #FF5733 |
| is_system | BOOLEAN | NOT NULL, DEFAULT false | System categories cannot be deleted |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- System categories (is_system = true, user_id = NULL) are visible to all users and cannot be modified or deleted by users.
- User-defined categories are only visible to their owner.
- Maximum category hierarchy depth: 2 levels (parent + child). A parent category cannot itself have a parent.
- A category cannot be deleted if it has any associated transactions. Use is_active = false.
- Category name must be unique per user per parent level (case-insensitive).
- TRANSFER categories cannot be used on Budget rules (transfers are not income or expense).
- Category type must be internally consistent: a child category must have the same category_type_id as its parent.

Pre-seeded System Categories (EXPENSE):
- Housing (Rent, Mortgage, HOA, Home Maintenance, Home Insurance)
- Transportation (Car Payment, Gas, Parking, Public Transit, Rideshare, Car Insurance, Car Maintenance)
- Food (Groceries, Dining Out, Coffee Shops, Takeaway)
- Utilities (Electricity, Water, Gas, Internet, Phone)
- Healthcare (Doctor, Dentist, Pharmacy, Health Insurance, Gym)
- Entertainment (Streaming, Movies, Concerts, Hobbies, Books)
- Shopping (Clothing, Electronics, Home Goods)
- Personal Care (Haircut, Beauty)
- Education (Tuition, Books, Courses)
- Travel (Flights, Hotels, Vacation)
- Pets (Food, Vet, Grooming)
- Subscriptions (Software, Memberships)
- Insurance (Life, Renters, Other)
- Taxes (Income Tax, Property Tax)
- Fees & Charges (Bank Fees, Late Fees)
- Miscellaneous

Pre-seeded System Categories (INCOME):
- Salary
- Freelance / Contract
- Investment Income (Dividends, Capital Gains)
- Rental Income
- Gifts Received
- Government Benefits
- Other Income

Pre-seeded System Categories (TRANSFER):
- Account Transfer

---

#### Entity: Transaction
A single financial event that moves money into, out of, or between accounts.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NOT NULL | Denormalized for query performance and multi-tenant security |
| account_id | BIGINT | FK -> accounts(id), NOT NULL | The primary account affected |
| category_id | BIGINT | FK -> categories(id), NOT NULL | Must match category_type: INCOME or EXPENSE |
| amount | NUMERIC(19,4) | NOT NULL, CHECK > 0 | Always stored as POSITIVE. Sign derived from category_type. |
| transaction_type | VARCHAR(20) | NOT NULL | INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT |
| transaction_date | DATE | NOT NULL | User-entered date of the real-world event |
| description | VARCHAR(500) | NULLABLE | Free text note |
| merchant_name | VARCHAR(200) | NULLABLE | Payee or payer name |
| reference_number | VARCHAR(100) | NULLABLE | Check number, confirmation code, etc. |
| is_recurring | BOOLEAN | NOT NULL, DEFAULT false | Linked to a RecurringTransaction template |
| recurring_transaction_id | BIGINT | FK -> recurring_transactions(id), NULLABLE | |
| transfer_pair_id | BIGINT | FK -> transactions(id), NULLABLE | Links the two legs of a TRANSFER |
| is_reconciled | BOOLEAN | NOT NULL, DEFAULT false | User has verified against bank statement |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- amount MUST be stored as a positive number (> 0). The system uses transaction_type to determine the effect on balance.
- EXPENSE and TRANSFER_OUT decrease the account's current_balance.
- INCOME and TRANSFER_IN increase the account's current_balance.
- A TRANSFER always creates TWO transaction rows: one TRANSFER_OUT on the source account, one TRANSFER_IN on the destination account. They share the same transfer_pair_id.
- Both legs of a TRANSFER must be for the same amount and the same user.
- Both legs of a TRANSFER must be created atomically (same DB transaction).
- transaction_date may be in the past (backdating is allowed), but not more than 10 years ago.
- transaction_date may be up to 30 days in the future (post-dating for known future bills).
- Deleting a transaction must recalculate account current_balance.
- Editing a transaction's amount or account must recalculate the affected account(s) current_balance.
- A transaction cannot be created on an inactive account.

---

#### Entity: RecurringTransaction
A template that defines a repeating financial event (e.g., monthly rent, weekly grocery budget).

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NOT NULL | |
| account_id | BIGINT | FK -> accounts(id), NOT NULL | |
| category_id | BIGINT | FK -> categories(id), NOT NULL | |
| amount | NUMERIC(19,4) | NOT NULL, CHECK > 0 | |
| transaction_type | VARCHAR(20) | NOT NULL | INCOME or EXPENSE |
| frequency | VARCHAR(20) | NOT NULL | DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY |
| start_date | DATE | NOT NULL | First occurrence date |
| end_date | DATE | NULLABLE | NULL = indefinite |
| next_due_date | DATE | NOT NULL | Computed; updated each time a transaction is generated |
| description | VARCHAR(500) | NULLABLE | |
| merchant_name | VARCHAR(200) | NULLABLE | |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| auto_post | BOOLEAN | NOT NULL, DEFAULT false | If true, auto-creates transaction on due date; if false, prompts user |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- next_due_date is computed by the system after each transaction is posted; it is never user-editable directly.
- end_date must be >= start_date if provided.
- When auto_post = false, the system generates a notification/reminder; the user manually confirms.
- Deactivating a recurring template does not delete historical transactions.

---

#### Entity: BudgetPeriod (Reference Data)
Defines the time period type for a budget.

| Code | Description |
|---|---|
| WEEKLY | 7-day rolling or calendar week |
| MONTHLY | Calendar month (default) |
| QUARTERLY | 3-month calendar period |
| ANNUALLY | Full calendar year |
| CUSTOM | User-defined start/end dates |

---

#### Entity: Budget
Defines a spending or income target for a specific category over a time period.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK, NOT NULL | |
| user_id | BIGINT | FK -> users(id), NOT NULL | |
| category_id | BIGINT | FK -> categories(id), NOT NULL | Must be EXPENSE or INCOME category |
| period_type | VARCHAR(20) | NOT NULL | WEEKLY, MONTHLY, QUARTERLY, ANNUALLY, CUSTOM |
| amount | NUMERIC(19,4) | NOT NULL, CHECK > 0 | Budget limit or income target |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NULLABLE | NULL for recurring periods (auto-renews); required for CUSTOM |
| rollover_enabled | BOOLEAN | NOT NULL, DEFAULT false | Carry unspent budget to next period |
| alert_threshold_pct | SMALLINT | NULLABLE, CHECK 1-100 | Alert when spending reaches X% of budget |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| created_by | BIGINT | FK -> users(id) | |
| updated_by | BIGINT | FK -> users(id) | |

Business rules:
- A user can have at most ONE active budget per category per time period (no overlapping budgets for same category/period).
- Budget type TRANSFER is not allowed (TRANSFER categories cannot be budgeted).
- CUSTOM period requires both start_date and end_date.
- For MONTHLY/WEEKLY/QUARTERLY/ANNUALLY, end_date is nullable; the budget auto-renews each period.
- Spent amount is computed in real-time from transactions (never stored; always queried).
- Budget overspend is allowed at the transaction level; the API returns a warning but does not block.
- alert_threshold_pct: when (spent / amount) >= threshold, system marks the budget as "alert triggered."
- Rollover: if enabled, unspent budget from previous period is added to the new period's budget amount.

---

### 1.2 Entity Relationship Diagram (Text)

```
users 1 ----< sessions (user_id)
users 1 ----< accounts (user_id)
users 1 ----< categories (user_id, nullable for system cats)
users 1 ----< transactions (user_id)
users 1 ----< budgets (user_id)
users 1 ----< recurring_transactions (user_id)

account_types 1 ----< accounts (account_type_id)

category_types 1 ----< categories (category_type_id)
categories 1 ----< categories (parent_category_id, self-ref, max 2 levels)
categories 1 ----< transactions (category_id)
categories 1 ----< budgets (category_id)
categories 1 ----< recurring_transactions (category_id)

accounts 1 ----< transactions (account_id)
accounts 1 ----< recurring_transactions (account_id)

transactions 1 ---- 1 transactions (transfer_pair_id, self-ref, for TRANSFER pairs)
recurring_transactions 1 ----< transactions (recurring_transaction_id)
```

---

## 2. Feature Backlog

### Phase 1 (MVP) — Must Have

---

#### F-001: User Registration & Authentication
**User story**: As a new user, I want to create an account and log in so that my financial data is private and secure.

**Acceptance criteria**:
- User can register with username, email, password, first_name, last_name
- Password is stored as bcrypt hash (minimum cost factor 12)
- Email must be unique; return HTTP 409 if duplicate
- Username must be unique; return HTTP 409 if duplicate
- Login returns a session token valid for 7 days
- All subsequent API calls require the session token in Authorization header
- Logout invalidates the session token
- Invalid credentials return HTTP 401 (never reveal which field was wrong)
- Rate limiting: max 5 failed login attempts per 5 minutes per IP

**Domain rules**: User entity rules, Session entity rules.

---

#### F-002: Account Management
**User story**: As a user, I want to add and manage my financial accounts so that I can track all my money in one place.

**Acceptance criteria**:
- User can create an account with: name, account_type, initial_balance, currency, institution_name (optional), account_number_last4 (optional)
- Account name must be unique per user (case-insensitive); HTTP 409 if duplicate
- User can view a list of all their active accounts
- User can view details of a single account
- User can edit account name and institution_name
- User can deactivate (soft-delete) an account
- current_balance is returned in all account responses; it is calculated, not user-entered
- API returns net_worth summary: sum of all asset account balances minus sum of all liability account balances
- User cannot create more than 20 active accounts

**Domain rules**: Account entity rules, AccountType rules.

---

#### F-003: Category Management
**User story**: As a user, I want to categorize my transactions so that I can understand where my money goes.

**Acceptance criteria**:
- System provides pre-seeded categories (INCOME, EXPENSE, TRANSFER) visible to all users
- User can create custom categories with name, category_type (INCOME or EXPENSE), optional parent_category_id, optional icon, optional color
- Custom category name is unique per user per parent level
- User can view all categories (system + their own custom)
- User can edit their own custom categories (name, icon, color only)
- User cannot edit or delete system categories; HTTP 403 if attempted
- User cannot delete a category that has transactions assigned to it; HTTP 409
- Deactivating a category hides it from new transaction entry but retains historical data
- Category hierarchy: max 2 levels (parent + child)
- Child category must have same category_type as parent; HTTP 422 if violated

**Domain rules**: Category entity rules, category hierarchy rules.

---

#### F-004: Transaction Entry (Income & Expense)
**User story**: As a user, I want to record my income and expenses so that I have an accurate picture of my financial activity.

**Acceptance criteria**:
- User can create a transaction with: account_id, category_id, amount, transaction_date, description (optional), merchant_name (optional)
- amount must be > 0; HTTP 422 if zero or negative
- transaction_date cannot be more than 10 years in the past; HTTP 422 if violated
- transaction_date cannot be more than 30 days in the future; HTTP 422 if violated
- Category type (INCOME or EXPENSE) must match the expected transaction direction
- On creation, current_balance on the linked account is updated atomically
- SAVINGS and CASH accounts: reject transaction if it would result in current_balance < 0; return HTTP 422 with clear message
- User can view a paginated list of their transactions (default: 30 per page, most recent first)
- User can filter transactions by: date range, account, category, transaction_type, amount range
- User can edit a transaction's: amount, date, description, merchant_name, category_id, account_id
- Editing amount or account recalculates affected account balances
- User can delete a transaction; balance is recalculated on delete
- All transaction list responses include: account name, category name, category type

**Domain rules**: Transaction entity rules.

---

#### F-005: Transfer Between Accounts
**User story**: As a user, I want to record transfers between my own accounts so that my balances remain accurate.

**Acceptance criteria**:
- User can create a transfer specifying: from_account_id, to_account_id, amount, transfer_date, description (optional)
- System creates two transaction rows atomically (TRANSFER_OUT and TRANSFER_IN)
- Both rows share the same transfer_pair_id
- Both legs have the same amount and transfer_date
- from_account and to_account must belong to the same user; HTTP 422 if not
- from_account and to_account cannot be the same account; HTTP 422
- SAVINGS and CASH source accounts: reject if balance would go negative
- Deleting either leg of a transfer automatically deletes the paired leg
- Editing a transfer amount updates both legs and both balances atomically
- Transfers appear in transaction list with category "Account Transfer"

**Domain rules**: Transfer rules in Transaction entity.

---

#### F-006: Dashboard / Net Worth Summary
**User story**: As a user, I want to see a summary of my financial position so that I can quickly assess my financial health.

**Acceptance criteria**:
- Dashboard returns: total assets, total liabilities, net worth (assets - liabilities)
- Dashboard returns: current month income total, current month expense total, current month net cash flow
- Dashboard returns: list of accounts with current_balance, sorted by account_type then name
- Dashboard returns: top 5 expense categories for current month with amounts
- All figures are for the authenticated user only
- Figures are real-time (no caching in MVP)

**Domain rules**: Account type (is_liability), transaction direction rules.

---

#### F-007: Budget Creation & Tracking
**User story**: As a user, I want to set spending budgets for categories so that I can control my expenses.

**Acceptance criteria**:
- User can create a budget for any EXPENSE or INCOME category
- User can specify: category, period_type (MONTHLY is default), amount, start_date, optional alert_threshold_pct
- Only one active budget per category per period type at a time; HTTP 409 if duplicate
- Budget detail view returns: budget amount, amount spent in current period, amount remaining, percentage used
- Amount spent is computed from transactions in the current period matching the category (and all child categories)
- User can view all active budgets as a list
- User can edit budget amount and alert_threshold_pct
- User can deactivate a budget
- API returns budget_status: ON_TRACK (< 75% used), WARNING (75-99%), OVER_BUDGET (>= 100%)

**Domain rules**: Budget entity rules.

---

### Phase 2 — Important, Not Blocking MVP

---

#### F-008: Recurring Transactions
**User story**: As a user, I want to set up recurring transactions so that regular income and expenses are automatically tracked.

**Acceptance criteria**:
- User can create a recurring transaction template with: account, category, amount, frequency, start_date, optional end_date, auto_post flag
- Supported frequencies: DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY
- When auto_post = true, transaction is created automatically on next_due_date
- When auto_post = false, a reminder notification is generated; user confirms and it posts
- User can view all recurring templates
- User can edit, pause, and delete recurring templates
- Deleting a template does not delete historical posted transactions
- User can view upcoming recurring transactions in a 30-day lookahead

---

#### F-009: Budget Rollover & Alerts
**User story**: As a user, I want unspent budget to roll over and receive alerts before I overspend.

**Acceptance criteria**:
- When rollover_enabled = true, unspent budget from previous period is carried forward
- Rollover amount is capped at 100% of original budget (cannot accumulate indefinitely)
- When spending reaches alert_threshold_pct, the budget record is flagged and returned with alert status
- API response includes: rollover_amount_added, effective_budget_this_period

---

#### F-010: Transaction Search & Reporting
**User story**: As a user, I want to search and export my transactions so that I can review and audit my finances.

**Acceptance criteria**:
- Full-text search on description and merchant_name
- Filter by: date range, account(s), category(s), amount range, transaction_type, is_reconciled
- Results sortable by: date (default), amount, merchant_name, category
- Monthly spending summary by category (for any specified month)
- Income vs. Expense comparison chart data (by month, for up to 12 months)
- CSV export of filtered transaction list

---

#### F-011: Transaction Reconciliation
**User story**: As a user, I want to mark transactions as reconciled so that I know my records match my bank statements.

**Acceptance criteria**:
- User can mark individual transactions as is_reconciled = true/false
- Reconciliation view shows: reconciled vs. unreconciled transactions for a date range
- Reconciled balance vs. statement balance comparison
- Bulk reconcile: mark all transactions up to a given date as reconciled

---

#### F-012: Subcategory Budgets
**User story**: As a user, I want to budget at the subcategory level so that I can be more precise.

**Acceptance criteria**:
- Budget can be assigned to a parent or child category
- Transactions in child categories count toward both the child budget and the parent budget
- API prevents creating a child budget that exceeds the parent budget

---

### Phase 3 — Nice to Have / Future

---

#### F-013: Multi-Currency Support
- Each account can have its own currency
- Transactions stored in account currency
- Exchange rates fetched from external API (e.g., Open Exchange Rates)
- Dashboard converts all balances to user.preferred_currency for net worth

#### F-014: Investment Account Tracking
- Manually log investment holdings (symbol, quantity, cost basis)
- Fetch current prices from external API
- Track unrealised gain/loss

#### F-015: Debt Payoff Planner
- Input: loan balance, interest rate, minimum payment
- Output: avalanche and snowball payoff schedules with timelines and total interest
- Scenario comparison

#### F-016: Savings Goals
- Define a goal: name, target amount, target date, linked savings account
- Track progress: amount saved, required monthly contribution, projected completion date
- Milestone alerts

#### F-017: Financial Insights & Recommendations
- Automated analysis: "Your dining spend is 40% above your 3-month average"
- Subscription detection from merchant name patterns
- Month-over-month category trend analysis

#### F-018: Bank Feed Integration (Open Banking / Plaid)
- Connect accounts via Plaid or similar
- Automatic transaction import
- Duplicate detection and user confirmation flow

#### F-019: Tax Year Reporting
- Filter income and expense totals by tax year
- Identify deductible expense categories
- Export summary for tax preparation

#### F-020: Mobile-Responsive PWA
- Progressive Web App with offline support
- Push notifications for budget alerts and recurring transaction reminders

---

## 3. Financial Domain Rules (Authoritative Reference)

### 3.1 Money Handling
- All monetary values stored as NUMERIC(19,4) — never FLOAT or DOUBLE
- Display precision: 2 decimal places for currencies that use cents (USD, EUR, GBP, etc.)
- All arithmetic performed server-side using BigDecimal in Java; never floating point
- No rounding until the final display step; intermediate calculations retain full 4 decimal precision

### 3.2 Account Balance Mechanics
- current_balance = initial_balance + SUM(signed_amounts) where:
  - INCOME and TRANSFER_IN contribute +amount
  - EXPENSE and TRANSFER_OUT contribute -amount
- Balance is never stored as a running total; it is always computable from the ledger
- For performance, current_balance IS materialized in the accounts table and updated on every transaction write
- On any balance inconsistency, the ledger (transactions) is the source of truth

### 3.3 Account Type Behavior Matrix

| Account Type | Category | Balance Effect | Can Go Negative |
|---|---|---|---|
| CHECKING | INCOME | + (increases) | YES |
| CHECKING | EXPENSE | - (decreases) | YES (overdraft) |
| SAVINGS | INCOME | + | NO |
| SAVINGS | EXPENSE | - | NO (reject) |
| CASH | INCOME | + | NO |
| CASH | EXPENSE | - | NO (reject) |
| CREDIT_CARD | EXPENSE | + (increases debt) | YES (credit line) |
| CREDIT_CARD | INCOME (payment) | - (reduces debt) | NO (overpayment blocked) |
| LOAN | INCOME (disbursement) | + | YES (interest accrual) |
| LOAN | EXPENSE (payment) | - | NO |
| DIGITAL_WALLET | INCOME | + | YES |
| DIGITAL_WALLET | EXPENSE | - | YES |

### 3.4 Transfer Rules
- Source deducted, destination credited — atomically
- Transfer amount is the same on both legs (no fee deduction in MVP)
- Transfers do not count as income or expense in budget calculations
- Net worth is not affected by transfers (only redistributes money between accounts)

### 3.5 Budget Period Logic

| Period Type | Start | End | Auto-Renew |
|---|---|---|---|
| WEEKLY | User-defined start date | Start + 6 days | YES |
| MONTHLY | 1st of month | Last day of month | YES |
| QUARTERLY | 1st of quarter | Last day of quarter | YES |
| ANNUALLY | Jan 1 | Dec 31 | YES |
| CUSTOM | User-defined | User-defined | NO |

For MONTHLY budgets: "current period" = transactions where transaction_date falls in the same calendar month/year as today.

### 3.6 Budget Spending Calculation
```
spent_this_period = SUM(amount)
  FROM transactions
  WHERE category_id IN (budget.category_id AND all descendant category_ids)
    AND transaction_type = 'EXPENSE'
    AND transaction_date BETWEEN period_start AND period_end
    AND user_id = budget.user_id
    AND account.is_active = true
```

### 3.7 Currency (MVP)
- Single currency per user (user.preferred_currency)
- All accounts must use the same currency as the user (enforced at creation)
- ISO 4217 three-letter codes only (USD, EUR, GBP, INR, etc.)

### 3.8 Audit Trail
- All entities include created_at, updated_at, created_by, updated_by
- updated_at is automatically set by DB trigger or application layer on every UPDATE
- No hard deletes in production for financial entities (User, Account, Transaction, Category, Budget)
- Soft-delete pattern: is_active = false

---

## 4. API Behavior Expectations

### 4.1 Standard HTTP Status Codes
| Scenario | Code |
|---|---|
| Successful GET | 200 |
| Successful POST (create) | 201 with Location header |
| Successful PUT/PATCH | 200 |
| Successful DELETE / deactivation | 204 |
| Validation error (field-level) | 422 with error details array |
| Business rule violation | 422 with error code and message |
| Duplicate (unique constraint) | 409 with field name |
| Not found | 404 |
| Unauthorized (no/invalid token) | 401 |
| Forbidden (token valid, wrong user) | 403 |
| Server error | 500 |

### 4.2 Error Response Format
```json
{
  "status": 422,
  "error": "VALIDATION_ERROR",
  "message": "One or more fields are invalid",
  "errors": [
    {
      "field": "amount",
      "code": "MUST_BE_POSITIVE",
      "message": "Amount must be greater than zero"
    }
  ],
  "timestamp": "2026-02-28T10:30:00Z",
  "path": "/api/v1/transactions"
}
```

### 4.3 Pagination
All list endpoints support:
- `page` (0-indexed, default 0)
- `size` (default 30, max 100)
- `sort` (field name, default varies by entity)
- `direction` (ASC or DESC)

Response wraps list in: `{ "content": [...], "page": 0, "size": 30, "totalElements": 150, "totalPages": 5 }`

### 4.4 Security
- All endpoints (except POST /auth/register and POST /auth/login) require Authorization: Bearer {token}
- Users can only access their own resources; querying another user's resource returns 404 (not 403, to prevent enumeration)
- Schema: `finance_tracker` — all tables live in this schema

---

## 5. Coordination Summary

- Tech Lead: see `brief-for-tech-lead.md`
- UX/UI Designer: see `brief-for-ux-designer.md`
- QA Tester: see `brief-for-qa-tester.md`
