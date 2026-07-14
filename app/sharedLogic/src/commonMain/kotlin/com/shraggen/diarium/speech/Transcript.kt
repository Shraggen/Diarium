package com.shraggen.diarium.speech

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Transcript(
    val text: String,
    val detectedLanguage: String?,
)

object WhisperTranscriptParser {

    fun parseSegments(json: String): Transcript {
        val root = Json.parseToJsonElement(json).jsonObject
        val language = root["language"]?.jsonPrimitive?.contentOrNull
        val text = root["segments"]
            ?.jsonArray
            ?.mapNotNull { segment ->
                segment.jsonObject["text"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }
            ?.joinToString(separator = " ")
            .orEmpty()

        require(text.isNotBlank()) {
            "Whisper returned an empty transcript."
        }

        return Transcript(
            text = text,
            detectedLanguage = language,
        )
    }
}
