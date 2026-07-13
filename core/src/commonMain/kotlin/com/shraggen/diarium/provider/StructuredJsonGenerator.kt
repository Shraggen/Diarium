package com.shraggen.diarium.provider

import kotlinx.serialization.json.JsonObject

interface StructuredJsonGenerator {

    suspend fun generate(
        prompt: String,
        schema: JsonObject,
    ): String
}
