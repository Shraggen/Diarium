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
