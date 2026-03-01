# Hexagonal Architecture — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: AUTHORITATIVE — supersedes ddd-architecture.md for all structural decisions.
**Triggered by**: PM mandate for strict Ports & Adapters (Hexagonal Architecture).

---

## 1. The Core Principle

The domain is a pure Java library. It has ZERO knowledge of Spring, JPA, Jackson, or any delivery mechanism. You can `javac` the entire `domain/` subtree with only the JDK in the classpath and it will compile. This is not aspirational — it is a hard constraint enforced by code review and, eventually, ArchUnit tests.

The boundary is:
- **Domain** → pure Java. Business rules, invariants, port interfaces. No `import org.springframework.*`. No `import jakarta.persistence.*`. No `import com.fasterxml.jackson.*`.
- **Adapter** → everything else. Spring, JPA, Jackson, HTTP, scheduled jobs, event publishers.
- **Config** → Spring `@Configuration` classes that wire adapters into ports via `@Bean`.

---

## 2. Bounded Contexts (unchanged from DDD design)

```
┌─────────────────────────────────────────────────────────────────────────┐
│  MONOLITH (single Spring Boot application)                               │
│                                                                          │
│  ┌──────────────────┐      ┌──────────────────────┐                     │
│  │  IDENTITY         │      │  ACCOUNT              │                     │
│  │  (identity/)      │      │  (account/)           │                     │
│  └───────┬──────────┘      └──────────┬────────────┘                    │
│          │ UserId (Long)              │ AccountId (Long)                  │
│          │                            │                                  │
│  ┌───────▼──────────────────────────▼────────────────────────────────┐  │
│  │  TRANSACTION (transaction/)                                        │  │
│  └───────────────────┬─────────────────┬──────────────────────────────┘  │
│                      │ events          │ calls                            │
│  ┌───────────────────▼──────┐  ┌───────▼────────────────────────────┐   │
│  │  BUDGET (budget/)        │  │  CATEGORIZATION (category/)        │   │
│  └──────────────────────────┘  └────────────────────────────────────┘   │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  REPORTING (reporting/) — read-only CQRS queries                   │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  SHARED KERNEL (shared/) — pure Java VOs + shared adapter infra    │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Layer Rules Per Bounded Context

### Mandatory structure

```
{context}/
├── domain/                    PURE JAVA. No framework imports.
│   ├── model/                 Aggregates, Entities, Value Objects
│   ├── port/
│   │   ├── inbound/           Use case interfaces — what the domain EXPOSES
│   │   └── outbound/          Port interfaces — what the domain NEEDS
│   ├── service/               Domain services (implement inbound ports)
│   ├── event/                 Domain event records (pure Java)
│   └── exception/             Domain exceptions (extend RuntimeException)
│
├── adapter/
│   ├── inbound/               DRIVING adapters — they CALL inbound ports
│   │   └── web/               REST controllers, request/response DTOs
│   └── outbound/              DRIVEN adapters — they IMPLEMENT outbound ports
│       ├── persistence/       JPA entities, Spring Data repos, mappers
│       └── event/             Spring ApplicationEventPublisher adapters
│
└── config/                    Spring @Configuration — wires adapters to ports
    └── {Context}Config.java
```

### Inbound Port (Use Case) — defined in domain, called by inbound adapter

```java
// account/domain/port/inbound/CreateAccountUseCase.java
// PURE JAVA — no Spring imports
public interface CreateAccountUseCase {
    AccountId createAccount(CreateAccountCommand command);
}
```

### Domain Service — implements inbound port, calls outbound ports

```java
// account/domain/service/AccountCommandService.java
// PURE JAVA — only imports domain classes and port interfaces
public class AccountCommandService implements CreateAccountUseCase, UpdateAccountUseCase, DeactivateAccountUseCase {
    private final AccountPersistencePort accountPersistencePort;
    private final AccountEventPublisherPort eventPublisherPort;

