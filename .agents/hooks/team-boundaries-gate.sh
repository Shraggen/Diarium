#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
ROOT="$(agent_root)"
load_agent_config "$ROOT"
MODE="$(mode_for boundary)"
PHASE="${1:-post}"
[[ "$MODE" == off ]] && exit 0
INPUT="$(read_hook_input)"
PATH_TOUCHED="$(json_value "$INPUT" '.tool_input.file_path // .tool_input.path')"

if grep -q 'team-billing' "$ROOT/.agents/architecture/context-map.yaml" 2>/dev/null; then
  log_warn "context-map.yaml still appears to contain starter placeholders; semantic boundary enforcement skipped"
  exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
  log_warn "python3 not available; boundary map validation skipped"
  exit 0
fi

# Full semantic parsing requires PyYAML. The gate intentionally skips rather than
# pretending a grep-based parser is authoritative.
if ! python3 -c 'import yaml' >/dev/null 2>&1; then
  log_warn "Python package PyYAML is not installed; boundary map validation skipped"
  exit 0
fi

export AGENT_BOUNDARY_ROOT="$ROOT"
export AGENT_BOUNDARY_PATH="$PATH_TOUCHED"
export AGENT_BOUNDARY_PHASE="$PHASE"
result="$(python3 "$SCRIPT_DIR/lib/validate_boundaries.py" 2>&1)"
code=$?
if [[ "$code" -eq 0 ]]; then
  [[ -n "$result" ]] && log_info "$result"
  exit 0
fi

if [[ "$PHASE" == pre && "$MODE" == block ]]; then
  claude_deny "$result"
  exit 0
fi
finish_gate "$MODE" 2 "$result"
