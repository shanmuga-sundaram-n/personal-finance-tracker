# Agent Pipeline — Personal Finance Tracker

This document describes the Claude Code agent crew that drives all engineering work on this project.
Every request — feature, bug, refactor, research, or UI change — flows through this pipeline.

> **Interactive visual**: Open [`docs/engineering-pipeline.html`](./engineering-pipeline.html) in a browser for a tab-navigable view of all 5 tracks.

---

## Entry Point — Always `engineering-manager`

**No request bypasses `engineering-manager`.** It classifies the track, selects the pipeline, spawns agents, manages gates, and owns delivery end-to-end.

```
You say: "I want X"
         ↓
  engineering-manager  ← single entry point for ALL tracks
         ↓
  Classifies → FEATURE / HOTFIX / CHORE / SPIKE / UI-ONLY
         ↓
  Runs the correct pipeline
```

---

## The 9 Agents

| Agent | Role | Pipeline Phase |
|---|---|---|
| `engineering-manager` | Orchestrator — entry point, track classification, gate management | Phase 0, 1C, 1D |
| `personal-finance-analyst` | Domain rules, acceptance criteria, financial invariants, edge cases | Phase 1A |
| `tech-lead` | Architecture decisions, ADRs, code review, final LGTM | Phase 1B, 3A, 3D, 3E |
| `full-stack-dev` | Backend + frontend implementation | Phase 2A, 2C, 3E, 5 |
| `ux-ui-designer` | Design system, mobile-first layout, accessibility (WCAG 2.1 AA) | Phase 2B, 4B |
| `devops-engineer` | Branch/PR strategy, deploy, smoke test, monitoring | Phase 2 (branch), 6A–6D |
| `security-reviewer` | OWASP, auth gaps, financial data exposure | Phase 3B |
| `performance-reviewer` | N+1 queries, DB indexes, frontend bundle size | Phase 3C |
| `qa-automation-tester` | Unit tests, integration tests, coverage gates, migration validation | Phase 4A |

---

## 5 Pipeline Tracks

### FEATURE — Full 7-Phase Pipeline

For new capabilities, new endpoints, domain model changes.

```
Phase 0    engineering-manager         Intake, track classification

Phase 1    ┌── personal-finance-analyst  1A: Domain rules, acceptance criteria
  PARALLEL └── tech-lead                1B: Architecture brief, ports, migration plan

           engineering-manager          1C: Merge → unified Feature Brief

           ★ GATE 1D                    YOU approve the Feature Brief
                                        No code written before this gate

Phase 2    devops-engineer + tech-lead  Branch & PR strategy

  PARALLEL ┌── full-stack-dev           2A: Backend (domain → adapters → REST)
           └── ux-ui-designer           2B: Frontend design & component spec

           full-stack-dev               2C: Frontend implementation

Phase 3    ┌── tech-lead                3A: Architecture & domain zone review
  PARALLEL ├── security-reviewer        3B: Auth, OWASP, financial data exposure
           ├── performance-reviewer     3C: N+1, DB indexes, bundle size
           └── tech-lead                3D: Dependency review (CVEs, licenses)

           tech-lead + full-stack-dev   3E: Consolidate findings, fix blockers

           ★ GATE 3E                    tech-lead LGTM → PR ready to merge

Phase 4    ┌── qa-automation-tester     4A: Unit + integration tests, coverage gates
  PARALLEL └── ux-ui-designer           4B: WCAG 2.1 AA + mobile review

           ★ GATE 4C                    All tests pass, all acceptance criteria verified

Phase 5    full-stack-dev + tech-lead   OpenAPI spec, CHANGELOG, release notes

Phase 6    devops-engineer              6A: Pre-deploy checklist
                                        6B: Deploy
                                        6C: Smoke test (verify-app-health.sh)
                                        6D: Monitoring & observability

           ★ GATE 6E                    YOU confirm the feature works
```

---

### HOTFIX — Accelerated Pipeline

For production bugs, startup failures, data integrity issues.

```
engineering-manager   Classify as HOTFIX
tech-lead             Diagnose root cause, scope minimal fix
full-stack-dev        Targeted fix (no refactoring, no scope creep)
tech-lead             3A architecture review of the fix
qa-automation-tester  Reproduce bug as test + full regression run
devops-engineer       Deploy + smoke test
★ GATE                YOU confirm the bug is resolved
```

---

### CHORE — Abbreviated Pipeline

For dependency upgrades, refactors, renames — no domain or API change.

```
engineering-manager   Classify as CHORE
full-stack-dev        Implement
tech-lead             3A architecture check + 3D dependency review
qa-automation-tester  Full regression run (no new tests required)
devops-engineer       Deploy + smoke test
★ GATE                YOU confirm everything is working
```

---

### SPIKE — Investigation Only

