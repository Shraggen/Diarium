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

The same abort was reproduced after upgrading to Llamatik 1.9.0.

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

---

## 2026-07-13 — Persistent inspection journal

### Completed

- Added a persistence-neutral `InspectionRepository` contract and inspection
  records in the beekeeping shared-logic package.
- Changed `RecordInspectionTool` to validate and persist inspections instead
  of returning an in-memory acknowledgement.
- Added repository-backed recent-inspection reads for the journal UI.
- Added an Android Room database, DAO, repository adapter, and exported
  version 1 schema.
- Added a journal section that loads independently of model initialization
  and refreshes after local tool execution.
- Split tool planning from execution and added an explicit confirmation card;
  no model-proposed journal write executes before user confirmation.
- Split Android runtime work out of the ViewModel to preserve the enforced
  class-size boundary.
- Replaced the boolean grammar flag with explicit `PROMPT_GUIDED` and
  `NATIVE_GRAMMAR` modes. The safe prompt-guided mode remains the default.
- Added shared tests covering persistence calls and blank hive rejection.

### Verification completed

- `./gradlew :app:sharedLogic:allTests`
- `./gradlew :core:jvmTest`
- `./gradlew :app:androidApp:assembleDebug`
- `./gradlew detekt`
- Installed the debug APK on an SM-A546B and processed:
  `I inspected hive 7 and saw the queen.`
- Room created record 1 and the journal immediately displayed Hive 7 with
  `Queen seen`.
- Force-stopped and relaunched the app; record 1 was restored from the Room
  database while the GGUF model also reinitialized.
- The Android crash log was empty and no Llamatik grammar abort occurred.

### Multi-tool safety finding

An on-device probe asked the 0.5B model to show the most recent inspection.
In prompt-guided mode it incorrectly selected the mutating record tool and
invented a placeholder hive identifier. The experimental
`list_recent_inspections` LLM tool was therefore removed from the registered
tool set. Recent records remain available directly through the repository and
journal UI. Multi-tool routing must remain deferred until it has a mutation
confirmation boundary or a reliably constrained selector.

The confirmation boundary was then verified on-device in both directions:
the misrouted read request displayed its invented arguments and **Cancel**
left the database unchanged, while a valid hive 8 proposal wrote record 3
only after **Confirm** was selected.

### Next milestone

Integrate microphone capture and Llamatik Whisper as an input adapter for the
existing text-to-kernel-to-persistent-journal path. Keep hotword detection,
TTS, multi-turn interaction, and production model downloading deferred.

---

## 2026-07-13 — Voice-to-persistent-journal milestone

### Completed

- Added Android 16 kHz mono PCM16 microphone capture with a 30-second cap.
- Added Silero VAD 2.0.10 with 512-sample frames, minimum speech filtering,
  and automatic stop after 900 ms of sustained non-speech.
- Added an independent Whisper `.bin` model importer, private model storage,
  model restoration, native lifecycle, and temporary WAV cleanup.
- Integrated Llamatik 1.9 segmented Whisper transcription with
  `translate = false` into the existing plan → confirm → Room path.
- Kept Whisper and LLM model lifecycles independent and split Android state
  ownership into LLM, voice, tool-call, speech-runtime, and app-runtime
  components to satisfy the architecture fitness limits.
- Added user-selectable English, German, and Serbian modes plus localized
  English, German, and Serbian Cyrillic UI copy.
- Added Serbian Latin/Cyrillic vocabulary prompting and explicit multilingual
  examples to tool routing. Identifiers must be preserved and never invented.
- Confirmed by inspecting Llamatik 1.9 sources and artifacts that its wrapper
  exposes Whisper transcription but not Silero/whisper.cpp VAD. Silero remains
  a replaceable Android capture adapter.
- Documented the later shared-apiary collaboration requirements without
  implementing a backend or premature conflict policy.

### Test and CI improvements

- Added unit tests for multilingual transcript parsing, Serbian Unicode,
  language selection, and Whisper-compatible PCM WAV encoding.
- Added an integration test proving a Serbian Cyrillic transcript can flow
  through kernel planning while persistence remains empty until execution.
- Added Android instrumentation tests for Room ordering/persistence, Activity
  launch, bundled Silero ONNX inference, and optional provisioned Whisper
  initialization through Llamatik.
- Fixed an instrumentation-only coroutines 1.11/1.9 runtime mismatch by
  declaring the app's direct Android coroutine dependency explicitly.
