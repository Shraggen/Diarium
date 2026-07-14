# ADR 0001: Use a microkernel with registered tool contracts

- Date: 2026-07-10
- Status: Accepted

## Context

Diarium must grow beyond a hardcoded beekeeping intent parser and may later
support other kinds of field work. Speech recognition and local inference are
infrastructure concerns, while operations such as recording an inspection are
domain concerns. Prompt-only extraction would couple these concerns and make
every new operation a special case.

## Decision

Use a Kotlin Multiplatform microkernel. `core` owns JSON Schema construction,
tool registration, prompt mapping, response parsing, and execution routing.
Domains provide registered `Tool` implementations with schemas and
deterministic Kotlin execution. The kernel contains no Android, audio, Room, or
beekeeping dependencies.

## Consequences

- New operations can be introduced through a typed tool contract.
- Core behavior is testable with fake generators and tools.
- The model can affect state only through registered operations.
- Schema, parsing, and domain adapter code add ceremony.
- Tool selection quality remains dependent on the local model and therefore
  still requires validation and user-visible proposals.
