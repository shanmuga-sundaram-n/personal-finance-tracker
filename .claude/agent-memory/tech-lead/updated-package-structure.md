# Package Structure — Hexagonal Architecture
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: AUTHORITATIVE — supersedes all previous package structure documents.
**Architecture**: Strict Ports & Adapters (Hexagonal Architecture)
**Root package**: `com.shan.cyber.tech.financetracker`

---

## Key Rules Before Reading the Tree

1. **`domain/`** sub-packages: ZERO Spring, ZERO JPA, ZERO Jackson imports. Pure Java only.
2. **`adapter/`** sub-packages: Spring and JPA annotations are allowed and expected here.
3. **`config/`**: Spring `@Configuration` classes that wire adapters to ports. One per bounded context.
4. **No cross-context domain imports**: `account/domain/` classes cannot appear in `transaction/domain/` imports.
5. **Port interfaces always defined in the context that NEEDS them**, not the context that provides them.

---

## Spring Boot Entry Point

```
com.shan.cyber.tech.PersonalFinanceTracker    @SpringBootApplication
                                              Component scan covers com.shan.cyber.tech.financetracker.*
```

---

## Complete Package Tree — Every Class

```
com.shan.cyber.tech.financetracker/
│
│══════════════════════════════════════════════════════════════════════════
│  SHARED KERNEL
│══════════════════════════════════════════════════════════════════════════
├── shared/
│   │
│   ├── domain/                              PURE JAVA — no framework imports
│   │   ├── model/
│   │   │   ├── Money.java                   VO: BigDecimal amount + String currency
│   │   │   │                                Methods: of(), zero(), add(), subtract(),
│   │   │   │                                         isNegative(), isPositive(), negate()
│   │   │   │                                Equality: by amount.compareTo() and currency
│   │   │   ├── DateRange.java               VO: LocalDate startDate + LocalDate endDate
│   │   │   │                                Methods: contains(LocalDate), overlaps(DateRange)
│   │   │   │                                Invariant: endDate >= startDate if endDate != null
│   │   │   ├── AuditInfo.java               VO: createdAt(OffsetDateTime), updatedAt,
│   │   │   │                                    createdBy(Long), updatedBy(Long)
│   │   │   ├── UserId.java                  record UserId(Long value) — validates > 0
│   │   │   ├── AccountId.java               record AccountId(Long value) — validates > 0
│   │   │   ├── CategoryId.java              record CategoryId(Long value) — validates > 0
│   │   │   ├── BudgetId.java                record BudgetId(Long value) — validates > 0
│   │   │   └── TransactionId.java           record TransactionId(Long value) — validates > 0
│   │   ├── port/
│   │   │   └── outbound/
│   │   │       └── EventPublisherPort.java  interface: publish(DomainEvent event)
│   │   ├── event/
│   │   │   └── DomainEvent.java             Marker interface: all domain events implement this
│   │   └── exception/
│   │       └── DomainException.java         Base: extends RuntimeException
│   │                                        Fields: String errorCode
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── SecurityContextHolder.java   ThreadLocal<Long> for authenticated userId
│   │   │       │                                setCurrentUserId(Long), getCurrentUserId(): Long
│   │   │       │                                clear() — called in filter's finally block
│   │   │       ├── GlobalExceptionHandler.java  @RestControllerAdvice
│   │   │       │                                Handles: DomainException → 422
│   │   │       │                                         ResourceNotFoundException → 404
│   │   │       │                                         DuplicateResourceException → 409
│   │   │       │                                         InsufficientFundsException → 422
│   │   │       │                                         ForbiddenOperationException → 403
│   │   │       │                                         UnauthorizedException → 401
│   │   │       │                                         MethodArgumentNotValidException → 422
│   │   │       │                                         ConstraintViolationException → 422
│   │   │       │                                         Exception (catch-all) → 500
│   │   │       └── dto/
│   │   │           ├── PageResponseDto.java     record: content, page, size, totalElements, totalPages
│   │   │           │                            static from(Page<T>): PageResponseDto<T>
│   │   │           ├── ErrorResponseDto.java    record: status, error, message, errors, timestamp, path
│   │   │           └── FieldErrorDto.java       record: field, code, message
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   └── AuditableJpaEntity.java      @MappedSuperclass
│   │       │                                    Fields: @Column createdAt(OffsetDateTime),
│   │       │                                            updatedAt, createdBy(Long), updatedBy(Long)
│   │       │                                    @PrePersist onCreate(), @PreUpdate onUpdate()
│   │       └── event/
│   │           └── SpringEventPublisherAdapter.java  @Component implements EventPublisherPort
│   │                                            Uses: ApplicationEventPublisher (Spring)
│   │
│   └── config/
│       ├── WebConfig.java               @Configuration implements WebMvcConfigurer
│       │                                @Bean FilterRegistrationBean<SessionAuthFilter>
│       │                                addCorsMappings: env-configurable via app.cors.allowed-origins
│       │                                addArgumentResolvers: PageableHandlerMethodArgumentResolver
│       │                                addInterceptors: security headers filter
│       ├── JacksonConfig.java           @Configuration
│       │                                @Bean Jackson2ObjectMapperBuilderCustomizer
│       │                                Enables: WRITE_BIGDECIMAL_AS_PLAIN
│       │                                Modules: JavaTimeModule
│       │                                SerializationInclusion: NON_NULL
│       └── OpenApiConfig.java           @Configuration
│                                        @Bean OpenAPI with bearerAuth SecurityScheme
│
│══════════════════════════════════════════════════════════════════════════
│  IDENTITY BOUNDED CONTEXT
│══════════════════════════════════════════════════════════════════════════
├── identity/
│   │
│   ├── domain/                          PURE JAVA
│   │   ├── model/
│   │   │   ├── User.java                Aggregate Root
│   │   │   │                            Fields: UserId id, String username,
│   │   │   │                                    String passwordHash, String email,
│   │   │   │                                    String firstName, String lastName,
│   │   │   │                                    boolean isActive, String preferredCurrency,
│   │   │   │                                    AuditInfo auditInfo
│   │   │   │                            Methods: activate(), deactivate(),
│   │   │   │                                     updateProfile(firstName, lastName),
│   │   │   │                                     updatePreferredCurrency(String)
│   │   │   │                            NOTE: passwordHash is a plain String here — domain
│   │   │   │                                  doesn't know about BCrypt. The
│   │   │   │                                  PasswordHasherPort does the hashing.
│   │   │   └── Session.java             Aggregate Root
│   │   │                                Fields: Long id, UserId userId, String token,
│   │   │                                        OffsetDateTime expiresAt, AuditInfo auditInfo
│   │   │                                Methods: isExpired(OffsetDateTime now): boolean
│   │   │                                         isValid(OffsetDateTime now): boolean
│   │   │
│   │   ├── port/
│   │   │   ├── inbound/
│   │   │   │   ├── RegisterUserUseCase.java      interface: register(RegisterUserCommand): UserId
│   │   │   │   ├── RegisterUserCommand.java      record: username, email, rawPassword,
│   │   │   │   │                                         firstName, lastName
│   │   │   │   ├── AuthenticateUserUseCase.java  interface: authenticate(AuthenticateUserCommand): LoginResult
│   │   │   │   ├── AuthenticateUserCommand.java  record: username, rawPassword, clientIp
│   │   │   │   ├── LoginResult.java              record: token(String), expiresAt(OffsetDateTime)
│   │   │   │   ├── LogoutUseCase.java            interface: logout(String token)
│   │   │   │   └── GetCurrentUserQuery.java      interface: getCurrentUser(UserId): UserProfile
│   │   │   │   └── UserProfile.java              record: id, username, email, firstName, lastName,
│   │   │   │                                             preferredCurrency, createdAt
│   │   │   └── outbound/
│   │   │       ├── UserPersistencePort.java      interface: findById, findByUsername, findByEmail,
│   │   │       │                                           existsByUsername, existsByEmail, save
│   │   │       ├── SessionPersistencePort.java   interface: findValidSession, save, deleteByToken,
│   │   │       │                                           deleteExpiredBefore
│   │   │       ├── PasswordHasherPort.java       interface: hash(String): String,
│   │   │       │                                           matches(String, String): boolean
│   │   │       ├── LoginRateLimiterPort.java     interface: isBlocked(String clientIp): boolean,
│   │   │       │                                           recordFailedAttempt(String clientIp),
│   │   │       │                                           resetAttempts(String clientIp)
│   │   │       └── IdentityEventPublisherPort.java  extends EventPublisherPort (typed publisher)
│   │   │
│   │   ├── service/
│   │   │   ├── IdentityCommandService.java      implements RegisterUserUseCase, AuthenticateUserUseCase,
│   │   │   │                                    LogoutUseCase
│   │   │   │                                    Constructor: (UserPersistencePort, SessionPersistencePort,
│   │   │   │                                                  PasswordHasherPort, LoginRateLimiterPort,
│   │   │   │                                                  IdentityEventPublisherPort, sessionDurationDays)
│   │   │   └── IdentityQueryService.java        implements GetCurrentUserQuery
│   │   │                                        Constructor: (UserPersistencePort)
│   │   │
│   │   ├── event/
│   │   │   ├── UserRegistered.java              record implements DomainEvent: UserId userId, String email
│   │   │   └── UserDeactivated.java             record implements DomainEvent: UserId userId
│   │   │
│   │   └── exception/
│   │       ├── UserNotFoundException.java       extends DomainException, errorCode=USER_NOT_FOUND
│   │       ├── DuplicateUsernameException.java  extends DomainException, errorCode=DUPLICATE_USERNAME
│   │       ├── DuplicateEmailException.java     extends DomainException, errorCode=DUPLICATE_EMAIL
│   │       ├── InvalidCredentialsException.java extends DomainException, errorCode=INVALID_CREDENTIALS
│   │       └── RateLimitExceededException.java  extends DomainException, errorCode=RATE_LIMIT_EXCEEDED
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── AuthController.java          @RestController @RequestMapping("/api/v1/auth")
│   │   │       │                                POST /register  -> 201 + UserProfileResponseDto
│   │   │       │                                POST /login     -> 200 + LoginResponseDto
│   │   │       │                                POST /logout    -> 204
│   │   │       │                                GET  /me        -> 200 + UserProfileResponseDto
│   │   │       │                                Injects: RegisterUserUseCase, AuthenticateUserUseCase,
│   │   │       │                                         LogoutUseCase, GetCurrentUserQuery
│   │   │       ├── SessionAuthFilter.java        extends OncePerRequestFilter
│   │   │       │                                 Reads: Authorization: Bearer {token}
│   │   │       │                                 Calls: SessionPersistencePort.findValidSession
│   │   │       │                                 Sets: SecurityContextHolder.setCurrentUserId
│   │   │       │                                 Public paths: /api/v1/auth/register, /api/v1/auth/login
│   │   │       ├── RegisterRequestDto.java       record: @NotBlank username, @Email email,
│   │   │       │                                         @Size(min=8) password, firstName, lastName
│   │   │       ├── LoginRequestDto.java          record: @NotBlank username, @NotBlank password
│   │   │       ├── LoginResponseDto.java         record: String token, OffsetDateTime expiresAt
│   │   │       └── UserProfileResponseDto.java   record: id, username, email, firstName, lastName,
│   │   │                                                 preferredCurrency, createdAt
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   ├── UserJpaEntity.java            @Entity @Table(schema="finance_tracker", name="users")
│   │       │   │                                 extends AuditableJpaEntity
│   │       │   │                                 Fields: @Id id, username, passwordHash, email,
│   │       │   │                                         firstName, lastName, isActive, preferredCurrency
│   │       │   ├── SessionJpaEntity.java         @Entity @Table(schema="finance_tracker", name="sessions")
│   │       │   │                                 extends AuditableJpaEntity
│   │       │   │                                 Fields: @Id id, userId, token, expiresAt
│   │       │   ├── UserJpaRepository.java        extends JpaRepository<UserJpaEntity, Long>
│   │       │   │                                 findByUsernameAndIsActiveTrue(String)
│   │       │   │                                 findByEmailAndIsActiveTrue(String)
│   │       │   │                                 existsByUsername(String)
│   │       │   │                                 existsByEmail(String)
│   │       │   ├── SessionJpaRepository.java     extends JpaRepository<SessionJpaEntity, Long>
│   │       │   │                                 @Query findValidSession(token, now)
│   │       │   │                                 deleteByToken(String)
│   │       │   │                                 deleteByExpiresAtBefore(OffsetDateTime)
│   │       │   ├── UserPersistenceAdapter.java   @Component implements UserPersistencePort
│   │       │   │                                 Delegates to UserJpaRepository + UserJpaMapper
│   │       │   ├── SessionPersistenceAdapter.java @Component implements SessionPersistencePort
│   │       │   │                                 Delegates to SessionJpaRepository + SessionJpaMapper
│   │       │   ├── UserJpaMapper.java            Maps UserJpaEntity <-> domain User
│   │       │   └── SessionJpaMapper.java         Maps SessionJpaEntity <-> domain Session
│   │       └── security/
│   │           ├── BcryptPasswordHasherAdapter.java   @Component implements PasswordHasherPort
│   │           │                                      Uses: BCryptPasswordEncoder(strength=12)
│   │           └── InMemoryRateLimiterAdapter.java    @Component implements LoginRateLimiterPort
│   │                                                  Uses: ConcurrentHashMap<String, AttemptRecord>
│   │                                                  AttemptRecord: count + windowStart(Instant)
│   │
│   └── config/
│       └── IdentityConfig.java          @Configuration
│                                        @Bean IdentityCommandService
│                                        @Bean IdentityQueryService
│                                        @Value("${app.session.duration-days}") injected into service
│
│══════════════════════════════════════════════════════════════════════════
│  ACCOUNT BOUNDED CONTEXT
│══════════════════════════════════════════════════════════════════════════
├── account/
│   │
│   ├── domain/                          PURE JAVA
│   │   ├── model/
│   │   │   ├── Account.java             Aggregate Root — NO JPA annotations
│   │   │   │                            Fields: AccountId id, UserId ownerId,
│   │   │   │                                    AccountType accountType, String name,
│   │   │   │                                    Money currentBalance, Money initialBalance,
│   │   │   │                                    String institutionName, String accountNumberLast4,
│   │   │   │                                    boolean isActive, boolean includeInNetWorth,
│   │   │   │                                    Long version (optimistic lock — value only)
│   │   │   │                            Methods:
│   │   │   │                              debit(Money amount): void
│   │   │   │                                 Throws InsufficientFundsException if
│   │   │   │                                 !accountType.allowsNegativeBalance() && result < 0
│   │   │   │                              credit(Money amount): void
│   │   │   │                              rename(String newName): void
│   │   │   │                              deactivate(): void — sets isActive=false
│   │   │   │                              canDebit(Money amount): boolean
│   │   │   │                              isLiability(): boolean — delegates to accountType
│   │   │   ├── AccountType.java         Value Object (Reference Data, immutable)
│   │   │   │                            Fields: Short id, String code, String name,
│   │   │   │                                    boolean allowsNegativeBalance, boolean isLiability
│   │   │   │                            Methods: allowsNegativeBalance(), isLiability()
│   │   │   └── NetWorth.java            Value Object
│   │   │                                Fields: Money totalAssets, Money totalLiabilities
│   │   │                                Methods: netWorth(): Money (= assets - liabilities)
│   │   │
│   │   ├── port/
│   │   │   ├── inbound/
│   │   │   │   ├── CreateAccountUseCase.java         interface: createAccount(CreateAccountCommand): AccountId
│   │   │   │   ├── CreateAccountCommand.java         record: UserId ownerId, String name,
│   │   │   │   │                                             String accountTypeCode, Money initialBalance,
│   │   │   │   │                                             String institutionName, String accountNumberLast4
│   │   │   │   ├── UpdateAccountUseCase.java         interface: updateAccount(UpdateAccountCommand)
│   │   │   │   ├── UpdateAccountCommand.java         record: AccountId, UserId ownerId, String name,
│   │   │   │   │                                             String institutionName
│   │   │   │   ├── DeactivateAccountUseCase.java     interface: deactivateAccount(AccountId, UserId)
│   │   │   │   ├── ApplyBalanceDeltaUseCase.java     interface:
│   │   │   │   │                                       applyDebit(AccountId, UserId, Money)
│   │   │   │   │                                       applyCredit(AccountId, UserId, Money)
│   │   │   │   │                                       reverseDebit(AccountId, UserId, Money)
│   │   │   │   │                                       reverseCredit(AccountId, UserId, Money)
│   │   │   │   │                                       canDebit(AccountId, UserId, Money): boolean
│   │   │   │   └── GetAccountsQuery.java             interface:
│   │   │   │                                           getAccountsByOwner(UserId): List<AccountView>
│   │   │   │                                           getAccountById(AccountId, UserId): Optional<AccountView>
│   │   │   │                                           getNetWorth(UserId): NetWorthView
│   │   │   │   ├── AccountView.java                  record: id, name, accountTypeCode, accountTypeName,
│   │   │   │   │                                             currentBalance(Money), initialBalance(Money),
│   │   │   │   │                                             institutionName, accountNumberLast4,
│   │   │   │   │                                             isActive, includeInNetWorth, isLiability,
│   │   │   │   │                                             createdAt(OffsetDateTime)
│   │   │   │   └── NetWorthView.java                 record: totalAssets(Money), totalLiabilities(Money),
│   │   │   │                                                 netWorth(Money)
│   │   │   └── outbound/
│   │   │       ├── AccountPersistencePort.java       interface: findById(AccountId, UserId),
│   │   │       │                                               findActiveByOwner(UserId),
│   │   │       │                                               countActiveByOwner(UserId),
│   │   │       │                                               findByOwnerAndName(UserId, String),
│   │   │       │                                               save(Account): Account
│   │   │       ├── AccountTypePersistencePort.java   interface: findByCode(String): Optional<AccountType>,
│   │   │       │                                               findAll(): List<AccountType>
│   │   │       └── AccountEventPublisherPort.java    extends EventPublisherPort
│   │   │
│   │   ├── service/
│   │   │   ├── AccountCommandService.java    implements CreateAccountUseCase, UpdateAccountUseCase,
│   │   │   │                                           DeactivateAccountUseCase, ApplyBalanceDeltaUseCase
│   │   │   │                                Constructor: (AccountPersistencePort,
│   │   │   │                                              AccountTypePersistencePort,
│   │   │   │                                              AccountEventPublisherPort)
│   │   │   │                                Business rules enforced:
│   │   │   │                                  createAccount: max 20 accounts, unique name, valid type
│   │   │   │                                  applyDebit: calls Account.debit() which throws if invalid
│   │   │   └── AccountQueryService.java      implements GetAccountsQuery
│   │   │                                     Constructor: (AccountPersistencePort)
│   │   │
│   │   ├── event/
│   │   │   ├── AccountCreated.java           record implements DomainEvent: AccountId, UserId, String name
│   │   │   ├── AccountDeactivated.java       record implements DomainEvent: AccountId, UserId
│   │   │   ├── AccountDebited.java           record implements DomainEvent: AccountId, Money amount,
│   │   │   │                                                                Money newBalance
│   │   │   └── AccountCredited.java          record implements DomainEvent: AccountId, Money amount,
│   │   │                                                                    Money newBalance
│   │   │
│   │   └── exception/
│   │       ├── AccountNotFoundException.java     extends DomainException, errorCode=ACCOUNT_NOT_FOUND
│   │       ├── InsufficientFundsException.java   extends DomainException, errorCode=INSUFFICIENT_FUNDS
│   │       │                                     Fields: AccountId, Money resultingBalance
│   │       ├── MaxAccountsExceededException.java extends DomainException, errorCode=MAX_ACCOUNTS_EXCEEDED
│   │       └── DuplicateAccountNameException.java extends DomainException, errorCode=DUPLICATE_ACCOUNT_NAME
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── AccountController.java       @RestController @RequestMapping("/api/v1/accounts")
│   │   │       │                                GET    /           -> 200 + List<AccountResponseDto>
│   │   │       │                                POST   /           -> 201 + AccountResponseDto + Location
│   │   │       │                                GET    /{id}       -> 200 + AccountResponseDto
│   │   │       │                                PUT    /{id}       -> 200 + AccountResponseDto
│   │   │       │                                DELETE /{id}       -> 204
│   │   │       │                                GET    /net-worth  -> 200 + NetWorthResponseDto
│   │   │       │                                Injects: CreateAccountUseCase, UpdateAccountUseCase,
│   │   │       │                                         DeactivateAccountUseCase, GetAccountsQuery
│   │   │       ├── CreateAccountRequestDto.java  record: @NotBlank name, @NotNull accountTypeCode,
│   │   │       │                                         @NotNull @DecimalMin("0") initialBalance,
│   │   │       │                                         @NotBlank @Size(3,3) currency,
│   │   │       │                                         institutionName, accountNumberLast4
│   │   │       ├── UpdateAccountRequestDto.java  record: @Size(max=100) name, institutionName
│   │   │       ├── AccountResponseDto.java        record: id, name, accountTypeCode, accountTypeName,
│   │   │       │                                          @JsonSerialize(ToStringSerializer) currentBalance,
│   │   │       │                                          @JsonSerialize(ToStringSerializer) initialBalance,
│   │   │       │                                          currency, institutionName, accountNumberLast4,
│   │   │       │                                          isActive, includeInNetWorth, isLiability, createdAt
│   │   │       ├── NetWorthResponseDto.java       record: totalAssets, totalLiabilities, netWorth
│   │   │       │                                          (all @JsonSerialize(ToStringSerializer))
│   │   │       └── AccountRequestMapper.java      Converts CreateAccountRequestDto + UserId -> CreateAccountCommand
│   │   │                                          Converts AccountView -> AccountResponseDto
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   ├── AccountJpaEntity.java          @Entity @Table(schema="finance_tracker", name="accounts")
│   │       │   │                                  extends AuditableJpaEntity
│   │       │   │                                  @Version Long version
│   │       │   │                                  Fields: id, userId(Long), accountType(ManyToOne lazy),
│   │       │   │                                          name, currentBalance(BigDecimal),
│   │       │   │                                          initialBalance(BigDecimal), currency,
│   │       │   │                                          institutionName, accountNumberLast4,
│   │       │   │                                          isActive, includeInNetWorth
│   │       │   ├── AccountTypeJpaEntity.java       @Entity @Table(schema="finance_tracker", name="account_types")
│   │       │   │                                   No @GeneratedValue (pre-seeded data)
│   │       │   │                                   Fields: Short id, code, name,
│   │       │   │                                           allowsNegativeBalance, isLiability
│   │       │   ├── AccountJpaRepository.java       extends JpaRepository<AccountJpaEntity, Long>
│   │       │   │                                   findByUserIdAndIsActiveTrue(Long)
│   │       │   │                                   findByIdAndUserIdAndIsActiveTrue(Long id, Long userId)
│   │       │   │                                   countByUserIdAndIsActiveTrue(Long userId)
│   │       │   │                                   @Query findByUserIdAndNameIgnoreCase(Long userId, String name)
│   │       │   ├── AccountTypeJpaRepository.java   extends JpaRepository<AccountTypeJpaEntity, Short>
│   │       │   │                                   findByCode(String code)
│   │       │   ├── AccountPersistenceAdapter.java  @Component implements AccountPersistencePort
│   │       │   │                                   Delegates: AccountJpaRepository + AccountJpaMapper
│   │       │   ├── AccountTypePersistenceAdapter.java @Component implements AccountTypePersistencePort
│   │       │   │                                   Delegates: AccountTypeJpaRepository + AccountTypeJpaMapper
│   │       │   ├── AccountJpaMapper.java            @Component
│   │       │   │                                   toDomain(AccountJpaEntity): Account
│   │       │   │                                   toJpaEntity(Account): AccountJpaEntity
│   │       │   └── AccountTypeJpaMapper.java        toDomain(AccountTypeJpaEntity): AccountType
│   │       ├── event/
│   │       │   └── AccountEventPublisherAdapter.java @Component implements AccountEventPublisherPort
│   │       │                                         Delegates to SpringEventPublisherAdapter
│   │       └── crosscontext/
│   │           └── AccountBalanceAdapter.java       @Component implements
│   │                                                transaction/domain/port/outbound/AccountBalancePort
│   │                                                Calls: ApplyBalanceDeltaUseCase (inbound port)
│   │                                                NOTE: This adapter lives in the account/ package
│   │                                                but implements a port defined in transaction/
│   │
│   └── config/
│       └── AccountConfig.java           @Configuration
│                                        @Bean AccountCommandService(...)
│                                        @Bean AccountQueryService(...)
│
│══════════════════════════════════════════════════════════════════════════
│  CATEGORY BOUNDED CONTEXT
│══════════════════════════════════════════════════════════════════════════
├── category/
│   │
│   ├── domain/                          PURE JAVA
│   │   ├── model/
│   │   │   ├── Category.java            Aggregate Root
│   │   │   │                            Fields: CategoryId id, UserId ownerId (null=system),
│   │   │   │                                    CategoryType categoryType,
│   │   │   │                                    CategoryId parentCategoryId (null=top-level),
│   │   │   │                                    String name, String icon, String color,
│   │   │   │                                    boolean isSystem, boolean isActive,
│   │   │   │                                    AuditInfo auditInfo
│   │   │   │                            Methods: updateDetails(name, icon, color): void
│   │   │   │                                     deactivate(): void
│   │   │   │                            Invariants (enforced in service):
│   │   │   │                              isSystem categories: cannot be modified
│   │   │   │                              max 2 hierarchy levels
│   │   │   │                              child type must match parent type
│   │   │   └── CategoryType.java        Value Object (Reference Data)
│   │   │                                Fields: Short id, String code (INCOME/EXPENSE/TRANSFER),
│   │   │                                        String name
│   │   │
│   │   ├── port/
│   │   │   ├── inbound/
│   │   │   │   ├── CreateCategoryUseCase.java      interface: createCategory(CreateCategoryCommand): CategoryId
│   │   │   │   ├── CreateCategoryCommand.java      record: UserId ownerId, String name,
│   │   │   │   │                                           Short categoryTypeId,
│   │   │   │   │                                           CategoryId parentCategoryId,
│   │   │   │   │                                           String icon, String color
│   │   │   │   ├── UpdateCategoryUseCase.java      interface: updateCategory(UpdateCategoryCommand)
│   │   │   │   ├── UpdateCategoryCommand.java      record: CategoryId, UserId ownerId,
│   │   │   │   │                                           String name, String icon, String color
│   │   │   │   ├── DeactivateCategoryUseCase.java  interface: deactivateCategory(CategoryId, UserId)
│   │   │   │   └── GetCategoriesQuery.java         interface:
│   │   │   │                                         getCategoriesVisibleToUser(UserId): List<CategoryView>
│   │   │   │                                         getCategoryById(CategoryId): Optional<CategoryView>
│   │   │   │                                         getCategoryAndDescendantIds(CategoryId): List<CategoryId>
│   │   │   │       CategoryView.java               record: id, name, categoryTypeCode, categoryTypeName,
│   │   │   │                                               parentCategoryId, icon, color, isSystem, isActive
│   │   │   └── outbound/
│   │   │       ├── CategoryPersistencePort.java    interface: findById(CategoryId),
│   │   │       │                                             findVisibleToUser(UserId),
│   │   │       │                                             findByOwnerAndParentAndName(UserId, CategoryId, String),
│   │   │       │                                             findCategoryAndDescendantIds(CategoryId): List<CategoryId>,
│   │   │       │                                             hasTransactions(CategoryId): boolean,
│   │   │       │                                             save(Category): Category
│   │   │       └── CategoryEventPublisherPort.java extends EventPublisherPort
│   │   │
│   │   ├── service/
│   │   │   ├── CategoryCommandService.java   implements CreateCategoryUseCase, UpdateCategoryUseCase,
│   │   │   │                                            DeactivateCategoryUseCase
│   │   │   │                                Constructor: (CategoryPersistencePort,
│   │   │   │                                              CategoryEventPublisherPort)
│   │   │   │                                Business rules:
│   │   │   │                                  max 2 hierarchy depth
│   │   │   │                                  child type must match parent type
│   │   │   │                                  no TRANSFER type for user-created
│   │   │   │                                  no modify/delete on isSystem
│   │   │   │                                  hasTransactions check before deactivate
│   │   │   └── CategoryQueryService.java     implements GetCategoriesQuery
│   │   │                                     Constructor: (CategoryPersistencePort)
│   │   │
│   │   ├── event/
│   │   │   └── CategoryDeactivated.java      record implements DomainEvent: CategoryId
│   │   │
│   │   └── exception/
│   │       ├── CategoryNotFoundException.java          extends DomainException
│   │       ├── SystemCategoryModificationException.java extends DomainException,
│   │       │                                               errorCode=SYSTEM_CATEGORY_IMMUTABLE
│   │       ├── CategoryHasTransactionsException.java   extends DomainException,
│   │       │                                               errorCode=CATEGORY_HAS_TRANSACTIONS
│   │       ├── HierarchyDepthExceededException.java    extends DomainException,
│   │       │                                               errorCode=HIERARCHY_DEPTH_EXCEEDED
│   │       └── CategoryTypeMismatchException.java      extends DomainException,
│   │                                                       errorCode=CATEGORY_TYPE_MISMATCH
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── CategoryController.java         @RestController @RequestMapping("/api/v1/categories")
│   │   │       │                                   GET    /      -> List<CategoryResponseDto>
│   │   │       │                                   POST   /      -> 201 + CategoryResponseDto
│   │   │       │                                   GET    /{id}  -> CategoryResponseDto
│   │   │       │                                   PUT    /{id}  -> CategoryResponseDto
│   │   │       │                                   DELETE /{id}  -> 204
│   │   │       ├── CreateCategoryRequestDto.java   record: @NotBlank name, @NotNull categoryTypeCode,
│   │   │       │                                           parentCategoryId, icon, color
│   │   │       ├── UpdateCategoryRequestDto.java   record: name, icon, color
│   │   │       ├── CategoryResponseDto.java         record: id, name, categoryTypeCode, categoryTypeName,
│   │   │       │                                            parentCategoryId, icon, color, isSystem, isActive
│   │   │       └── CategoryRequestMapper.java       Converts DTOs <-> Commands and Views <-> ResponseDtos
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   ├── CategoryJpaEntity.java           @Entity @Table(schema="finance_tracker", name="categories")
│   │       │   │                                    extends AuditableJpaEntity
│   │       │   │                                    @ManyToOne categoryType (lazy)
│   │       │   │                                    @Column parentCategoryId (no FK navigation — just ID)
│   │       │   ├── CategoryTypeJpaEntity.java       @Entity @Table(name="category_types")
│   │       │   │                                    No @GeneratedValue
│   │       │   ├── CategoryJpaRepository.java       extends JpaRepository<CategoryJpaEntity, Long>
│   │       │   │                                    @Query findAllVisibleToUser(Long userId)
│   │       │   │                                    @Query findByUserIdAndParentAndNameIgnoreCase(...)
│   │       │   │                                    @Query findCategoryAndDescendantIds(Long parentId)
│   │       │   │                                    @Query hasTransactions(Long categoryId)
│   │       │   ├── CategoryPersistenceAdapter.java  @Component implements CategoryPersistencePort
│   │       │   ├── CategoryJpaMapper.java            toDomain, toJpaEntity
│   │       │   └── CategoryTypeJpaMapper.java        toDomain(CategoryTypeJpaEntity): CategoryType
│   │       ├── event/
│   │       │   └── CategoryEventPublisherAdapter.java @Component implements CategoryEventPublisherPort
│   │       └── crosscontext/
│   │           ├── CategoryValidationAdapter.java   @Component implements
│   │           │                                    transaction/domain/port/outbound/CategoryValidationPort
│   │           │                                    Calls: GetCategoriesQuery
│   │           └── CategoryQueryAdapter.java         @Component implements
│   │                                                budget/domain/port/outbound/CategoryQueryPort
│   │                                                Calls: GetCategoriesQuery
│   │
│   └── config/
│       └── CategoryConfig.java          @Configuration
│                                        @Bean CategoryCommandService(...)
│                                        @Bean CategoryQueryService(...)
│
│══════════════════════════════════════════════════════════════════════════
│  TRANSACTION BOUNDED CONTEXT
│══════════════════════════════════════════════════════════════════════════
├── transaction/
│   │
│   ├── domain/                          PURE JAVA
│   │   ├── model/
│   │   │   ├── Transaction.java          Aggregate Root
│   │   │   │                             Fields: TransactionId id, UserId userId,
│   │   │   │                                     AccountId accountId, CategoryId categoryId,
│   │   │   │                                     Money amount, TransactionType transactionType,
│   │   │   │                                     LocalDate transactionDate, String description,
│   │   │   │                                     String merchantName, String referenceNumber,
│   │   │   │                                     boolean isRecurring, Long recurringTransactionId,
│   │   │   │                                     TransactionId transferPairId,
│   │   │   │                                     boolean isReconciled, AuditInfo auditInfo
│   │   │   │                             Methods: updateDetails(date, description, merchantName,
│   │   │   │                                                    categoryId): void
│   │   │   │                                      changeAccount(AccountId): void
│   │   │   │                                      linkToPair(TransactionId): void
│   │   │   │                                      markReconciled(): void
│   │   │   │                             Invariants:
│   │   │   │                               amount > 0 always
│   │   │   │                               transactionDate range (validated in service)
│   │   │   ├── RecurringTransaction.java Aggregate Root
│   │   │   │                             Fields: Long id, UserId userId, AccountId accountId,
│   │   │   │                                     CategoryId categoryId, Money amount,
│   │   │   │                                     TransactionType transactionType, Frequency frequency,
│   │   │   │                                     LocalDate startDate, LocalDate endDate,
│   │   │   │                                     LocalDate nextDueDate, String description,
│   │   │   │                                     String merchantName, boolean isActive, boolean autoPost
│   │   │   │                             Methods: advanceNextDueDate(): void
│   │   │   │                                      deactivate(): void
│   │   │   │                                      isDue(LocalDate today): boolean
│   │   │   ├── TransactionType.java      Enum: INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT
│   │   │   │                             Methods: isDebit(): boolean (EXPENSE || TRANSFER_OUT)
│   │   │   │                                      isCredit(): boolean (INCOME || TRANSFER_IN)
│   │   │   │                                      isTransfer(): boolean (TRANSFER_IN || TRANSFER_OUT)
│   │   │   └── Frequency.java            Enum: DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY
│   │   │                                 Methods: nextDate(LocalDate from): LocalDate
│   │   │
│   │   ├── port/
│   │   │   ├── inbound/
│   │   │   │   ├── CreateTransactionUseCase.java    interface: createTransaction(CreateTransactionCommand): TransactionId
│   │   │   │   ├── CreateTransactionCommand.java    record: UserId, AccountId, CategoryId, Money amount,
│   │   │   │   │                                            TransactionType, LocalDate transactionDate,
│   │   │   │   │                                            String description, String merchantName
│   │   │   │   ├── UpdateTransactionUseCase.java    interface: updateTransaction(UpdateTransactionCommand)
│   │   │   │   ├── UpdateTransactionCommand.java    record: TransactionId, UserId ownerId, AccountId newAccountId,
│   │   │   │   │                                            CategoryId, Money newAmount, LocalDate newDate,
│   │   │   │   │                                            String description, String merchantName
│   │   │   │   ├── DeleteTransactionUseCase.java    interface: deleteTransaction(TransactionId, UserId)
│   │   │   │   ├── CreateTransferUseCase.java       interface: createTransfer(CreateTransferCommand): TransferResult
│   │   │   │   ├── CreateTransferCommand.java       record: UserId, AccountId fromAccountId,
│   │   │   │   │                                            AccountId toAccountId, Money amount,
│   │   │   │   │                                            LocalDate transferDate, String description
│   │   │   │   ├── TransferResult.java              record: TransactionId outId, TransactionId inId
│   │   │   │   ├── UpdateTransferUseCase.java       interface: updateTransfer(UpdateTransferCommand)
│   │   │   │   ├── UpdateTransferCommand.java       record: TransactionId eitherLegId, UserId ownerId,
│   │   │   │   │                                            Money newAmount, LocalDate newDate, String description
│   │   │   │   ├── DeleteTransferUseCase.java       interface: deleteTransfer(TransactionId eitherLeg, UserId)
│   │   │   │   └── GetTransactionsQuery.java        interface:
│   │   │   │                                          getTransactions(UserId, TransactionFilter, Pageable):
│   │   │   │                                                          Page<TransactionView>
│   │   │   │                                          getTransactionById(TransactionId, UserId): Optional<TransactionView>
│   │   │   │       TransactionFilter.java            record: startDate, endDate, accountId, categoryId,
│   │   │   │                                                 transactionType, minAmount, maxAmount
│   │   │   │       TransactionView.java              record: id, accountId, accountName, categoryId,
│   │   │   │                                                 categoryName, categoryTypeCode, amount(Money),
│   │   │   │                                                 transactionType, transactionDate, description,
│   │   │   │                                                 merchantName, referenceNumber, isRecurring,
│   │   │   │                                                 transferPairId, isReconciled, createdAt
│   │   │   └── outbound/
│   │   │       ├── TransactionPersistencePort.java     interface: findByIdAndOwner, findPage, save,
│   │   │       │                                                  saveAndFlush, delete,
│   │   │       │                                                  updateTransferPairId(TransactionId, TransactionId)
│   │   │       ├── RecurringTransactionPersistencePort.java interface: findById, findActiveByOwner,
│   │   │       │                                                        findDueBy, save
│   │   │       ├── TransactionEventPublisherPort.java  extends EventPublisherPort
│   │   │       ├── AccountBalancePort.java             interface: (cross-context port)
│   │   │       │                                         applyDebit(AccountId, UserId, Money)
│   │   │       │                                         applyCredit(AccountId, UserId, Money)
│   │   │       │                                         reverseDebit(AccountId, UserId, Money)
│   │   │       │                                         reverseCredit(AccountId, UserId, Money)
│   │   │       │                                         canDebit(AccountId, UserId, Money): boolean
│   │   │       └── CategoryValidationPort.java         interface: (cross-context port)
│   │   │                                                 categoryExistsAndVisibleToUser(CategoryId, UserId): boolean
│   │   │                                                 getCategoryTypeCode(CategoryId): String
│   │   │
│   │   ├── service/
│   │   │   ├── TransactionCommandService.java    implements CreateTransactionUseCase,
│   │   │   │                                               UpdateTransactionUseCase,
│   │   │   │                                               DeleteTransactionUseCase
│   │   │   │                                     Constructor: (TransactionPersistencePort,
│   │   │   │                                                   AccountBalancePort,
│   │   │   │                                                   CategoryValidationPort,
│   │   │   │                                                   TransactionEventPublisherPort)
│   │   │   │                                     Rules: date range validation, category type check,
│   │   │   │                                            balance check before commit,
│   │   │   │                                            blocks delete on transfer legs
│   │   │   ├── TransferCommandService.java       implements CreateTransferUseCase,
│   │   │   │                                               UpdateTransferUseCase,
│   │   │   │                                               DeleteTransferUseCase
│   │   │   │                                     Constructor: (TransactionPersistencePort,
│   │   │   │                                                   AccountBalancePort,
│   │   │   │                                                   TransactionEventPublisherPort)
│   │   │   │                                     Two-phase insert for transfer creation
│   │   │   └── TransactionQueryService.java      implements GetTransactionsQuery
│   │   │                                         Constructor: (TransactionPersistencePort)
│   │   │
│   │   ├── event/
│   │   │   ├── TransactionCreated.java      record implements DomainEvent:
│   │   │   │                                  TransactionId, AccountId, Money, TransactionType, UserId
│   │   │   ├── TransactionDeleted.java      record implements DomainEvent:
│   │   │   │                                  TransactionId, AccountId, Money, TransactionType, UserId
│   │   │   ├── TransactionAmountChanged.java record implements DomainEvent:
│   │   │   │                                  TransactionId, AccountId, Money oldAmount,
│   │   │   │                                  Money newAmount, TransactionType, UserId
│   │   │   ├── TransferCreated.java         record implements DomainEvent:
│   │   │   │                                  TransactionId outId, TransactionId inId,
│   │   │   │                                  AccountId fromAccount, AccountId toAccount,
│   │   │   │                                  Money amount, UserId
│   │   │   └── RecurringTransactionDue.java record implements DomainEvent:
│   │   │                                      Long recurringId, UserId, boolean autoPost
│   │   │
│   │   └── exception/
│   │       ├── TransactionNotFoundException.java        extends DomainException
│   │       ├── InvalidTransactionDateException.java     extends DomainException,
│   │       │                                               errorCode=INVALID_TRANSACTION_DATE
│   │       ├── TransferDeleteViaTransactionException.java extends DomainException,
│   │       │                                               errorCode=USE_TRANSFER_DELETE
│   │       └── SameAccountTransferException.java        extends DomainException,
│   │                                                        errorCode=SAME_ACCOUNT_TRANSFER
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── TransactionController.java      @RestController @RequestMapping("/api/v1/transactions")
│   │   │       │                                   GET    /           -> PageResponseDto<TransactionResponseDto>
│   │   │       │                                   POST   /           -> 201 + TransactionResponseDto
│   │   │       │                                   GET    /{id}       -> TransactionResponseDto
│   │   │       │                                   PUT    /{id}       -> TransactionResponseDto
│   │   │       │                                   DELETE /{id}       -> 204
│   │   │       ├── TransferController.java          @RestController @RequestMapping("/api/v1/transfers")
│   │   │       │                                   POST   /           -> 201 + TransferResponseDto
│   │   │       │                                   GET    /{id}       -> TransferResponseDto
│   │   │       │                                   PUT    /{id}       -> TransferResponseDto
│   │   │       │                                   DELETE /{id}       -> 204
│   │   │       ├── CreateTransactionRequestDto.java  record with Bean Validation
│   │   │       ├── UpdateTransactionRequestDto.java  record with Bean Validation
│   │   │       ├── CreateTransferRequestDto.java     record with Bean Validation
│   │   │       ├── UpdateTransferRequestDto.java     record
│   │   │       ├── TransactionResponseDto.java        record (full view including accountName, categoryName)
│   │   │       ├── TransferResponseDto.java           record (both legs)
│   │   │       └── TransactionRequestMapper.java      Converts DTOs <-> Commands and Views <-> ResponseDtos
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   ├── TransactionJpaEntity.java           @Entity @Table(name="transactions")
│   │       │   │                                       extends AuditableJpaEntity
│   │       │   ├── RecurringTransactionJpaEntity.java  @Entity @Table(name="recurring_transactions")
│   │       │   │                                       extends AuditableJpaEntity
│   │       │   ├── TransactionJpaRepository.java       extends JpaRepository + JpaSpecificationExecutor
│   │       │   ├── RecurringTransactionJpaRepository.java extends JpaRepository
│   │       │   ├── TransactionPersistenceAdapter.java  @Component implements TransactionPersistencePort
│   │       │   ├── RecurringTransactionPersistenceAdapter.java @Component implements RecurringTransactionPersistencePort
│   │       │   ├── TransactionJpaMapper.java            toDomain, toJpaEntity
│   │       │   ├── RecurringTransactionJpaMapper.java   toDomain, toJpaEntity
│   │       │   └── TransactionSpecification.java        JPA Specification<TransactionJpaEntity> for filters
│   │       └── event/
│   │           └── TransactionEventPublisherAdapter.java @Component implements TransactionEventPublisherPort
│   │
│   └── config/
│       └── TransactionConfig.java       @Configuration
│                                        @Bean TransactionCommandService(...)
│                                        @Bean TransferCommandService(...)
│                                        @Bean TransactionQueryService(...)
│
│══════════════════════════════════════════════════════════════════════════
│  BUDGET BOUNDED CONTEXT
│══════════════════════════════════════════════════════════════════════════
├── budget/
│   │
│   ├── domain/                          PURE JAVA
│   │   ├── model/
│   │   │   ├── Budget.java              Aggregate Root
│   │   │   │                            Fields: BudgetId id, UserId userId, CategoryId categoryId,
│   │   │   │                                    BudgetPeriodType periodType, Money limit,
│   │   │   │                                    LocalDate startDate, LocalDate endDate,
│   │   │   │                                    boolean rolloverEnabled, Integer alertThresholdPct,
│   │   │   │                                    boolean isActive, AuditInfo auditInfo
│   │   │   │                            Methods:
│   │   │   │                              evaluateStatus(Money spent): BudgetStatus
│   │   │   │                              isThresholdBreached(Money spent): boolean
│   │   │   │                              computePeriodRange(LocalDate today): DateRange
│   │   │   │                              updateLimit(Money newLimit): void
│   │   │   │                              updateAlertThreshold(Integer pct): void
│   │   │   │                              deactivate(): void
│   │   │   ├── BudgetPeriodType.java    Enum: WEEKLY, MONTHLY, QUARTERLY, ANNUALLY, CUSTOM
│   │   │   │                            Methods: computeRange(LocalDate start, LocalDate today): DateRange
│   │   │   │                              MONTHLY: 1st of month -> last day of month
│   │   │   │                              WEEKLY: start -> start + 6 days
│   │   │   │                              QUARTERLY: quarter start -> quarter end
│   │   │   │                              ANNUALLY: Jan 1 -> Dec 31 of start year
│   │   │   │                              CUSTOM: startDate -> endDate (from budget)
│   │   │   └── BudgetStatus.java        Enum: ON_TRACK (< 75%), WARNING (75-99%),
│   │   │                                      OVER_BUDGET (>= 100%)
│   │   │                                Methods: from(BigDecimal percentageUsed): BudgetStatus
│   │   │
│   │   ├── port/
│   │   │   ├── inbound/
│   │   │   │   ├── CreateBudgetUseCase.java         interface: createBudget(CreateBudgetCommand): BudgetId
│   │   │   │   ├── CreateBudgetCommand.java         record: UserId, CategoryId, BudgetPeriodType,
│   │   │   │   │                                            Money limit, LocalDate startDate,
│   │   │   │   │                                            LocalDate endDate, boolean rolloverEnabled,
│   │   │   │   │                                            Integer alertThresholdPct
│   │   │   │   ├── UpdateBudgetUseCase.java         interface: updateBudget(UpdateBudgetCommand)
│   │   │   │   ├── UpdateBudgetCommand.java         record: BudgetId, UserId ownerId,
│   │   │   │   │                                            Money newLimit, Integer newAlertThresholdPct
│   │   │   │   ├── DeactivateBudgetUseCase.java     interface: deactivateBudget(BudgetId, UserId)
│   │   │   │   └── GetBudgetsQuery.java             interface:
│   │   │   │                                          getActiveBudgets(UserId): List<BudgetView>
│   │   │   │                                          getBudgetSummary(BudgetId, UserId): BudgetSummaryView
│   │   │   │       BudgetView.java                  record: id, categoryId, categoryName, periodType,
│   │   │   │                                                limit(Money), startDate, endDate,
│   │   │   │                                                rolloverEnabled, alertThresholdPct, isActive
│   │   │   │       BudgetSummaryView.java           record: budget(BudgetView), spent(Money),
│   │   │   │                                                remaining(Money), percentageUsed(BigDecimal),
│   │   │   │                                                status(BudgetStatus), periodStart, periodEnd
│   │   │   └── outbound/
│   │   │       ├── BudgetPersistencePort.java        interface: findById(BudgetId, UserId),
│   │   │       │                                               findActiveByOwner(UserId),
│   │   │       │                                               findActiveByOwnerAndCategoryAndPeriod(...),
│   │   │       │                                               save(Budget): Budget
│   │   │       ├── BudgetSpendCalculationPort.java   interface:
│   │   │       │                                       calculateSpend(UserId, List<CategoryId>, DateRange): Money
│   │   │       ├── BudgetEventPublisherPort.java      extends EventPublisherPort
│   │   │       └── CategoryQueryPort.java             interface: (cross-context)
│   │   │                                               getCategoryTypeCode(CategoryId): Optional<String>
│   │   │                                               getCategoryAndDescendantIds(CategoryId): List<CategoryId>
│   │   │
│   │   ├── service/
│   │   │   ├── BudgetCommandService.java    implements CreateBudgetUseCase, UpdateBudgetUseCase,
│   │   │   │                                           DeactivateBudgetUseCase
│   │   │   │                                Constructor: (BudgetPersistencePort, CategoryQueryPort,
│   │   │   │                                              BudgetEventPublisherPort)
│   │   │   │                                Rules: no TRANSFER category, CUSTOM needs end_date,
│   │   │   │                                       no duplicate active budget per category+period
│   │   │   └── BudgetQueryService.java      implements GetBudgetsQuery
│   │   │                                    Constructor: (BudgetPersistencePort,
│   │   │                                                  BudgetSpendCalculationPort,
│   │   │                                                  CategoryQueryPort,
│   │   │                                                  BudgetEventPublisherPort)
│   │   │                                    getBudgetSummary: computes period range, queries spend,
│   │   │                                    calls Budget.evaluateStatus(), publishes
│   │   │                                    BudgetThresholdExceeded if breached
│   │   │
│   │   ├── event/
│   │   │   └── BudgetThresholdExceeded.java  record implements DomainEvent:
│   │   │                                       BudgetId, UserId, BudgetStatus,
│   │   │                                       Money spent, Money limit
│   │   │
│   │   └── exception/
│   │       ├── BudgetNotFoundException.java          extends DomainException
│   │       ├── DuplicateBudgetException.java         extends DomainException,
│   │       │                                             errorCode=DUPLICATE_BUDGET
│   │       └── InvalidBudgetCategoryException.java   extends DomainException,
│   │                                                     errorCode=TRANSFER_CATEGORY_BUDGET
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── web/
│   │   │       ├── BudgetController.java            @RestController @RequestMapping("/api/v1/budgets")
│   │   │       │                                    GET    /              -> List<BudgetResponseDto>
│   │   │       │                                    POST   /              -> 201 + BudgetResponseDto
│   │   │       │                                    GET    /{id}          -> BudgetResponseDto
│   │   │       │                                    PUT    /{id}          -> BudgetResponseDto
│   │   │       │                                    DELETE /{id}          -> 204
│   │   │       │                                    GET    /{id}/summary  -> BudgetSummaryResponseDto
│   │   │       ├── CreateBudgetRequestDto.java       record with Bean Validation
│   │   │       ├── UpdateBudgetRequestDto.java       record
│   │   │       ├── BudgetResponseDto.java             record
│   │   │       ├── BudgetSummaryResponseDto.java      record: limit, spent, remaining, pct, status,
│   │   │       │                                              periodStart, periodEnd
│   │   │       └── BudgetRequestMapper.java           DTOs <-> Commands, Views <-> ResponseDtos
│   │   └── outbound/
│   │       ├── persistence/
│   │       │   ├── BudgetJpaEntity.java            @Entity @Table(name="budgets")
│   │       │   │                                   extends AuditableJpaEntity
│   │       │   ├── BudgetJpaRepository.java         extends JpaRepository<BudgetJpaEntity, Long>
│   │       │   │                                   findByUserIdAndIsActiveTrue(Long)
│   │       │   │                                   findByUserIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(...)
│   │       │   ├── BudgetPersistenceAdapter.java   @Component implements BudgetPersistencePort
│   │       │   ├── BudgetSpendCalculationAdapter.java @Component implements BudgetSpendCalculationPort
│   │       │   │                                   Uses: native SQL via EntityManager
│   │       │   │                                   @PersistenceContext EntityManager
│   │       │   │                                   Query: SELECT SUM(t.amount) FROM transactions t
│   │       │   │                                     WHERE t.user_id=:userId
│   │       │   │                                       AND t.category_id IN :categoryIds
│   │       │   │                                       AND t.transaction_type='EXPENSE'
│   │       │   │                                       AND t.transaction_date BETWEEN :start AND :end
│   │       │   └── BudgetJpaMapper.java             toDomain, toJpaEntity
│   │       └── event/
│   │           └── BudgetEventPublisherAdapter.java @Component implements BudgetEventPublisherPort
│   │
│   └── config/
│       └── BudgetConfig.java            @Configuration
│                                        @Bean BudgetCommandService(...)
│                                        @Bean BudgetQueryService(...)
│
│══════════════════════════════════════════════════════════════════════════
│  REPORTING BOUNDED CONTEXT (Read-Only CQRS)
│══════════════════════════════════════════════════════════════════════════
└── reporting/
    │
    ├── domain/
    │   ├── port/
    │   │   ├── inbound/
    │   │   │   └── GetDashboardQuery.java     interface: getDashboard(UserId): DashboardView
    │   │   │       DashboardView.java         record: netWorth(NetWorthView),
    │   │   │                                          currentMonthIncome(Money),
    │   │   │                                          currentMonthExpense(Money),
    │   │   │                                          netCashFlow(Money),
    │   │   │                                          accountBalances(List<AccountBalanceView>),
    │   │   │                                          topExpenseCategories(List<TopCategoryView>)
    │   │   │       NetWorthView.java          record: totalAssets(Money), totalLiabilities(Money),
    │   │   │                                          netWorth(Money)
    │   │   │       MonthlyFlowView.java       record: income(Money), expense(Money), netCashFlow(Money)
    │   │   │       AccountBalanceView.java    record: accountId, accountName, accountTypeCode,
    │   │   │                                          currentBalance(Money), isLiability
    │   │   │       TopCategoryView.java       record: categoryId, categoryName, totalSpent(Money)
    │   │   └── outbound/
    │   │       └── DashboardReadPort.java     interface:
    │   │                                        getNetWorth(UserId): NetWorthView
    │   │                                        getMonthlyFlow(UserId, YearMonth): MonthlyFlowView
    │   │                                        getTopExpenseCategories(UserId, YearMonth, int limit):
    │   │                                                              List<TopCategoryView>
    │   │                                        getAccountsWithBalances(UserId): List<AccountBalanceView>
    │   └── service/
    │       └── DashboardQueryService.java     implements GetDashboardQuery
    │                                          Constructor: (DashboardReadPort)
    │                                          Assembles DashboardView from read port results
    │
    ├── adapter/
    │   ├── inbound/
    │   │   └── web/
    │   │       ├── DashboardController.java   @RestController @RequestMapping("/api/v1/dashboard")
    │   │       │                              GET /summary -> 200 + DashboardResponseDto
    │   │       │                              Injects: GetDashboardQuery
    │   │       └── DashboardResponseDto.java  record (full dashboard shape)
    │   └── outbound/
    │       └── persistence/
    │           └── DashboardReadAdapter.java  @Component implements DashboardReadPort
    │                                          @PersistenceContext EntityManager
    │                                          Uses native SQL queries (NOT JpaRepository)
    │                                          All queries scoped to userId (no cross-user data)
    │
    └── config/
        └── ReportingConfig.java         @Configuration
                                         @Bean DashboardQueryService(...)
```