- Expanded CI to run Detekt, Konsist/JVM/KMP tests, Android lint/build,
  API 35 emulator tests, an iOS simulator build, artifacts, timeouts, and
  stale-run cancellation.
- Improved CodeQL manual builds to compile actual Kotlin/Android/Swift targets
  and removed the unused JavaScript product-language scan.

### Verification completed

- `./gradlew :core:jvmTest :app:sharedLogic:allTests`
- `./gradlew :app:androidApp:detekt :app:androidApp:compileDebugKotlin`
- `./gradlew :app:androidApp:connectedDebugAndroidTest`
- Four instrumentation tests passed on SM-A546B with zero failures and zero
  skips, including Llamatik initialization of a SHA-256-verified multilingual
  `ggml-base-q5_1.bin` and native Silero inference.
- The final APK was reinstalled after instrumentation and both the existing
  Qwen GGUF and multilingual Whisper model were restored to private storage.

### Remaining physical acceptance

The device was locked during the automated run, so a human-spoken microphone
round trip still needs one unlocked-device pass in English, German, Serbian
Latin, and Serbian Cyrillic. The native model/VAD/lifecycle pieces are verified;
speech-recognition quality and real apiary acoustics remain acceptance tests,
not claims made by the automated suite.

---

## 2026-07-14 — Multilingual field acceptance and next-session handoff

### Field acceptance completed

The application owner tested real microphone capture through persistence in
English, German, and Serbian and reported that the complete flow works well.
This closes the outstanding human-spoken acceptance item from the voice
milestone. Recognition is occasionally imperfect, after which the small local
LLM can misunderstand the damaged transcript. Larger multilingual models are a
candidate improvement, not a substitute for deterministic safety checks.

One supplied Serbian screenshot is an important regression lead. Its transcript
contains `košnica, pet` (`hive, five`) while the visible persisted result is
`hive_id: "4"`. Reproduce the exact utterance before concluding which model
introduced the disagreement, but treat explicit identifier disagreement as a
blocking condition rather than allowing the LLM to guess.

### Documentation overhaul

- Replaced the mostly empty arc42 template with a project-specific architecture
  document covering all twelve sections.
- Added ADRs for the microkernel, plan/confirm/execute safety boundary,
  prompt-guided JSON workaround, independent native-model lifecycles, and
  offline-first persistence with deferred synchronization.
- Added a project-structure guide explaining the combined Gradle, KMP, Kotlin,
  reverse-domain package, and feature directory hierarchy.

### Start here next session

Objective: make multilingual tool interpretation trustworthy before expanding
the beekeeping feature set.

1. Add a pure common Kotlin identifier-consistency component between planning
   and confirmation. It should conservatively extract explicit hive identifiers
   from the transcript and compare them with `record_inspection.hive_id`.
2. Cover Arabic digits and unambiguous number words used in English, German,
   Serbian Latin, and Serbian Cyrillic. Prefer “cannot verify” over guessing.
3. If transcript and proposal disagree, block confirmation and show both values
   in localized, human-readable copy. Do not silently rewrite a model proposal.
4. Replace the raw JSON confirmation with domain-aware labels for Hive and
   Queen seen while retaining the exact tool-call data for diagnostics.
5. Add a table-driven multilingual evaluation corpus: valid commands, noisy
   transcripts, missing identifiers, contradictions, malformed JSON, cancel,
   and successful confirmed persistence.
6. Re-run unit/KMP/instrumentation gates and repeat the Serbian physical phrase.

After that guardrail is green, add model profiles and compare multilingual
Whisper base versus small plus the current 0.5B command model versus a stronger
candidate. Measure accuracy, latency, memory, thermal behavior, and battery on
the same fixed corpus before changing the default.

---

## 2026-07-14 — Multilingual identifier consistency guardrail

### Completed

- Added a pure common Kotlin hive-identifier consistency checker between tool
  planning and confirmation.
- The checker extracts only identifiers that follow explicit hive vocabulary.
  It supports Arabic digits and unambiguous number words through 99 in English,
  German, Serbian Latin, and Serbian Cyrillic.
- A proposal can now be confirmed only when one explicit transcript identifier
  matches `record_inspection.hive_id`. Missing identifiers, multiple different
  identifiers, non-record tools, and malformed identifier arguments remain
  unverifiable instead of being guessed.
