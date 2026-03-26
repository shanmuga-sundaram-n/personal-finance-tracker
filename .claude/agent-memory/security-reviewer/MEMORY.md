# Security Reviewer Memory — Personal Finance Tracker
**Last Updated**: 2026-03-26

## Role
Phase 3B — Security review in the FEATURE pipeline. Runs in parallel with 3A (architecture) and 3C (performance).
Also invoked for HOTFIX if the fix touches auth or data exposure.

## Project Security Model
- Auth: opaque UUID session token via `Authorization: Bearer {token}` — no Spring Security, no JWT
- Data isolation: all queries scoped to authenticated user — cross-user access returns 404, not 403
- Soft-delete: `is_active = false` — inactive records must never be accessible
- Financial data: balances, amounts, budgets are sensitive — never log at INFO level

## Reviews Completed

| Feature | Date | Result | Key Findings |
|---|---|---|---|
| MVP sprint audit | 2026-03-26 | APPROVED | Cross-user 404 rule enforced; soft-delete filter moved to DB query level; `AccountNotFoundException` returns 404 not 422 |

## Pipeline Entry
All tracks enter via `engineering-manager`. security-reviewer is spawned at Phase 3B (parallel with 3A, 3C, 3D).

## Health Feedback Loop Awareness
After Phase 3E fixes are applied, `engineering-manager` runs:
```bash
.claude/hooks/verify-app-health.sh
```
Security fixes that change bean wiring or configuration must pass `ApplicationContextLoadTest` (Layer 2)
before the track closes. If a security fix introduces a new Spring component, flag it for the context load test.

## Recurring Patterns to Watch
- **Soft-delete bypass**: All `findById` methods must use `findByIdAndUserIdAndIsActiveTrue`, not `findByIdAndUserId`. Inactive records must be invisible even on direct ID lookup.
- **Wrong HTTP status on not-found**: `*NotFoundException` must extend `ResourceNotFoundException` (→ 404), never `DomainException` (→ 422). Fixed in `AccountNotFoundException` 2026-03-26.
- **Duplicate bean wiring**: When wrapping a domain service with an application service in `config/`, ensure only the application service implements inbound port interfaces — not both. Dual implementation = ambiguous injection = startup crash (BUG-012).
