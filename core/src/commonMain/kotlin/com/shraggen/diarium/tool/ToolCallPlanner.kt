package com.shraggen.diarium.tool

fun interface ToolCallPlanner {

    suspend fun plan(userInput: String): ToolCall
}