---

## Cross-Context Adapter Wiring Summary

The key architectural question is: "Where does the adapter that bridges two contexts live?"

**Rule**: Cross-context adapters live in the **providing** context (the one that has the data), but **implement** the port defined in the **consuming** context.

| Adapter Class | Lives In | Implements Port From | Purpose |
|---|---|---|---|
| `AccountBalanceAdapter` | `account/adapter/outbound/crosscontext/` | `transaction/domain/port/outbound/AccountBalancePort` | Transaction context updates account balances |
| `CategoryValidationAdapter` | `category/adapter/outbound/crosscontext/` | `transaction/domain/port/outbound/CategoryValidationPort` | Transaction context validates categories |
| `CategoryQueryAdapter` | `category/adapter/outbound/crosscontext/` | `budget/domain/port/outbound/CategoryQueryPort` | Budget context queries category hierarchy |

---

## Naming Reference Table

| What | Where | Naming Pattern |
|---|---|---|
| Inbound Port (command) | `domain/port/inbound/` | `{Action}{Entity}UseCase` |
| Inbound Port (query) | `domain/port/inbound/` | `Get{Entity}Query` |
| Domain Command | `domain/port/inbound/` | `{Action}{Entity}Command` |
| Domain View (query output) | `domain/port/inbound/` | `{Entity}View` |
| Outbound Port | `domain/port/outbound/` | `{Entity}PersistencePort`, `{Context}EventPublisherPort` |
| Cross-context Outbound Port | `domain/port/outbound/` (consuming context) | `{Providing Entity}Port` |
| Domain Aggregate | `domain/model/` | Plain noun |
| Domain Service (command) | `domain/service/` | `{Context}CommandService` |
| Domain Service (query) | `domain/service/` | `{Context}QueryService` |
| Domain Event | `domain/event/` | Past tense verb phrase |
| Domain Exception | `domain/exception/` | `{Reason}Exception` |
| REST Controller | `adapter/inbound/web/` | `{Entity}Controller` |
| Request DTO | `adapter/inbound/web/` | `{Action}{Entity}RequestDto` |
| Response DTO | `adapter/inbound/web/` | `{Entity}ResponseDto` |
| Web Mapper | `adapter/inbound/web/` | `{Entity}RequestMapper` |
| JPA Entity | `adapter/outbound/persistence/` | `{Entity}JpaEntity` |
| JPA Repository | `adapter/outbound/persistence/` | `{Entity}JpaRepository` |
| Persistence Adapter | `adapter/outbound/persistence/` | `{Entity}PersistenceAdapter` |
| JPA Mapper | `adapter/outbound/persistence/` | `{Entity}JpaMapper` |
| Event Publisher Adapter | `adapter/outbound/event/` | `{Context}EventPublisherAdapter` |
| Cross-Context Adapter | `adapter/outbound/crosscontext/` (providing context) | `{Port Name}Adapter` |
| Config Class | `config/` | `{Context}Config` |