    // Constructor injection — no @Autowired
    public AccountCommandService(AccountPersistencePort accountPersistencePort,
                                  AccountEventPublisherPort eventPublisherPort) {
        this.accountPersistencePort = accountPersistencePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public AccountId createAccount(CreateAccountCommand command) {
        // pure business logic; calls ports for persistence and events
    }
}
```

### Outbound Port — defined in domain, implemented by outbound adapter

```java
// account/domain/port/outbound/AccountPersistencePort.java
// PURE JAVA — no JPA, no Spring
public interface AccountPersistencePort {
    Optional<Account> findById(AccountId id, UserId owner);
    List<Account> findActiveByOwner(UserId owner);
    long countActiveByOwner(UserId owner);
    Optional<Account> findByOwnerAndName(UserId owner, String name);
    Account save(Account account);
}
```

### Inbound Adapter (REST Controller) — calls inbound ports

```java
// account/adapter/inbound/web/AccountController.java
// Has Spring @RestController — lives in adapter, NOT domain
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final CreateAccountUseCase createAccountUseCase;  // inbound port

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDto create(@Valid @RequestBody CreateAccountRequestDto dto) {
        CreateAccountCommand command = AccountRequestMapper.toCommand(dto, currentUserId());
        AccountId accountId = createAccountUseCase.createAccount(command);
        return new AccountResponseDto(accountId.value(), ...);
    }
}
```

### Outbound Adapter (Persistence) — implements outbound port

```java
// account/adapter/outbound/persistence/AccountPersistenceAdapter.java
// Has Spring @Component — lives in adapter, NOT domain
@Component
public class AccountPersistenceAdapter implements AccountPersistencePort {
    private final AccountJpaRepository jpaRepository;
    private final AccountJpaMapper mapper;

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = mapper.toJpaEntity(account);
        AccountJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

### Config — wires domain service to ports

```java
// account/config/AccountConfig.java
@Configuration
public class AccountConfig {

    @Bean
    public AccountCommandService accountCommandService(
            AccountPersistencePort accountPersistencePort,
            AccountEventPublisherPort accountEventPublisherPort) {
        return new AccountCommandService(accountPersistencePort, accountEventPublisherPort);
    }

    @Bean
    public AccountQueryService accountQueryService(
            AccountPersistencePort accountPersistencePort) {
        return new AccountQueryService(accountPersistencePort);
    }
}
```

---

## 4. Naming Conventions

| Layer | Suffix | Example |
|---|---|---|
| Inbound Port | `UseCase` (command) / `Query` (read) | `CreateAccountUseCase`, `GetAccountsQuery` |
| Outbound Port | `Port` | `AccountPersistencePort`, `EventPublisherPort` |
| Domain Service | `CommandService` / `QueryService` | `AccountCommandService`, `AccountQueryService` |
| Domain Model | Plain noun | `Account`, `Money`, `Transaction` |
| Domain Event | Past tense | `AccountCreated`, `TransactionDeleted` |
| Domain Exception | `Exception` | `InsufficientFundsException` |
| Domain Command (input to service) | `Command` | `CreateAccountCommand` |
| Inbound Adapter (Web) | `Controller` | `AccountController` |
| Web Request DTO | `RequestDto` | `CreateAccountRequestDto` |
| Web Response DTO | `ResponseDto` | `AccountResponseDto` |
| Web Mapper | `RequestMapper` | `AccountRequestMapper` |
| Outbound Adapter (Persistence) | `PersistenceAdapter` | `AccountPersistenceAdapter` |
| JPA Entity | `JpaEntity` | `AccountJpaEntity` |
| JPA Repository | `JpaRepository` | `AccountJpaRepository` |
| JPA Mapper | `JpaMapper` | `AccountJpaMapper` |
| Outbound Adapter (Event) | `EventPublisherAdapter` | `AccountEventPublisherAdapter` |
| Config | `Config` | `AccountConfig` |

---

## 5. Shared Kernel

The shared kernel is also framework-free in its `domain/` portion.

```
shared/
├── domain/
│   ├── model/
│   │   ├── Money.java              Pure Java VO
│   │   ├── DateRange.java          Pure Java VO
│   │   ├── AuditInfo.java          Pure Java VO
│   │   ├── UserId.java             Typed ID VO
│   │   ├── AccountId.java          Typed ID VO
│   │   ├── CategoryId.java         Typed ID VO
│   │   ├── BudgetId.java           Typed ID VO
│   │   └── TransactionId.java      Typed ID VO
│   ├── port/
│   │   └── outbound/
│   │       └── EventPublisherPort.java  Generic domain event publisher interface
│   ├── event/
│   │   └── DomainEvent.java        Marker interface for all domain events
│   └── exception/
│       └── DomainException.java    Base domain exception (extends RuntimeException)
│
├── adapter/
│   ├── inbound/
│   │   └── web/
│   │       ├── GlobalExceptionHandler.java     @RestControllerAdvice
│   │       ├── SecurityContextHolder.java      ThreadLocal for current user
│   │       └── dto/
│   │           ├── PageResponseDto.java        Paginated wrapper
│   │           ├── ErrorResponseDto.java       Error envelope
│   │           └── FieldErrorDto.java          Per-field error
│   └── outbound/
│       ├── persistence/
│       │   └── AuditableJpaEntity.java         @MappedSuperclass with audit fields
│       └── event/
│           └── SpringEventPublisherAdapter.java  implements EventPublisherPort using
│                                                  Spring ApplicationEventPublisher
└── config/
    ├── WebConfig.java              CORS, filter registration, pageable resolver
    ├── JacksonConfig.java          BigDecimal-as-string, JavaTimeModule
    └── OpenApiConfig.java          SpringDoc bearer auth scheme
```

### EventPublisherPort (defined in shared domain, used by all contexts)

```java
// shared/domain/port/outbound/EventPublisherPort.java
public interface EventPublisherPort {
    void publish(DomainEvent event);
}
```

### SpringEventPublisherAdapter (outbound adapter in shared)

```java
// shared/adapter/outbound/event/SpringEventPublisherAdapter.java
@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {
    private final ApplicationEventPublisher publisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
```

---

## 6. Complete Port Catalogue — Every Bounded Context

### 6.1 Identity Context

#### Inbound Ports (Use Cases)

```java
// identity/domain/port/inbound/RegisterUserUseCase.java
public interface RegisterUserUseCase {
    UserId registerUser(RegisterUserCommand command);
}

// identity/domain/port/inbound/AuthenticateUserUseCase.java
public interface AuthenticateUserUseCase {
    LoginResult authenticate(AuthenticateUserCommand command);
    // LoginResult: record(SessionToken token, OffsetDateTime expiresAt)
}

// identity/domain/port/inbound/LogoutUseCase.java
public interface LogoutUseCase {
    void logout(SessionToken token);
}

// identity/domain/port/inbound/GetCurrentUserQuery.java
public interface GetCurrentUserQuery {
    UserProfile getCurrentUser(UserId userId);
    // UserProfile: record(id, username, email, firstName, lastName, preferredCurrency, createdAt)
}
```

#### Outbound Ports

```java
// identity/domain/port/outbound/UserPersistencePort.java
public interface UserPersistencePort {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
}

// identity/domain/port/outbound/SessionPersistencePort.java
public interface SessionPersistencePort {
    Optional<Session> findValidSession(String token, OffsetDateTime now);
    Session save(Session session);
    void deleteByToken(String token);
    void deleteExpiredBefore(OffsetDateTime cutoff);
}

// identity/domain/port/outbound/PasswordHasherPort.java
public interface PasswordHasherPort {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}

// identity/domain/port/outbound/LoginRateLimiterPort.java
public interface LoginRateLimiterPort {
    boolean isBlocked(String clientIp);
    void recordFailedAttempt(String clientIp);
    void resetAttempts(String clientIp);
}

// identity/domain/port/outbound/IdentityEventPublisherPort.java
public interface IdentityEventPublisherPort extends EventPublisherPort {}
// (No additional methods — inherits publish(DomainEvent); kept separate for
//  wiring clarity — each context gets its own typed publisher port)
```

### 6.2 Account Context

#### Inbound Ports

```java
// account/domain/port/inbound/CreateAccountUseCase.java
public interface CreateAccountUseCase {
    AccountId createAccount(CreateAccountCommand command);
}

// account/domain/port/inbound/UpdateAccountUseCase.java
public interface UpdateAccountUseCase {
    void updateAccount(UpdateAccountCommand command);
}

// account/domain/port/inbound/DeactivateAccountUseCase.java
public interface DeactivateAccountUseCase {
    void deactivateAccount(AccountId accountId, UserId requestingUser);
}

// account/domain/port/inbound/ApplyBalanceDeltaUseCase.java
public interface ApplyBalanceDeltaUseCase {
    // Called by Transaction context cross-context adapter
    void applyDebit(AccountId accountId, Money amount);
    void applyCredit(AccountId accountId, Money amount);
    void reverseDebit(AccountId accountId, Money amount);
    void reverseCredit(AccountId accountId, Money amount);
}

// account/domain/port/inbound/GetAccountsQuery.java
public interface GetAccountsQuery {
    List<AccountView> getAccountsByOwner(UserId owner);
    Optional<AccountView> getAccountById(AccountId id, UserId owner);
    NetWorthView getNetWorth(UserId owner);
}
// AccountView: record(accountId, name, accountTypeCode, accountTypeName, currentBalance,
//                     initialBalance, currency, institutionName, accountNumberLast4,
//                     isActive, includeInNetWorth, isLiability, createdAt)
// NetWorthView: record(totalAssets Money, totalLiabilities Money, netWorth Money)
```

#### Outbound Ports

```java
// account/domain/port/outbound/AccountPersistencePort.java
public interface AccountPersistencePort {
    Optional<Account> findById(AccountId id, UserId owner);
    List<Account> findActiveByOwner(UserId owner);
    long countActiveByOwner(UserId owner);
    Optional<Account> findByOwnerAndName(UserId owner, String name);
    Account save(Account account);
}

// account/domain/port/outbound/AccountTypePersistencePort.java
public interface AccountTypePersistencePort {
    Optional<AccountType> findByCode(String code);
    List<AccountType> findAll();
}

// account/domain/port/outbound/AccountEventPublisherPort.java
public interface AccountEventPublisherPort extends EventPublisherPort {}
```

### 6.3 Category Context

#### Inbound Ports

```java
// category/domain/port/inbound/CreateCategoryUseCase.java
public interface CreateCategoryUseCase {
    CategoryId createCategory(CreateCategoryCommand command);
}

// category/domain/port/inbound/UpdateCategoryUseCase.java
public interface UpdateCategoryUseCase {
    void updateCategory(UpdateCategoryCommand command);
}

// category/domain/port/inbound/DeactivateCategoryUseCase.java
public interface DeactivateCategoryUseCase {
    void deactivateCategory(CategoryId categoryId, UserId requestingUser);
}

// category/domain/port/inbound/GetCategoriesQuery.java
public interface GetCategoriesQuery {
    List<CategoryView> getCategoriesVisibleToUser(UserId userId);
    Optional<CategoryView> getCategoryById(CategoryId id);
    List<CategoryId> getCategoryAndDescendantIds(CategoryId parentId);
}
// CategoryView: record(id, name, categoryTypeCode, parentCategoryId, icon, color, isSystem, isActive)
```

#### Outbound Ports

```java
// category/domain/port/outbound/CategoryPersistencePort.java
public interface CategoryPersistencePort {
    Optional<Category> findById(CategoryId id);
    List<Category> findVisibleToUser(UserId userId);
    Optional<Category> findByOwnerAndParentAndName(UserId owner, CategoryId parentId, String name);
    List<CategoryId> findCategoryAndDescendantIds(CategoryId parentId);
    boolean hasTransactions(CategoryId categoryId);
    Category save(Category category);
}

// category/domain/port/outbound/CategoryEventPublisherPort.java
public interface CategoryEventPublisherPort extends EventPublisherPort {}
```

### 6.4 Transaction Context

#### Inbound Ports

```java
// transaction/domain/port/inbound/CreateTransactionUseCase.java
public interface CreateTransactionUseCase {
    TransactionId createTransaction(CreateTransactionCommand command);
}

// transaction/domain/port/inbound/UpdateTransactionUseCase.java
public interface UpdateTransactionUseCase {
    void updateTransaction(UpdateTransactionCommand command);
}

// transaction/domain/port/inbound/DeleteTransactionUseCase.java
public interface DeleteTransactionUseCase {
    void deleteTransaction(TransactionId id, UserId requestingUser);
}

// transaction/domain/port/inbound/CreateTransferUseCase.java
public interface CreateTransferUseCase {
    TransferResult createTransfer(CreateTransferCommand command);
    // TransferResult: record(outId TransactionId, inId TransactionId)
}

// transaction/domain/port/inbound/UpdateTransferUseCase.java
public interface UpdateTransferUseCase {
    void updateTransfer(UpdateTransferCommand command);
}

// transaction/domain/port/inbound/DeleteTransferUseCase.java
public interface DeleteTransferUseCase {
    void deleteTransfer(TransactionId eitherLegId, UserId requestingUser);
}

// transaction/domain/port/inbound/GetTransactionsQuery.java
public interface GetTransactionsQuery {
    Page<TransactionView> getTransactions(UserId owner, TransactionFilterCommand filter, Pageable pageable);
    Optional<TransactionView> getTransactionById(TransactionId id, UserId owner);
}
// TransactionView: record(id, accountId, accountName, categoryId, categoryName, categoryType,
//                         amount, transactionType, transactionDate, description, merchantName,
//                         referenceNumber, isRecurring, transferPairId, isReconciled, createdAt)
```

#### Outbound Ports

```java
// transaction/domain/port/outbound/TransactionPersistencePort.java
public interface TransactionPersistencePort {
    Optional<Transaction> findByIdAndOwner(TransactionId id, UserId owner);
    Page<Transaction> findPage(UserId owner, TransactionFilter filter, Pageable pageable);
    Transaction save(Transaction transaction);
    Transaction saveAndFlush(Transaction transaction);  // for two-phase transfer insert
    void delete(Transaction transaction);
    void updateTransferPairId(TransactionId transactionId, TransactionId pairId);
}

// transaction/domain/port/outbound/TransactionEventPublisherPort.java
public interface TransactionEventPublisherPort extends EventPublisherPort {}

// transaction/domain/port/outbound/AccountBalancePort.java
// Cross-context port: Transaction context calls Account context for balance changes
public interface AccountBalancePort {
    void applyDebit(AccountId accountId, UserId owner, Money amount);
    void applyCredit(AccountId accountId, UserId owner, Money amount);
    void reverseDebit(AccountId accountId, UserId owner, Money amount);
    void reverseCredit(AccountId accountId, UserId owner, Money amount);
    boolean canDebit(AccountId accountId, UserId owner, Money amount);  // returns false if would go negative
}

// transaction/domain/port/outbound/CategoryValidationPort.java
// Cross-context port: Transaction context validates category exists and type is correct
public interface CategoryValidationPort {
    boolean categoryExistsAndVisibleToUser(CategoryId categoryId, UserId userId);
    String getCategoryTypeCode(CategoryId categoryId);  // returns "INCOME", "EXPENSE", "TRANSFER"
}

// transaction/domain/port/outbound/RecurringTransactionPersistencePort.java
public interface RecurringTransactionPersistencePort {
    Optional<RecurringTransaction> findById(Long id, UserId owner);
    List<RecurringTransaction> findActiveByOwner(UserId owner);
    List<RecurringTransaction> findDueBy(LocalDate dueDate);
    RecurringTransaction save(RecurringTransaction rt);
}
```

### 6.5 Budget Context

#### Inbound Ports

```java
// budget/domain/port/inbound/CreateBudgetUseCase.java
public interface CreateBudgetUseCase {
    BudgetId createBudget(CreateBudgetCommand command);
}

// budget/domain/port/inbound/UpdateBudgetUseCase.java
public interface UpdateBudgetUseCase {
    void updateBudget(UpdateBudgetCommand command);
}

// budget/domain/port/inbound/DeactivateBudgetUseCase.java
public interface DeactivateBudgetUseCase {
    void deactivateBudget(BudgetId budgetId, UserId requestingUser);
}

// budget/domain/port/inbound/GetBudgetsQuery.java
public interface GetBudgetsQuery {
    List<BudgetView> getActiveBudgets(UserId owner);
    BudgetSummaryView getBudgetSummary(BudgetId budgetId, UserId owner);
}
// BudgetView: record(id, categoryId, categoryName, periodType, limit, startDate, endDate,
//                    rolloverEnabled, alertThresholdPct, isActive)
// BudgetSummaryView: record(budgetView, spent, remaining, percentageUsed, status, periodStart, periodEnd)
```

#### Outbound Ports

```java
// budget/domain/port/outbound/BudgetPersistencePort.java
public interface BudgetPersistencePort {
    Optional<Budget> findById(BudgetId id, UserId owner);
    List<Budget> findActiveByOwner(UserId owner);
    Optional<Budget> findActiveByOwnerAndCategoryAndPeriod(UserId owner, CategoryId categoryId, BudgetPeriodType periodType);
    Budget save(Budget budget);
}

// budget/domain/port/outbound/BudgetSpendCalculationPort.java
public interface BudgetSpendCalculationPort {
    Money calculateSpend(UserId userId, List<CategoryId> categoryIds, DateRange period);
}

// budget/domain/port/outbound/BudgetEventPublisherPort.java
public interface BudgetEventPublisherPort extends EventPublisherPort {}

// budget/domain/port/outbound/CategoryQueryPort.java
// Cross-context port: Budget context reads category metadata
public interface CategoryQueryPort {
    Optional<String> getCategoryTypeCode(CategoryId categoryId);
    List<CategoryId> getCategoryAndDescendantIds(CategoryId parentId);
}
```

### 6.6 Reporting Context

Reporting has no domain service (it is purely a read-model). The controller calls the query port directly.

```java
// reporting/domain/port/inbound/GetDashboardQuery.java
public interface GetDashboardQuery {
    DashboardView getDashboard(UserId userId);
}
// DashboardView: record(netWorth, currentMonthIncome, currentMonthExpense,
//                       netCashFlow, accountBalances, topExpenseCategories)
```

Outbound port:

```java
// reporting/domain/port/outbound/DashboardReadPort.java
public interface DashboardReadPort {
    NetWorthView getNetWorth(UserId userId);
    MonthlyFlowView getMonthlyFlow(UserId userId, YearMonth month);
    List<TopCategoryView> getTopExpenseCategories(UserId userId, YearMonth month, int limit);
    List<AccountBalanceView> getAccountsWithBalances(UserId userId);
}
```

---

## 7. Cross-Context Communication (Anti-Corruption Layers)

### Rule: No context imports domain classes from another context.

Communication is exclusively via:
1. **Outbound ports with dedicated cross-context adapters** (synchronous, in-process)
2. **Domain events via EventPublisherPort** (async-capable, decoupled)

### 7.1 Transaction → Account (Balance Updates)

The Transaction context defines `AccountBalancePort` as its outbound port. The Account context provides the adapter.

```
transaction/domain/port/outbound/AccountBalancePort.java   (port — in Transaction context)
account/adapter/outbound/crosscontext/AccountBalanceAdapter.java  (adapter — in Account context)
  └── implements AccountBalancePort
  └── calls account domain service: ApplyBalanceDeltaUseCase
```

Wiring in `account/config/AccountConfig.java`:

```java
@Bean
public AccountBalanceAdapter accountBalanceAdapter(ApplyBalanceDeltaUseCase applyBalanceDeltaUseCase) {
    return new AccountBalanceAdapter(applyBalanceDeltaUseCase);
}
```

Wiring in `transaction/config/TransactionConfig.java`:

```java
@Bean
public CreateTransactionCommandService createTransactionCommandService(
        TransactionPersistencePort transactionPersistencePort,
        AccountBalancePort accountBalancePort,           // injected from Account context
        CategoryValidationPort categoryValidationPort,
        TransactionEventPublisherPort eventPublisher) {
    return new CreateTransactionCommandService(...);
}
```

### 7.2 Transaction → Category (Validation)

```
transaction/domain/port/outbound/CategoryValidationPort.java  (port — in Transaction context)
category/adapter/outbound/crosscontext/CategoryValidationAdapter.java  (adapter — in Category context)
  └── implements CategoryValidationPort
  └── calls category domain: GetCategoriesQuery
```

### 7.3 Budget → Category (Hierarchy Query)

```
budget/domain/port/outbound/CategoryQueryPort.java  (port — in Budget context)
category/adapter/outbound/crosscontext/CategoryQueryAdapter.java  (adapter — in Category context)
  └── implements CategoryQueryPort
  └── calls category domain: GetCategoriesQuery
```

### 7.4 Domain Events (Transaction → Account balance via events — alternative path)

For MVP, the Transaction context uses `AccountBalancePort` (synchronous direct call) to ensure the balance update is atomic with the transaction insert. The event-based path is used for eventually-consistent side effects only.

```
Transaction domain publishes: TransactionCreated, TransactionDeleted, TransferCreated
  ↓ via TransactionEventPublisherPort → SpringEventPublisherAdapter → Spring ApplicationEventPublisher
  ↓
account/adapter/inbound/event/TransactionEventListenerAdapter.java
  └── @EventListener on TransactionCreated, TransactionDeleted, TransferCreated
  └── Purpose: Phase 2 — cache invalidation, audit log, dashboard cache
  └── NOT used for balance updates in MVP (balance is done synchronously via AccountBalancePort)

reporting/adapter/inbound/event/DashboardCacheInvalidatorAdapter.java  (Phase 2)
  └── @TransactionalEventListener(phase = AFTER_COMMIT)
  └── Invalidates dashboard read cache after transaction commits
```

### 7.5 ACL Summary Table

| Source Context | Target Context | Port (defined in source) | Adapter (defined in target) | Pattern |
|---|---|---|---|---|
| Transaction | Account | `AccountBalancePort` | `AccountBalanceAdapter` | Synchronous direct call |
| Transaction | Category | `CategoryValidationPort` | `CategoryValidationAdapter` | Synchronous direct call |
| Budget | Category | `CategoryQueryPort` | `CategoryQueryAdapter` | Synchronous direct call |
| Transaction | (broadcast) | `TransactionEventPublisherPort` | `SpringEventPublisherAdapter` | Domain event |
| Account | (broadcast) | `AccountEventPublisherPort` | `SpringEventPublisherAdapter` | Domain event |
| Budget | (broadcast) | `BudgetEventPublisherPort` | `SpringEventPublisherAdapter` | Domain event |
| Identity | (broadcast) | `IdentityEventPublisherPort` | `SpringEventPublisherAdapter` | Domain event |

---

## 8. Domain Model — Pure Java Classes

### What is allowed in domain model classes:
- `java.util.*`, `java.math.*`, `java.time.*`
- Other domain model classes from the same context
- Shared kernel VOs from `shared/domain/model/`
- Port interfaces from `{context}/domain/port/`
- Domain exceptions from `{context}/domain/exception/`
- Domain events from `{context}/domain/event/`

### What is FORBIDDEN in domain model classes:
- `jakarta.persistence.*` (JPA)
- `org.springframework.*` (Spring)
- `com.fasterxml.jackson.*` (Jackson)
- `lombok.*` (Lombok annotations) — domain is pure Java, use getters manually
- `org.hibernate.*`

### Domain Command Objects (pure Java records)

Commands are the input to inbound ports. They are pure Java records, defined in `{context}/domain/port/inbound/` (same package as the use case interface):

```java
// account/domain/port/inbound/CreateAccountCommand.java
public record CreateAccountCommand(
    UserId ownerId,
    String name,
    String accountTypeCode,
    Money initialBalance,
    String institutionName,
    String accountNumberLast4
) {}
```

The web adapter converts the HTTP request DTO to the domain command:

```java
// account/adapter/inbound/web/AccountRequestMapper.java
public class AccountRequestMapper {
    public static CreateAccountCommand toCommand(CreateAccountRequestDto dto, UserId userId) {
        return new CreateAccountCommand(
            userId,
            dto.name(),
            dto.accountTypeCode(),
            Money.of(dto.initialBalance(), dto.currency()),
            dto.institutionName(),
            dto.accountNumberLast4()
        );
    }
}
```

---

## 9. JPA Entity / Domain Model Separation

### Why Two Classes?

The domain `Account` carries business logic. The JPA `AccountJpaEntity` carries persistence metadata. They must not be the same class because:
- JPA requires a no-arg constructor and mutable fields — domain invariants cannot be guaranteed on a class the JPA proxy can bypass
- Jackson annotations on a domain class couple the domain to the serialization format
- The domain class changes when business rules change; the JPA class changes when the schema changes — different axes of change

### Pattern

```java
// DOMAIN class — pure Java
// account/domain/model/Account.java
public class Account {
    private final AccountId id;
    private UserId ownerId;
    private AccountType accountType;
    private String name;
    private Money currentBalance;
    private Money initialBalance;
    private boolean isActive;
    private boolean includeInNetWorth;
    private Long version;  // for optimistic locking — value only, no JPA

    // Business methods
    public void debit(Money amount) {
        if (!accountType.allowsNegativeBalance()) {
            Money result = currentBalance.subtract(amount);
            if (result.isNegative()) {
                throw new InsufficientFundsException(id, result);
            }
        }
        this.currentBalance = currentBalance.subtract(amount);
    }

    public void credit(Money amount) {
        this.currentBalance = currentBalance.add(amount);
    }
}
```

```java
// JPA entity — lives in adapter/outbound/persistence/
// account/adapter/outbound/persistence/AccountJpaEntity.java
@Entity
@Table(schema = "finance_tracker", name = "accounts")
public class AccountJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_id_seq")
    @SequenceGenerator(name = "accounts_id_seq", sequenceName = "finance_tracker.accounts_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountTypeJpaEntity accountType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "include_in_net_worth", nullable = false)
    private boolean includeInNetWorth;

    @Version
    @Column(name = "version")
    private Long version;

    // No-arg constructor required by JPA
    protected AccountJpaEntity() {}

    // Getters and setters...
}
```

```java
// Mapper — lives in adapter/outbound/persistence/
// account/adapter/outbound/persistence/AccountJpaMapper.java
@Component
public class AccountJpaMapper {

    public Account toDomain(AccountJpaEntity entity) {
        AccountType accountType = toAccountTypeDomain(entity.getAccountType());
        return new Account(
            new AccountId(entity.getId()),
            new UserId(entity.getUserId()),
            accountType,
            entity.getName(),
            Money.of(entity.getCurrentBalance(), entity.getCurrency()),
            Money.of(entity.getInitialBalance(), entity.getCurrency()),
            entity.isActive(),
            entity.isIncludeInNetWorth(),
            entity.getVersion()
        );
    }

    public AccountJpaEntity toJpaEntity(Account domain) {
        AccountJpaEntity entity = new AccountJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId().value());
        }
        entity.setUserId(domain.getOwnerId().value());
        entity.setName(domain.getName());
        entity.setCurrentBalance(domain.getCurrentBalance().amount());
        entity.setInitialBalance(domain.getInitialBalance().amount());
        entity.setCurrency(domain.getCurrentBalance().currency());
        entity.setActive(domain.isActive());
        entity.setIncludeInNetWorth(domain.isIncludeInNetWorth());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
```

---

## 10. How Spring Wiring Works (End-to-End)

### 10.1 Spring sees: Adapters and Config classes

Spring component scan picks up all classes in `com.shan.cyber.tech.financetracker`. It finds:
- Adapter classes (`@Component`, `@RestController`, `@Repository`)
- Config classes (`@Configuration`)
- The shared `SpringEventPublisherAdapter` (`@Component`)

Spring does NOT manage domain service classes directly. They are created as `@Bean` in Config classes.

### 10.2 Example: Account Context wiring

```
AccountConfig (@Configuration)
├── @Bean AccountCommandService(AccountPersistencePort, AccountEventPublisherPort)
│                                      ↑                        ↑
│                            AccountPersistenceAdapter    AccountEventPublisherAdapter
│                                (@Component)                 (@Component)
│                              implements                    implements
│                            AccountPersistencePort      AccountEventPublisherPort
│
├── @Bean AccountQueryService(AccountPersistencePort)
│
└── AccountController (@RestController)
    └── injects CreateAccountUseCase (= AccountCommandService @Bean)
    └── injects GetAccountsQuery (= AccountQueryService @Bean)
```

### 10.3 Why not @Service on domain services?

The domain service is pure Java. Adding `@Service` would introduce a Spring import into the domain. Instead, the Config class creates the `@Bean` with constructor injection. The result is identical at runtime — Spring manages the lifecycle — but the domain class stays import-clean.

---

## 11. Testing Strategy with Hexagonal Architecture

### Advantage: Domain tests require ZERO Spring context

```java
// account/domain/service/AccountCommandServiceTest.java
// No @SpringBootTest, no @MockBean, no application context
class AccountCommandServiceTest {

    private AccountPersistencePort persistencePort;  // Mockito mock
    private AccountEventPublisherPort eventPublisher;
    private AccountCommandService service;

    @BeforeEach
    void setUp() {
        persistencePort = mock(AccountPersistencePort.class);
        eventPublisher = mock(AccountEventPublisherPort.class);
        service = new AccountCommandService(persistencePort, eventPublisher);
    }

    @Test
    void createAccount_enforces_max20_limit() {
        when(persistencePort.countActiveByOwner(any())).thenReturn(20L);
        assertThrows(MaxAccountsExceededException.class,
            () -> service.createAccount(validCommand()));
    }
}
```

### Adapter tests

```java
// account/adapter/outbound/persistence/AccountPersistenceAdapterTest.java
// @DataJpaTest + Testcontainers — tests the JPA adapter in isolation
@DataJpaTest
@Testcontainers
class AccountPersistenceAdapterTest {
    // Tests actual SQL execution against real Postgres 15
}
```

### Integration tests (acceptance module)

```java
// Full stack: HTTP → Controller → Domain Service → JPA Adapter → DB
// acceptance/src/test/java/AccountIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class AccountIntegrationTest {
    // Uses RestAssured against the running app
}
```

---

## 12. What Does NOT Change from DDD Design

The hexagonal transition changes **how classes are organised and what annotations are allowed where**. It does NOT change:

- Database schema (Liquibase changesets, table names, column names)
- API contract (HTTP endpoints, request/response shapes)
- Authentication mechanism (custom SessionAuthFilter)
- Money representation (BigDecimal, NUMERIC(19,4), serialized as string)
- Optimistic locking on Account (the @Version field value is carried through the mapper)
- Transfer two-phase insert (handled in `CreateTransferCommandService`)
- Domain event strategy (sync for MVP, @TransactionalEventListener for Phase 2)
- Bounded context boundaries

---

## 13. ArchUnit Enforcement (Phase 1.5 Addition)

Add ArchUnit tests to enforce the hexagonal boundaries at CI time. Prevents framework imports from sneaking into the domain.

```java
// application/src/test/java/ArchitectureTest.java
@AnalyzeClasses(packages = "com.shan.cyber.tech.financetracker")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_import_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule adapters_must_not_import_other_context_domains =
        noClasses()
            .that().resideInAPackage("..account..")
            .should().dependOnClassesThat()
            .resideInAPackage("..transaction.domain..");

    @ArchTest
    static final ArchRule controllers_only_call_inbound_ports =
        noClasses()
            .that().resideInAPackage("..adapter.inbound.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain.service..");
}
```
