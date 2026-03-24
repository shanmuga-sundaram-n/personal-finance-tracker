#!/bin/bash
# Hook: Guard domain package purity.
# Blocks Spring/JPA/Hibernate/Lombok/Jackson imports from being written into domain/ packages.
# ArchUnit catches this at build time — this catches it at edit time (faster feedback).
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

# Only check Java files inside a domain/ package
if [[ ! "$FILE_PATH" =~ /domain/[^/]+\.java$ ]]; then
    exit 0
fi

# Extract new content: new_string (Edit) or content (Write)
NEW_CONTENT=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {})
    print(ti.get('new_string') or ti.get('content') or '')
except Exception:
    print('')
" 2>/dev/null || echo "")

FORBIDDEN=(
    "import org.springframework"
    "import javax.persistence"
    "import jakarta.persistence"
    "import org.hibernate"
    "import lombok"
    "import com.fasterxml.jackson"
    "@Service"
    "@Component"
    "@Repository"
    "@Autowired"
    "@Entity"
    "@Table("
    "@Column("
)

for pattern in "${FORBIDDEN[@]}"; do
    if echo "$NEW_CONTENT" | grep -qF "$pattern"; then
        echo "BLOCKED: Domain purity violation in $(basename "$FILE_PATH")"
        echo ""
        echo "  Forbidden: $pattern"
        echo "  File: $FILE_PATH"
        echo ""
        echo "Domain classes must be pure Java — no Spring, JPA, Hibernate, Lombok, or Jackson."
        echo "If this is a persistence concern, move it to adapter/outbound/persistence/."
        echo "If this is a web concern, move it to adapter/inbound/web/."
        exit 2
    fi
done

exit 0
