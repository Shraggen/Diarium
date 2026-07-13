package com.shraggen.diarium.provider.llamatik

import com.llamatik.library.platform.LlamaBridge
import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

actual class LlamatikStructuredJsonGenerator actual constructor() :
    StructuredJsonGenerator {

    actual fun initialize(modelLocation: String): Boolean {
        val modelPath = LlamaBridge.getModelPath(modelLocation)
        return LlamaBridge.initGenerateModel(modelPath)
    }

    actual override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String {
        if (USE_NATIVE_GRAMMAR) {
            val formattedPrompt = LlamaBridge.applyChatTemplate(
                messages = listOf("user" to prompt),
                addAssistantPrefix = true,
            ) ?: prompt

            // Re-enabled after upgrading to Llamatik 1.9. Not independently
            // confirmed fixed upstream — verify on a physical device before
            // trusting this. Flip USE_NATIVE_GRAMMAR back to false if the
            // "Unexpected empty grammar stack" abort reappears.
            return LlamaBridge.generateJson(
                prompt = formattedPrompt,
                jsonSchema = schema.toString(),
            )
        }

        val structuredPrompt = structuredJsonPrompt(prompt, schema)
        val formattedPrompt = LlamaBridge.applyChatTemplate(
            messages = listOf("user" to structuredPrompt),
            addAssistantPrefix = true,
        ) ?: structuredPrompt
        return LlamaBridge.generate(formattedPrompt)
    }

    actual fun shutdown() {
        LlamaBridge.shutdown()
    }

    private companion object {
        const val USE_NATIVE_GRAMMAR = false // reproduced identically on 1.9 — see tombstone 2026-07-13 23:02
    }
}
