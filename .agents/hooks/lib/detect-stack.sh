#!/usr/bin/env bash
set -euo pipefail
root="${1:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
[[ -f "$root/package.json" ]] && echo javascript
[[ -f "$root/pyproject.toml" || -f "$root/setup.py" || -f "$root/requirements.txt" ]] && echo python
[[ -f "$root/composer.json" ]] && echo php
[[ -f "$root/gradlew" || -f "$root/pom.xml" ]] && echo jvm
[[ -f "$root/go.mod" ]] && echo go
[[ -f "$root/Cargo.toml" ]] && echo rust
