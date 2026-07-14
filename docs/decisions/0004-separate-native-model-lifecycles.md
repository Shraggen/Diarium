# ADR 0004: Separate speech, VAD, and command-model lifecycles

- Date: 2026-07-13
- Status: Accepted

## Context

Voice input combines three different native concerns: continuous audio/VAD,
Whisper transcription, and command-model inference. Llamatik 1.9 exposes
Whisper transcription but does not expose or bundle Silero VAD. Whisper and
GGUF models also have different formats, storage, initialization, and failure
modes.

## Decision

Use Silero through the Android VAD adapter during capture, Llamatik Whisper in
an independent speech runtime, and the llama.cpp command model in the app
runtime. Store `.bin` and `.gguf` models separately. A transcript becomes normal
editable command input; the kernel does not depend on microphone APIs.

## Consequences

- Either model can be imported, restored, failed, or replaced independently.
- Typed and spoken commands share the same planning and safety path.
- VAD is replaceable without changing Whisper or the kernel.
- Android currently owns more orchestration classes and lifecycle state.
- iOS requires its own platform adapters before it reaches feature parity.
