#!/usr/bin/env bash
# verify-app-health.sh — Multi-layer application health feedback loop
#
# Run this at the end of EVERY pipeline track to confirm the application
# is fully operational. Covers 5 layers of verification.
#
# Usage:
#   .claude/hooks/verify-app-health.sh           # all layers
#   .claude/hooks/verify-app-health.sh --quick   # layers 4+5 only (skip build)
#
# Exit codes:
#   0 = all layers passed
#   1 = one or more layers failed (details printed to stderr)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
QUICK=${1:-""}
FAILED=()

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅ $1${NC}"; }
fail() { echo -e "${RED}❌ $1${NC}"; FAILED+=("$1"); }
info() { echo -e "${BLUE}▶ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }

echo ""
echo "=================================================="
echo "  Application Health Feedback Loop"
echo "=================================================="
echo ""

cd "$ROOT"

# ──────────────────────────────────────────────────────
# LAYER 1 — Backend compile
# ──────────────────────────────────────────────────────
if [[ "$QUICK" != "--quick" ]]; then
    info "Layer 1: Backend compile (./gradlew :application:compileJava)"
    if ./gradlew :application:compileJava --no-daemon -q 2>&1; then
        pass "Layer 1: Backend compiles clean"
    else
        fail "Layer 1: Backend compile FAILED — fix compile errors first"
    fi
    echo ""

    # ──────────────────────────────────────────────────────
    # LAYER 2 — Unit + integration tests (includes context load)
    # ──────────────────────────────────────────────────────
    info "Layer 2: Unit tests + context load test (./gradlew :application:test)"
    TEST_OUTPUT=$(./gradlew :application:test --no-daemon 2>&1 || true)
    if echo "$TEST_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
        TOTAL=$(echo "$TEST_OUTPUT" | grep -oP '\d+ tests' | head -1 || echo "?")
        pass "Layer 2: All tests pass ($TOTAL)"
    else
        FAILURES=$(echo "$TEST_OUTPUT" | grep -E "FAILED|ERROR" | head -5)
        fail "Layer 2: Tests FAILED"
        echo "$FAILURES" | sed 's/^/    /'
    fi
    echo ""

    # ──────────────────────────────────────────────────────
    # LAYER 3 — Frontend build
    # ──────────────────────────────────────────────────────
    info "Layer 3: Frontend build (npm run build)"
    if cd "$ROOT/frontend" && npm run build --silent 2>&1 | grep -qE "built in|Build complete"; then
        pass "Layer 3: Frontend builds clean"
    else
        FRONTEND_OUT=$(cd "$ROOT/frontend" && npm run build 2>&1 | tail -5)
        fail "Layer 3: Frontend build FAILED"
        echo "$FRONTEND_OUT" | sed 's/^/    /'
    fi
    cd "$ROOT"
    echo ""
fi

# ──────────────────────────────────────────────────────
# LAYER 4 — Docker containers running
# ──────────────────────────────────────────────────────
info "Layer 4: Docker containers health"

check_container() {
    local name=$1
    local status
    status=$(docker compose ps --format json 2>/dev/null \
        | python3 -c "
import sys, json
data = sys.stdin.read().strip()
containers = json.loads('[' + data.replace('}\n{', '},{') + ']') if data else []
for c in containers:
    if '$name' in c.get('Name', '') or '$name' in c.get('Service', ''):
        print(c.get('State', 'unknown'))
        break
" 2>/dev/null || echo "unknown")
    echo "$status"
}

DB_STATE=$(check_container "db")
BACKEND_STATE=$(check_container "backend")
FRONTEND_STATE=$(check_container "frontend")

if [[ "$DB_STATE" == "running" ]]; then
    pass "Layer 4a: Database container running"
else
    fail "Layer 4a: Database container not running (state: $DB_STATE)"
fi

if [[ "$BACKEND_STATE" == "running" ]]; then
    pass "Layer 4b: Backend container running"
else
    fail "Layer 4b: Backend container not running (state: $BACKEND_STATE)"
    warn "Try: docker compose up --build backend -d"
fi

if [[ "$FRONTEND_STATE" == "running" ]]; then
    pass "Layer 4c: Frontend container running"
else
    fail "Layer 4c: Frontend container not running (state: $FRONTEND_STATE)"
fi
echo ""

# ──────────────────────────────────────────────────────
# LAYER 5 — API smoke test
# ──────────────────────────────────────────────────────
info "Layer 5: API smoke test (backend responds to HTTP)"

BACKEND_PORT=${BACKEND_PORT:-8080}
SMOKE_URL="http://localhost:${BACKEND_PORT}/api/v1/auth/login"
MAX_WAIT=30
WAITED=0

# Wait for backend to be ready (up to 30s)
while [[ $WAITED -lt $MAX_WAIT ]]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$SMOKE_URL" \
        -H "Content-Type: application/json" \
        -d '{"username":"_healthcheck_","password":"_healthcheck_"}' \
        --connect-timeout 3 --max-time 5 2>/dev/null || echo "000")

    if [[ "$HTTP_CODE" != "000" ]]; then
        break
    fi

    warn "Backend not yet responding, waiting... (${WAITED}s / ${MAX_WAIT}s)"
    sleep 3
    WAITED=$((WAITED + 3))
done

# Any non-000 response means the backend is up and routing requests
if [[ "$HTTP_CODE" == "000" ]]; then
    fail "Layer 5: Backend not responding — connection refused after ${MAX_WAIT}s"
    warn "Check: docker compose logs backend --tail=30"
elif [[ "$HTTP_CODE" == "401" || "$HTTP_CODE" == "422" || "$HTTP_CODE" == "400" ]]; then
    pass "Layer 5: Backend responding (HTTP $HTTP_CODE — auth working as expected)"
elif [[ "$HTTP_CODE" == "200" ]]; then
    pass "Layer 5: Backend responding (HTTP $HTTP_CODE)"
else
    warn "Layer 5: Backend returned HTTP $HTTP_CODE — unexpected but alive"
    pass "Layer 5: Backend responding"
fi

FRONTEND_PORT=${FRONTEND_PORT:-3000}
FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:${FRONTEND_PORT}" \
    --connect-timeout 3 --max-time 5 2>/dev/null || echo "000")

if [[ "$FRONTEND_CODE" != "000" ]]; then
    pass "Layer 5: Frontend responding (HTTP $FRONTEND_CODE)"
else
    fail "Layer 5: Frontend not responding on port $FRONTEND_PORT"
fi

echo ""
echo "=================================================="

# ──────────────────────────────────────────────────────
# SUMMARY
# ──────────────────────────────────────────────────────
if [[ ${#FAILED[@]} -eq 0 ]]; then
    echo -e "${GREEN}  ALL LAYERS PASSED — Application is healthy ✅${NC}"
    echo "=================================================="
    echo ""
    exit 0
else
    echo -e "${RED}  FAILED LAYERS (${#FAILED[@]}):${NC}"
    for f in "${FAILED[@]}"; do
        echo -e "${RED}    • $f${NC}"
    done
    echo "=================================================="
    echo ""
    exit 1
fi
