# Personal Finance Tracker

A full-stack personal finance management application built with **Spring Boot 3** (Java 21) and **React 19** (TypeScript), following strict **Hexagonal Architecture** and **Domain-Driven Design** principles.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Development](#development)
- [Architecture Overview](#architecture-overview)
- [Agent Pipeline](#agent-pipeline)

---

## Features

| Module | Capabilities |
|---|---|
| **Auth** | Register, login, logout with opaque Bearer token sessions |
| **Accounts** | Checking, Savings, Credit Card, Loan, Investment — with real-time balance tracking |
| **Categories** | System-seeded + custom income/expense categories with hierarchy |
| **Transactions** | Income, Expense, and Transfers (debit/credit pairs) with pagination and filtering |
| **Budgets** | Weekly/Monthly/Quarterly/Annual budgets with rollover and alert thresholds |
| **Dashboard** | Net worth, monthly cash flow, top expense categories, budget alerts, recent transactions |
| **Reports** | Monthly spending breakdown by category, 6-month income vs expense trend chart |
| **Profile** | Editable first/last name and preferred currency (propagated app-wide) |

---

## Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.2 |
| Language | Java 17 source / Java 21 runtime |
| Database | PostgreSQL 15.2 |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Liquibase |
| Build | Gradle 8.0 |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Architecture Tests | ArchUnit 1.2.1 |
| Password Hashing | Spring Security Crypto (BCrypt) |

### Frontend
| Layer | Technology |
|---|---|
| Framework | React 19 |
| Language | TypeScript 5.9 |
| Build | Vite 7 |
| Routing | React Router 7 |
| Forms | React Hook Form + Zod |
| Styling | Tailwind CSS 3 + Shadcn/ui |
| Charts | Recharts 3 |
| Toasts | Sonner |
| Icons | Lucide React |

### Infrastructure
| Component | Technology |
|---|---|
| Container Runtime | Docker + Docker Compose |
| Web Server | Nginx (SPA + API reverse proxy) |
| Backend Image | eclipse-temurin:21-jre-alpine |
| Frontend Image | node:20-alpine → nginx:alpine |

---

## Quick Start

### Prerequisites
- Docker and Docker Compose installed

### Start All Services

```bash
git clone <repo-url>
cd personal-finance-tracker
make start
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

### Stop Services

```bash
make stop
```

### Other Makefile Commands

```bash
make build   # Build Docker images without starting
make logs    # Tail all container logs
make ps      # Show container status
make clean   # Stop and remove all data volumes (resets database)
```

---

## Usage

### Register & Login

1. Open http://localhost:3000
2. Click **Register** and create an account
3. Log in with your credentials
4. You are redirected to the **Dashboard**

### Setting Up

1. **Set your preferred currency** → Settings (top-right avatar) → Profile → Edit Profile → Save
2. **Create accounts** → Accounts → Create Account (currency defaults to your preferred currency)
3. **Create categories** → Categories → Create Category (or use the seeded defaults)
4. **Log transactions** → Transactions → New Transaction
5. **Set budgets** → Budgets → Create Budget
6. **View reports** → Reports (spending breakdown, trend chart)

### Preferred Currency

Changing preferred currency in Profile Settings:
- Pre-fills the currency field when creating new Accounts and Budgets (read-only — change from Profile)
- Drives the currency symbol on the Dashboard (net worth, cash flow)
- Used as the fallback in all `MoneyDisplay` components

---

## API Documentation

The full REST API is available via Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

All endpoints (except `/api/v1/auth/register` and `/api/v1/auth/login`) require a Bearer token:

```http
Authorization: Bearer <token>
```

### Endpoints Summary

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user |
| `POST` | `/api/v1/auth/login` | Login, returns token |
| `POST` | `/api/v1/auth/logout` | Invalidate session |
| `GET` | `/api/v1/auth/me` | Get current user profile |
| `PUT` | `/api/v1/users/me` | Update profile (name, currency) |
| `GET` | `/api/v1/accounts` | List accounts |
| `POST` | `/api/v1/accounts` | Create account |
| `GET` | `/api/v1/accounts/{id}` | Get account by ID |
| `PUT` | `/api/v1/accounts/{id}` | Update account |
| `DELETE` | `/api/v1/accounts/{id}` | Deactivate account |
| `GET` | `/api/v1/accounts/net-worth` | Get net worth summary |
| `GET` | `/api/v1/categories` | List categories (optional `?type=INCOME\|EXPENSE`) |
| `POST` | `/api/v1/categories` | Create category |
| `PUT` | `/api/v1/categories/{id}` | Update category |
| `DELETE` | `/api/v1/categories/{id}` | Delete category |
| `GET` | `/api/v1/transactions` | List transactions (paginated, filterable) |
| `POST` | `/api/v1/transactions` | Create income/expense transaction |
| `POST` | `/api/v1/transactions/transfers` | Create transfer (2-sided) |
| `PUT` | `/api/v1/transactions/{id}` | Update transaction |
| `DELETE` | `/api/v1/transactions/{id}` | Delete transaction |
| `GET` | `/api/v1/budgets` | List budgets |
| `POST` | `/api/v1/budgets` | Create budget |
| `PUT` | `/api/v1/budgets/{id}` | Update budget |
| `DELETE` | `/api/v1/budgets/{id}` | Delete budget |
| `GET` | `/api/v1/reports/dashboard` | Dashboard summary |
| `GET` | `/api/v1/reports/spending` | Monthly spending report (`?month=YYYY-MM`) |
| `GET` | `/api/v1/reports/trend` | Monthly trend (`?months=6`) |

### Transaction Filters

```
GET /api/v1/transactions?page=0&size=30&accountId=1&categoryId=2&type=EXPENSE&from=2025-01-01&to=2025-03-31
```

---

## Project Structure

```
personal-finance-tracker/
├── application/                        # Spring Boot backend
│   └── src/main/java/.../
│       ├── shared/                     # Shared kernel (Money, IDs, audit, infra)
│       ├── identity/                   # Auth & user management
│       ├── account/                    # Account management
│       ├── category/                   # Category management
│       ├── transaction/                # Transaction management
│       ├── budget/                     # Budget management
│       └── reporting/                  # Dashboard & reports
├── frontend/                           # React TypeScript frontend
│   └── src/
│       ├── api/                        # API client modules
│       ├── contexts/                   # AuthContext
│       ├── hooks/                      # Custom React hooks
│       ├── pages/                      # Page components
│       ├── components/                 # UI & shared components
│       ├── types/                      # TypeScript interfaces
│       └── constants/                  # Lookup data (types, currencies)
├── Dockerfile                          # Backend multi-stage build
├── docker-compose.yml                  # Full stack orchestration
└── Makefile                            # Developer commands
```

---

## Development

### Backend (without Docker)

Requires Java 21 and PostgreSQL 15 running locally.

```bash
# Run backend
./gradlew :application:bootRun

# Run tests
./gradlew :application:test

# Build JAR
./gradlew :application:bootJar
```

### Frontend (without Docker)

Requires Node.js 20+.

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173 (proxies /api to localhost:8080)
npm run build   # Production build
```

### Database

Default credentials (configured in `docker-compose.yml` and `application.yaml`):

```
Host:     localhost:5432
Database: personal-finance-tracker
Username: pft-app-user
Password: pft-app-user-secret
Schema:   finance_tracker
```

Liquibase runs automatically on backend startup and applies all pending migrations.

---

## Architecture Overview

This project implements **Hexagonal Architecture** (Ports & Adapters) with **6 DDD bounded contexts**. Each bounded context is fully self-contained with its own domain model, inbound/outbound ports, domain services, and adapters.

For detailed architecture documentation, see [ARCHITECTURE.md](./ARCHITECTURE.md).

---

## Agent Pipeline

All engineering work on this project is driven by a **9-agent Claude Code crew** with a mandatory 5-track pipeline. Every request — feature, bug, refactor, research, or UI change — flows through `engineering-manager` as the single entry point.

| Track | When to use |
|---|---|
| **FEATURE** | New capability, new endpoint, domain model change |
| **HOTFIX** | Production bug, startup failure, data integrity issue |
| **CHORE** | Dependency upgrade, refactor, rename |
| **SPIKE** | Research / investigation — output is a doc, never code |
| **UI-ONLY** | Visual or copy change, no API contract change |

Full documentation: [`docs/agent-pipeline.md`](./docs/agent-pipeline.md)

Interactive pipeline visual: [`docs/engineering-pipeline.html`](./docs/engineering-pipeline.html)

---

## License

This project is for educational and personal use.
