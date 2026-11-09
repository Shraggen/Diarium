# ADR 0007: Share one Llamatik command-model adapter across platforms

- Date: 2026-11-09
- Status: Accepted

## Context

`LlamatikStructuredJsonGenerator` was an `expect` class with separate Android,
iOS, and JVM `actual` implementations. All three made the same `LlamaBridge`
calls, and the JVM copy was not compiled because `sharedLogic` had no JVM
target. The copies had already drifted in structure, so a change verified on
one platform did not prove anything about the others.

Llamatik 1.10.1 declares `LlamaBridge` and `WhisperBridge` as `expect object`
in its common source set. Platform differences live inside Llamatik, not in
Diarium.

Laptop model evaluation (issue #59) needs the same adapter on the JVM that the
phone runs, so a measured result describes the production harness.

## Decision

Keep one `LlamatikStructuredJsonGenerator` in `app/sharedLogic/src/commonMain`,
using the former Android logic unchanged, and depend on Llamatik from
`commonMain`. Delete the per-platform copies. Add a platform-specific adapter
again only when a platform needs a call that Llamatik does not expose in common
code.

## Consequences

- Android, iOS, and JVM run the same prompt-building and generation path.
- The JVM target can exercise the production adapter against real GGUF models.
- Every platform now resolves Llamatik for common metadata compilation.
- `LlamaBridge` remains a process-global, non-thread-safe native model; callers
  still own sequencing and shutdown.
