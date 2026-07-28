#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
ROOT="$(agent_root)"
load_agent_config "$ROOT"
MODE="$(mode_for policy)"
[[ "$MODE" == off ]] && exit 0

INPUT="$(read_hook_input)"
PATH_TOUCHED="$(json_value "$INPUT" '.tool_input.file_path // .tool_input.path')"
CONTENT="$(json_value "$INPUT" '.tool_input.content // .tool_input.new_string')"

[[ -z "$PATH_TOUCHED" ]] && exit 0
relative="${PATH_TOUCHED#$ROOT/}"
reason=""

case "$relative" in
  .env|.env.*|secrets/*|*/secrets/*|*.pem|*.key|*.p12|*.pfx)
    reason="Refusing a write to a credential or secret-like path: $relative"
    ;;
  node_modules/*|vendor/*|dist/*|build/*|coverage/*)
    reason="Refusing a direct write to generated or vendored content: $relative"
    ;;
esac

if [[ -z "$reason" && "$relative" =~ /(domain|core)/ && -n "$CONTENT" ]]; then
  if grep -Eqi '(from|import|require).*(infrastructure|adapters|controllers|persistence|cloud|aws|gcp|azure)' <<<"$CONTENT"; then
    reason="Possible domain-to-infrastructure dependency in $relative. Introduce a port or move orchestration outward."
  fi
fi

[[ -z "$reason" ]] && exit 0
if [[ "$MODE" == block ]]; then
  claude_deny "$reason"
  exit 0
fi
log_warn "$reason (report-only)"
exit 0
