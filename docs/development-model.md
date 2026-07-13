# Android development model

Diarium's first real inference path uses an instruction-tuned GGUF model
selected through Android's system file picker. The app copies the selected
file into its private `files/models` directory, initializes Llamatik with the
absolute path, and reloads the newest copied model on later launches.

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

Keep at least 1 GB of free device storage while importing the model because
the source file and Diarium's private copy temporarily coexist.

## Device verification

1. Download the GGUF file to the Android device, or copy it into the device's
   Downloads directory.
2. Build and install `app/androidApp` on a physical Android device.
3. Open Diarium and select **Select GGUF model**.
4. Choose `qwen2.5-0.5b-instruct-q4_k_m.gguf` in the system picker.
5. Wait until the status changes to **Ready**.
6. Enter: `I inspected hive 4 and saw the queen.`
7. Select **Process locally**.
8. Review the proposed `record_inspection` arguments and select **Confirm**.

The expected result is a successful `record_inspection` execution containing
`hive_id`, `queen_seen`, and `recorded`. The inspection is stored in Room and
must remain visible in the inspection journal after force-stopping and
relaunching the app.

## Current limitations

- Model acquisition is manual; there is no in-app downloader yet.
- The model is copied without an import progress percentage.
- Android is the only verified platform for this milestone.
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
