# ADR 0003: Use prompt-guided JSON until native grammar is safe

- Date: 2026-07-13
- Status: Accepted as a workaround

## Context

Llamatik 1.8.1 and 1.9.0 aborted the Android process during native constrained
generation with `Unexpected empty grammar stack after accepting piece`. The
failure occurs in native code and cannot be recovered as a Kotlin exception.
Ordinary generation does not trigger the double-accept grammar failure.

## Decision

Use ordinary Llamatik generation with the complete tool-call schema and strict
JSON instructions in the prompt. Parse the returned envelope in Kotlin and
accept an otherwise valid response wrapped in a Markdown fence. Keep native
grammar mode available only as an explicit experimental option until an
upstream version is verified on a physical device.

## Consequences

- Local command inference no longer crashes the process through the known path.
- The workaround is covered by prompt and parser regression tests.
- Output is not token-constrained and may still be malformed or semantically
  wrong, particularly with small models.
- The project must re-evaluate this ADR after a Llamatik/llama.cpp upgrade; do
  not remove the workaround based only on compilation success.
