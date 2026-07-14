# Android development model

Diarium's first real inference path uses an instruction-tuned GGUF model
selected through Android's system file picker. The app copies the selected
file into its private `files/models` directory, initializes Llamatik with the
absolute path, and reloads the newest copied model on later launches.

Voice input uses a second, independent Llamatik lifecycle. The app records
16 kHz mono PCM16 audio, uses Silero VAD to detect speech and stop after a
completed utterance, writes a temporary WAV, and asks Llamatik Whisper for a
language-preserving segmented transcript. The transcript is passed through
the same plan → confirm → Room persistence path as typed text.

## Recommended baseline

- Model: Qwen2.5-0.5B-Instruct
- Quantization: Q4_K_M
- File: `qwen2.5-0.5b-instruct-q4_k_m.gguf`
- Approximate size: 491 MB
- Source:
  <https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF>
- Direct file page:
  <https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/blob/main/qwen2.5-0.5b-instruct-q4_k_m.gguf>

This is a deliberately small baseline for proving constrained tool execution.
It is not yet a final production-model decision.

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

Keep at least 1 GB of free device storage while importing the model because
the source file and Diarium's private copy temporarily coexist.

## Device verification

1. Download the GGUF file to the Android device, or copy it into the device's
   Downloads directory.
2. Build and install `app/androidApp` on a physical Android device.
3. Open Diarium and select **Select GGUF model**.
4. Choose `qwen2.5-0.5b-instruct-q4_k_m.gguf` in the system picker.
5. Select **Select Whisper model** and choose a multilingual `.bin` model.
6. Wait until both statuses change to **Ready**.
7. Select English, German, or Serbian, then use **Record voice** or enter text.
8. Review the proposed `record_inspection` arguments and select **Confirm**.

The expected result is a successful `record_inspection` execution containing
`hive_id`, `queen_seen`, and `recorded`. The inspection is stored in Room and
must remain visible in the inspection journal after force-stopping and
relaunching the app.

## Current limitations

- Model acquisition is manual; there is no in-app downloader yet.
- The model is copied without an import progress percentage.
- Android is the only verified platform for this milestone.
- Android microphone capture is implemented; the iOS speech capture adapter is
  still pending even though the shared iOS app remains part of CI.
- Silero stops after 900 ms of sustained non-speech and recording has a
  30-second safety cap. These thresholds need field calibration in apiaries.
- Whisper preserves the selected source language (`translate = false`). The
  tool prompt explicitly covers English, German, Serbian Latin, and Serbian
  Cyrillic, but final language quality remains model-dependent.
- Every model-proposed journal write requires explicit confirmation. This is
  a safety boundary for prompt-guided generation, not a substitute for
  reliable intent routing.
- Llamatik's schema-grammar generation path double-accepts sampled tokens and
  can abort the process on the first JSON token. The behavior was reproduced
  on both 1.8.1 and 1.9.0. Diarium defaults to `PROMPT_GUIDED`, supplying the
  schema in a strict prompt and parsing the returned JSON in Kotlin. The
  explicit `NATIVE_GRAMMAR` mode is retained only for testing a future fix;
  do not treat the default path as token-level schema enforcement.
- The crash workaround was verified on an SM-A546B with
  `qwen2.5-0.5b-instruct-q4_0.gguf`: the sample inspection returned
  `{"hive_id":"4","queen_seen":true,"recorded":true}` without a native
  abort. Q4_K_M remains the recommended baseline; broader latency, memory,
  and resource-shutdown measurements are still outstanding.
- LLM-driven multi-tool routing is deferred. The 0.5B development model
  misrouted a read request to the mutating record tool during device testing;
  the journal therefore reads the repository directly for now.
