# ADR 0006: Use deterministic planning for bounded inspection fields

- Date: 2026-07-14
- Status: Accepted

## Context

The prompt-guided 0.5B command model produced only one fully usable proposal in
four repeated English, German, and Serbian field runs. It omitted an explicit
identifier, retained translated field labels, and returned an inflected number
word instead of the canonical identifier. The confirmation guard prevented
unsafe writes, but the user could not reliably complete the primary workflow.

`record_inspection` currently contains two bounded fields: one hive identifier
and one explicit boolean queen observation. Existing consistency checkers
already perform the conservative multilingual extraction needed to verify both
values.

## Decision

Introduce `ToolCallPlanner` as a provider-neutral core contract. Use a common
Kotlin `RecordInspectionPlanner` as the production planner for the current
inspection tool. It canonicalizes one explicit hive identifier and one explicit
queen observation from English, German, Serbian Latin, Serbian Cyrillic, or
mixed-script transcripts. Missing, multiple, contradictory, or hedged values
are omitted instead of guessed.

Keep plan, confirm, and execute as separate boundaries. Retain transcript-plan
consistency checks and tool argument validation as defense in depth. Do not load
or expose a GGUF command model in the current inspection UI. Keep the existing
model adapter available for later open-ended tools behind `ToolCallPlanner`.

## Consequences

- The current journal workflow no longer depends on stochastic command-model
  output, a GGUF import, or command-model startup memory.
- Typed inspection input works without any native model; voice input requires
  only Whisper.
- Every field failure becomes a pure common Kotlin regression test.
- Adding a language means extending an isolated extractor and its corpus rather
  than modifying Android UI or persistence code.
- The supported grammar is deliberately finite. Unsupported or uncertain
  language must abstain and use the existing blocked-confirmation recovery path.
- Future LLM, laptop, or remote planners can implement the same contract, but
  their proposals remain subject to confirmation and deterministic validation.
- ADR 0003 remains relevant only to the dormant model-backed planner path; it no
  longer describes production planning for `record_inspection`.
