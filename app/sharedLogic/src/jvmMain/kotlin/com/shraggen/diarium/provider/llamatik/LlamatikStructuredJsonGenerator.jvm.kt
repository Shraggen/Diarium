package com.shraggen.diarium.provider.llamatik

import com.llamatik.library.platform.LlamaBridge
import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

actual class LlamatikStructuredJsonGenerator actual constructor(
    private val mode: StructuredGenerationMode,
) :
    StructuredJsonGenerator {

    actual fun initialize(modelLocation: String): Boolean {
        val modelPath = LlamaBridge.getModelPath(modelLocation)
        return LlamaBridge.initGenerateModel(modelPath)
    }

    actual override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String {
        val structuredPrompt = when (mode) {
            StructuredGenerationMode.PROMPT_GUIDED ->
                structuredJsonPrompt(prompt, schema)
            StructuredGenerationMode.NATIVE_GRAMMAR -> prompt
        }
        val formattedPrompt = LlamaBridge.applyChatTemplate(
            messages = listOf("user" to structuredPrompt),
            addAssistantPrefix = true,
        ) ?: structuredPrompt

        return when (mode) {
            StructuredGenerationMode.PROMPT_GUIDED ->
                LlamaBridge.generate(formattedPrompt)
            StructuredGenerationMode.NATIVE_GRAMMAR ->
                LlamaBridge.generateJson(formattedPrompt, schema.toString())
        }
    }

    actual fun shutdown() {
        LlamaBridge.shutdown()
    }
}
