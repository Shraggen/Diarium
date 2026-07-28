#!/usr/bin/env bash
set -euo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
STATE="$ROOT/.agents/state/current-change.md"
if [[ -f "$STATE" ]]; then
  printf 'Critical repository task context follows. Treat it as current only if it matches the active request.\n\n'
  sed -n '1,120p' "$STATE"
fi
