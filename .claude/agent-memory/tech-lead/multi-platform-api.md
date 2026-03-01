# Multi-Platform API Architecture — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Status**: Authoritative — full-stack-dev implements from this document

---

## 1. Platform Requirements

| Platform | Timeline | Auth Model | Connectivity | Special Needs |
|---|---|---|---|---|
| React SPA (web) | Now (MVP) | Bearer token (cookie optional) | Always online | CORS, standard REST |
| React Native (mobile) | Phase 2 | Bearer token (header only) | Intermittent / offline | Lightweight payloads, ETag caching, push notifications |
| 3rd Party API consumers | Phase 3 | Bearer token (long-lived API key) | Online | Rate limiting per key, API keys distinct from session tokens |

**Core principle**: The backend API is **platform-agnostic from day one**. It does not know whether the caller is a browser or a mobile app. All platform-specific concerns are handled at the client, not the server — except CORS (browser-only) and push notification registration (mobile-only extension).

---

## 2. Authentication — Multi-Platform Design

### 2.1 Current Design (Session Tokens) — Already Correct

The current design uses `Authorization: Bearer {token}` header. This works for **all platforms**:
- Web SPA: stores token in `localStorage` or `sessionStorage`, attaches as header
- Mobile app: stores token in OS secure keychain (`SecureStore` in Expo / `Keychain` in bare RN), attaches as header
- 3rd party: stores token as API key, attaches as header

This is better than cookie-based sessions because cookies require `SameSite`/`Secure` flags and are subject to browser restrictions. Mobile apps do not use cookies at all.

### 2.2 Session Token vs. JWT — Decision

**Decision: Keep opaque session tokens stored in the `sessions` table (NOT JWT)**

Rationale:
1. **Immediate revocation**: Logout deletes the row. With JWT, you cannot revoke a token before expiry without a blocklist, which reintroduces statefulness.
2. **Simplicity**: No need to choose signing algorithm, manage signing keys, handle JWT parsing errors, or deal with clock skew.
3. **Multi-device support**: The `sessions` table already supports one row per device. No changes needed.
4. **Security**: An opaque token reveals nothing about the user. A JWT payload is base64-decodable.

**Consequence for mobile**: Mobile apps that go offline do not need to validate token freshness locally. The app retries on reconnect; the server validates. Token is 7-day sliding window (refreshed on activity). This is sufficient for mobile use.

### 2.3 Token Refresh (Mobile Extension — Phase 2)

Add a `POST /api/v1/auth/refresh` endpoint:
- Request: current valid token in `Authorization: Bearer` header
- Response: new token, new `expiresAt`
- Old token is invalidated (or optionally kept alive until its natural expiry — simpler)
- Mobile app calls this endpoint when the token is within 24 hours of expiry

This endpoint does NOT exist in MVP. The session simply expires after 7 days and the user re-logs in.

### 2.4 Device Registration (Mobile Extension — Phase 2)

For push notifications, the mobile app sends a **device token** (FCM/APNs token) at login or on background refresh:

```
POST /api/v1/auth/devices
Authorization: Bearer {session_token}
{
  "platform": "ios",          // or "android"
  "deviceToken": "fcm-or-apns-token-string",
  "appVersion": "1.2.0"
}
```

This requires a `devices` table (Phase 2 addition):
```sql
CREATE TABLE finance_tracker.devices (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES finance_tracker.users(id),
  session_id BIGINT REFERENCES finance_tracker.sessions(id) ON DELETE CASCADE,
  platform VARCHAR(10) NOT NULL,       -- 'ios', 'android'
  device_token VARCHAR(500) NOT NULL,
  app_version VARCHAR(20),
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_devices_token ON finance_tracker.devices(device_token) WHERE is_active = true;
```

---

## 3. API Design — URL Structure and Versioning

### 3.1 URL Versioning (Confirmed)

All endpoints are prefixed `/api/v1/`. This is correct for multi-platform because:
- URL versioning is visible to all clients — no hidden header negotiation
- Works identically for web, mobile, and 3rd party
- Simple to route in API gateways (AWS API Gateway, Nginx, Kong)
- v2 can coexist at `/api/v2/` with no impact on v1 consumers

```
https://api.personalfinance.app/api/v1/accounts
https://api.personalfinance.app/api/v2/accounts   (when breaking changes needed)
```

### 3.2 No HATEOAS

**Decision: Do NOT implement HATEOAS.**

HATEOAS adds `_links` objects to every response. The purported benefit (discoverability) is rarely used in practice — mobile and web apps both have hard-coded route knowledge. The cost (doubled response payload, implementation complexity, link maintenance as URLs evolve) does not justify the benefit for this project.

Mobile apps parse `id` fields and construct URLs client-side. This is standard practice (Stripe, GitHub API v3, Plaid all do this).

