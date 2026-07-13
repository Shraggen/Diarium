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
        val structuredPrompt = structuredJsonPrompt(prompt, schema)
        val formattedPrompt = LlamaBridge.applyChatTemplate(
            messages = listOf("user" to structuredPrompt),
            addAssistantPrefix = true,
        ) ?: structuredPrompt

        // Llamatik 1.8.1 double-accepts generated tokens in its grammar sampler,
        // which aborts the process on the first JSON token. Keep schema guidance
        // in the prompt until the native dependency removes that second accept.
        return LlamaBridge.generate(formattedPrompt)
    }

    actual fun shutdown() {
        LlamaBridge.shutdown()
    }
}
