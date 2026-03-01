# Code Standards — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: Authoritative — all code written by full-stack-dev must conform

---

## 1. Naming Conventions

### Java

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, dot-separated | `com.shan.cyber.tech.financetracker.account.service` |
| Class | UpperCamelCase | `AccountService`, `CreateAccountRequest` |
| Interface | UpperCamelCase, no "I" prefix | `AccountRepository` |
| Method | lowerCamelCase, verb-first | `createAccount`, `findByUserId`, `calculateNetWorth` |
| Variable | lowerCamelCase | `currentBalance`, `transactionDate` |
| Constant | UPPER_SNAKE_CASE | `MAX_ACCOUNTS_PER_USER`, `SESSION_DURATION_DAYS` |
| Record | UpperCamelCase, noun or noun-phrase | `AccountResponse`, `CreateTransactionRequest` |
| Enum | UpperCamelCase type, UPPER_SNAKE values | `TransactionType.TRANSFER_OUT` |
| Test class | `{ClassUnderTest}Test` (unit) or `{Feature}IntegrationTest` | `AccountServiceTest`, `AuthIntegrationTest` |
| Test method | `should_{expectedBehaviour}_when_{condition}` | `should_reject_transaction_when_savings_account_would_go_negative` |

### Database

| Element | Convention | Example |
|---|---|---|
| Table | snake_case, plural | `accounts`, `account_types`, `recurring_transactions` |
| Column | snake_case | `user_id`, `current_balance`, `is_active` |
| Index | `idx_{table}_{columns}` | `idx_transactions_user_date` |
| Constraint | `fk_{table}_{referenced}`, `chk_{table}_{column}`, `uq_{table}_{columns}` | `fk_accounts_user`, `chk_transactions_amount` |
| Sequence | `{table}_id_seq` | `accounts_id_seq` |
| Liquibase changeset ID | snake_case descriptive verb phrase | `create_accounts_table`, `alter_users_table_fix_types` |

### API

| Element | Convention | Example |
|---|---|---|
| URL path segments | kebab-case, plural for collections | `/api/v1/recurring-transactions` |
| Query parameters | snake_case | `?start_date=2026-01-01&page=0&size=30` |
| JSON request/response fields | camelCase | `{"transactionDate": "2026-01-15", "categoryId": 5}` |
| Error codes | UPPER_SNAKE_CASE | `VALIDATION_ERROR`, `INSUFFICIENT_BALANCE` |

---

## 2. Package Structure Rules

Root package for all application code: `com.shan.cyber.tech.financetracker`

### Rules
1. Code is organized **domain-first**, then layer: `{domain}/{layer}/`
2. Cross-domain dependencies go through service interfaces, never repository-to-repository.
3. `common/` is the only package that other domains may import freely.
4. A domain package may not import from another domain's `entity/` or `repository/` packages directly.
   - Exception: `TransactionService` reads `Account` entity (for balance update). This is acceptable because transactions are tightly coupled to accounts by design. Do not proliferate cross-domain entity imports.
5. The `filter/` sub-package belongs to `auth/` — it is not in `common/` because only auth produces filters.
6. `config/` classes go in `common/config/` if they are cross-cutting (Jackson, Web, JPA), or in the domain package if domain-specific.

