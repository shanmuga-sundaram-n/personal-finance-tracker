#!/usr/bin/env bash
# Pipeline Guard — UserPromptSubmit hook
# Reads the user prompt and injects pipeline routing rules into every session.
# Runs on EVERY user message so Claude always has the routing rules in context.

set -euo pipefail

INPUT=$(cat)

# Extract the user prompt text from the JSON payload
PROMPT=$(echo "$INPUT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
# UserPromptSubmit provides 'prompt' key
print(data.get('prompt', ''))
" 2>/dev/null || echo "")

# Always emit the pipeline routing reminder so Claude never skips the gate
cat <<'REMINDER'
[PIPELINE ENFORCER — mandatory, read before responding]

Before responding to ANY request in this project, classify it into one of these tracks and route accordingly. NEVER implement directly without going through the correct track first.

TRACK CLASSIFICATION (pick exactly one):
  FEATURE  → new capability, domain change, new endpoint, new page
             MUST start with: engineering-manager (full 7-phase pipeline)
  HOTFIX   → production bug, data integrity issue, startup failure
             MUST start with: engineering-manager (accelerated pipeline)
  CHORE    → dependency upgrade, refactor, rename, no domain change
             MUST start with: engineering-manager (abbreviated pipeline)
  SPIKE    → research, investigation, ADR — output is a doc, NEVER code
             MUST start with: engineering-manager (research pipeline)
  UI-ONLY  → visual/copy change, no API contract change
             MUST start with: engineering-manager (design-first pipeline)

engineering-manager is the ONLY entry point. Never route directly to any other agent.

HARD RULES — enforced every session, no exceptions:
  1. FEATURE track → engineering-manager FIRST. No direct implementation.
  2. Gate 1D → user must approve Feature Brief before any code is written.
  3. Gate 3E → tech-lead LGTM required before PR merges.
  4. Gate 4C → all tests must pass before Phase 5.
  5. Gate 6E → smoke test must pass; user must confirm before done.
  6. Parallel phases → 1A+1B, 2A+2B, 3A+3B+3C+3D, 4A+4B must run simultaneously.
  7. Context load test → always verify @SpringBootTest passes after any backend change.
  8. Smoke test → always verify backend starts after any deploy.

If the user's request is ambiguous, ask which track before doing anything else.
REMINDER

exit 0
