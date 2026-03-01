# DDD Architecture — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: Authoritative — full-stack-dev implements this structure

---

## 1. Architecture Assessment: Where We Are vs. Where We Need to Be

The original package structure (`auth/`, `account/`, `category/`, etc. each with `controller/`, `service/`, `repository/`, `entity/`, `dto/`) is **domain-first layering** — a pragmatic improvement over pure layer-first. However, it does NOT implement DDD because:

1. **No domain layer exists**: The service classes contain both business logic AND application orchestration (use-case flow). The `Account.java` entity is a pure JPA data bag with no behaviour.
2. **No aggregate boundaries**: There is no explicit statement of what is an Aggregate Root, what consistency boundary it owns, or what cannot be accessed except through the root.
3. **No domain events**: Cross-domain side effects (e.g., TransactionCreated triggers balance update, BudgetExceeded triggers alert) are encoded as direct service-to-service calls, coupling contexts.
4. **Anemic domain model**: All logic — even simple invariants like "SAVINGS cannot go negative" — lives in service classes rather than in the entity that owns the rule.
5. **No Value Objects**: `Money` (amount + currency), `DateRange` (start + end), `AccountType` semantics, are all represented as primitives scattered across entities and services.

### Decision: Pragmatic DDD, Not Purist DDD

This is a Spring Boot monolith, not a microservices system. We apply DDD principles **within the monolith** — specifically:
- Rich domain model (entities carry invariants)
- Aggregate boundaries with explicit roots
- Value Objects for Money and DateRange
- Domain events for cross-context communication (in-process, synchronous for MVP; async-ready for Phase 2)
- Application layer for use-case orchestration
- Infrastructure layer for framework details

We do NOT introduce hexagonal architecture ports/adapters, a separate domain jar, or interface abstractions for every repository — that would be over-engineering for this scale. The boundary between domain and infrastructure is enforced by **package convention and code review**, not by build module separation.

---

## 2. Bounded Contexts

A bounded context is a logical boundary within which a domain model is internally consistent, terms have unambiguous meaning, and business rules are enforced by the aggregate(s) inside.

### 2.1 Context Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│  MONOLITH BOUNDARY (single Spring Boot application)                      │
│                                                                          │
│  ┌──────────────────┐      ┌──────────────────────┐                     │
│  │  IDENTITY &      │      │  ACCOUNT              │                     │
│  │  AUTH            │      │  MANAGEMENT           │                     │
│  │                  │      │                        │                     │
│  │  User            │      │  Account               │                     │
│  │  Session         │      │  AccountType           │                     │
│  └───────┬──────────┘      └──────────┬────────────┘                    │
│          │ UserRef (userId)            │ AccountRef (accountId)           │
│          │                            │                                  │
│  ┌───────▼──────────────────────────▼────────────────────────────────┐  │
│  │  TRANSACTION MANAGEMENT                                            │  │
│  │                                                                    │  │
│  │  Transaction  TransferPair  RecurringTransaction                   │  │
│  └───────────────────┬─────────────────┬──────────────────────────────┘  │
│                      │                  │                                 │
│  ┌───────────────────▼──────┐  ┌───────▼────────────────────────────┐   │
│  │  BUDGET MANAGEMENT       │  │  CATEGORIZATION                    │   │
│  │                          │  │                                    │   │
│  │  Budget                  │  │  Category  CategoryType            │   │
│  └──────────────────────────┘  └────────────────────────────────────┘   │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  REPORTING & DASHBOARD (read-only, cross-context queries)          │  │
│  │  DashboardQuery  NetWorthQuery  BudgetSummaryQuery                 │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  SHARED KERNEL                                                     │  │
│  │  Money (VO)  DateRange (VO)  AuditInfo (VO)  UserId (VO)          │  │
│  │  DomainEvent  PageResponse  ErrorResponse                          │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Context Boundaries — What Belongs Where

#### Identity & Auth Context (`identity/`)
**Owns**: User lifecycle, credential management, session lifecycle, authentication.
**Does NOT own**: Account creation logic, transaction data, financial rules.
**Exports**: `userId` (a Long value — contexts reference users only by ID, never by importing the User entity).

Key distinction: No other context imports `User.java` or `UserRepository`. They use `userId: Long` as a reference. This is the "Published Language" of the Identity context.

