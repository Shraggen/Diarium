package com.shraggen.diarium

import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.provider.llamatik.LlamatikToolCallParser
import com.shraggen.diarium.provider.llamatik.LlamatikToolMapper
import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolExecutor
import com.shraggen.diarium.tool.ToolRegistry
import com.shraggen.diarium.tool.ToolResult

class DiariumKernel(
    registeredTools: List<Tool>,
    private val generator: StructuredJsonGenerator,
    private val parser: LlamatikToolCallParser = LlamatikToolCallParser(),
) {

    private val tools = registeredTools.toList()

    private val executor = ToolExecutor(
        ToolRegistry(tools),
    )

    init {
        require(tools.isNotEmpty()) {
            "DiariumKernel requires at least one registered tool."
        }

        require(tools.distinctBy { it.specification.name }.size == tools.size) {
            "Tool names must be unique."
        }
    }

    suspend fun process(userInput: String): ToolResult {
        if (userInput.isBlank()) {
            return ToolResult.Failure(
                message = "User input must not be blank.",
            )
        }

        val response = generator.generate(
            prompt = LlamatikToolMapper.promptFor(
                userInput = userInput,
                tools = tools,
            ),
            schema = LlamatikToolMapper.schemaFor(tools),
        )

        val call = parser.parse(response)

        return executor.execute(call)
    }
}
