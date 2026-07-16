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

[Explore the architecture](architecture.md){ .md-button .md-button--primary }
[Read the release process](releasing.md){ .md-button }

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

| If you want to understand… | Read… |
| --- | --- |
| The system and its boundaries | [Architecture overview](architecture.md) |
| Why planning cannot save data | [Plan, confirm, execute](decisions/0002-plan-confirm-execute.md) |
| Why inspection planning became deterministic | [ADR 0006](decisions/0006-deterministic-inspection-planning.md) |
| How behavior is specified and verified | [Testing strategy](testing.md) |
| How trunk becomes a GitHub Release | [Release process](releasing.md) |
| How the project evolved | [Project journal](journal.md) |