#### Account Management Context (`account/`)
**Owns**: Account creation, naming, type classification, balance state, net-worth computation.
**Does NOT own**: Transaction recording (that belongs to Transaction Management).
**Exports**: `accountId` (Long), `AccountSnapshot` (a read-only view struct used by Dashboard), `AccountBalanceUpdated` (domain event).

The `BalanceService` (currently in `transaction/`) belongs in the `account/` domain because **balance is an Account invariant**, not a Transaction concern. Transactions request balance changes; the Account enforces the constraint.

#### Categorization Context (`category/`)
**Owns**: Category hierarchy, system vs. user-defined categories, category type consistency.
**Does NOT own**: How categories are used in transactions or budgets.
**Exports**: `categoryId` (Long), `CategoryType` enum (INCOME, EXPENSE, TRANSFER).

#### Transaction Management Context (`transaction/`)
**Owns**: Transaction recording (INCOME, EXPENSE), Transfer orchestration, RecurringTransaction templates, transaction lifecycle (create/edit/delete).
**Does NOT own**: Balance enforcement (defers to Account context via `BalanceUpdater` domain service), category hierarchy rules.
**Integration points**:
- Calls `account/domain/service/BalanceDomainService` to enforce balance constraints and apply deltas
- Publishes `TransactionCreated`, `TransactionDeleted`, `TransferCreated` domain events
- Reads `categoryId` from Categorization context (ID reference only)

#### Budget Management Context (`budget/`)
**Owns**: Budget definition, budget period logic, spend calculation, alert threshold evaluation.
**Does NOT own**: Transaction data (queries it read-only via repository), category hierarchy.
**Integration points**:
- Reads transaction data via its own `BudgetSpendQuery` (a repository query — does not import Transaction domain classes)
- Listens for `TransactionCreated`/`TransactionDeleted` events to invalidate budget spend cache (Phase 2)

#### Reporting & Dashboard Context (`reporting/`)
**Owns**: Aggregated views for dashboard, net-worth summaries, monthly income/expense totals.
**Does NOT own**: Any domain entity — this context is **read-only and query-only**.
**Integration**: Queries directly against the database via JPQL/native SQL. Does not call other contexts' services; uses its own query objects for performance. This is a CQRS read model pattern.

---

## 3. DDD Building Blocks Classification

### 3.1 Aggregate Roots

An Aggregate Root is the single entry point to a cluster of related objects. External objects hold only the root's ID.

| Aggregate Root | Context | Owns |
|---|---|---|
| `Account` | Account Management | `AccountType` reference, balance invariants |
| `Transaction` | Transaction Management | Amount, date, category reference, account reference |
| `TransferPair` | Transaction Management | Two `Transaction` legs (conceptual — implemented as paired transactions) |
| `RecurringTransaction` | Transaction Management | Frequency rules, next_due_date |
| `Budget` | Budget Management | Period rules, amount, alert threshold |
| `User` | Identity & Auth | Credentials, profile |
| `Session` | Identity & Auth | Token, expiry |
| `Category` | Categorization | Hierarchy, type, system flag |

**Note on `Category`**: Category has a self-referential parent relationship. The parent IS an aggregate root itself. Children are navigated from the parent. External contexts hold only `categoryId`.

### 3.2 Entities (Non-Root, Within Aggregate)

| Entity | Aggregate It Belongs To | Notes |
|---|---|---|
| `AccountType` | `Account` aggregate | Reference data; identity by code. Accessed through Account aggregate. |
| `CategoryType` | `Category` aggregate | Reference data; identity by code. |

In our pragmatic DDD, `AccountType` and `CategoryType` are **reference data entities** — they have identity (by ID/code), are pre-seeded, and never mutated through the API. They are not independent Aggregate Roots because no use case creates or modifies them through the domain.

### 3.3 Value Objects

Value Objects have no identity — they are equal when their values are equal. They are immutable.

