#!/bin/bash
# Hook: Guard Liquibase migration file immutability.
# Existing migration files must never be modified — create a new one instead.
# Receives tool event JSON on stdin. Exit 2 = block the tool call.
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

# Only check files in the changelog changes directory
if [[ ! "$FILE_PATH" =~ /db\.changelog/changes/[^/]+\.(yml|yaml|xml|sql)$ ]]; then
    exit 0
fi

# Block if the file already exists (committed migrations are immutable)
if [[ -f "$FILE_PATH" ]]; then
    FILENAME=$(basename "$FILE_PATH")

    # Detect the next sequence number from existing files
    CHANGES_DIR=$(dirname "$FILE_PATH")
    LAST_NUM=$(ls "$CHANGES_DIR" 2>/dev/null | grep -oE '^[0-9]+' | sort -n | tail -1 || echo "0")
    NEXT_NUM=$(printf "%03d" $((10#$LAST_NUM + 1)))

    echo "BLOCKED: Migration file is immutable: $FILENAME"
    echo ""
    echo "Liquibase migrations are append-only. Once a migration file exists it must not"
    echo "be modified — doing so corrupts the changelog checksum and breaks deployments."
    echo ""
    echo "Create a new migration file instead:"
    echo "  $CHANGES_DIR/${NEXT_NUM}_<description>.yml"
    exit 2
fi

exit 0