---

## What is DELETED vs. What Replaces It

| Removed | Replaced By |
|---|---|
| `ddd-architecture.md` (as authority) | `hexagonal-architecture.md` |
| `common/entity/BaseAuditEntity` | `shared/adapter/outbound/persistence/AuditableJpaEntity` |
| `common/security/SecurityContextHelper` | `shared/adapter/inbound/web/SecurityContextHolder` |
| `common/dto/PageResponse` | `shared/adapter/inbound/web/dto/PageResponseDto` |
| `common/dto/ErrorResponse` | `shared/adapter/inbound/web/dto/ErrorResponseDto` |
| `common/config/JacksonConfig` | `shared/config/JacksonConfig` |
| `common/config/WebConfig` | `shared/config/WebConfig` |
| `{context}/application/command/*CommandHandler` | `{context}/domain/service/{Context}CommandService` |
| `{context}/application/query/*QueryHandler` | `{context}/domain/service/{Context}QueryService` |
| `{context}/application/dto/` | `{context}/adapter/inbound/web/*RequestDto`, `*ResponseDto` + `domain/port/inbound/*Command`, `*View` |
| `{context}/infrastructure/persistence/` | `{context}/adapter/outbound/persistence/` |
| `{context}/infrastructure/web/` | `{context}/adapter/inbound/web/` |
| `{context}/infrastructure/config/` | `{context}/config/` |
| `{context}/domain/repository/` interfaces | `{context}/domain/port/outbound/{Entity}PersistencePort` |
| `account/infrastructure/event/TransactionEventHandler` | Balance updates are synchronous via `AccountBalancePort`, not event-driven |
