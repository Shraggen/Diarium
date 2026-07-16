package com.shraggen.diarium.conformance

import com.shraggen.diarium.tool.ToolCallPlanner
import kotlinx.serialization.json.JsonObject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal data class PlannerExample(
    val name: String,
    val input: String,
    val expectedArguments: JsonObject,
)

internal suspend fun assertDeterministicPlannerConformance(
    planner: ToolCallPlanner,
    expectedToolName: String,
    examples: List<PlannerExample>,
) {
    assertTrue(expectedToolName.isNotBlank())
    assertTrue(examples.isNotEmpty())

    examples.forEach { example ->
        val firstCall = planner.plan(example.input)

        assertEquals(expectedToolName, firstCall.toolName, example.name)
        assertEquals(example.expectedArguments, firstCall.arguments, example.name)

        repeat(DETERMINISM_REPETITIONS) {
            assertEquals(firstCall, planner.plan(example.input), example.name)
        }
    }
}

private const val DETERMINISM_REPETITIONS = 5
