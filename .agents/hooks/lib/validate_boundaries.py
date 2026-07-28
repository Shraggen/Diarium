#!/usr/bin/env python3
"""Validate a touched path against the starter context map.

This intentionally performs conservative path ownership checks only. Import-edge
validation belongs in language-specific architecture tools.
"""
from __future__ import annotations

import os
from pathlib import Path
import sys
import yaml

root = Path(os.environ["AGENT_BOUNDARY_ROOT"]).resolve()
touched_raw = os.environ.get("AGENT_BOUNDARY_PATH", "")
if not touched_raw:
    print("No touched path supplied; boundary check skipped")
    raise SystemExit(0)

touched = Path(touched_raw)
if touched.is_absolute():
    try:
        rel = touched.resolve().relative_to(root)
    except ValueError:
        print(f"Touched path is outside repository: {touched}")
        raise SystemExit(2)
else:
    rel = touched
rel_text = rel.as_posix().lstrip("./")

with (root / ".agents/architecture/context-map.yaml").open() as fh:
    data = yaml.safe_load(fh) or {}

owners: list[tuple[str, str]] = []
for name, cfg in (data.get("contexts") or {}).items():
    for item in cfg.get("roots") or []:
        owners.append((str(item).rstrip("/"), name))
for name, cfg in (data.get("platforms") or {}).items():
    for item in cfg.get("roots") or []:
        owners.append((str(item).rstrip("/"), name))

matched = [(prefix, owner) for prefix, owner in owners if rel_text == prefix or rel_text.startswith(prefix + "/")]
if not matched:
    print(f"Path is not assigned to a bounded context or platform: {rel_text}")
    raise SystemExit(1)

matched.sort(key=lambda item: len(item[0]), reverse=True)
prefix, owner = matched[0]
print(f"Boundary owner for {rel_text}: {owner} ({prefix})")
