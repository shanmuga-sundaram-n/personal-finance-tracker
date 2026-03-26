# DevOps Engineer Memory — Personal Finance Tracker
**Last Updated**: 2026-03-19

This file is loaded into every session. Keep it under 200 lines.

---

## Infrastructure Snapshot

| Component | Detail |
|---|---|
| Backend port | 8080 (internal), proxied via Nginx |
| Frontend/Nginx port | 3000 (exposed) |
| PostgreSQL port | 49883 (host) → 5432 (container) |
| DB name | `personal-finance-tracker` |
| DB schema | `finance_tracker` |
| DB user | `pft-app-user` / `pft-app-user-secret` |
| Java runtime | eclipse-temurin 17 |
| Node runtime | 20 |

---

## Key Files

| File | Purpose |
|---|---|
| `Dockerfile` | Backend multi-stage build |
| `frontend/Dockerfile` | Frontend multi-stage → nginx |
| `localEnvironment/docker-compose.yaml` | Local dev stack |
| `docker-compose.yml` | CI/production compose |
| `Makefile` | `make start` / `make stop` |
| `frontend/nginx.conf` | SPA routes + `/api/*` proxy to backend:8080 |
| `.github/workflows/` | CI/CD pipeline |
| `build.gradle.kts` | Root Gradle config |
| `settings.gradle.kts` | Multi-module: `application`, `acceptance`, `database` |

---

## Gradle Multi-Module Structure

```
root/
├── application/   (main Spring Boot app — source + unit/repo tests)
├── acceptance/    (integration tests — @SpringBootTest + RestAssured + Testcontainers)
└── database/      (placeholder — Liquibase files here are duplicates, use application/ only)
```

Migrations live ONLY in `application/src/main/resources/db.changelog/changes/`.

---

## CI/CD Notes

When modifying GitHub Actions:
- Cache key for Gradle: hash of `build.gradle.kts` + `settings.gradle.kts`
- Cache key for npm: hash of `frontend/package-lock.json`
- Integration tests require Docker (Testcontainers uses postgres:15.2)
- Use `eclipse-temurin` distribution, Java 17

---

## Docker Compose Services

Services expected in docker-compose:
- `backend` — Spring Boot on port 8080
- `frontend` — Nginx on port 3000, proxies `/api/*` to backend
- `postgres` — PostgreSQL 15.2 on port 49883:5432

---

## Application Health Feedback Loop (YOUR responsibility after every deploy)

Run after every deploy before reporting done:
```bash
.claude/hooks/verify-app-health.sh           # FEATURE, HOTFIX, CHORE
.claude/hooks/verify-app-health.sh --quick   # UI-ONLY (layers 4+5 only)
```

| Layer | Checks | Catches |
|---|---|---|
| 1 | `./gradlew :application:compileJava` | Compile errors |
| 2 | `./gradlew :application:test` — runs `ApplicationContextLoadTest` | Test failures + Spring bean wiring (duplicate beans, missing @Bean) |
| 3 | `npm run build` | Frontend TypeScript errors |
| 4 | `docker compose ps` | Containers crashed or not started |
| 5 | `curl :8080` + `curl :3000` | Runtime startup failures |

**Never report DEPLOYED until all 5 layers are green.**

Standard deploy pattern:
```bash
docker compose up --build backend -d    # rebuild + restart
sleep 10                                # wait for JVM startup
.claude/hooks/verify-app-health.sh      # verify
```

Smoke test: `POST localhost:8080/api/v1/auth/login` with bad creds → HTTP 401/422 = alive. HTTP `000` = down.

## Lessons Learned
- 2026-03-26: CHORE track skipped smoke test. Backend had duplicate bean wiring bug (`TransactionCommandService`
  + `TransactionApplicationService` both implementing same inbound port interfaces). User noticed manually.
  `verify-app-health.sh` Layer 2 (`ApplicationContextLoadTest`) would have caught it. Health gate is now
  non-negotiable after every deploy on every track.
