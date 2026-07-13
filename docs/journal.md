# 📖 Project Journal: Diarium (formerly Beekeeper)
---

**Date:** July 2025

**Architect / Lead:** Shraggen

**Current Phase:** Foundation & Governance (Walking Skeleton)

## 1. Executive Summary: The Evolution to a Platform
This session marked the formal transition of the project from a single-purpose, Android-bound application ("Beekeeper") into a multi-domain, Kotlin Multiplatform (KMP) Voice Operating System named **Diarium**.

By recognizing that the core business driver wasn't "bees" but rather "hands-free, offline data entry," we inverted the architecture. We adopted a **Microkernel (Plugin) Architecture**, where the complex ML/Voice pipeline lives in a pristine, domain-agnostic `core`, and specific professions (Beekeeping, Mechanics) are implemented as highly unstable, easily swappable `plugins`.

## 2. Milestones Completed Today
We successfully implemented the "Walking Skeleton"—a minimal, end-to-end slice of the architecture that proves the concept and establishes the build pipeline.

*   **The Core Contract Established:** Defined `DiariumTool` and `DiariumKernel` in `commonMain`. The Kernel routes data; it does not know what the data means.
*   **The First Plugin Isolated:** Created the `RecordInspectionTool`. This proves we can inject domain-specific logic (Beekeeping) into the Kernel via Dependency Injection from the UI layer.
*   **Documentation as Code:** Updated the `arc42` architecture documentation with embedded `Mermaid.js` diagrams (C4 Context and Building Block Views). These now live in version control alongside the code.

## 3. Automated Governance (Fitness Functions)
To ensure the architecture survives future development, we implemented "Evolutionary Architecture" principles by coding our rules into the build system:
*   **Complexity Shield (Detekt):** Configured in `detekt.yml` to fail the build if any method exceeds a Cyclomatic Complexity of 10. This officially outlaws the "giant `if/else` intent parsing" anti-pattern of the past.
*   **Boundary Shield (Konsist):** Wrote `ArchitectureFitnessTest.kt` in the `jvmTest` source set. If a developer attempts to import domain logic (e.g., `com.shraggen.diarium.app..`) into the `core` module, the CI/CD pipeline will reject the commit. `core` must remain at 0.0 Instability.

## 4. The Strategic Technical Pivot: Llamatik & Agentic OS
*As identified in the initial project scope*, we are abandoning prompt-hacking (MediaPipe/Gemma) and manual C++ JNI compiling (`whisper.cpp` via Kaldi/custom bridges).

We are adopting **Llamatik**, a Kotlin wrapper for `llama.cpp` and `whisper.cpp`.
*   **Why this matters:** It abstracts the lowest-level C++ nightmares. More importantly, it unlocks **native Tool Calling (GBNF / JSON Schema enforcement)**.
*   **The Impact:** Diarium is no longer an app that "guesses" what the user wants. It is an **Agentic OS** where the LLM is constrained at the token-generation level to output strictly typed JSON that perfectly matches the `parametersSchema` defined in our `DiariumTool` plugins.

---

## 🚀 Handoff: Roadmap for the Next Session
When development resumes, the infrastructure is ready to receive the AI components. We will execute the following "Small Batches":

### Batch 1: Implement the Llamatik JSON Bridge
*   Add the `llamatik-core` and `kotlinx-serialization-json` dependencies.
*   Update `DiariumTool.kt` so that `parametersSchema` returns a strict `JsonObject` (JSON Schema) instead of a String.
*   Update `DiariumKernel.kt` with a `getToolsForLlmPrompt()` function to format our registered tools into the exact JSON array expected by modern LLM Tool-Calling APIs.

### Batch 2: Integrate Whisper (Speech-to-Text)
*   Utilize Llamatik's `whisper.cpp` wrapper to replace the old Android `SpeechRecognizer`.
*   Implement the audio capture logic (expecting 16kHz PCM data) to feed into the Llamatik Whisper context.

### Batch 3: Integrate the LLM (Inference Loop)
*   Initialize a Llamatik LLM Context (e.g., loading a lightweight `.gguf` model like Llama-3.2-1B-Instruct).
*   Create the actual interaction loop in the Kernel: `Audio -> Whisper -> Text -> LLM (with Tools) -> JSON Tool Call -> DiariumTool.execute() -> Output`.

***
---

## 2026-07-10 — Structured tool execution foundation

### Completed

- Simplified project to a mobile-first Android/iOS architecture.
- Removed web, server, and desktop application modules.
- Added provider-neutral tool abstractions in `core`.
- Added typed JSON Schema AST and builder DSL.
- Added JSON Schema serialization.
- Added tool registry, parser, executor, and kernel.
- Added Llamatik constrained-JSON adapter.
- Added Android and iOS implementations of
  `LlamatikStructuredJsonGenerator`.
