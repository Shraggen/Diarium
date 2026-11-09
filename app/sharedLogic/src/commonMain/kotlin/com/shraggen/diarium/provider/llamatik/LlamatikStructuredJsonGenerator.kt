package com.shraggen.diarium.provider.llamatik

import com.llamatik.library.platform.LlamaBridge
import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

enum class StructuredGenerationMode {
    PROMPT_GUIDED,
    NATIVE_GRAMMAR,
}

/**
 * Command-model adapter over Llamatik's common `LlamaBridge` API. One class
 * serves every platform (ADR 0007). The bridge holds one global model per
 * process and generation is not thread-safe, so callers must not generate
 * concurrently.
 */
class LlamatikStructuredJsonGenerator(
    private val mode: StructuredGenerationMode = StructuredGenerationMode.PROMPT_GUIDED,
) : StructuredJsonGenerator {

    fun initialize(modelLocation: String): Boolean {
        val modelPath = LlamaBridge.getModelPath(modelLocation)
        return LlamaBridge.initGenerateModel(modelPath)
    }

    override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String {
        return when (mode) {
            StructuredGenerationMode.PROMPT_GUIDED ->
                generateWithPrompt(prompt, schema)
            StructuredGenerationMode.NATIVE_GRAMMAR ->
                generateWithNativeGrammar(prompt, schema)
        }
    }

    private fun generateWithPrompt(prompt: String, schema: JsonObject): String {
        val structuredPrompt = structuredJsonPrompt(prompt, schema)
        val formattedPrompt = LlamaBridge.applyChatTemplate(
            messages = listOf("user" to structuredPrompt),
            addAssistantPrefix = true,
        ) ?: structuredPrompt
        return LlamaBridge.generate(formattedPrompt)
    }

    private fun generateWithNativeGrammar(
        prompt: String,
        schema: JsonObject,
    ): String {
        val formattedPrompt = LlamaBridge.applyChatTemplate(
            messages = listOf("user" to prompt),
            addAssistantPrefix = true,
        ) ?: prompt
        return LlamaBridge.generateJson(
            prompt = formattedPrompt,
            jsonSchema = schema.toString(),
        )
    }

    fun shutdown() {
        LlamaBridge.shutdown()
    }
}