| Value Object | Context | Fields | Replaces |
|---|---|---|---|
| `Money` | Shared Kernel | `amount: BigDecimal`, `currency: String` | Raw `BigDecimal` + separate currency field on entities |
| `DateRange` | Shared Kernel | `startDate: LocalDate`, `endDate: LocalDate` | Raw start/end date pairs in Budget, RecurringTransaction |
| `UserId` | Shared Kernel | `value: Long` | Bare `Long userId` fields used as FK references |
| `AccountId` | Shared Kernel | `value: Long` | Bare `Long accountId` FK references |
| `CategoryId` | Shared Kernel | `value: Long` | Bare `Long categoryId` FK references |
| `BudgetId` | Shared Kernel | `value: Long` | Bare `Long budgetId` |
| `AuditInfo` | Shared Kernel | `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | Scattered audit fields |
| `SessionToken` | Identity | `value: String` | Bare `String token` |
| `PasswordHash` | Identity | `value: String` | Bare `String passwordHash` |
| `TransactionType` | Transaction | Enum: INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT | Raw String in current entity |
| `Frequency` | Transaction | Enum: DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY | Raw String |
| `BudgetPeriodType` | Budget | Enum: WEEKLY, MONTHLY, QUARTERLY, ANNUALLY, CUSTOM | Raw String |
| `BudgetStatus` | Budget | Enum: ON_TRACK, WARNING, OVER_BUDGET | Computed in service, not stored |

**Implementation note on typed IDs**: Typed ID value objects (`UserId`, `AccountId`, etc.) are a DDD best practice that eliminates "primitive obsession" and prevents passing wrong IDs. For this MVP monolith, implement them as Java records:

```java
// shared/domain/model/UserId.java
public record UserId(Long value) {
    public UserId {
        if (value == null || value <= 0) throw new IllegalArgumentException("UserId must be positive");
    }
}
```

The full-stack dev should introduce these progressively — start with `Money` and `DateRange` (highest value), then typed IDs in Phase 2 refactoring. Do NOT block MVP delivery on typed IDs.

### 3.4 Domain Events

Domain events describe something that happened in the domain. They are facts, named in past tense.

| Event | Publisher | Subscribers | Purpose |
|---|---|---|---|
| `TransactionCreated` | `Transaction` aggregate | `Account` (balance update), `Budget` (invalidate spend cache in Phase 2) | Decouple transaction recording from balance side-effects |
| `TransactionDeleted` | `Transaction` aggregate | `Account` (balance reversal), `Budget` (Phase 2) | Same |
| `TransactionAmountChanged` | `Transaction` aggregate | `Account` (re-apply delta) | On edit |
| `TransferCreated` | `TransferPair` (via TransactionService) | `Account` (debit source, credit dest) | Atomic transfer |
| `AccountDeactivated` | `Account` aggregate | `Budget` (mark orphaned budgets inactive), `RecurringTransaction` | Soft-delete cascade |
| `BudgetThresholdExceeded` | `Budget` aggregate | `Notification` context (Phase 2 — push notification) | Alert user |
| `RecurringTransactionDue` | `RecurringTransaction` aggregate | `Transaction` (auto-post if enabled), `Notification` (remind if not) | Scheduled trigger |
| `UserDeactivated` | `User` aggregate | All contexts that hold userId FK | Soft-delete cascade |

**MVP Implementation**: Domain events are published synchronously within the same transaction using Spring's `ApplicationEventPublisher`. The publisher calls `eventPublisher.publishEvent(new TransactionCreated(...))` and Spring dispatches to `@EventListener` methods in the same thread. This is not truly async but it decouples the code.

**Phase 2**: Replace `@EventListener` with `@TransactionalEventListener(phase = AFTER_COMMIT)` for events that should trigger out-of-transaction work (e.g., push notifications). For budget spend cache invalidation, this is the correct pattern.

```java
// Example: publishing
public class Transaction {
    // Rich domain method
    public TransactionCreated recordTransaction(...) {
        // Validate invariants here
        // Return the event — the application service publishes it
        return new TransactionCreated(this.id, this.accountId, this.amount, this.transactionType);
    }
}

