package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.provider.StructuredJsonGenerator
import kotlinx.serialization.json.JsonObject

expect class LlamatikStructuredJsonGenerator() : StructuredJsonGenerator {

    fun initialize(modelFileName: String): Boolean

    override suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String

    fun shutdown()
}
