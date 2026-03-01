# Coordination Brief for Tech Lead
**From**: personal-finance-analyst (Product Domain Owner)
**Date**: 2026-02-28
**Full domain reference**: `DOMAIN-OWNERSHIP.md` in this directory

This document tells you what to build from a domain/data perspective.
All architectural decisions (framework choices, caching strategy, layering) are yours to make.
Do NOT deviate from the data types, constraints, or business rules defined here without consulting the domain owner.

---

## 1. Tech Stack Confirmed (Do Not Change Without PM Approval)

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.2.2 |
| Persistence | Spring Data JPA + PostgreSQL 15.2 |
| Migrations | Liquibase (already wired in) |
| Build | Gradle (multi-module: application, database, acceptance) |
| DB Schema | `finance_tracker` |
| DB Port (local) | 49883 -> 5432 (Docker Compose) |
| DB User | `pft-app-user` |
| DB Name | `personal-finance-tracker` |

**Critical**: The `application.yaml` references `personal_finance_tracker` as the Liquibase default schema but the migration scripts create `finance_tracker`. Resolve this naming inconsistency before writing any new migrations. Domain owner recommends standardising on `finance_tracker`.

---

## 2. Entity Model & Database Schema

### 2.1 Required Tables (implement in this order — respect FK dependencies)

```
1. account_types          (reference/lookup, pre-seeded)
2. category_types         (reference/lookup, pre-seeded)
3. users                  (EXISTS — verify columns match spec)
4. sessions               (EXISTS — verify columns match spec)
5. accounts               (depends on users, account_types)
6. categories             (depends on users, category_types, self-ref)
7. transactions           (depends on users, accounts, categories, self-ref for transfer_pair_id)
8. recurring_transactions (depends on users, accounts, categories)
9. budgets                (depends on users, categories)
```

### 2.2 Primary Key Strategy
- Use BIGSERIAL (or BIGINT with a named sequence) for all user-data tables
- Use SMALLINT for reference/lookup tables (account_types, category_types, budget_period_types)
- Do NOT use UUID as PK in MVP (complicates joins and Liquibase sequences)
- Do NOT use INT — use BIGINT to avoid overflow as transaction counts grow

### 2.3 Money: Always NUMERIC(19,4)
- NEVER use FLOAT, DOUBLE, or REAL for any monetary field
- Java mapping: `BigDecimal` on all entity fields for money
- JPA annotation: `@Column(precision = 19, scale = 4)`

### 2.4 Timestamps: Always TIMESTAMPTZ
- Store all timestamps in UTC
- Never use TIMESTAMP WITHOUT TIME ZONE
- Application layer sends and receives ISO 8601 with timezone offset

### 2.5 Critical Constraints Per Table

**users**
- username: UNIQUE, NOT NULL, VARCHAR(50), CHECK constraint: `username ~ '^[a-z0-9_]{3,50}$'`
- email: UNIQUE, NOT NULL, VARCHAR(254)
- password_hash: NOT NULL, VARCHAR(255)
- is_active: BOOLEAN NOT NULL DEFAULT true
- preferred_currency: CHAR(3) NOT NULL DEFAULT 'USD'

**account_types** (pre-seeded reference — create Liquibase changeset with inserts)
```sql
INSERT INTO account_types (id, code, name, allows_negative_balance, is_liability) VALUES
(1, 'CHECKING',       'Checking Account',  true,  false),
(2, 'SAVINGS',        'Savings Account',   false, false),
(3, 'CREDIT_CARD',    'Credit Card',       true,  true),
(4, 'INVESTMENT',     'Investment',        false, false),
(5, 'LOAN',           'Loan',              true,  true),
(6, 'CASH',           'Cash',              false, false),
(7, 'DIGITAL_WALLET', 'Digital Wallet',    true,  false);
```