// Application service publishes:
DomainEvent event = transaction.recordTransaction(...);
transactionRepository.save(transaction);
eventPublisher.publishEvent(event);
```

### 3.5 Repository Interfaces (Domain Layer)

Repository interfaces belong to the **domain layer**. They are abstractions. The Spring Data JPA implementation lives in the infrastructure layer.

| Repository Interface | Domain Context | Key Methods |
|---|---|---|
| `AccountRepository` | Account | `findById(AccountId)`, `findActiveByOwner(UserId)`, `save(Account)` |
| `TransactionRepository` | Transaction | `findById(id, UserId)`, `findPage(UserId, TransactionFilter, Pageable)`, `save(Transaction)`, `delete(Transaction)` |
| `RecurringTransactionRepository` | Transaction | `findByOwner(UserId)`, `findDueBy(LocalDate)` |
| `BudgetRepository` | Budget | `findActiveByOwner(UserId)`, `findByOwnerAndCategoryAndPeriod(...)` |
| `BudgetSpendQuery` | Budget | `calculateSpend(UserId, categoryIds, DateRange): Money` — this is a query object, not a CRUD repo |
| `CategoryRepository` | Categorization | `findVisibleToUser(UserId)`, `findById(CategoryId)` |
| `UserRepository` | Identity | `findByUsername(String)`, `findByEmail(String)`, `findById(UserId)` |
| `SessionRepository` | Identity | `findValidSession(token, now)`, `deleteByToken(token)` |

---

## 4. Rich Domain Model — Domain Logic Belongs in Entities

### Current Problem (Anemic Model)
```java
// WRONG — business logic leaking into service
public class AccountService {
    public void validateBalance(Account account, BigDecimal amount) {
        if (!account.getAccountType().isAllowsNegativeBalance()) {
            if (account.getCurrentBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(...);
            }
        }
    }
}
```

### Correct (Rich Domain Model)
```java
// RIGHT — the Account entity enforces its own invariants
public class Account {
    // Domain method — behaviour belongs on the aggregate root
    public void debit(Money amount) {
        if (!this.accountType.allowsNegativeBalance()) {
            Money resulting = this.currentBalance.subtract(amount);
            if (resulting.isNegative()) {
                throw new InsufficientFundsException(this.id, resulting);
            }
        }
        this.currentBalance = this.currentBalance.subtract(amount);
        // Register domain event
        this.registerEvent(new AccountDebited(this.id, amount, this.currentBalance));
    }

    public void credit(Money amount) {
        this.currentBalance = this.currentBalance.add(amount);
        this.registerEvent(new AccountCredited(this.id, amount, this.currentBalance));
    }
}
```

```java
// Budget enforces its own status computation
public class Budget {
    public BudgetStatus evaluateStatus(Money spent) {
        BigDecimal pct = spent.amount().divide(this.limit.amount(), 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100));
        if (pct.compareTo(BigDecimal.valueOf(100)) >= 0) return BudgetStatus.OVER_BUDGET;
        if (pct.compareTo(BigDecimal.valueOf(75)) >= 0)  return BudgetStatus.WARNING;
        return BudgetStatus.ON_TRACK;
    }

    public boolean isThresholdBreached(Money spent) {
        if (this.alertThresholdPct == null) return false;
        BigDecimal pct = spent.amount().divide(this.limit.amount(), 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100));
        return pct.compareTo(BigDecimal.valueOf(this.alertThresholdPct)) >= 0;
    }
}
```

---

## 5. DDD Layers Explained (Per Context)

Each bounded context follows the same internal structure:

```
{context}/
├── domain/
│   ├── model/        Pure Java. No Spring. No JPA. Business rules live here.
│   │                 Aggregates, Entities, Value Objects.
│   ├── repository/   Java interfaces only. No @Repository annotation.
│   │                 No Spring Data. Pure domain contracts.
│   ├── service/      Domain services: logic that spans multiple aggregates
│   │                 within the same context. NOT application orchestration.
│   └── event/        Domain event classes (plain Java records).
│
├── application/
│   ├── command/      Write use cases (CommandHandler pattern).
│   │                 One class per use case. Calls domain, publishes events,
│   │                 coordinates infrastructure.
│   ├── query/        Read use cases. May bypass domain layer entirely
│   │                 and call repositories directly for performance.
│   └── dto/          Input DTOs (Command/Query objects from controller)
│                     and Output DTOs (Response objects to controller).
│                     Java records with Bean Validation annotations.
│
└── infrastructure/
    ├── persistence/  Spring Data JPA repos (implements domain repository interfaces).
    │                 JPA @Entity classes (separate from domain model classes).
    │                 JPA mappers (domain model <-> JPA entity).
    ├── web/          @RestController, @RequestMapping.
    │                 Request parsing, response serialization.
    │                 Calls application layer only.
    └── config/       @Configuration, @Bean. Spring-specific wiring.
