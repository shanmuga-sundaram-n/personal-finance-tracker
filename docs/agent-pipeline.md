# Agent Pipeline — Personal Finance Tracker

This document describes the Claude Code agent crew that drives all engineering work on this project.
Every request — feature, bug, refactor, research, or UI change — flows through this pipeline.

> **Interactive visual**: Open [`docs/engineering-pipeline.html`](./engineering-pipeline.html) in a browser for a tab-navigable view of all 5 tracks.

---

## Entry Point — Always `engineering-manager`

**No request bypasses `engineering-manager`.** It classifies the track, selects the pipeline, spawns agents, manages gates, and owns delivery end-to-end.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart TD
    request([your request]) --> em[engineering-manager]
    em --> classify{classify}

    classify -->|new capability or endpoint| feature[FEATURE]
    classify -->|production bug or startup failure| hotfix[HOTFIX]
    classify -->|refactor or dependency upgrade| chore[CHORE]
    classify -->|research or investigation| spike[SPIKE]
    classify -->|visual or copy change only| ui[UI-ONLY]

    classDef em      fill:#1e293b,stroke:#475569,color:#f1f5f9
    classDef feature fill:#1e3a5f,stroke:#1e40af,color:#60a5fa
    classDef hotfix  fill:#3b1a1a,stroke:#7f1d1d,color:#f87171
    classDef chore   fill:#1a2e1a,stroke:#14532d,color:#4ade80
    classDef spike   fill:#2e2000,stroke:#78350f,color:#fbbf24
    classDef ui      fill:#2a1a3e,stroke:#581c87,color:#c084fc

    class em em
    class feature feature
    class hotfix hotfix
    class chore chore
    class spike spike
    class ui ui
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

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart TD
    p0[engineering-manager\nPhase 0 · intake & classification]

    subgraph ph1[Phase 1 · parallel]
        direction LR
        p1a[personal-finance-analyst\ndomain rules & acceptance criteria]
        p1b[tech-lead\narchitecture brief & migration plan]
    end

    p1c[engineering-manager\nmerge outputs → Feature Brief]
    g1d{{Gate 1D · you approve the Feature Brief\nno code is written before this point}}

    p2br[devops-engineer & tech-lead\nbranch & PR strategy]

    subgraph ph2[Phase 2 · parallel]
        direction LR
        p2a[full-stack-dev\n2A · backend]
        p2b[ux-ui-designer\n2B · frontend design & component spec]
    end

    p2c[full-stack-dev\n2C · frontend implementation]

    subgraph ph3[Phase 3 · parallel review]
        direction LR
        p3a[tech-lead\n3A · architecture]
        p3b[security-reviewer\n3B · auth & OWASP]
        p3c[performance-reviewer\n3C · N+1 & indexes]
        p3d[tech-lead\n3D · dependencies]
    end

    p3e[tech-lead & full-stack-dev\n3E · consolidate findings & fix blockers]
    g3e{{Gate 3E · tech-lead LGTM\nPR ready to merge}}

    subgraph ph4[Phase 4 · parallel]
        direction LR
        p4a[qa-automation-tester\n4A · unit & integration tests]
        p4b[ux-ui-designer\n4B · WCAG 2.1 AA & mobile]
    end

    g4c{{Gate 4C · all tests pass\nall acceptance criteria verified}}
    p5[full-stack-dev & tech-lead\nPhase 5 · OpenAPI · CHANGELOG · release notes]

    subgraph ph6[Phase 6 · devops-engineer]
        direction LR
        p6a[pre-deploy checklist] --> p6b[deploy] --> p6c[smoke test] --> p6d[monitoring]
    end

    g6e{{Gate 6E · you confirm the feature works}}

    p0 --> ph1 --> p1c --> g1d --> p2br --> ph2 --> p2c --> ph3 --> p3e --> g3e --> ph4 --> g4c --> p5 --> ph6 --> g6e

    classDef gate fill:#1e3a5f,stroke:#1e40af,color:#93c5fd
    classDef human fill:#2e1a00,stroke:#b45309,color:#fbbf24

    class g1d,g6e human
    class g3e,g4c gate
