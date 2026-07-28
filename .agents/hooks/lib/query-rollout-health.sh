#!/usr/bin/env bash
set -euo pipefail
root="${1:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "$root"
source "$root/.agents/config.env" 2>/dev/null || true
if [[ -z "${AGENT_ROLLOUT_HEALTH_CMD:-}" ]]; then
  echo "[agent-gate] AGENT_ROLLOUT_HEALTH_CMD is not configured; no rollout health query ran." >&2
  exit 3
fi
exec bash -lc "$AGENT_ROLLOUT_HEALTH_CMD"
