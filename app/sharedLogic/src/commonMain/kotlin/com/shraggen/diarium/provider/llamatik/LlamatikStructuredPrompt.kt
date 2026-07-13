package com.shraggen.diarium.provider.llamatik

import kotlinx.serialization.json.JsonObject

internal fun structuredJsonPrompt(
    prompt: String,
    schema: JsonObject,
): String =
    buildString {
        appendLine(prompt)
        appendLine()
        appendLine("Return exactly one JSON object matching this JSON Schema.")
        appendLine("Do not use markdown fences, commentary, or reasoning text.")
        appendLine()
        appendLine("JSON Schema:")
        append(schema)
    }
