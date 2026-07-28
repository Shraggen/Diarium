#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
status=0
"$SCRIPT_DIR/post-write-quality-gate.sh" </dev/null || status=2
"$SCRIPT_DIR/post-write-architecture-gate.sh" </dev/null || status=2
exit "$status"
