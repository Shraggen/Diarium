#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
ROOT="$(agent_root)"
load_agent_config "$ROOT"
MODE="$(mode_for deploy)"
[[ "$MODE" == off ]] && exit 0
INPUT="$(read_hook_input)"
COMMAND="$(json_value "$INPUT" '.tool_input.command')"
REGEX="${AGENT_DEPLOY_COMMAND_REGEX:-(kubectl apply|helm upgrade|argocd app sync|argo rollouts)}"
[[ -z "$COMMAND" || ! "$COMMAND" =~ $REGEX ]] && exit 0

reason="Deployment-like command detected. Explicit user approval, a clean reviewed diff, and a rollback plan are required."
if [[ "$MODE" == block ]]; then
  claude_deny "$reason"
  exit 0
fi
log_warn "$reason (report-only)"
