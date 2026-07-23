# Mobile reliability proof and syncable reprocessing

Date: 2026-07-24

## Summary

First prove that a midrange Android phone can perform the complete offline path
safely: record audio, transcribe locally, select among four tools, extract
structured arguments, present a reviewable proposal, and persist only after
confirmation.

The milestone passes when at least 85% of supported, unambiguous recordings
produce an entirely correct draft without correction, while zero incorrect
drafts are eligible for confirmation. Sync and desktop processing follow only
after this gate.

## 1. Make `core` a trustworthy proposal harness

- Replace bare `ToolCall` planning with an `ActionProposal` containing:
  - a globally unique proposal ID;
  - the source evidence and transcript revision IDs and digest;
  - the candidate tool call;
  - a `ReadyForReview` or `Blocked` status;
  - validation issues;
  - processor, model, prompt, and schema versions;
  - transcript evidence spans supporting extracted arguments.
- Change `Tool` to expose deterministic argument validation. `ToolExecutor`
  must validate again immediately before execution.
- Remove the `process()` shortcut that plans and executes without confirmation.
- Execute only a `ConfirmedAction` tied to the exact proposal and transcript
  digest. Editing the transcript, changing context, or producing a newer
  proposal invalidates confirmation.
- Require idempotency keys so repeated confirmation cannot create duplicate
  journal entries.
- Keep planners and processors replaceable; neither Android, Room, audio, nor
  beekeeping belongs in `core`.

## 2. Build a sync-ready local evidence record

- Store every field recording in private app storage by default instead of
  deleting the temporary WAV.
- Persist:
  - a UUID-based evidence ID;
  - capture time, language, audio hash, and private path;
  - immutable transcript revisions;
  - generated proposals and provenance;
  - confirmation and cancellation decisions;
  - the resulting journal event ID.
- Allow users to delete retained audio without deleting the confirmed journal
  entry; mark the evidence as unavailable for future reprocessing.
- Give every journal event a global UUID. Keep Room's numeric key internal
  only.
- Migrate existing inspections without data loss, marking them as legacy
  entries without source evidence.
- Keep the Android manifest free of network permission during this milestone
  and verify the workflow in airplane mode.
- Record an ADR for the future encrypted relay: it transports opaque encrypted
  events and evidence, does not run models, and does not decide journal truth.

## 3. Exercise a meaningful hybrid agent workflow

Implement four narrow write tools:

- `record_inspection`: `hive_id`, required explicit `queen_seen`, and optional
  non-negative `brood_frames`.
- `record_treatment`: `hive_id`, required treatment name, and optional dose
  text.
- `record_feeding`: `hive_id`, required feed type, and optional amount plus
  unit; amount and unit must appear together.
- `create_follow_up`: `hive_id`, required description, and optional
  non-negative `due_in_days`, converted deterministically from capture time.

Planning behavior:

- Run deterministic extractors for known high-risk values such as hive IDs,
  negation, quantities, and contradictory observations.
- Run the local LLM as a candidate tool router and broader argument extractor.
- Prefer a complete deterministic result; record model agreement as additional
  provenance.
- Block confirmation when deterministic and model outputs conflict, required
  fields lack transcript evidence, multiple tools remain plausible, or the
  utterance is ambiguous, hedged, contradictory, read-only, or unsupported.
- Preserve the complete transcript as a free-form note even when structured
  extraction abstains.
- Benchmark the verified Qwen2.5 0.5B Q4 model first, then the 1.5B Q4
  candidate. Select the smallest model meeting every gate. Load Whisper and
  the command model sequentially where necessary to control memory.

## 4. Evaluation and device acceptance

- Add a versioned corpus of at least 240 consented recordings:
  - 160 positive cases balanced across the four tools and English, German,
    Serbian Latin, and Serbian Cyrillic;
  - 40 ambiguous, contradictory, or incomplete cases;
  - 40 read-only, irrelevant, and adversarial cases;
  - multiple speakers and quiet, phone-distance, Bluetooth-headset, and
    apiary-like noise conditions.
- Export privacy-preserving local diagnostics as JSON: stage timings, model
  hashes, raw and edited transcripts, proposals, validation outcomes,
  corrections, confirmations, cancellations, and execution results. Audio is
  exported only by explicit user action.
- Measure ASR WER/CER separately from end-to-end tool accuracy so transcription
  and interpretation failures remain distinguishable.
- Pass gates:
  - at least 85% exact tool-and-required-argument accuracy on supported,
    unambiguous recordings;
  - zero incorrect proposals in `ReadyForReview`;
  - 100% of ambiguous, contradictory, and unsupported inputs blocked;
  - zero writes before confirmation and zero duplicates after repeated
    confirmation;
  - p95 stop-to-proposal latency no greater than 15 seconds on the target
    device class;
  - peak process RSS no greater than 1.5 GB;
  - 30 consecutive recordings without a crash, native abort, or progressive
    resource leak.
- Run on at least two Android devices from 2022 or newer with approximately
  6 GB RAM, reporting battery and thermal behavior even if they are not initial
  hard gates.

## 5. Conditional roadmap after the proof

- If all gates pass, implement the encrypted relay, device identities, notebook
  membership, append-only replication, and a desktop processor that emits new
  proposals against existing evidence.
- If safety passes but utility or performance fails, keep phone-local ASR and
  deterministic tools; move broad LLM extraction to the paired laptop.
- If safety fails, prohibit phone-generated LLM proposals from becoming
  confirmable and retain deterministic parsing plus manual structured entry.
- Desktop reprocessing never silently changes confirmed facts. It creates a
  new versioned proposal that an authorized user reviews on any device.
- A secondary phone only syncs evidence and confirmed events; it does not need
  to run inference. If it processes evidence, its output remains another
  untrusted proposal.

## Assumptions

- Beekeeping remains the reference domain; profession-neutral abstractions are
  extracted only when the four-tool vertical needs them.
- Android is the first validated runtime; iOS parity and live sync are outside
  this milestone.
- Raw audio is retained by default in private storage with user-controlled
  deletion.
- The future relay is optional, client-encrypted, and compute-free; local
  journaling never depends on it.
- Human confirmation remains mandatory for every mutating tool regardless of
  benchmark accuracy.
