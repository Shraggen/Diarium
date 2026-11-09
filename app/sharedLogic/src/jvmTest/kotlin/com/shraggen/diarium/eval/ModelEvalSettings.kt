package com.shraggen.diarium.eval

import com.llamatik.library.platform.LlamaBridge
import com.shraggen.diarium.provider.llamatik.StructuredGenerationMode
import java.io.File

/**
 * Llamatik generation parameters. Llamatik has no seed control, so only
 * [GREEDY] (`topK = 1`) gives repeatable output. [PRODUCTION] mirrors the
 * native defaults of Llamatik 1.10.1, which the app uses because it never
 * calls `updateGenerateParams`.
 */
@Suppress("MagicNumber")
enum class SamplingProfile(
    val temperature: Float,
    val maxTokens: Int,
    val topP: Float,
    val topK: Int,
    val repeatPenalty: Float,
    val contextLength: Int,
    val numThreads: Int,
    val useMmap: Boolean,
    val flashAttention: Boolean,
    val batchSize: Int,
    val gpuLayers: Int,
) {
    GREEDY(0.55f, 640, 0.80f, 1, 1.10f, 4096, 4, true, false, 512, 0),
    PRODUCTION(0.55f, 640, 0.80f, 20, 1.10f, 4096, 4, true, false, 512, 0),
    ;

    /** Must run before the model is initialized. */
    fun applyToLlamaBridge() {
        LlamaBridge.updateGenerateParams(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            topK = topK,
            repeatPenalty = repeatPenalty,
            contextLength = contextLength,
            numThreads = numThreads,
            useMmap = useMmap,
            flashAttention = flashAttention,
            batchSize = batchSize,
            gpuLayers = gpuLayers,
        )
    }

    fun describe(): String =
        "${name.lowercase()} (temperature=$temperature, topK=$topK, topP=$topP, " +
            "repeatPenalty=$repeatPenalty, maxTokens=$maxTokens, contextLength=$contextLength, " +
            "numThreads=$numThreads, batchSize=$batchSize, gpuLayers=$gpuLayers)"
}

/** Inputs of one `modelEval` run, passed by Gradle as system properties. */
data class ModelEvalSettings(
    val llmModel: File,
    val promptMode: StructuredGenerationMode,
    val sampling: SamplingProfile,
    val repeats: Int,
    val reportDir: File,
) {
    companion object {
        fun fromSystemProperties(): ModelEvalSettings {
            val modelPath = System.getProperty("diarium.llmModel")
            require(!modelPath.isNullOrBlank()) {
                "No command model given. Run ./gradlew :app:sharedLogic:modelEval -Pdiarium.llmModel=<path.gguf>"
            }
            val llmModel = File(modelPath)
            require(llmModel.isFile) { "Command model not found: ${llmModel.absolutePath}" }
            return ModelEvalSettings(
                llmModel = llmModel,
                promptMode = enumProperty("diarium.promptMode", StructuredGenerationMode.PROMPT_GUIDED),
                sampling = enumProperty("diarium.sampling", SamplingProfile.GREEDY),
                repeats = System.getProperty("diarium.evalRepeats")?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                reportDir = File(System.getProperty("diarium.reportDir") ?: "build/reports/model-eval"),
            )
        }

        private inline fun <reified T : Enum<T>> enumProperty(name: String, default: T): T {
            val value = System.getProperty(name)?.takeIf { it.isNotBlank() } ?: return default
            return enumValues<T>().firstOrNull { it.name.equals(value.replace('-', '_'), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown $name '$value'; expected one of ${enumValues<T>().joinToString { it.name.lowercase() }}",
                )
        }
    }
}
