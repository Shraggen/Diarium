package com.shraggen.diarium.tool

import kotlinx.serialization.json.JsonObject

data class ToolCall(
    val id: String,
    val toolName: String,
    val arguments: JsonObject,
)