```

---

### HOTFIX — Accelerated Pipeline

For production bugs, startup failures, data integrity issues.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    em[engineering-manager\nclassify as HOTFIX]
    tl1[tech-lead\ndiagnose root cause]
    fsd[full-stack-dev\ntargeted fix]
    tl2[tech-lead\n3A architecture review]
    qa[qa-automation-tester\nreproduce as test + regression]
    dev[devops-engineer\ndeploy & smoke test]
    gate{{Gate · you confirm bug resolved}}

    em --> tl1 --> fsd --> tl2 --> qa --> dev --> gate

    classDef hotfix fill:#3b1a1a,stroke:#7f1d1d,color:#f87171
    classDef gate   fill:#2e1a00,stroke:#b45309,color:#fbbf24

    class em,tl1,fsd,tl2,qa,dev hotfix
    class gate gate
```

---

### CHORE — Abbreviated Pipeline

For dependency upgrades, refactors, renames — no domain or API change.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    em[engineering-manager\nclassify as CHORE]
    fsd[full-stack-dev\nimplement]
    tl[tech-lead\n3A architecture + 3D dependencies]
    qa[qa-automation-tester\nfull regression run]
    dev[devops-engineer\ndeploy & smoke test]
    gate{{Gate · you confirm working}}

    em --> fsd --> tl --> qa --> dev --> gate

    classDef chore fill:#1a2e1a,stroke:#14532d,color:#4ade80
    classDef gate  fill:#2e1a00,stroke:#b45309,color:#fbbf24

    class em,fsd,tl,qa,dev chore
    class gate gate
```

---

### SPIKE — Investigation Only

For research questions, technology evaluation. Output is an ADR document — never code.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    em[engineering-manager\nclassify as SPIKE]
    tl[tech-lead\nresearch & author ADR]
    out([ADR document\nno code · no deploy])

    em --> tl --> out

    classDef spike fill:#2e2000,stroke:#78350f,color:#fbbf24
    classDef out   fill:#1e293b,stroke:#475569,color:#94a3b8

    class em,tl spike
    class out out
```

---

### UI-ONLY — Design-First Pipeline

For visual or copy changes with no API contract change.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    em[engineering-manager\nclassify as UI-ONLY]
    ux1[ux-ui-designer\ndesign review & component spec]
    fsd[full-stack-dev\nfrontend implementation]
    tl[tech-lead\nconfirm no API contract changes]
    ux2[ux-ui-designer\nWCAG 2.1 AA & mobile review]
    dev[devops-engineer\ndeploy · verify-app-health --quick]
    gate{{Gate · you confirm}}

    em --> ux1 --> fsd --> tl --> ux2 --> dev --> gate

    classDef ui   fill:#2a1a3e,stroke:#581c87,color:#c084fc
    classDef gate fill:#2e1a00,stroke:#b45309,color:#fbbf24

    class em,ux1,fsd,tl,ux2,dev ui
    class gate gate
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

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    l1[Layer 1\ncompileJava]
    l2[Layer 2\ngradlew test]
    l3[Layer 3\nnpm run build]
    l4[Layer 4\ndocker compose ps]
    l5[Layer 5\nHTTP curl]
    closed([track closed])
    fix[fix the issue]

    l1 --> l2 --> l3 --> l4 --> l5
    l5 -->|all green| closed
    l1 & l2 & l3 & l4 & l5 -->|any red| fix
    fix -->|re-run| l1

    classDef layer  fill:#1e293b,stroke:#334155,color:#cbd5e1
    classDef closed fill:#1a2e1a,stroke:#14532d,color:#4ade80
    classDef fix    fill:#3b1a1a,stroke:#7f1d1d,color:#f87171

    class l1,l2,l3,l4,l5 layer
    class closed closed
    class fix fix
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