**accounts**
- current_balance: computed field, updated atomically on every transaction write
- UNIQUE constraint: (user_id, LOWER(name)) — case-insensitive uniqueness
- Soft-delete: is_active = false, never hard-delete
- Currency validation: must match user.preferred_currency (enforce in application layer for MVP)

**categories**
- Hierarchy depth check: enforce in application layer — if parent_category_id is set, verify parent has no parent itself
- UNIQUE constraint: (user_id, parent_category_id, LOWER(name))
- System categories: user_id IS NULL, is_system = true — seed with Liquibase
- category_type consistency: child must equal parent's category_type_id (check in application layer)

**transactions**
- amount: NUMERIC(19,4), CHECK (amount > 0) — always positive, sign is implicit from transaction_type
- transaction_type: VARCHAR(20) with DB CHECK IN ('INCOME', 'EXPENSE', 'TRANSFER_IN', 'TRANSFER_OUT')
- transfer_pair_id: self-referencing FK; both legs created in one DB transaction
- Index requirements:
  ```sql
  CREATE INDEX idx_transactions_user_date   ON transactions(user_id, transaction_date DESC);
  CREATE INDEX idx_transactions_account     ON transactions(account_id);
  CREATE INDEX idx_transactions_category    ON transactions(category_id);
  CREATE INDEX idx_transactions_user_cat_date ON transactions(user_id, category_id, transaction_date);
  ```

**budgets**
- UNIQUE partial index: `CREATE UNIQUE INDEX idx_budget_unique_active ON budgets(user_id, category_id, period_type) WHERE is_active = true;`
- period_type: CHECK IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUALLY', 'CUSTOM')
- amount: CHECK (amount > 0)
- alert_threshold_pct: CHECK (alert_threshold_pct BETWEEN 1 AND 100)

---

## 3. Business Logic That Must Live in the Service Layer

### 3.1 Balance Recalculation
When a transaction is created, updated, or deleted:
```java
// Pseudocode
BigDecimal signedAmount = (txType == INCOME || txType == TRANSFER_IN) ? amount : amount.negate();
account.setCurrentBalance(account.getCurrentBalance().add(signedAmount));
```
This must happen within the SAME database transaction as the insert/update/delete.

For editing: reverse old effect first, then apply new effect.

### 3.2 Transfer Atomicity
Transfer creation must:
1. Begin one DB transaction
2. Insert TRANSFER_OUT row for source account
3. Insert TRANSFER_IN row for destination account
4. Set transfer_pair_id on both rows to point to the other row's id (update after insert, or use a generated UUID approach)
5. Update source account balance (decrease)
6. Update destination account balance (increase)
7. Commit — or rollback all on any failure

### 3.3 Budget Spent Calculation (Read-Only — Do Not Store)
```sql
SELECT COALESCE(SUM(t.amount), 0) AS spent
FROM transactions t
JOIN categories c ON c.id = t.category_id
WHERE t.user_id = :userId
  AND t.transaction_type = 'EXPENSE'
  AND t.transaction_date BETWEEN :periodStart AND :periodEnd
  AND c.id IN (
    SELECT id FROM categories
    WHERE id = :budgetCategoryId
       OR parent_category_id = :budgetCategoryId
  );
```

### 3.4 SAVINGS/CASH Account Overdraft Prevention
Before persisting any EXPENSE or TRANSFER_OUT on a SAVINGS or CASH account:
```java
if (!accountType.isAllowsNegativeBalance()) {
    BigDecimal newBalance = account.getCurrentBalance().subtract(transaction.getAmount());
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
        throw new InsufficientBalanceException(account.getId(), newBalance);
    }
}
```

### 3.5 Category Hierarchy Validation (Application Layer)
When creating a category with a parent_category_id:
1. Fetch the parent category
2. If parent has its own parent_category_id set, reject with HTTP 422: "Category hierarchy cannot exceed 2 levels"
3. If parent.category_type_id != new_category.category_type_id, reject with HTTP 422: "Child category type must match parent category type"

