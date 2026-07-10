package com.shraggen.diarium.tool

interface Tool {

    val specification: ToolSpecification

    suspend fun execute(
        arguments: ToolArguments,
    ): ToolResult
}
