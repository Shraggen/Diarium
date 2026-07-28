#!/usr/bin/env bash
set -euo pipefail
root="${1:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "$root"
# This helper deliberately requires an explicit command. Generic test commands can
# be expensive or unsafe in unknown repositories.
source "$root/.agents/config.env" 2>/dev/null || true
if [[ -n "${AGENT_TEST_CMD:-}" ]]; then
  bash -lc "$AGENT_TEST_CMD"
else
  echo "[agent-gate] AGENT_TEST_CMD is not configured; targeted tests skipped." >&2
fi
