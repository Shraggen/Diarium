# Diarium Lean Canvas

- Date: 2026-07-24
- Status: Working hypotheses
- Initial domain: Beekeeping

This canvas describes assumptions to test, not established facts. Beekeeping is
the first vertical used to validate the product and the underlying journaling
platform.

## Product hypothesis

Field workers with occupied or dirty hands will prefer a private mobile
assistant that turns speech into reviewable journal entries entirely offline,
provided that it is faster than their current method and never silently records
an invented fact.

## Canvas

| Area | Current hypothesis |
| --- | --- |
| Problem | Field observations are difficult to record while wearing gloves or handling equipment. Remote work sites may have no usable connection. Notes reconstructed after the work are incomplete or inaccurate. |
| Existing alternatives | Paper notebooks, memory, generic voice memos, generic note apps, manual beekeeping forms, and beekeeping apps whose voice or AI features require connectivity or payment. |
| Customer segments | Initial user: an independent beekeeper performing repeated hive inspections in locations with unreliable connectivity. Later candidates include farm, maintenance, forestry, and other field workers with similar evidence-capture needs. |
| Early adopters | Beekeepers who already attempt to keep inspection records, manage several hives, work in remote apiaries, and are dissatisfied with paper, typing, or cloud-dependent voice tools. |
| Unique value proposition | A trustworthy field journal that listens, drafts a structured entry locally, shows exactly what it understood, and saves nothing until the worker confirms it. |
| High-level concept | A private offline field assistant: voice memo plus structured form plus an audit trail. |
| Initial solution | On-device audio capture and transcription; hybrid deterministic and local-model extraction; narrow typed tools; editable review; explicit confirmation; local journal; retained evidence for optional reprocessing on stronger hardware. |
| Channels | Direct field observation, local beekeeper associations, personal referrals, small pilot groups, and open-source communities. These channels have not yet been validated. |
| Revenue | Undecided. Preserve offline capture, review, and local journaling as a usable core. Test whether optional encrypted sync, team collaboration, managed backup, desktop reprocessing, or support can fund the product without making field capture subscription-dependent. |
| Cost structure | Mobile and desktop development, device testing, model evaluation, user research, support, signing and distribution, and an optional encrypted relay. Local inference avoids per-entry cloud inference cost but increases device-support work. |
| Key metrics | Successful field entries, time from observation to confirmed entry, percentage of drafts requiring correction, safe abstention rate, false-confirmable proposal rate, completed field days, repeated weekly use, and retained users who choose the workflow over their previous method. |
| Unfair advantage | Not yet established. Possible advantages are a consented multilingual field-audio corpus, safety-focused proposal contracts, and an offline reprocessing architecture. These become advantages only if they produce demonstrably better outcomes. |

## Riskiest assumptions

| ID | Assumption | Why it matters | Cheapest useful test | Evidence |
| --- | --- | --- | --- | --- |
| H1 | Recording inspections is painful enough that beekeepers want a different workflow. | Without a meaningful problem, technical quality does not create a product. | Observe and interview 5–10 beekeepers about their last field day without first pitching Diarium. | Missing |
| H2 | Speaking and reviewing is faster or easier than paper, typing, or an unstructured voice memo. | This is the central workflow advantage. | Compare the three methods during representative inspections and measure time, omissions, and corrections. | Missing |
| H3 | Workers will trust a proposal-and-confirmation interaction. | Low trust will prevent adoption even when extraction is accurate. | Test a clickable or working prototype containing both correct and deliberately incorrect proposals. | Missing |
| H4 | A midrange phone can complete offline transcription and extraction with acceptable latency, memory, battery use, and heat. | Failure makes the proposed field experience impractical. | Run the mobile reliability benchmark on at least two representative devices. | Partially explored |
| H5 | The system can be useful while allowing zero incorrect proposals to become confirmable in the evaluation corpus. | Journal integrity is the product's safety promise. | Evaluate supported, ambiguous, contradictory, and adversarial recordings against the release gates. | Partially explored |
| H6 | English, German, Serbian Latin, and Serbian Cyrillic address a real initial user need. | Every required language multiplies testing and model constraints. | Recruit target users and record which languages they actually use during field work. | Missing |
| H7 | Retaining audio for later reprocessing is valuable enough to justify storage and privacy costs. | It drives the evidence model and laptop workflow. | Show users transcript-only and retained-audio workflows and ask them to complete deletion/reprocessing tasks. | Missing |
| H8 | The same trusted proposal harness transfers to other professions. | This determines whether `core` is a product platform or internal architecture. | Validate the complete beekeeping vertical first, then implement one small second-domain tool without changing core contracts. | Deferred |

## Experiment order

1. Validate the problem through observation and interviews.
2. Compare the proposed workflow with paper, typing, and raw voice memos.
3. Run the mobile feasibility and safety benchmark.
4. Conduct a small real-apiary pilot and measure corrections, time, and repeat
   use.
5. Prototype laptop reprocessing and shared-journal review before implementing
   live synchronization.
6. Consider a second profession only after the beekeeping workflow has repeat
   users.

## Learning rules

- A successful technical benchmark validates feasibility, not demand.
- A user saying the idea sounds useful is not evidence of repeated use.
- Abstention is acceptable when the worker can quickly correct or save a raw
  note; silent invention is not.
- Platform abstractions are extracted from proven workflows rather than
  predicted professions.
- A failed local-LLM experiment does not invalidate offline journaling. It may
  support a product with local transcription and deferred laptop extraction.

