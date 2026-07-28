#!/usr/bin/env bash
set -euo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
LOG_DIR="$ROOT/.agents/state/logs"
mkdir -p "$LOG_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
{
  echo "timestamp: $STAMP"
  echo "branch: $(git -C "$ROOT" branch --show-current 2>/dev/null || true)"
  echo "head: $(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || true)"
  echo "changed_files:"
  git -C "$ROOT" status --short 2>/dev/null | sed 's/^/  /' || true
} > "$LOG_DIR/$STAMP.txt"
# Logs are local evidence and should normally remain uncommitted.
