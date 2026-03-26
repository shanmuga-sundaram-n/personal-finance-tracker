# Performance Reviewer Memory — Personal Finance Tracker
**Last Updated**: 2026-03-26

## Role
Phase 3C — Performance review in the FEATURE pipeline. Runs in parallel with 3A (architecture) and 3B (security).

## Project Performance Context
- Backend: Spring Data JPA + PostgreSQL 15.2, NUMERIC(19,4) for money
- Soft-delete: all queries must filter `is_active = true` — watch for missing filter causing full table scans
- Frontend: React + Vite + shadcn/ui — new npm deps need bundle size justification
- Common N+1 risk: Budget → Category, Transaction → Category, RecurringTransaction → Account

## Known Indexes (existing — from migrations)

| Index | Table | Columns | Type | Migration |
|---|---|---|---|---|
| `idx_transactions_user_date` | `transactions` | `(user_id, transaction_date DESC)` | Composite | 008 |
| `idx_transactions_account` | `transactions` | `(account_id)` | Single | 008 |
| `idx_transactions_category` | `transactions` | `(category_id)` | Single | 008 |
| `idx_transactions_user_cat_date` | `transactions` | `(user_id, category_id, transaction_date)` | Composite | 008 |
| `idx_budgets_user_active_dates` | `budgets` | `(user_id, start_date, end_date) WHERE is_active = true` | Partial | 014 |
| `idx_sessions_expires_at` | `sessions` | `(expires_at)` | Single | 015 |

**Implication**: New filter/sort columns on `transactions`, `budgets`, or `sessions` tables likely already indexed. Check before recommending a new index.

## Reviews Completed

| Feature | Date | Result | Key Findings |
|---|---|---|---|
| — | — | — | — |

## Pipeline Entry
All tracks enter via `engineering-manager`. performance-reviewer is spawned at Phase 3C (parallel with 3A, 3B, 3D).

## Health Feedback Loop Awareness
Performance fixes (e.g. JOIN FETCH rewrites, new indexes, query refactors) must not break the build.
After Phase 3E consolidation, `engineering-manager` runs:
```bash
.claude/hooks/verify-app-health.sh
```
All 5 layers must pass. If a performance fix changes a JPA query method signature, it may affect
the Spring context — flag for `ApplicationContextLoadTest` (Layer 2).

## Recurring Patterns to Watch
Nothing recorded yet. Update after first few reviews.
