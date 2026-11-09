package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

enum class StructuredGenerationMode {
    PROMPT_GUIDED,
    NATIVE_GRAMMAR,
}

expect class LlamatikStructuredJsonGenerator(
    mode: StructuredGenerationMode = StructuredGenerationMode.PROMPT_GUIDED,
) : StructuredJsonGenerator {

    fun initialize(modelLocation: String): Boolean

    override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String

    fun shutdown()
}