- Added `RecordInspectionTool`.
- Verified the domain/tool path builds and tests pass.

### Current architecture

Android / iOS
-> sharedUI
-> sharedLogic
-> core

`core` contains provider-neutral tool and schema logic.

`sharedLogic` contains application orchestration and Llamatik integration.

`sharedUI` contains shared presentation code.

### Important decision

Llamatik does not expose a dedicated tool-calling API in the current
integration. Diarium uses `generateJson` with a constrained tool-call
envelope:

{
"tool": "<registered tool name>",
"arguments": { ... }
}

The schema DSL exists to keep domain tools independent of Llamatik's
JSON representation.

### Next milestone

Run one real end-to-end tool call on Android using an actual local model.

Steps:

1. Decide and document model files used for development.
2. Add model acquisition or bundled-development-model handling.
3. Initialize Llamatik outside Compose UI code.
4. Introduce an application-level state holder.
5. Pass user text to `DiariumKernel.process`.
6. Display loading, success, and failure states.
7. Verify `RecordInspectionTool` executes on a physical Android device.
8. Shut down model resources correctly.

### After that

Rebuild the original Beekeeper voice workflow:

microphone
-> recorded WAV
-> Whisper transcription
-> DiariumKernel
-> tool execution
-> persisted journal data
-> optional spoken confirmation

### Do not do next

- no additional schema DSL features
- no retries
- no multi-agent loop
- no desktop or web support
- no provider abstraction beyond what the running path requires
- no UI polishing before real model inference works

### Verification

Run:

./gradlew :core:jvmTest
./gradlew :app:sharedLogic:assemble
./gradlew :app:androidApp:assembleDebug
./gradlew detekt

---

## 2026-07-13 — Android local-inference vertical slice

### Completed

- Repaired the core architecture fitness tests so they inspect the real
  `core` production module and fail if that scope is empty.
- Added `DiariumController` in `sharedLogic` to own Llamatik initialization,
  `DiariumKernel`, registered tools, processing, and shutdown.
- Applied the loaded GGUF model's embedded chat template before constrained
  JSON generation.
- Added an Android model importer using the system document picker.
- Models are copied into private app storage and the newest imported GGUF is
  restored on later app launches.
- Added an Android `ViewModel` that performs model copying, initialization,
  and inference away from the UI thread.
- Added Compose states for model selection, loading, ready, processing,
  success, and failure.
- Documented the Qwen2.5-0.5B-Instruct Q4_K_M development baseline in
  `docs/development-model.md`.

### Verification completed

- `./gradlew :core:jvmTest`
- `./gradlew :app:sharedLogic:assemble`
- `./gradlew :app:androidApp:assembleDebug`
- `./gradlew detekt`

### Next action

Install the debug build on a physical Android device, import the documented
GGUF model, and run:

`I inspected hive 4 and saw the queen.`

The milestone is complete only after Llamatik produces the constrained call
and `RecordInspectionTool` returns the visible result on-device.

### Still deferred

- no microphone or Whisper integration
- no hotword detection
- no Room persistence
- no TTS
- no multi-turn interaction
- no production model downloader

---

## 2026-07-13 — Llamatik grammar crash workaround

### Device finding

The first physical-device call aborted in native code with:

`Unexpected empty grammar stack after accepting piece: {`

The stack trace reached `llama_sampler_accept` from Llamatik's
`llama_generate_json_schema` loop. Llamatik 1.8.1 calls
`llama_sampler_accept` after `llama_sampler_sample`, although the latter
already accepts the selected token in the bundled llama.cpp version. Grammar
state is advanced twice and the uncaught C++ exception terminates Android
before Kotlin can recover.

### Workaround

- Temporarily stopped calling Llamatik's `generateJson` native path.
- Added the complete tool-call JSON Schema and strict output instructions to
  the model prompt, then used ordinary generation.
- Continued parsing the resulting envelope in Kotlin.
- Added support for a JSON response wrapped in a Markdown fence.
- Added regression tests for prompt construction and fenced response parsing.

This restores process safety, but it is prompt-guided structured generation,
not token-level constrained generation. Restore `generateJson` only after the
native double-accept has been removed in a verified Llamatik release.

### Physical-device verification

Rebuilt and installed the debug APK over the existing installation on an
SM-A546B, preserving its app data. The app restored
`qwen2.5-0.5b-instruct-q4_0.gguf` and processed:

`I inspected hive 4 and saw the queen.`

The local execution completed with:

`{"hive_id":"4","queen_seen":true,"recorded":true}`

Logcat contained no `libc++abi`, `runtime_error`, `Fatal signal`, or native
grammar abort during that run. Q4_K_M remains the documented development
baseline; Q4_0 is the quantization verified in this device test.
