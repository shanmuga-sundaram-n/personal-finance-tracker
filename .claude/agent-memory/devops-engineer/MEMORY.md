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
