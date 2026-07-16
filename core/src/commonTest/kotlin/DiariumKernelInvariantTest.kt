package com.shraggen.diarium

import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolCallPlanner
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.tool
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DiariumKernelInvariantTest {

    @Test
    fun planningNeverExecutesRegisteredTools() {
        val tool = InvariantCountingTool()
        val planner = InvariantPlanner()
        val kernel = DiariumKernel(listOf(tool), planner)
        val inputs = listOf(
            "first command",
            "zweiter Befehl",
            "treća naredba",
            "четврта наредба",
        )

        inputs.forEach { input ->
            runInvariantSuspend { kernel.plan(input) }
        }

        assertEquals(inputs.size, planner.planningCount)
        assertEquals(0, tool.executionCount)
    }

    @Test
    fun blankProcessingInvokesNeitherPlannerNorTool() {
        val tool = InvariantCountingTool()
        val planner = InvariantPlanner()
        val kernel = DiariumKernel(listOf(tool), planner)

        val result = runInvariantSuspend { kernel.process(" \n\t ") }

        assertIs<ToolResult.Failure>(result)
        assertEquals(0, planner.planningCount)
        assertEquals(0, tool.executionCount)
    }

    @Test
    fun unknownCallsCannotExecuteRegisteredTools() {
        val tool = InvariantCountingTool()
        val kernel = DiariumKernel(listOf(tool), InvariantPlanner())
        val unknownCall = ToolCall(
            id = "unknown-call",
            toolName = "unknown_tool",
            arguments = buildJsonObject {},
        )

        val result = runInvariantSuspend { kernel.execute(unknownCall) }

        assertIs<ToolResult.Failure>(result)
        assertEquals(0, tool.executionCount)
    }

    @Test
    fun duplicateToolNamesAreRejectedAtCompositionTime() {
        assertFailsWith<IllegalArgumentException> {
            DiariumKernel(
                registeredTools = listOf(
                    InvariantCountingTool(),
                    InvariantCountingTool(),
                ),
                planner = InvariantPlanner(),
            )
        }
    }
}

private class InvariantPlanner : ToolCallPlanner {
    var planningCount: Int = 0
        private set

    override suspend fun plan(userInput: String): ToolCall {
        planningCount += 1
        return ToolCall(
            id = "invariant-call",
            toolName = INVARIANT_TOOL_NAME,
            arguments = buildJsonObject {
                put("value", userInput)
            },
        )
    }
}

private class InvariantCountingTool : Tool {
    var executionCount: Int = 0
        private set

    override val specification = tool(INVARIANT_TOOL_NAME) {
        description("Counts executions for kernel invariant tests.")
        parameters {
            stringRequired("value")
        }
    }

    override suspend fun execute(arguments: ToolArguments): ToolResult {
        executionCount += 1
        return ToolResult.Success(JsonPrimitive(arguments.string("value")))
    }
}

private const val INVARIANT_TOOL_NAME = "invariant_counted_tool"

private fun <T> runInvariantSuspend(block: suspend () -> T): T {
    var completed: Result<T>? = null

    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                completed = result
            }
        },
    )

    return checkNotNull(completed) {
        "Test coroutine did not complete synchronously."
    }.getOrThrow()
}
