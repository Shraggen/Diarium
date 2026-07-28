# Repository agent guidance

This repository uses `.agents/` as its canonical agent policy layer.

Before making changes, read:

- `.agents/policy/agentconfig.md`
- `.agents/architecture/bounded-contexts.yaml`
- `.agents/architecture/context-map.yaml`
- `.agents/architecture/glossary.yaml`
- `.agents/state/current-change.md`

Use the relevant skill:

- `.agents/skills/assess-tradeoffs/SKILL.md` before architecture-sensitive work
- `.agents/skills/evolutionary-refactor/SKILL.md` for legacy or structural modernization
- `.agents/skills/trunk-delivery/SKILL.md` for ordinary features, fixes, and migrations

Rules:

- Keep work inside the owning bounded context.
- Use published interfaces for cross-context dependencies.
- Keep trunk releasable and changes independently reversible.
- Run the narrowest useful verification after every meaningful slice.
- Do not deploy, merge, revert, push, or enable blocking gates without explicit user approval.
- Treat `.agents/hooks/` as required checks even when the current client does not run them automatically.
- Before completion, run `./.agents/verify.sh` and the configured manual gate commands.
