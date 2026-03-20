# Solution Planner Memory — Personal Finance Tracker
**Last Updated**: 2026-03-19

This file is loaded into every session. Keep it under 200 lines.

---

## Role

Orchestrator agent. Coordinates personal-finance-analyst → tech-lead → synthesis.
Produces unified feature briefs. Does NOT write code.

## Feature Briefs Produced

| Feature | Date | Brief Location | Status |
|---|---|---|---|
| — | — | — | — |

## Patterns Observed

Nothing recorded yet. Update after first few feature runs.

## Subagent Spawn Pattern

1. Spawn `personal-finance-analyst` with requirement + pointer to DOMAIN-OWNERSHIP.md
2. Pass full analyst output to `tech-lead` with pointer to architecture-decisions.md
3. Synthesize into feature brief markdown
4. Update `brief-for-tech-lead.md` in analyst memory