### 3.3 Endpoint Catalogue (Platform-Agnostic)

All endpoints below work identically for web and mobile:

```
# Identity
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
POST   /api/v1/auth/refresh          (Phase 2)
POST   /api/v1/auth/devices          (Phase 2 — mobile push registration)
DELETE /api/v1/auth/devices/{id}     (Phase 2 — deregister device)

# Account Management
GET    /api/v1/accounts
POST   /api/v1/accounts
GET    /api/v1/accounts/{id}
PUT    /api/v1/accounts/{id}
DELETE /api/v1/accounts/{id}
GET    /api/v1/accounts/net-worth

# Categories
GET    /api/v1/categories
POST   /api/v1/categories
GET    /api/v1/categories/{id}
PUT    /api/v1/categories/{id}
DELETE /api/v1/categories/{id}

# Transactions
GET    /api/v1/transactions
POST   /api/v1/transactions
GET    /api/v1/transactions/{id}
PUT    /api/v1/transactions/{id}
DELETE /api/v1/transactions/{id}
GET    /api/v1/accounts/{id}/transactions

# Transfers
POST   /api/v1/transfers
GET    /api/v1/transfers/{id}
PUT    /api/v1/transfers/{id}
DELETE /api/v1/transfers/{id}

# Budgets
GET    /api/v1/budgets
POST   /api/v1/budgets
GET    /api/v1/budgets/{id}
PUT    /api/v1/budgets/{id}
DELETE /api/v1/budgets/{id}
GET    /api/v1/budgets/{id}/summary

# Recurring Transactions (Phase 2)
GET    /api/v1/recurring-transactions
POST   /api/v1/recurring-transactions
GET    /api/v1/recurring-transactions/{id}
PUT    /api/v1/recurring-transactions/{id}
DELETE /api/v1/recurring-transactions/{id}
GET    /api/v1/recurring-transactions/upcoming  # 30-day lookahead

# Dashboard / Reporting
GET    /api/v1/dashboard/summary
GET    /api/v1/reports/monthly-summary          (Phase 2)
GET    /api/v1/reports/income-expense-trend     (Phase 2)
```

---

## 4. Response Format — Consistent Envelope

### 4.1 Standard Responses (Unchanged from Code Standards)

```json
// Single resource (200, 201)
{
  "id": 42,
  "name": "Chase Checking",
  "currentBalance": "1250.0000"
}

// Paginated collection (200)
{
  "content": [...],
  "page": 0,
  "size": 30,
  "totalElements": 87,
  "totalPages": 3
}

// Error (4xx, 5xx)
{
  "status": 422,
  "error": "VALIDATION_ERROR",
  "message": "One or more fields are invalid",
  "errors": [{"field": "amount", "code": "MUST_BE_POSITIVE", "message": "..."}],
  "timestamp": "2026-02-28T10:30:00Z",
  "path": "/api/v1/transactions"
}
```

### 4.2 No Wrapping Envelope

**Decision: Do NOT add a top-level `{ "data": {...}, "meta": {...} }` wrapper.**

Some APIs (JSON:API spec, GraphQL) use a wrapper envelope. This is unnecessary complexity for REST. The mobile app parses the same JSON shape as the web app. Adding a wrapper would require both clients to unwrap before using data, adding code with zero benefit.

### 4.3 Mobile-Specific: Sparse Fieldsets (Phase 2)

Mobile apps on slow connections benefit from reduced payloads. Implement sparse fieldsets as an opt-in query parameter:

```
GET /api/v1/transactions?fields=id,amount,transactionDate,merchantName
```

This returns only the specified fields per item. Implementation: a custom `@FieldFilter` aspect or Jackson `FilterProvider` applied based on the `fields` query param.

**Phase 2 feature** — do not implement in MVP. Mobile app receives full payloads in MVP. Typical transaction payload is ~400 bytes — negligible even on slow connections.

---

## 5. CORS Configuration

CORS is a **browser-only concern**. Mobile apps (React Native) do not send CORS preflight requests — the OS handles HTTP directly, not through a browser sandbox. 3rd party API consumers also do not need CORS.

### 5.1 MVP CORS Policy

```java
// infrastructure/config/WebConfig.java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(
            "http://localhost:3000",         // local development (React)
            "https://app.personalfinance.app" // production web app (update when known)
        )
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With")
        .exposedHeaders("Location")          // Expose Location header on 201 responses
        .allowCredentials(false)             // Not needed — we use Authorization header, not cookies
        .maxAge(3600);                        // Preflight cache: 1 hour
}
```

**Note**: `allowCredentials(false)` because we use `Authorization: Bearer` header, not cookies. If you set `allowCredentials(true)` you cannot use wildcard `*` for origins — all origins must be explicitly listed anyway.

### 5.2 Environment-Based CORS Origins

Origins must be configurable per environment:

