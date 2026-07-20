package com.shraggen.diarium.tool

import kotlinx.serialization.json.JsonElement

sealed interface ToolResult {

    data class Success(
        val content: JsonElement,
    ) : ToolResult

    data class Failure(
        val message: String,
    ) : ToolResult
}
