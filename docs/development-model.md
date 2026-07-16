# Local model baselines

The active inspection workflow requires only a multilingual Whisper model.
Typed commands are planned by common deterministic Kotlin, and voice commands
use the same planner after transcription. The Android app no longer imports,
restores, or loads a GGUF command model for `record_inspection`.

## Whisper baseline

- Device/native smoke test: `ggml-base-q5_1.bin` (about 60 MB)
- Multilingual quality candidate: `ggml-small-q5_1.bin` (about 190 MB)
- Source: <https://huggingface.co/ggerganov/whisper.cpp>
- Required format: whisper.cpp GGML `.bin`

Never select an `.en` model: those variants cannot satisfy the German and
Serbian requirements. Base Q5_1 keeps smoke testing fast; Small Q5_1 is the
current quality candidate for German and Serbian acceptance, not yet a final
production decision.

Llamatik 1.9 exposes Whisper model initialization and WAV transcription, but
does not expose whisper.cpp's VAD API and does not bundle a Silero model.
Diarium therefore uses the MIT-licensed `android-vad` Silero 2.0.10 adapter,
whose ONNX model is bundled with the Android dependency. Keep that adapter
behind the recorder boundary so it can be replaced if Llamatik later exposes
native whisper.cpp VAD.

## Device verification

1. Download a multilingual Whisper `.bin` file to the Android device.
2. Build and install `app/androidApp` on a physical Android device.
3. Open Diarium and select **Select Whisper model**.
4. Wait until the Whisper status changes to **Ready**.
5. Select English, German, or Serbian, then use **Record voice** or enter text.
6. Review the deterministic `record_inspection` plan and select **Confirm**.

The expected result is a successful execution containing `hive_id`,
`queen_seen`, and `recorded`. The inspection is stored in Room and must remain
visible after force-stopping and relaunching the app. Typed input must work even
when no Whisper model is installed.

## Experimental command-model baseline

The provider adapter and prompt/parser regression tests remain available for a
future open-ended `ToolCallPlanner`. The previously verified development model
was Qwen2.5-0.5B-Instruct Q4_K_M in GGUF format. It is not part of the current
inspection UI because repeated field tests showed unreliable canonical
structured extraction and because loading it consumed resources without adding
value to the bounded two-field command.

Llamatik's schema-grammar generation path in versions 1.8.1 and 1.9.0 can abort
the process due to a native double-accept failure. Any future model-backed
planner must keep prompt-guided generation isolated, remain behind explicit
confirmation and deterministic validation, and re-evaluate the native path only
after an upstream version is verified on a physical device.

## Current limitations

- Whisper acquisition is manual; there is no in-app downloader yet.
- Import has no progress percentage.
- Android is the only verified production runtime for this milestone.
- Android microphone capture is implemented; the iOS speech capture adapter is
  still pending even though the shared iOS app remains part of CI.
- Silero stops after 900 ms of sustained non-speech and recording has a
  30-second safety cap. These thresholds need field calibration in apiaries.
- Whisper preserves the selected source language (`translate = false`).
- The deterministic planner intentionally supports a finite inspection grammar.
  Unsupported, ambiguous, contradictory, or hedged fields must remain blocked
  instead of being guessed.
- LLM-driven multi-tool routing remains deferred. A future planner can use the
  core `ToolCallPlanner` seam without changing the tool or repository contracts.
