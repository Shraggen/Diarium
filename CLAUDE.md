# Repository agent guidance

The canonical policy and workflows are under `.agents/`.

Read and follow:

- `.agents/policy/agentconfig.md`
- `.agents/architecture/bounded-contexts.yaml`
- `.agents/architecture/context-map.yaml`
- `.agents/architecture/glossary.yaml`
- `.agents/state/current-change.md`

Use skills on demand:

- `/assess-tradeoffs` before architecture-affecting work
- `/evolutionary-refactor` for legacy modernization or structural replacement
- `/trunk-delivery` for ordinary feature, fix, and migration delivery

Do not bypass `.claude/settings.json` hooks. Keep changes inside owning bounded
contexts unless the context map explicitly allows the interaction. Never deploy,
merge, revert, push, or enable blocking gates without explicit user approval.
