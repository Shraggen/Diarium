#!/usr/bin/env bash
set -uo pipefail

agent_root() {
  if [[ -n "${CLAUDE_PROJECT_DIR:-}" ]]; then
    printf '%s\n' "$CLAUDE_PROJECT_DIR"
  elif git rev-parse --show-toplevel >/dev/null 2>&1; then
    git rev-parse --show-toplevel
  else
    pwd
  fi
}

load_agent_config() {
  local root="$1"
  if [[ -f "$root/.agents/config.env" ]]; then
    # shellcheck disable=SC1090
    source "$root/.agents/config.env"
  fi
}

mode_for() {
  local category="$1"
  local upper
  upper="$(printf '%s' "$category" | tr '[:lower:]-' '[:upper:]_')"
  local specific="AGENT_${upper}_MODE"
  printf '%s\n' "${!specific:-${AGENT_GATES_MODE:-report}}"
}

log_info() { printf '[agent-gate] %s\n' "$*" >&2; }
log_warn() { printf '[agent-gate] WARNING: %s\n' "$*" >&2; }
log_error() { printf '[agent-gate] ERROR: %s\n' "$*" >&2; }

finish_gate() {
  local mode="$1" status="$2" message="$3"
  if [[ "$status" -eq 0 ]]; then
    log_info "$message"
    return 0
  fi

  case "$mode" in
    off)
      return 0
      ;;
    report)
      log_warn "$message (report-only)"
      return 0
      ;;
    block)
      log_error "$message"
      return "$status"
      ;;
    *)
      log_warn "Unknown mode '$mode'; treating as report-only: $message"
      return 0
      ;;
  esac
}

run_configured_command() {
  local label="$1" command_text="$2" mode="$3"
  [[ -z "$command_text" ]] && return 0
  log_info "Running $label: $command_text"
  if bash -lc "$command_text"; then
    log_info "$label passed"
    return 0
  fi
  finish_gate "$mode" 2 "$label failed"
}

read_hook_input() {
  cat || true
}

json_value() {
  local json="$1" expression="$2"
  if ! command -v jq >/dev/null 2>&1; then
    printf '%s' ""
    return 0
  fi
  printf '%s' "$json" | jq -r "$expression // empty" 2>/dev/null || true
}

claude_deny() {
  local reason="$1"
  if command -v jq >/dev/null 2>&1; then
    jq -n --arg reason "$reason" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$reason}}'
  else
    printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}\n' \
      "$(printf '%s' "$reason" | sed 's/"/\\"/g')"
  fi
}
