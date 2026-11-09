---
hide:
  - navigation
  - toc
---

<div class="diarium-hero" markdown>

# Diarium

## An offline-first, multilingual field journal for beekeepers

Record hive inspections by typing or speaking in English, German, or Serbian.
Diarium plans a reviewable journal action locally and writes nothing until the
beekeeper explicitly confirms it.

[Build your first APK](getting-started.md){ .md-button .md-button--primary }
[Explore the architecture](architecture.md){ .md-button }

</div>

<div class="grid cards diarium-cards" markdown>

-   :material-shield-check:{ .lg .middle } **Confirmation before persistence**

    ---

    Planning is side-effect free. Ambiguous, contradictory, or incomplete
    observations remain blocked instead of being guessed.

    [Plan-confirm-execute boundary](decisions/0002-plan-confirm-execute.md)

-   :material-translate:{ .lg .middle } **Multilingual field input**

    ---

    The current inspection workflow recognizes bounded facts in English,
    German, Serbian Latin, and Serbian Cyrillic.

    [Deterministic planning decision](decisions/0006-deterministic-inspection-planning.md)

-   :material-cellphone-lock:{ .lg .middle } **Local and offline-first**

    ---

    Speech processing, command planning, confirmation, and Room persistence
    remain on the device for the primary workflow.

    [Architecture overview](architecture.md)

-   :material-test-tube:{ .lg .middle } **Executable specifications**

    ---

    Gherkin scenarios, BDD tests, kernel invariants, conformance contracts, and
    property tests define the safety envelope.

    [Testing strategy](testing.md)

</div>

## Current workflow

<div class="diarium-flow" markdown>

1.  :material-microphone-message: **Capture**

    Type a note or transcribe speech locally.

2.  :material-call-split: **Plan**

    Convert explicit facts into a reviewable tool call.

3.  :material-shield-search: **Verify**

    Check the proposed hive and queen observation against the transcript.

4.  :material-book-check: **Confirm or abstain**

    Confirmation writes once; cancellation or uncertainty writes nothing.

</div>

## Start here

### Learn by doing

Follow [Build your first Diarium APK](getting-started.md) for one concrete path
from a fresh checkout to a successful Android build.

### Complete a task

- [Verify a change](verifying-changes.md) before opening a pull request.
- [Prepare a release](releasing.md) when a version is ready for publication.

### Look up the project

- [Module and source-set reference](module-reference.md) maps modules, source
  sets, and placement rules.
- [Testing strategy](testing.md) describes test layers, gates, and coverage.

### Understand the design

- [Architecture overview](architecture.md) explains the system and its
  boundaries.
- [Why source paths are deep](project-structure.md) explains the combined
  Gradle, Kotlin Multiplatform, and JVM package layout.
- [Plan, confirm, execute](decisions/0002-plan-confirm-execute.md) explains why
  planning cannot save data.
- [Deterministic inspection planning](decisions/0006-deterministic-inspection-planning.md)
  explains why bounded facts no longer use generated JSON.
- [Project journal](journal.md) records how the project evolved.
