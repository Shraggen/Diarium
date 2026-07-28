#!/usr/bin/env bash
set -euo pipefail
root="${1:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "$root"
source "$root/.agents/config.env" 2>/dev/null || true

if [[ -n "${AGENT_SECURITY_CMD:-}" ]]; then
  exec bash -lc "$AGENT_SECURITY_CMD"
fi

ran=0
if command -v gitleaks >/dev/null 2>&1; then
  ran=1
  gitleaks dir --source . --no-git --exit-code 1
fi
if command -v semgrep >/dev/null 2>&1; then
  ran=1
  semgrep scan --config "$root/.agents/architecture/semgrep/architecture.yml" --error --metrics=off
fi
if [[ "$ran" -eq 0 ]]; then
  echo "[agent-gate] No security command or supported security tool configured; skipped." >&2
fi