- A transcript/proposal mismatch is never rewritten. Confirmation is blocked
  and the UI shows both the transcript hive and model-proposed hive in localized
  English, German, or Serbian copy.
- Replaced the raw-JSON confirmation presentation with domain-aware Hive and
  Queen seen fields. The exact `ToolCall`, including its original arguments, is
  retained in pending state for diagnostics.
- Added a table-driven multilingual evaluation corpus covering valid commands,
  noisy punctuation, missing and multiple identifiers, the Serbian `košnica,
  pet` versus `4` regression, malformed model JSON, cancel, and confirmed
  persistence.

### Verification completed

- `./gradlew :app:sharedLogic:allTests :app:sharedUI:allTests`
- `./gradlew detekt :app:sharedLogic:allTests :app:sharedUI:allTests`
- `./gradlew :core:jvmTest :app:androidApp:assembleDebug`
- `./gradlew :app:androidApp:connectedDebugAndroidTest` on an SM-A546B
- Four connected-device tests passed. The optional provisioned-Whisper model
  initialization test was skipped because no model was supplied to that test
  invocation.

### Remaining physical acceptance

Repeat the exact Serbian microphone phrase that previously transcribed as
`košnica, pet`. If the model proposes hive 5, confirmation should remain
available. If it proposes hive 4 or any other identifier, the localized
mismatch card must display both values and keep confirmation disabled. Do not
start model-profile benchmarking until this unlocked-device check is complete.

---

## 2026-07-14 — Field-driven dual consistency boundary

### Four-run voice finding

The application owner repeated voice input in English, German, and Serbian and
supplied four screenshots. The identifier guard prevented every unsafe write,
including the Serbian five case, so the physical safety acceptance is complete.
The command-model output was not yet usable:

- English transcript identifier `4` became `Hive 4`.
- German transcript identifier `4` became `Bienenstock 4`.
- A damaged Serbian four transcript produced `trapeze_1` and incorrectly set
  `queen_seen` to false despite `video maticu` in the transcript.
- Serbian transcript identifier `5` became the inflected word `peta`.

The first identifier guard therefore worked as intended, but the Serbian four
run exposed that identifier agreement alone was insufficient to approve a
journal write.

### Completed

- Added a pure common Kotlin queen-observation consistency checker for explicit
  positive and negative statements in English, German, Serbian Latin, and
  Serbian Cyrillic.
- Confirmation now requires both a verified hive identifier and a verified
  queen observation. A mismatch, missing observation, contradictory statement,
  missing boolean, or quoted boolean keeps confirmation disabled.
- Made `queen_seen` required in the tool schema and removed the previous silent
  default to false.
- Hardened the structured prompt and parameter descriptions so identifier
  arguments must contain only their canonical value. Numeric number words must
  become ASCII digits; field labels, ordinals such as `peta`, and placeholders
  are explicitly forbidden.
- Added the unambiguous Whisper spelling `četri`/`четри` for Serbian four while
  retaining conservative blocking for damaged queen phrases that cannot be
  verified.
- Added all four physical transcripts and their exact bad model proposals to
  the table-driven evaluation corpus.
- Tightened `ToolArguments.booleanOrNull` so the JSON string `"true"` is not
  accepted as the boolean `true`.

### Verification completed

- `./gradlew :core:jvmTest :app:sharedLogic:allTests :app:sharedUI:allTests`
- `./gradlew detekt :app:androidApp:assembleDebug`
- `./gradlew :core:detektMetadataMain :app:sharedLogic:detektMetadataMain`
- `./gradlew :app:androidApp:connectedDebugAndroidTest` on an SM-A546B
- Four connected-device tests passed with zero failures and zero skips. The
  connected run installed the rebuilt debug application.

### Governance finding

The aggregate `detekt` task remains green, but it reports `NO-SOURCE` for the
KMP common modules. Explicit shared-logic metadata analysis is now green.
Explicit shared-UI metadata analysis is not a usable gate yet because it scans
generated Compose resource code and also exposes pre-existing App composable
complexity and naming findings. Configure exclusions and address the real UI
findings before claiming that aggregate Detekt covers all common code.

### Next physical acceptance

Repeat the same four voice runs against the newly installed build. A successful
proposal must use only `4` or `5` for `hive_id` and must match the explicit queen
observation. Any prefixed, inflected, invented, missing, or contradictory value
must remain blocked. Record the four canonical-identifier and queen-observation
outcomes before deciding whether prompt hardening is enough or a stronger local
command model is required.

