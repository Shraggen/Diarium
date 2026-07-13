package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LlamatikToolCallParser(
    private val json: Json = Json,
) {

    fun parse(
        response: String,
        callId: String = DEFAULT_CALL_ID,
    ): ToolCall {
        val root = try {
            json.parseToJsonElement(response).jsonObject
        } catch (exception: SerializationException) {
            throw IllegalArgumentException(
                "Llamatik returned invalid JSON.",
                exception,
            )
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Llamatik response must be a JSON object.",
                exception,
            )
        }

        val toolName = root["tool"]
            ?.jsonPrimitive
            ?.content
            ?: throw IllegalArgumentException(
                "Llamatik response is missing 'tool'.",
            )

        val arguments = root["arguments"]
            ?.jsonObject
            ?: throw IllegalArgumentException(
                "Llamatik response is missing object 'arguments'.",
            )

        return ToolCall(
            id = callId,
            toolName = toolName,
            arguments = arguments,
        )
    }

    private companion object {
        const val DEFAULT_CALL_ID = "local-tool-call"
    }
}
