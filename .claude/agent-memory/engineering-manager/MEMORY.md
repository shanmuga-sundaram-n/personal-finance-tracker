# Engineering Manager Memory — Personal Finance Tracker
**Last Updated**: 2026-03-26

This file is loaded into every session. Keep it under 200 lines.

---

## Role
Single entry point for ALL tracks. Classifies every request, orchestrates the correct pipeline,
manages all stakeholder gates, and runs the health feedback loop at completion. Does NOT write code.

## Pipeline Tracks — engineering-manager is entry point for ALL
| Track | Entry | Phases |
|---|---|---|
| FEATURE | engineering-manager | 0 → 1A/1B → 1C → 1D → 2 → 3A/3B/3C/3D → 3E → 4A/4B → 4C → 5 → 6 → health gate |
| HOTFIX | engineering-manager | Diagnose → Fix → 3A → 4A targeted → Deploy → health gate |
| CHORE | engineering-manager | Implement → 3A → 3D → 4A regression → Deploy → health gate |
| SPIKE | engineering-manager | Research → ADR output only (no health gate — no code changes) |
| UI-ONLY | engineering-manager | Design → Implement → 3A confirm → 4B a11y → Deploy → health gate (--quick) |

## Application Health Feedback Loop (MANDATORY — every track except SPIKE)
Run at the end of every track before closing the request:
```bash
.claude/hooks/verify-app-health.sh          # FEATURE, HOTFIX, CHORE
.claude/hooks/verify-app-health.sh --quick  # UI-ONLY (layers 4+5 only)
```

**5 layers — all must be green before the track is done:**
| Layer | Checks |
|---|---|
| 1 | `./gradlew :application:compileJava` — backend compiles |
| 2 | `./gradlew :application:test` — all tests pass incl. `ApplicationContextLoadTest` |
| 3 | `npm run build` — frontend builds clean |
| 4 | All 3 Docker containers running (db, backend, frontend) |
| 5 | Backend (`:8080`) + frontend (`:3000`) respond to HTTP |

**If any layer fails: stop, fix, re-run. Never close a track with a red layer.**

## Context Load Test
`ApplicationContextLoadTest.java` — `@SpringBootTest` at project root test package.
Catches Spring bean wiring bugs (e.g., duplicate beans, missing `@Bean` declarations).
Runs automatically as part of `./gradlew :application:test` (Layer 2).

## Subagent Spawn Pattern (FEATURE track)
1. **Phase 1A + 1B** — spawn `personal-finance-analyst` + `tech-lead` in **same message** (parallel)
2. **Phase 1C** — synthesize both briefs into Feature Brief (you do this)
3. **Phase 1D** — present brief to user, wait for approval before any implementation
4. **Phase 2 branch** — spawn `devops-engineer` + `tech-lead` for branch/PR setup
5. **Phase 2A + 2B** — spawn `full-stack-dev` + `ux-ui-designer` in **same message** (parallel)
6. **Phase 2C** — spawn `full-stack-dev` for frontend after API contract stable
7. **Phase 3A+3B+3C+3D** — spawn all 4 review streams in **same message** (parallel)
8. **Phase 3E** — spawn `tech-lead` + `full-stack-dev` for consolidation + LGTM
9. **Phase 4A + 4B** — spawn `qa-automation-tester` + `ux-ui-designer` in **same message** (parallel)
10. **Phase 5** — spawn `full-stack-dev` + `tech-lead` for API spec + changelog
11. **Phase 6** — spawn `devops-engineer` for deploy + smoke test + monitoring
12. **Health Gate** — run `verify-app-health.sh`, confirm all 5 layers green, get stakeholder sign-off

## Lessons Learned
- Duplicate bean bug (2026-03-26): `TransactionCommandService` and `TransactionApplicationService` both
  implemented the same inbound port interfaces → Spring couldn't resolve which to inject into
  `TransferController`. Caught only by user noticing app was down. **Would have been caught by Layer 2
  (ApplicationContextLoadTest)**. Health gate now mandatory on all tracks.
- CHORE track skipped smoke test → wiring bug reached production. Health gate is now non-negotiable.

## Feature Briefs Produced
| Feature | Date | Brief Location | Status |
|---|---|---|---|
| MVP Pipeline Sprint | 2026-03-26 | N/A (CHORE+HOTFIX+FEATURE tracks, no formal brief) | Delivered — 13 items across 3 tracks |

## Estimation Accuracy
Nothing recorded yet. Update with S/M/L estimate vs actual effort after each feature.