```yaml
# application.yaml
app:
  cors:
    allowed-origins:
      - "http://localhost:3000"
      - "http://localhost:5173"   # Vite dev server
```

```yaml
# application-prod.yaml
app:
  cors:
    allowed-origins:
      - "https://app.personalfinance.app"
```

```java
@Value("${app.cors.allowed-origins}")
private List<String> allowedOrigins;
```

### 5.3 OpenAPI / Swagger UI CORS

The Swagger UI at `/swagger-ui.html` is only for development. It is disabled in production:

```yaml
# application-prod.yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

---

## 6. Push Notifications (Phase 2)

Push notifications are required for:
- Budget threshold exceeded (e.g., "You've used 90% of your Dining budget")
- Recurring transaction due reminder (when `auto_post = false`)
- Transfer confirmation (Phase 3)

### 6.1 Architecture

```
Server (Spring Boot)
    ↓ HTTP POST
FCM (Firebase Cloud Messaging) — handles both iOS (via APNs) and Android
    ↓
Mobile App
```

No direct APNs integration needed — FCM handles both platforms. This reduces server-side complexity to a single HTTP client call to FCM's REST API.

### 6.2 Server-Side Implementation (Phase 2)

```java
// shared/infrastructure/notification/PushNotificationService.java
@Service
public class PushNotificationService {

    private final FcmClient fcmClient;  // Thin wrapper around FCM HTTP API
    private final DeviceRepository deviceRepository;

    public void sendBudgetAlert(UserId userId, Budget budget, BudgetStatus status) {
        List<Device> devices = deviceRepository.findActiveByUser(userId);
        devices.forEach(device -> fcmClient.send(FcmMessage.builder()
            .token(device.getDeviceToken())
            .title("Budget Alert")
            .body(buildBudgetAlertBody(budget, status))
            .data(Map.of("type", "BUDGET_ALERT", "budgetId", budget.id().toString()))
            .build()));
    }
}
```

The `data` payload allows the mobile app to deep-link to the specific budget screen on notification tap.

### 6.3 Event-Driven Trigger

```java
// budget/application/event/BudgetAlertHandler.java
@Component
public class BudgetAlertHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BudgetThresholdExceeded event) {
        pushNotificationService.sendBudgetAlert(
            event.userId(), event.budget(), event.status()
        );
    }
}
```

Using `AFTER_COMMIT` ensures the notification is only sent if the transaction that triggered the event actually committed.

---

## 7. Offline Support Considerations (Mobile)

React Native apps need to work without connectivity. The API should support patterns that make offline-first feasible.

### 7.1 ETags for Cache Validation

Add `ETag` support to list endpoints:

```java
// infrastructure/web/ETagFilter.java
// Spring MVC supports this natively via ShallowEtagHeaderFilter
@Bean
public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
}
```

The mobile app sends `If-None-Match: {etag}` on subsequent requests. If content has not changed, server returns `304 Not Modified` with no body — saving bandwidth and battery.

**Priority endpoints for ETags**: `GET /api/v1/accounts`, `GET /api/v1/categories` (rarely changing data).

### 7.2 Last-Modified Headers

Add `Last-Modified` response headers to list endpoints. Mobile app sends `If-Modified-Since` to get only changed data. Implementation:

```java
// In AccountController
return ResponseEntity.ok()
    .lastModified(latestUpdatedAt.toInstant())
    .body(accountService.listAccounts(userId));
