#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
ROOT="$(agent_root)"
load_agent_config "$ROOT"
MODE="$(mode_for architecture)"
[[ "$MODE" == off ]] && exit 0
cd "$ROOT"
status=0
ran=0

if [[ -n "${AGENT_ARCH_CMD:-}" ]]; then
  ran=1
  run_configured_command "architecture checks" "$AGENT_ARCH_CMD" "$MODE" || status=2
fi

if [[ -f package.json ]] && command -v npx >/dev/null 2>&1; then
  if npx --no-install depcruise --version >/dev/null 2>&1; then
    ran=1
    npx --no-install depcruise --config .agents/architecture/dependency-cruiser.cjs . --output-type err \
      || finish_gate "$MODE" 2 "dependency-cruiser architecture checks failed" || status=2
  fi
fi

if command -v lint-imports >/dev/null 2>&1 && [[ -f .agents/architecture/importlinter.ini ]]; then
  if ! grep -q 'your_python_package' .agents/architecture/importlinter.ini; then
    ran=1
    lint-imports --config .agents/architecture/importlinter.ini \
      || finish_gate "$MODE" 2 "Import Linter architecture checks failed" || status=2
  else
    log_warn "Import Linter config still contains placeholders; skipped"
  fi
fi

if [[ -x vendor/bin/deptrac && -f .agents/architecture/deptrac.yaml ]]; then
  ran=1
  vendor/bin/deptrac analyse -c .agents/architecture/deptrac.yaml \
    || finish_gate "$MODE" 2 "Deptrac architecture checks failed" || status=2
fi

if command -v semgrep >/dev/null 2>&1; then
  ran=1
  semgrep scan --config .agents/architecture/semgrep/architecture.yml --error --metrics=off \
    || finish_gate "$MODE" 2 "Semgrep architecture checks failed" || status=2
fi

if [[ "$ran" -eq 0 ]]; then
  log_warn "No configured or installed architecture checker ran"
fi

[[ "$MODE" == block && "$status" -ne 0 ]] && exit "$status"
exit 0