```

### Layer Dependency Rule (enforced by code review)

```
infrastructure  →  application  →  domain
(web/persistence)  (use cases)    (pure business logic)
```

- `domain` has ZERO imports from `infrastructure` or `application`.
- `application` imports from `domain`. No Spring framework imports except `@Transactional`.
- `infrastructure` imports from both `application` and `domain`. All Spring/JPA annotations live here.

---

## 6. Context Communication Patterns

### Within Same Context
Direct method calls. No event needed. Example: `TransactionApplicationService` calls `Account.debit()` — both are in the transaction context's scope.

Wait — this requires clarification. `Account.debit()` belongs to the Account context. The Transaction context cannot directly call Account domain methods without violating boundaries.

**Resolution**: The Transaction context calls the `AccountApplicationService` (a use-case method exposed as an internal API). In a monolith, this is a direct Java call. The `AccountApplicationService.applyDebit(accountId, amount)` method is the published interface of the Account context.

### Cross-Context via Domain Events (MVP: Synchronous)

```java
// Transaction context publishes
eventPublisher.publishEvent(new TransactionCreated(txId, accountId, amount, type, userId));

// Account context listens
@Component
public class AccountBalanceEventHandler {
    @EventListener
    @Transactional(propagation = REQUIRES_NEW)  // or REQUIRED to share TX
    public void on(TransactionCreated event) {
        accountApplicationService.applyBalanceDelta(
            event.accountId(), event.amount(), event.transactionType()
        );
    }
}
```

**For MVP**: Use `@Transactional(propagation = REQUIRED)` — share the parent transaction so balance update and transaction insert are atomic. This is safe and simple.

**For Phase 2**: Move to `@TransactionalEventListener(phase = AFTER_COMMIT)` for events that are truly cross-cutting (budget cache, notifications). These can tolerate eventual consistency.

### Cross-Context Read (Dashboard / Reporting)
The Reporting context uses its own read-model queries. It does NOT call other contexts' application services for reads. It queries the shared database tables directly via `@Query` JPQL/native SQL. This is the CQRS read-side pattern.

---

## 7. Ubiquitous Language Alignment

The code must match the finance-analyst's terms exactly.

| Domain Term | Java Class | Package |
|---|---|---|
| Account | `Account` | `account/domain/model/Account.java` |
| Account Type | `AccountType` | `account/domain/model/AccountType.java` |
| Transaction | `Transaction` | `transaction/domain/model/Transaction.java` |
| Transfer | `TransferPair` | `transaction/domain/model/TransferPair.java` |
| Recurring Transaction | `RecurringTransaction` | `transaction/domain/model/RecurringTransaction.java` |
| Budget | `Budget` | `budget/domain/model/Budget.java` |
| Category | `Category` | `category/domain/model/Category.java` |
| Category Type | `CategoryType` | `category/domain/model/CategoryType.java` |
| User | `User` | `identity/domain/model/User.java` |
| Session | `Session` | `identity/domain/model/Session.java` |
| Money | `Money` | `shared/domain/model/Money.java` |
| Budget Status | `BudgetStatus` | `budget/domain/model/BudgetStatus.java` |
| Transaction Type | `TransactionType` | `transaction/domain/model/TransactionType.java` |
| Net Worth | `NetWorth` | `account/domain/model/NetWorth.java` (Value Object) |
| Insufficient Funds | `InsufficientFundsException` | `account/domain/exception/InsufficientFundsException.java` |

Terms the domain uses that must NOT be renamed in code:
- "debit" and "credit" (Account operations)
- "transfer pair" (the two-row atomic transfer)
- "system category" (is_system = true)
- "soft delete" / "deactivate" (is_active = false)
- "spending period" (the computed date window for budget calculation)

---

## 8. What Does NOT Change from the Original Plan

The DDD restructuring does not change:
- Database schema (Liquibase changesets, table names, column names) — unchanged
- API contract (HTTP endpoints, request/response shapes) — unchanged
- Authentication mechanism (custom SessionAuthFilter) — unchanged
- Money representation (BigDecimal, serialized as string) — unchanged
- Optimistic locking on Account (@Version) — unchanged
- Transfer two-phase insert — unchanged

What changes is purely the **internal Java package organisation and where business logic lives**.
