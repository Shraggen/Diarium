# ADR 0005: Keep persistence offline-first and defer collaboration

- Date: 2026-07-13
- Status: Accepted

## Context

Field work must remain usable without connectivity. The original application
had centralized synchronization so multiple workers could share an apiary, but
reintroducing a backend before the local domain model stabilizes would force
premature conflict and identity decisions. Current Room auto-increment IDs are
only valid inside one database.

## Decision

Persist current inspection records locally through a repository abstraction
backed by Room on Android. Do not implement synchronization in this milestone.
Before collaboration begins, create a separate ADR and threat model covering
global identities, apiary ownership, actors, versions, tombstones, idempotency,
outbox delivery, authorization, audit history, encryption, and conflict rules.

## Consequences

- The current application works fully offline with a small operational surface.
- Domain code is not directly coupled to Room and can gain synchronized
  adapters later.
- Collaboration is explicitly deferred, not accidentally forgotten.
- Existing schema migrations must prepare for global IDs before data is shared.
- Current numeric record IDs must never be exposed as cross-device identity.
