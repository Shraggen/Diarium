package com.shraggen.diarium.tool

class ToolExecutor(
    private val registry: ToolRegistry,
) {

    suspend fun execute(
        call: ToolCall,
    ): ToolResult {

        val tool =
            registry[call.toolName]
                ?: return ToolResult.Failure(
                    "Unknown tool '${call.toolName}'."
                )

        return tool.execute(
            ToolArguments(call.arguments)
        )
    }
}
