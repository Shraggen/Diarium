package com.shraggen.diarium.conformance

import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal data class MutatingToolContract(
    val tool: Tool,
    val validArguments: JsonObject,
    val invalidArguments: List<JsonObject>,
    val effectCount: () -> Int,
)

internal suspend fun assertMutatingToolConformance(
    contract: MutatingToolContract,
) {
    assertTrue(contract.tool.specification.name.isNotBlank())
    assertTrue(contract.invalidArguments.isNotEmpty())

    contract.invalidArguments.forEach { arguments ->
        val effectCountBefore = contract.effectCount()
        val execution = runCatching {
            contract.tool.execute(ToolArguments(arguments))
        }

        assertTrue(
            execution.isSuccess,
            "Invalid arguments must return Failure instead of throwing: $arguments",
        )
        assertIs<ToolResult.Failure>(execution.getOrThrow())
        assertEquals(effectCountBefore, contract.effectCount())
    }

    val effectCountBefore = contract.effectCount()
    assertIs<ToolResult.Success>(
        contract.tool.execute(ToolArguments(contract.validArguments)),
    )
    assertEquals(effectCountBefore + 1, contract.effectCount())
}
