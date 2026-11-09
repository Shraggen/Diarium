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
Delivery status: PR #65 (feat/59-golden-corpus-model-evals -> main) is open and awaiting CI and review. Merging needs explicit approval. It is titled `test(eval):` so Release Please does not cut a feature release; rename it to `feat:` if a release is wanted.
Next steps: (1) Watch PR #65 CI. The macOS ios-build runs only after merging to main. (2) Record consented clips for the golden voice notes into eval/voice-notes/<GoldenVoiceNote.audioClipName> (16 kHz mono PCM16), after checking the LFS quota. (3) Use modelEval to compare greedy and production sampling for the production-sampling ADR. (4) Optional: an eval-only Koog-backed ToolCallPlanner scored with modelEval. (5) Optional: switch sharedLogic mutation testing to jvmTest.
Last updated: 2026-11-09
