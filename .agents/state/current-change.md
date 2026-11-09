# Current change

Outcome: Voice-to-journal quality is checked by a golden corpus of voice notes: deterministic harness specs on every test run, and opt-in laptop scoring of real GGUF and Whisper models (issue #59).
Owning bounded context: beekeeping-journal (app/sharedLogic); test-only evaluation code
Owning team: Shraggen
Current slice: All issue #59 slices on branch feat/59-golden-corpus-model-evals: one common Llamatik command-model adapter (ADR 0007); sharedLogic JVM target; GoldenCorpus + GoldenCorpusSpecTest; :app:sharedLogic:modelEval report; recorded model outputs replayed by RecordedModelOutputSpecTest; common Whisper transcriber and speech scoring.
Acceptance criteria: AC1-AC6 of issue #59. jvmTest/allTests need no model; modelEval fails fast without -Pdiarium.llmModel and writes build/reports/model-eval/<timestamp>.{md,json}; a deliberate parser regression fails the replay specs without a model.
Files changed: app/sharedLogic (build.gradle.kts, gradle.lockfile, commonMain Llamatik adapters, commonTest corpus, jvmTest eval and recordings), .gitattributes, .agents/config.env, docs (ADR 0007, testing, module reference, indexes), mkdocs.yml
Open risks: No consented audio clips exist yet, so Whisper scoring was verified only with synthetic local clips. CI ios-build (Xcode) not run locally; iOS klibs compile on Linux. Model eval measured on one 4-CPU laptop only. The sharedLogic mutation run still uses the Android host-test jar path.
Required checks: See `.agents/config.env`
Rollout strategy: Test-only and opt-in; modelEval is never part of jvmTest/allTests; quality thresholds stay report-only. Promoting any threshold to a blocking gate requires explicit approval.
Rollback strategy: Revert the slice commits individually; 1b (JVM target) reverts without 1a (shared adapter).
Last updated: 2026-11-09