### Full Package Tree (reference)
```
com.shan.cyber.tech.financetracker
├── PersonalFinanceTracker.java          (Spring Boot main class — stays at com.shan.cyber.tech)
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/RegisterRequest.java
│   ├── dto/LoginRequest.java
│   ├── dto/LoginResponse.java
│   ├── dto/UserProfileResponse.java
│   ├── entity/User.java
│   ├── entity/Session.java
│   ├── filter/SessionAuthFilter.java
│   ├── repository/UserRepository.java
│   ├── repository/SessionRepository.java
│   └── service/AuthService.java
├── account/
│   ├── controller/AccountController.java
│   ├── dto/CreateAccountRequest.java
│   ├── dto/UpdateAccountRequest.java
│   ├── dto/AccountResponse.java
│   ├── dto/NetWorthResponse.java
│   ├── entity/Account.java
│   ├── entity/AccountType.java
│   ├── repository/AccountRepository.java
│   ├── repository/AccountTypeRepository.java
│   └── service/AccountService.java
├── category/
│   ├── controller/CategoryController.java
│   ├── dto/CreateCategoryRequest.java
│   ├── dto/UpdateCategoryRequest.java
│   ├── dto/CategoryResponse.java
│   ├── entity/Category.java
│   ├── entity/CategoryType.java
│   ├── repository/CategoryRepository.java
│   ├── repository/CategoryTypeRepository.java
│   └── service/CategoryService.java
├── transaction/
│   ├── controller/TransactionController.java
│   ├── controller/TransferController.java
│   ├── dto/CreateTransactionRequest.java
│   ├── dto/UpdateTransactionRequest.java
│   ├── dto/CreateTransferRequest.java
│   ├── dto/UpdateTransferRequest.java
│   ├── dto/TransactionResponse.java
│   ├── dto/TransferResponse.java
│   ├── dto/TransactionFilter.java
│   ├── entity/Transaction.java
│   ├── repository/TransactionRepository.java
│   └── service/
│       ├── TransactionService.java
│       ├── TransferService.java
│       └── BalanceService.java
├── budget/
│   ├── controller/BudgetController.java
│   ├── dto/CreateBudgetRequest.java
│   ├── dto/UpdateBudgetRequest.java
│   ├── dto/BudgetResponse.java
│   ├── dto/BudgetSummaryResponse.java
│   ├── entity/Budget.java
│   ├── repository/BudgetRepository.java
│   └── service/BudgetService.java
├── dashboard/
│   ├── controller/DashboardController.java
│   ├── dto/DashboardSummaryResponse.java
│   └── service/DashboardService.java
└── common/
    ├── config/
    │   ├── JacksonConfig.java
    │   └── WebConfig.java
    ├── dto/
    │   ├── ErrorResponse.java
    │   └── PageResponse.java
    ├── entity/
    │   └── BaseAuditEntity.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   ├── DuplicateResourceException.java
    │   ├── BusinessRuleException.java
    │   ├── InsufficientBalanceException.java
    │   ├── ForbiddenOperationException.java
    │   └── UnauthorizedException.java
    └── security/
        └── SecurityContextHelper.java
```

---

## 3. API Response Format

### Success — Single Resource
```json
{
  "id": 42,
  "name": "Chase Checking",
  "accountTypeCode": "CHECKING",
  "currentBalance": "1250.0000",
  "currency": "USD",
  "isActive": true,
  "createdAt": "2026-02-28T10:30:00Z"
}
```

HTTP 200 for GET/PUT/PATCH, HTTP 201 for POST (with `Location: /api/v1/accounts/42` header).

### Success — Collection (Paginated)
```json
{
  "content": [ {...}, {...} ],
  "page": 0,
  "size": 30,
  "totalElements": 87,
  "totalPages": 3
}
```

### Success — No Body
HTTP 204 No Content (DELETE, logout).

### Error Response Format
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

For single-error responses (404, 409, 401, 403, 500), `"errors"` array is **omitted** (null is excluded by `@JsonInclude(NON_NULL)`).

### Money Fields in JSON
All `BigDecimal` money values are serialized as **strings** in JSON:
```json
{
  "amount": "1250.0000",
  "currentBalance": "0.0000"
}
```
Never as JSON number: `"amount": 1250.0`.

### Date/Time Fields in JSON
- `LocalDate` fields: ISO 8601 date string `"2026-02-28"`
- `OffsetDateTime` fields: ISO 8601 with UTC offset `"2026-02-28T10:30:00Z"`
- Never serialize as epoch milliseconds.

---

## 4. Error Response Codes (Canonical List)

