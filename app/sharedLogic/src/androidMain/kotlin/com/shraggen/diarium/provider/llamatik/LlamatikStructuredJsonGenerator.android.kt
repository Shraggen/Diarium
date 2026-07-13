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

    actual fun shutdown() {
        LlamaBridge.shutdown()
    }
}