---

## 4. Authentication Architecture

### Session Flow
```
POST /api/v1/auth/register  -> creates user, returns 201
POST /api/v1/auth/login     -> creates session, returns { token, expiresAt }
POST /api/v1/auth/logout    -> deletes session, returns 204
GET  /api/v1/auth/me        -> returns current user profile
```

### Token Validation (every secured request)
1. Extract token from `Authorization: Bearer {token}` header
2. Look up sessions WHERE token = :token AND expires_at > NOW()
3. If not found: return 401
4. Attach user_id to request context (ThreadLocal or SecurityContextHolder)
5. All subsequent queries filter by user_id from context (never trust client-supplied user_id)

### Security Critical Requirements
- Never accept user_id from request body for security-sensitive operations
- All resource queries must append `AND user_id = :authenticatedUserId`
- Return 404 (not 403) when a user tries to access another user's resource

---

## 5. API Versioning & URL Structure

All endpoints under `/api/v1/`:

```
/api/v1/auth/register
/api/v1/auth/login
/api/v1/auth/logout
/api/v1/auth/me

/api/v1/accounts
/api/v1/accounts/{id}
/api/v1/accounts/{id}/transactions

/api/v1/categories
/api/v1/categories/{id}

/api/v1/transactions
/api/v1/transactions/{id}

/api/v1/transfers
/api/v1/transfers/{id}

/api/v1/budgets
/api/v1/budgets/{id}
/api/v1/budgets/{id}/summary

/api/v1/dashboard/summary

/api/v1/recurring-transactions
/api/v1/recurring-transactions/{id}
```

**Note**: The existing `ManageExpense.java` at `/expense/list` is a placeholder. It should be replaced, not extended.

---

## 6. Existing Code Issues to Address

1. **Schema name conflict**: `application.yaml` says `personal_finance_tracker`, migrations create `finance_tracker`. Pick one (`finance_tracker`) and fix both.
2. **OpenAPI spec**: The draft uses `type: number` for money fields. Must be changed to `type: string, format: decimal` or use a pattern to avoid float precision issues in JSON serialization.
3. **ID types**: The draft SQL uses `INT PRIMARY KEY` — must be changed to `BIGINT` with sequences.
4. **Missing `ON UPDATE CURRENT_TIMESTAMP`**: PostgreSQL does not support this MySQL syntax. Use triggers or application-layer timestamp management.
5. **Package structure**: Current code is under `com.shan.cyber.tech` — controller at `com.shan.cyber.tech.personal.finance.tracker`. Establish a clear package convention (e.g., `com.shan.cyber.tech.financialtracker.{domain}.{layer}`).

---

## 7. Liquibase Migration Ordering

Suggested changeset file naming:
```
001_create_finance_tracker_schema.yml        (EXISTS — fix schema name)
002_create_users_and_sessions_tables.yml     (EXISTS — add missing columns)
003_create_reference_tables.yml              (NEW: account_types, category_types)
004_create_accounts_table.yml                (NEW)
005_create_categories_table.yml              (NEW: with self-ref FK)
006_seed_system_categories.yml               (NEW: insert all system categories)
007_create_transactions_table.yml            (NEW: with self-ref transfer_pair_id)
008_create_recurring_transactions_table.yml  (NEW)
009_create_budgets_table.yml                 (NEW)
010_create_indexes.yml                       (NEW: all performance indexes)
```

---

## 8. Non-Negotiable Implementation Rules

1. Money = BigDecimal, always. Zero tolerance for float/double.
2. All financial writes are atomic — begin/commit/rollback, never partial updates.
3. current_balance is never directly user-settable via API.
4. All list queries are user-scoped — never return another user's data.
5. Soft-delete only for: users, accounts, categories, budgets, recurring_transactions.
6. Transactions are never deleted in production (soft-delete with is_deleted flag if needed; this will be discussed in Phase 2).
