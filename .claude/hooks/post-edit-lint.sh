#!/bin/bash
# Hook: Auto-lint TypeScript files after Claude edits them.
# Receives tool event JSON on stdin.
set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except Exception:
    print('')
" 2>/dev/null || echo "")

if [[ "$FILE_PATH" =~ \.(ts|tsx)$ ]]; then
    FRONTEND_DIR="/Volumes/Learnings/urmail2ss-git/personal-finance-tracker/frontend"
    ESLINT_BIN="$FRONTEND_DIR/node_modules/.bin/eslint"
    if [[ -f "$ESLINT_BIN" ]]; then
        cd "$FRONTEND_DIR"
        "$ESLINT_BIN" --fix "$FILE_PATH" --quiet 2>/dev/null || true
    fi
fi