For research questions, technology evaluation. Output is an ADR document — never code.

```
engineering-manager   Classify as SPIKE
tech-lead             Research options, evaluate trade-offs, author ADR
                      Output: ADR document only — no code, no deploy
```

---

### UI-ONLY — Design-First Pipeline

For visual or copy changes with no API contract change.

```
engineering-manager   Classify as UI-ONLY
ux-ui-designer        Design review & component spec
full-stack-dev        Frontend implementation
tech-lead             Confirm no API contract changes introduced
ux-ui-designer        WCAG 2.1 AA accessibility + mobile review
devops-engineer       Deploy + quick smoke test
★ GATE                YOU confirm
```

---

## Parallel Phases

These phases spawn multiple agents simultaneously to reduce total time:

| Phase | Agents (parallel) | Sequential dependency |
|---|---|---|
| 1A + 1B | `personal-finance-analyst` + `tech-lead` | Both needed before 1C |
| 2A + 2B | `full-stack-dev` + `ux-ui-designer` | 2C waits for 2A API contract |
| 3A + 3B + 3C + 3D | All 4 review streams | 3E waits for all 4 |
| 4A + 4B | `qa-automation-tester` + `ux-ui-designer` | Gate 4C waits for both |

---

## Human Gates

There are exactly **2 points where you must approve** before the pipeline continues:

| Gate | When | What you decide |
|---|---|---|
| **Gate 1D** | After Feature Brief is produced | Approve scope, AC, and complexity before any code is written |
| **Gate 6E** | After smoke test passes | Confirm the deployed feature works as expected |

All other steps are fully automated by the agent crew.

---

## Application Health Feedback Loop

At the end of every track (except SPIKE), the pipeline runs a 5-layer health check:

```bash
.claude/hooks/verify-app-health.sh          # FEATURE, HOTFIX, CHORE
.claude/hooks/verify-app-health.sh --quick  # UI-ONLY (layers 4+5 only)
```

| Layer | Checks | Catches |
|---|---|---|
| 1 | `./gradlew :application:compileJava` | Compile errors, syntax errors |
| 2 | `./gradlew :application:test` — includes `ApplicationContextLoadTest` | Logic bugs, Spring bean wiring failures |
| 3 | `npm run build` | TypeScript errors, bad imports |
| 4 | `docker compose ps` | Containers crashed or not started |
| 5 | `curl :8080` + `curl :3000` | Runtime startup failures |

**All 5 layers must be green before a track is closed.** If any layer fails, the pipeline stops, the issue is fixed, and the script is re-run.

---

## Hook Enforcement

The pipeline is enforced by two mechanisms loaded into every session:

| Mechanism | File | Effect |
|---|---|---|
| `CLAUDE.md` top section | `CLAUDE.md` | Track classification table + hard rules loaded at session start |
| Pre-tool hooks | `.claude/settings.json` | Domain purity, migration guard, bash guard fire on every edit |
| Health script | `.claude/hooks/verify-app-health.sh` | 5-layer app health — run manually or by `devops-engineer` at Gate 6 |

---

## Agent Memory

Each agent maintains a persistent memory file that carries context across sessions:

| Agent | Memory file |
|---|---|
| `engineering-manager` | `.claude/agent-memory/engineering-manager/MEMORY.md` |
| `tech-lead` | `.claude/agent-memory/tech-lead/MEMORY.md` + `architecture-decisions.md` |
| `personal-finance-analyst` | `.claude/agent-memory/personal-finance-analyst/DOMAIN-OWNERSHIP.md` |
| `full-stack-dev` | `.claude/agent-memory/full-stack-dev/MEMORY.md` |
| `ux-ui-designer` | `.claude/agent-memory/ux-ui-designer/MEMORY.md` + `design-system.md` |
| `qa-automation-tester` | `.claude/agent-memory/qa-automation-tester/MEMORY.md` + `testing-strategy.md` |
| `devops-engineer` | `.claude/agent-memory/devops-engineer/MEMORY.md` |
| `security-reviewer` | `.claude/agent-memory/security-reviewer/MEMORY.md` |
| `performance-reviewer` | `.claude/agent-memory/performance-reviewer/MEMORY.md` |

---

## Quick Reference

```
Any request → engineering-manager → classifies track → runs pipeline → health gate → you confirm
```

| I want to… | Track | First step |
|---|---|---|
| Add a new feature / endpoint / page | FEATURE | `engineering-manager` → Phase 0 |
| Fix a bug / app not starting | HOTFIX | `engineering-manager` → `tech-lead` diagnose |
| Upgrade a dependency / refactor | CHORE | `engineering-manager` → `full-stack-dev` |
| Research a technical question | SPIKE | `engineering-manager` → `tech-lead` research |
| Change UI / copy / styling only | UI-ONLY | `engineering-manager` → `ux-ui-designer` |
