#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
ROOT="$(agent_root)"
load_agent_config "$ROOT"
MODE="$(mode_for quality)"
[[ "$MODE" == off ]] && exit 0
cd "$ROOT"

changed="$($SCRIPT_DIR/lib/changed-files.sh "$ROOT" 2>/dev/null || true)"
if [[ -z "$changed" ]]; then
  log_info "No changed files detected; quality gate skipped"
  exit 0
fi

status=0
run_configured_command "format check" "${AGENT_FORMAT_CMD:-}" "$MODE" || status=2
run_configured_command "lint" "${AGENT_LINT_CMD:-}" "$MODE" || status=2
run_configured_command "typecheck/compile" "${AGENT_TYPECHECK_CMD:-}" "$MODE" || status=2
run_configured_command "targeted tests" "${AGENT_TEST_CMD:-}" "$MODE" || status=2

if [[ -z "${AGENT_FORMAT_CMD:-}${AGENT_LINT_CMD:-}${AGENT_TYPECHECK_CMD:-}${AGENT_TEST_CMD:-}" ]]; then
  log_warn "No quality commands configured in .agents/config.env; quality gate is advisory only"
fi

[[ "$MODE" == block && "$status" -ne 0 ]] && exit "$status"
exit 0
