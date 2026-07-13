package com.shraggen.diarium.provider.llamatik

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContains

class LlamatikStructuredPromptTest {

    @Test
    fun includesSchemaAndForbidsExtraText() {
        val schema = buildJsonObject {
            put("type", "object")
        }

        val result = structuredJsonPrompt(
            prompt = "Choose a tool.",
            schema = schema,
        )

        assertContains(result, "Choose a tool.")
        assertContains(result, "exactly one JSON object")
        assertContains(result, schema.toString())
        assertContains(result, "Do not use markdown fences")
    }
}
