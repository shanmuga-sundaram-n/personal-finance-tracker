#!/bin/bash
# Hook: Guard against destructive git and database commands.
# Receives tool event JSON on stdin. Exit 2 = block the tool call.
set -euo pipefail

INPUT=$(cat)

COMMAND=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null || echo "")

# Destructive git patterns
DESTRUCTIVE_GIT_PATTERNS=(
    "git reset --hard"
    "git push --force"
    "git push -f"
    "git clean -f"
    "git checkout -- "
    "git restore \."
    "git branch -D"
    "git rebase -i"
)

for pattern in "${DESTRUCTIVE_GIT_PATTERNS[@]}"; do
    if echo "$COMMAND" | grep -qF "$pattern"; then
        echo "BLOCKED: Destructive git command requires explicit confirmation."
        echo ""
        echo "  Command: $COMMAND"
        echo ""
        echo "This command can cause irreversible data loss (lost commits, overwritten history,"
        echo "or deleted branches). If this is intentional, ask the user to confirm and run it"
        echo "directly as: ! $COMMAND"
        exit 2
    fi
done

# Hard-delete SQL patterns (soft-delete only in this project)
HARD_DELETE_PATTERNS=(
    "DELETE FROM"
    "delete from"
    "DROP TABLE"
    "drop table"
    "TRUNCATE"
    "truncate"
)

for pattern in "${HARD_DELETE_PATTERNS[@]}"; do
    if echo "$COMMAND" | grep -qF "$pattern"; then
        echo "BLOCKED: Hard-delete SQL requires explicit confirmation."
        echo ""
        echo "  Command: $COMMAND"
        echo ""
        echo "This project uses soft-delete only (is_active = false). Hard deletes and table drops"
        echo "should never happen against the finance_tracker schema."
        echo "If this is a test/migration context, ask the user to confirm first."
        exit 2
    fi
done

exit 0