| Scenario | HTTP | `error` field value |
|---|---|---|
| Bean Validation failure | 422 | `VALIDATION_ERROR` |
| Business rule violation | 422 | (from exception's `errorCode` field) |
| Insufficient balance | 422 | `INSUFFICIENT_BALANCE` |
| Resource not found | 404 | `NOT_FOUND` |
| Duplicate / unique conflict | 409 | `CONFLICT` |
| No/invalid auth token | 401 | `UNAUTHORIZED` |
| Valid token, wrong user | 403 | `FORBIDDEN` |
| Rate limit exceeded | 429 | `RATE_LIMIT_EXCEEDED` |
| Internal server error | 500 | `INTERNAL_ERROR` |

Domain-specific business rule error codes:
- `MAX_ACCOUNTS_REACHED`
- `CURRENCY_MISMATCH`
- `HIERARCHY_DEPTH_EXCEEDED`
- `CATEGORY_TYPE_MISMATCH`
- `CATEGORY_HAS_TRANSACTIONS`
- `SYSTEM_CATEGORY_IMMUTABLE`
- `TRANSFER_SAME_ACCOUNT`
- `TRANSFER_ACCOUNT_MISMATCH`
- `USE_TRANSFER_DELETE`
- `ACCOUNT_INACTIVE`
- `DATE_TOO_FAR_PAST`
- `DATE_TOO_FAR_FUTURE`
- `BUDGET_PERIOD_OVERLAP`
- `CUSTOM_PERIOD_REQUIRES_END_DATE`

---

## 5. Entity Layer Standards

### JPA Annotations
```java
@Entity
@Table(schema = "finance_tracker", name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_seq")
    @SequenceGenerator(name = "accounts_seq", sequenceName = "finance_tracker.accounts_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false, insertable = false, updatable = false)
    private AccountType accountType;

    @Column(name = "account_type_id", nullable = false)
    private Short accountTypeId;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Version
    private Long version;  // Optimistic locking on accounts (balance updates)
}
```

Rules:
- Use `FetchType.LAZY` on all `@ManyToOne` and `@OneToMany` — never EAGER.
- Store FK as a scalar `Long` field alongside the `@ManyToOne` association (avoids loading the association when you only need the FK value).
- Use `@SequenceGenerator` with `allocationSize = 1` to match the DB sequence increment of 1.
- `@Column` must specify `name` explicitly — never rely on Hibernate's automatic naming strategy for column names (avoids surprises when entity field names use camelCase that doesn't map cleanly).
- All entity fields must have explicit `@Column(nullable = false)` or `nullable = true` — no defaults.

### Reference Entity Pattern
For `AccountType` and `CategoryType` (pre-seeded, never created/updated by API):
```java
@Entity
@Table(schema = "finance_tracker", name = "account_types")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountType {

    @Id
    private Short id;  // No @GeneratedValue — fixed pre-seeded IDs

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    // ... other fields

    // No setter — reference data is read-only after seeding
}
```

---

## 6. Service Layer Standards

- All methods that write to the database must be annotated `@Transactional`.
- Read-only methods should be annotated `@Transactional(readOnly = true)`.
- Service classes are annotated `@Service`.
- Service methods never return `null` — throw an appropriate exception or return `Optional` if the caller needs to handle absence.
- Services never accept or return JPA entity types in their public API — always use DTOs.
  - Exception: internal `BalanceService` methods accept `Account` entity because it operates within the same transaction context.
- User ID for all operations comes from `SecurityContextHelper.getCurrentUserId()` — never from request body or path parameters.
  - Exception: `SessionAuthFilter` and `AuthController.register` (no session yet).

---

## 7. Controller Layer Standards

- Controllers are annotated `@RestController` + `@RequestMapping("/api/v1/...")`.
- Controllers contain **no business logic** — only: deserialize request, call service, serialize response.
- Method parameters: use `@Valid` on request body DTOs, `@PathVariable Long id` for path params.
- The authenticated user ID is NOT accepted from the request — retrieve via `SecurityContextHelper.getCurrentUserId()` in the service, or pass it from controller as `Long userId = SecurityContextHelper.getCurrentUserId()` at the start of the controller method.
- All controller methods declare their HTTP method explicitly with `@GetMapping`, `@PostMapping`, etc. — never use `@RequestMapping(method = RequestMethod.X)` (the old style).
- POST endpoints return `ResponseEntity` to set the Location header:
```java
@PostMapping
public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
    Long userId = SecurityContextHelper.getCurrentUserId();
    AccountResponse response = accountService.createAccount(userId, request);
    URI location = URI.create("/api/v1/accounts/" + response.id());
    return ResponseEntity.created(location).body(response);
}
```

