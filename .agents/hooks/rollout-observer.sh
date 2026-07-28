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

log_warn "Deployment-like command completed. This starter will not auto-promote, revert, commit, or push."
if [[ -n "${AGENT_ROLLOUT_HEALTH_CMD:-}" ]]; then
  "$SCRIPT_DIR/lib/query-rollout-health.sh" "$ROOT" \
    || finish_gate "$MODE" 2 "Configured rollout health check failed or was unavailable"
else
  log_warn "AGENT_ROLLOUT_HEALTH_CMD is not configured; inspect rollout telemetry manually"
fi