---

## 2026-07-14 — Deterministic inspection planning pivot

### Field decision

A second four-run English, German, and Serbian voice test produced only one
fully usable command-model proposal. The guardrails prevented unsafe writes,
but prompt hardening did not make the 0.5B model reliable enough for the primary
journal workflow. Llamatik's one-shot generation was confirmed to start with a
fresh KV state, so accumulated conversation state was not the cause.

The owner approved replacing model-backed planning for the bounded
`record_inspection` fields with deterministic common Kotlin and supplied the
field transcripts as permanent regressions.

### Completed

- Added the provider-neutral `ToolCallPlanner` core contract. The existing
  structured-model adapter and deterministic planners can use the same kernel
  plan/execute boundary.
- Added `RecordInspectionPlanner`, which canonicalizes a single explicit hive
  identifier and queen observation. Missing, multiple, contradictory, hedged,
  or unsupported values are omitted instead of guessed.
- Promoted the identifier and queen extraction logic into reusable components;
  the consistency checkers remain as independent defense in depth.
- Replaced Serbian queen phrase enumeration with a bounded token grammar that
  accepts auxiliary word order such as `video sam maticu`, including mixed
  Latin/Cyrillic transcripts, while retaining negation and uncertainty checks.
- Routed typed and Whisper-transcribed inspection input through the
  deterministic planner. The production path no longer requires GGUF model
  initialization.
- Removed the unused GGUF picker, automatic command-model restoration, and
  command-model UI state. The Llamatik planner adapter remains available for a
  future open-ended tool but is not loaded by the inspection screen.
- Added exact regressions for the successful English run, both German runs,
  and the mixed-script Serbian `pet` / `video sam maticu` run. All four now
  produce canonical numeric identifiers and `queen_seen = true` without model
  inference.
- Added ADR 0006 and updated the architecture, model, and testing documents to
  reflect deterministic production planning.

### Observability and evaluation decision

Do not add a backend telemetry stack to the offline app yet. Establish a fixed,
versioned corpus and measure per-field exact match, complete-command accuracy,
abstention, and false accepts. False accepts are the safety-critical metric;
abstention is an intended outcome for uncertain input.

Once consented golden audio exists, measure Whisper WER/CER separately from
end-to-end field accuracy. On representative phones, measure p50/p95 stage
latency, peak process RSS, Java/native heap, thermal behavior, and battery over
a fixed repeated run. Android Studio Profiler and Perfetto are sufficient for
the initial device work. The next runtime increment should be a
privacy-preserving local diagnostic event stream and user-driven JSON export,
not OpenTelemetry or continuous profiling infrastructure.

### Verification

- `:app:sharedLogic:testAndroidHostTest --rerun-tasks` passed after the new
  field regressions exposed and corrected a receiver-scoping defect in the
  Serbian matcher.
- `:app:sharedLogic:allTests`, `:app:sharedUI:allTests`, Android Detekt,
  explicit core/shared-logic metadata Detekt, `assembleDebug`, and `lintDebug`
  passed.
- `:core:jvmTest` production and test compilation passed, but the suite is
  currently red because a separate uncommitted architecture-metrics test scans
  the nonexistent package `com.shraggen.diarium.core`, obtains A=0 and I=0, and
  intentionally fails its distance threshold. Validate that metric's scope and
  purpose before treating it as a CI fitness function.

### Next physical acceptance

Install the rebuilt APK and repeat the same four voice recordings. No GGUF
model should be requested or loaded. Each confirmation card should show hive 4
or 5 and queen seen Yes/Ja/Да. Then test one ambiguous or hedged phrase and
verify that confirmation remains blocked. After that, add the small local
diagnostic event model and export before considering any telemetry SDK.

---

## 2026-07-16 — Executable specification foundation

### Decision

The journal remains the product domain; deterministic parsing is an adapter for
one bounded journaling command, not the product itself. Add a lightweight
specification layer now so future tools inherit common safety contracts without
requiring every possible utterance to become a hand-written regression.

Keep dependency injection explicit through constructors while the composition
root is small. Koin may be introduced later at the Android/iOS composition
edge if object graph lifecycle or platform bindings become difficult; domain,
planner, kernel, and tool tests must remain container-free.

### Completed