---

## 8. Logging Standards

Framework: SLF4J + Logback (default Spring Boot).

| Level | When to Use |
|---|---|
| `ERROR` | Unhandled exceptions, data integrity violations (500 handler) |
| `WARN` | Expected business exceptions (rate limit, auth failures, not-found) |
| `INFO` | Application startup events, significant state changes (user registered, session created) |
| `DEBUG` | SQL queries (controlled by `show-sql: false` in prod; enable per session), detailed flow |

Rules:
- Use `private static final Logger log = LoggerFactory.getLogger(ClassName.class)` — or `@Slf4j` Lombok annotation.
- Never log passwords, tokens, or PII (email, names) at INFO or above.
- Log session token only as a truncated prefix (first 8 chars) for traceability: `token=abc12345...`.
- Every significant service operation logs at INFO: `log.info("User {} created account {}", userId, account.getId())`.
- Log WARN on all rejected business operations: `log.warn("Balance check failed for account {}: would result in {}", accountId, newBalance)`.

---

## 9. Testing Standards

### Unit Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTypeRepository accountTypeRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void should_throw_business_rule_exception_when_user_has_20_active_accounts() {
        // given
        when(accountRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(20L);

        // when / then
        assertThatThrownBy(() -> accountService.createAccount(1L, validRequest()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("MAX_ACCOUNTS_REACHED");
    }
}
```

### Repository Test Pattern
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SessionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2")
        .withDatabaseName("personal-finance-tracker")
        .withUsername("pft-app-user")
        .withPassword("pft-app-user-secret");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void should_return_empty_when_session_is_expired() {
        // ...
    }
}
```

### Integration Test Pattern
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class AuthIntegrationTest {

    @LocalServerPort
    int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2");

    @Test
    void should_return_201_and_token_on_successful_registration_and_login() {
        // Register
        given().port(port).contentType("application/json")
            .body("""{"username":"testuser","email":"test@example.com","password":"Test1234!","firstName":"Test","lastName":"User"}""")
            .when().post("/api/v1/auth/register")
            .then().statusCode(201);

        // Login
        String token = given().port(port).contentType("application/json")
            .body("""{"username":"testuser","password":"Test1234!"}""")
            .when().post("/api/v1/auth/login")
            .then().statusCode(200)
            .extract().path("token");

        assertThat(token).isNotBlank();
    }
}
```

---

## 10. Git Commit Standards

Format: `{type}: {short imperative description}`

| Type | When |
|---|---|
| `feat` | New feature implementation |
| `fix` | Bug fix |
| `refactor` | Code change without behaviour change |
| `test` | Adding or updating tests |
| `db` | Liquibase changeset additions |
| `chore` | Build config, dependency updates |

Examples:
- `feat: implement account creation with balance initialization`
- `fix: correct Liquibase changelog path in application.yaml`
- `db: add accounts table changeset (005)`
- `test: add unit tests for BalanceService overdraft prevention`

---

## 11. What NOT To Do (Prohibited Patterns)

- **No floating-point for money**: Never use `float`, `double`, `Float`, `Double` for any monetary value. BigDecimal only.
- **No eager fetching**: `FetchType.EAGER` is prohibited. Always lazy-load associations.
- **No `@Transactional` on controllers**: Transactions belong in the service layer.
- **No `SELECT *` in native queries**: Specify column names explicitly.
- **No `Optional.get()` without `isPresent()` check**: Use `orElseThrow()` with a meaningful exception.
- **No swallowing exceptions**: Never `catch (Exception e) { /* do nothing */ }`.
- **No hard deletes**: Use `is_active = false` for User, Account, Category, Budget, RecurringTransaction. Transactions are never deleted in production (MVP allows it, but set `is_active` flag in preference to physical delete).
- **No trusting user_id from request**: Always derive the acting user from `SecurityContextHelper.getCurrentUserId()`.
- **No `System.out.println`**: Use SLF4J logging.
- **No entity returned from controller**: Map to DTO before returning.
- **No business logic in controllers**: Controllers are thin wrappers — routing + serialization only.
- **No magic numbers**: Extract constants. `private static final int MAX_ACTIVE_ACCOUNTS = 20;`
