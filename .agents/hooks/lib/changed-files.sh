#!/usr/bin/env bash
set -euo pipefail
root="${1:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "$root"
if git rev-parse --verify HEAD >/dev/null 2>&1; then
  git diff --name-only --relative HEAD --diff-filter=ACMRTUXB
else
  git status --short | awk '{print $2}'
fi
