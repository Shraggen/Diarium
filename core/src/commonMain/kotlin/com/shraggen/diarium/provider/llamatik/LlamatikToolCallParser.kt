package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class LlamatikToolCallParser(
    private val json: Json = Json,
) {

    fun parse(
        response: String,
        callId: String = DEFAULT_CALL_ID,
    ): ToolCall {
        val root = parseRoot(response)
        val toolName = root.requireString("tool")
        val arguments = root.requireObject("arguments")

        return ToolCall(
            id = callId,
            toolName = toolName,
            arguments = arguments,
        )
    }

    private fun parseRoot(response: String): JsonObject {
        val root = try {
            json.parseToJsonElement(response.withoutMarkdownFence())
        } catch (exception: SerializationException) {
            throw IllegalArgumentException(
                "Llamatik returned invalid JSON.",
                exception,
            )
        } as? JsonObject ?: throw IllegalArgumentException(
            "Llamatik response must be a JSON object.",
        )
        return root
    }

    private companion object {
        const val DEFAULT_CALL_ID = "local-tool-call"
    }
}

private fun String.withoutMarkdownFence(): String =
    trim()
        .removeSurrounding("```json", "```")
        .removeSurrounding("```", "```")
        .trim()

private fun JsonObject.requireString(name: String): String =
    (get(name) as? JsonPrimitive)
        ?.takeIf(JsonPrimitive::isString)
        ?.content
        ?: throw IllegalArgumentException(
            "Llamatik response must contain string '$name'.",
        )

private fun JsonObject.requireObject(name: String): JsonObject =
    get(name) as? JsonObject
        ?: throw IllegalArgumentException(
            "Llamatik response must contain object '$name'.",
        )
