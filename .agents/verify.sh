#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0

printf 'Verifying agent ecosystem in %s\n' "$ROOT"

if command -v python3 >/dev/null 2>&1; then
  python3 -m json.tool "$ROOT/.claude/settings.json" >/dev/null || fail=1
else
  echo "WARNING: python3 not found; JSON validation skipped" >&2
fi

while IFS= read -r -d '' script; do
  bash -n "$script" || fail=1
done < <(find "$ROOT/.agents" -type f -name '*.sh' -print0)

for skill in "$ROOT"/.agents/skills/*/SKILL.md; do
  [[ -f "$skill" ]] || continue
  if ! grep -q '^name: [a-z0-9][a-z0-9-]*$' "$skill"; then
    echo "Invalid or missing skill name in $skill" >&2
    fail=1
  fi
  if ! grep -q '^description: .\+' "$skill"; then
    echo "Missing skill description in $skill" >&2
    fail=1
  fi
done

if command -v python3 >/dev/null 2>&1 && python3 -c 'import yaml' >/dev/null 2>&1; then
  python3 - <<'PY' || fail=1
from pathlib import Path
import yaml
root = Path.cwd()
for p in (root / '.agents/architecture').glob('*.yaml'):
    with p.open() as f:
        yaml.safe_load(f)
    print(f'YAML OK: {p}')
PY
else
  echo "WARNING: PyYAML unavailable; YAML parsing skipped" >&2
fi

if [[ "$fail" -ne 0 ]]; then
  echo "Agent ecosystem verification failed." >&2
  exit 1
fi

echo "Agent ecosystem verification passed."