- Added Gherkin features for recording and reviewing hive inspections.
- Added executable BDD-style common tests covering multilingual planning,
  explicit confirmation, cancellation, ambiguity, and newest-first retrieval.
- Added kernel invariants for the plan/execute boundary, blank input, unknown
  tools, and duplicate tool registration.
- Added reusable deterministic-planner and mutating-tool conformance helpers.
- Added KMP property tests that generate arbitrary numeric identifiers and
  formatting variations across English, German, Serbian Latin, and Serbian
  Cyrillic, plus ambiguity and no-invention properties.
- Extended the shared-UI confirmation contract so only complete proposals whose
  identifier and queen observation both match the transcript can be confirmed.
- Hardened `RecordInspectionTool` so a missing identifier returns a failure
  without throwing or writing, as required by the mutating-tool contract.

### Verification

- `:core:jvmTest` passed, including four kernel invariant tests.
- `:app:sharedLogic:allTests` passed. The Android host run executed five feature
  scenarios, two conformance tests, and three property checks with 300 generated
  cases per property; iOS simulator test sources also compiled successfully.
- `:app:sharedUI:allTests` passed.
- aggregate Detekt plus explicit core and shared-logic metadata Detekt passed.
- Android `assembleDebug` and `lintDebug` passed.

---

## 2026-07-16 — Tag-driven GitHub release pipeline

### Decision

Keep `main` as the only long-lived development branch. Release selection is a
manual product decision made by merging a generated Release Please pull
request; release branches are unnecessary until multiple supported production
lines actually exist.

Use Conventional Commit squash-merge titles as version metadata. `fix` bumps
the patch version, `feat` bumps the minor version, and a breaking-change marker
bumps the major version. Documentation, test, CI, and chore commits do not
create a release by themselves.

### Completed

- Added Release Please configuration bootstrapped from version `0.1.0`.
- Added `version.txt` as the shared release-version source and derived Android
  `versionName` and monotonic `versionCode` from it.
- Added CI validation for Conventional Commit pull request titles.
- Added a manual-gated GitHub Release workflow that creates a draft release,
  checks out the exact tagged commit, reruns non-device quality gates, restores
  an ephemeral Android keystore, builds and verifies a signed release APK,
  generates a SHA-256 checksum, adds public-repository provenance, uploads the
  assets, and only then publishes the release.
- Added repository keystore exclusions and documented the one-time signing and
  Release Please token setup.

### Remaining external setup

Create and safely back up the Android release keystore, then configure the four
Android signing secrets. Add the recommended `RELEASE_PLEASE_TOKEN` so generated
release pull requests trigger normal branch-protection checks.

### Verification

- Release Please configuration passed its published JSON Schema.
- `ci.yml`, `release.yml`, and `mkdocs.yml` passed YAML parsing; both GitHub
  workflows passed `actionlint`.
- Android debug and unsigned local release builds passed with
  `versionName=0.1.0` and derived `versionCode=1000`; release lint passed.
- Partial signing configuration failed at Gradle configuration time with the
  intended missing-property diagnostic.
- The complete release gate passed with a disposable keystore:
  Detekt, core tests, shared-logic tests, shared-UI tests, release lint, and
  `assembleRelease`.
- `apksigner` verified the generated release APK with APK Signature Scheme v2.
- `mkdocs build --strict` passed.

---

## 2026-07-16 — Release permission and documentation-site repair

### Finding

The first Release Please run successfully created its release commit and branch
but GitHub rejected pull-request creation because the workflow fell back to a
restricted `GITHUB_TOKEN`.

The deployed documentation root was a 404 page. The project also lacked a
configured GitHub Project Pages base URL, so generated assets could resolve
against `/assets` instead of `/Diarium/assets`.

### Completed

- Made `RELEASE_PLEASE_TOKEN` explicit and added an early diagnostic when the
  secret is missing. The existing Release Please branch can be reused on rerun.
- Added the canonical GitHub Pages `site_url`, repository metadata, and strict
  documentation builds.
- Added a real landing page with a concise product explanation, architecture
  entry points, workflow overview, and responsive documentation cards.
- Reorganized navigation into Home, Architecture, and Development sections.
- Added light/dark palettes, repository links, mobile styling, and project
  branding while retaining Material for MkDocs.

### Verification

- MkDocs strict build passed.
- The generated root contains the landing-page content, no 404 marker, a
  canonical `/Diarium/` URL, and relative project-safe asset links.
- All GitHub workflows passed `actionlint`.