```

### 7.3 Sync Endpoint (Phase 2)

For true offline-first, add a delta-sync endpoint:

```
GET /api/v1/sync?since=2026-02-01T00:00:00Z
```

Response:
```json
{
  "serverTime": "2026-02-28T10:30:00Z",
  "accounts": {
    "created": [...],
    "updated": [...],
    "deleted": [42, 67]
  },
  "transactions": {
    "created": [...],
    "updated": [...],
    "deleted": [101]
  },
  "categories": { ... },
  "budgets": { ... }
}
```

The mobile app applies this delta to its local SQLite store (e.g., WatermelonDB or Realm). This requires soft-delete tombstones (we already have `is_active` = false, which can serve as a deletion marker). The `updated_at` timestamp is used to filter changes since a given time.

**Phase 2 feature** — requires mobile app architecture decisions. Do not pre-build server-side sync without confirmed mobile architecture.

### 7.4 Idempotency Keys

Mobile apps may retry requests on reconnect. Without idempotency, a transaction could be created twice (network sent request, connection dropped, app retried).

Add optional `Idempotency-Key` header to all POST endpoints:

```
POST /api/v1/transactions
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{...}
```

Server stores the key + response in a short-lived cache (Redis or DB table). If the same key arrives again within 24 hours, return the cached response without re-executing.

**Phase 2 feature** — requires Redis or a `idempotency_keys` table. Not in MVP.

---

## 8. Rate Limiting

### 8.1 MVP Rate Limiting (Current)

Login endpoint: in-memory `ConcurrentHashMap` with 5 attempts per 5 minutes per IP. This is sufficient for MVP because:
- Single instance deployment
- Auth endpoint is the only attack surface

### 8.2 Phase 2: Per-Platform Rate Limiting

When the API has web + mobile + 3rd party consumers, apply different rate limits:

| Consumer | Limit | Identification |
|---|---|---|
| Web SPA | 300 req/min per user | `Authorization` header (user's session token) |
| Mobile app | 120 req/min per user | `Authorization` header |
| 3rd Party API | 60 req/min per API key | `Authorization` header (API key prefix: `pft_live_`) |
| Unauthenticated | 20 req/min per IP | Source IP (`X-Forwarded-For` behind load balancer) |

Implementation: Spring-based `HandlerInterceptor` + Redis for distributed rate limit counting. Bucket4j library integrates cleanly with Spring Boot.

**Response on rate limit**: HTTP 429 with `Retry-After` header:
```json
{
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please retry after 60 seconds.",
  "retryAfterSeconds": 60
}
```

### 8.3 X-RateLimit Headers (Phase 2)

Standard rate-limit feedback headers for clients:

```
X-RateLimit-Limit: 300
X-RateLimit-Remaining: 247
X-RateLimit-Reset: 1709113200
```

Mobile apps use `X-RateLimit-Remaining` to back off before hitting the limit.

---

## 9. Platform Detection and `X-Client-Platform` Header (Optional)

**Decision: Do NOT route behavior based on client platform in MVP.**

Some APIs branch logic on `User-Agent` or a custom `X-Client-Platform` header. This adds server-side complexity and makes testing harder. All platform-specific differences (push notification registration, offline sync) are separate endpoint extensions, not conditionals in existing endpoints.

If platform telemetry is needed (e.g., logging which platform made which request), use a passive `X-Client-Platform: web|ios|android` header that is **logged only**, never used for conditional logic.

---

## 10. API Documentation (OpenAPI)

SpringDoc OpenAPI serves the spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html` in development.

Mobile app developer workflow:
1. Open `https://api.personalfinance.app/swagger-ui.html` (dev/staging only)
2. Authenticate via the "Authorize" button with a valid Bearer token
3. Test endpoints interactively

Production: OpenAPI UI is disabled. The spec YAML is committed at `docs/openapi.yaml` and kept in sync.

### 10.1 SpringDoc Configuration

```java
// shared/infrastructure/config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Personal Finance Tracker API")
                .description("REST API for web and mobile personal finance management")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("UUID")
                        .description("Session token obtained from POST /api/v1/auth/login")));
    }
}
```

---

## 11. Breaking Change Policy

When a breaking API change is needed (field removed, type changed, required field added):
1. Bump version: add `/api/v2/{endpoint}` alongside v1
2. Maintain v1 for **minimum 6 months** after v2 launch
3. Send deprecation warning via `Deprecation` header on v1 responses:
   ```
   Deprecation: true
   Sunset: Sat, 01 Jan 2027 00:00:00 GMT
   Link: </api/v2/accounts>; rel="successor-version"
   ```
4. Mobile apps must handle graceful degradation (never crash on unknown JSON fields — use `FAIL_ON_UNKNOWN_PROPERTIES = false` in the mobile JSON parser)

**Note**: On the server side, `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false` is already set (from existing JacksonConfig) — this means older mobile app versions will not break when new fields are added to responses.

---

## 12. Security Hardening for Multi-Platform

### 12.1 Security Headers

Add via `WebConfig` or a security filter:

```java
// applied to all API responses
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("X-Frame-Options", "DENY");
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
response.setHeader("Pragma", "no-cache");
// Note: X-XSS-Protection is obsolete in modern browsers but harmless
// Strict-Transport-Security is set at the load balancer / CDN level, not the app
```

### 12.2 Token Security

- Tokens are UUID v4 (128-bit random) — cryptographically sufficient
- Store in DB with no hashing (unlike passwords) — the token IS the credential and must be retrieved for comparison
- Transmit ONLY over HTTPS — enforce at load balancer level
- Mobile app stores token in OS secure enclave (`SecureStore` in Expo / `Keychain` in React Native bare)
- Web app stores token in `localStorage` (acceptable for non-sensitive SPAs; HttpOnly cookie is more secure but requires `allowCredentials(true)` and `SameSite=Strict`, which conflicts with mobile API sharing)

### 12.3 Certificate Pinning (Mobile — Phase 2)

Mobile apps should pin the TLS certificate for `api.personalfinance.app` to prevent man-in-the-middle attacks. This is a client-side concern, implemented in React Native via `fetch` overrides or a library like `react-native-ssl-pinning`. No server changes needed.
