# PRD: Offline mobile field-journal validation

- Date: 2026-07-24
- Status: Draft for validation
- Product domain: Beekeeping
- Related plan:
  [Mobile reliability proof and syncable reprocessing](../plans/mobile-reliability-and-reprocessing.md)

## 1. Objective

Validate that a beekeeper can record an observation without connectivity,
receive a trustworthy structured draft, correct or reject it, and save a local
journal entry more conveniently than using the worker's existing method.

This PRD defines a product experiment. It does not define the complete Diarium
platform.

## 2. Target user and situation

The initial user is a beekeeper inspecting multiple hives at an apiary where
network connectivity cannot be assumed. The user's hands may be occupied,
gloved, or dirty, and the phone must remain usable without an account or
backend.

The job to be done is:

> While I inspect a hive, help me capture what I observed and what I need to do
> next, without interrupting the work or creating an untrustworthy record.

## 3. Success criteria

The product experiment succeeds only when both product and technical evidence
are positive.

### Product evidence

- Pilot users complete real field entries without connectivity.
- Voice plus review has lower median capture-and-correction time than their
  current structured-entry method.
- Users can identify what the assistant understood before saving.
- Users return for another field day without being prompted solely for the
  experiment.
- Qualitative feedback shows that retained audio, structured fields, or
  follow-up tasks solve a real problem beyond storing an ordinary voice memo.

No numeric retention threshold is set until the first pilot establishes a
baseline. Report individual participant outcomes rather than presenting a
small pilot as statistically representative.

### Technical gates

- At least 85% of supported, unambiguous evaluation recordings produce the
  exact tool and all required arguments without field correction.
- Zero incorrect proposals are eligible for confirmation.
- Every ambiguous, contradictory, incomplete, unsupported, or read-only input
  is blocked from execution.
- Planning never writes data, and every mutation requires confirmation.
- Repeated confirmation cannot create a duplicate entry.
- On the target midrange device class, p95 stop-to-proposal latency is no more
  than 15 seconds and peak process RSS is no more than 1.5 GB.
- Thirty consecutive recordings complete without a crash, native abort, or
  progressive resource leak.

## 4. User experience

1. The user selects a language and starts recording.
2. The app captures audio locally and stops on sustained silence or manual
   action.
3. The app retains the recording privately and creates an on-device transcript.
4. The user may edit the transcript. An edit invalidates any existing
   proposal.
5. Diarium creates either:
   - a complete reviewable proposal;
   - a blocked proposal explaining what is missing or conflicting; or
   - a raw journal note when structured extraction is unsupported.
6. The review presents localized, domain-specific fields rather than raw JSON.
7. Confirming the unchanged proposal writes exactly one entry. Cancelling,
   editing, or leaving a blocked proposal writes no structured entry.
8. The local journal shows the saved entry and its capture time.
9. The user may delete retained audio without deleting the confirmed journal
   entry.

## 5. Functional requirements

### Offline capture and evidence

- Audio capture, transcription, planning, review, confirmation, persistence,
  and journal reading work in airplane mode.
- Each recording receives a global evidence ID, capture time, language, content
  hash, and immutable transcript revisions.
- Raw audio is retained by default in private storage and is never exported
  without explicit user action.
- The app remains useful for typed input when no speech model is installed.

### Structured actions

The experiment supports four mutating tools:

| Tool | Required fields | Optional fields |
| --- | --- | --- |
| `record_inspection` | `hive_id`, explicit `queen_seen` | non-negative `brood_frames` |
| `record_treatment` | `hive_id`, treatment name | dose text |
| `record_feeding` | `hive_id`, feed type | amount and unit as a pair |
| `create_follow_up` | `hive_id`, description | non-negative `due_in_days` |

- Deterministic extractors handle safety-sensitive identifiers, negation,
  quantities, and contradictions.
- A local model proposes tool routing and broader arguments.
- Conflicting deterministic and model interpretations block confirmation.
- Every proposed value must be traceable to supporting transcript text.
- Domain validation runs during planning and again immediately before
  execution.

### Review and correction

- The review names the action and renders every field in the selected language.
- Missing, conflicting, or uncertain fields are visible and cannot be
  confirmed.
- The user can return to the transcript, correct it, and request a new
  proposal.
- The saved entry records the evidence, transcript, proposal, processor
  provenance, confirmation decision, and globally unique event ID.

### Local diagnostics

- A user-triggered JSON export contains model and prompt versions, stage
  timings, transcripts, proposal outcomes, corrections, and execution results.
- Diagnostic export excludes raw audio unless the user separately selects it.
- The application performs no background telemetry in this experiment.

## 6. Failure behavior

- Missing or failed speech model: retain the audio and offer retry, model
  selection, or manual transcription.
- Unclear audio or unsupported language: preserve the evidence and block
  structured execution.
- Model failure, timeout, malformed output, or native error: preserve the
  transcript and offer deterministic or manual entry; do not write a partial
  action.
- Unknown tool, invalid arguments, stale proposal, or transcript digest
  mismatch: reject execution without repository mutation.
- Storage failure: show that the entry was not saved and retain recoverable
  evidence where possible.
- Application restart: restore retained evidence and confirmed journal entries;
  never restore a proposal as silently confirmed.

## 7. Evaluation

- Use a versioned corpus of at least 240 consented recordings:
  - 160 positive cases balanced across four tools and English, German, Serbian
    Latin, and Serbian Cyrillic;
  - 40 ambiguous, contradictory, or incomplete cases;
  - 40 read-only, irrelevant, or adversarial cases.
- Include multiple speakers and quiet, phone-distance, Bluetooth-headset, and
  apiary-like noise conditions.
- Report speech WER/CER separately from exact tool and field accuracy.
- Test on at least two Android phones from 2022 or newer with approximately
  6 GB RAM.
- For the field pilot, record task time, proposal corrections, abstentions,
  cancellations, confirmed entries, user-reported omissions, and whether the
  user chooses Diarium again on a later field day.

## 8. Out of scope

- Live synchronization, accounts, organizations, and shared notebooks.
- Desktop processing and automatic reprocessing.
- Cloud inference or mandatory connectivity.
- Production iOS voice parity.
- Beekeeping recommendations, diagnosis, or autonomous decisions.
- Analytics dashboards, sensors, QR/NFC workflow, model marketplace, and
  automatic production-model downloads.
- Generalization to another profession.

## 9. Decision after the experiment

- If product evidence and all technical gates pass, proceed to an encrypted
  sync relay and desktop reprocessing prototype.
- If users value the workflow but the mobile LLM misses utility or performance
  gates, retain phone-local capture and transcription and move broad extraction
  to paired stronger hardware.
- If users prefer raw searchable transcripts, simplify the product rather than
  forcing structured automation.
- If users do not repeatedly choose the workflow, stop platform expansion and
  revisit the customer, problem, or interaction.
- If safety gates fail, local-model output cannot become confirmable; use
  deterministic or manual structured entry only.

