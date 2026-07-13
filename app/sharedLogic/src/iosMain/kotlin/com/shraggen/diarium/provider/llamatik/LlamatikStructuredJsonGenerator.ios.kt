package com.shraggen.diarium.provider.llamatik

import com.llamatik.library.platform.LlamaBridge
import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

actual class LlamatikStructuredJsonGenerator actual constructor() :
    StructuredJsonGenerator {

    actual fun initialize(modelFileName: String): Boolean {
        val modelPath = LlamaBridge.getModelPath(modelFileName)
        return LlamaBridge.initGenerateModel(modelPath)
    }

    actual override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String =
        LlamaBridge.generateJson(
            prompt = prompt,
            jsonSchema = schema.toString(),
        )

    actual fun shutdown() {
        LlamaBridge.shutdown()
    }
}
