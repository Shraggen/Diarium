# ADR 0002: Separate planning from confirmed execution

- Date: 2026-07-13
- Status: Accepted

## Context

Local language models can select the wrong tool or invent arguments. A device
test demonstrated a read-like request being mapped to a mutating inspection
tool with an invented hive identifier. Directly executing a model response
would therefore allow probabilistic inference to corrupt the journal.

## Decision

Expose planning and execution as separate kernel operations. Planning produces
a `ToolCall` proposal and must not intentionally mutate domain state. The UI
shows the tool and arguments. Only an explicit user confirmation calls
execution. Editing the source text or cancelling invalidates the proposal.
Each tool validates its arguments again at the execution boundary.

## Consequences

- Model output cannot silently write to Room through the supported UI flow.
- Users can catch transcription and interpretation errors.
- Unit and integration tests can assert that planning leaves persistence empty.
- Every mutating interaction requires an additional user action.
- A generic JSON preview is not sufficient long-term; important fields need a
  clearer localized confirmation presentation.
