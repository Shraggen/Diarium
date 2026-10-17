package provider

import com.shraggen.diarium.provider.llamatik.LlamatikToolMapper
import com.shraggen.diarium.schema.toJsonObject
import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.ToolSpecification
import com.shraggen.diarium.tool.tool
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlamatikToolMapperPropertyTest {

    @Test
    fun promptListsEveryToolAndEndsWithTheUserRequest() = runTest {
        checkAll(
            PROPERTY_ITERATIONS,
            Arb.list(Arb.pair(Arb.string(1..12), Arb.string(0..24)), 1..MAX_TOOLS),
            Arb.string(1..40),
        ) { declarations, userInput ->
            val tools = declarations.distinctBy { it.first }.map { (name, description) ->
                DescribedTool(toolName = name, toolDescription = description)
            }

            val prompt = LlamatikToolMapper.promptFor(userInput, tools)

            tools.forEach { tool ->
                val line = "- ${tool.specification.name}: ${tool.specification.description}"
                assertTrue(line in prompt, "Prompt is missing tool line '$line'.")
            }
            assertTrue(
                prompt.endsWith("User request:\n$userInput"),
                "Prompt must end with the verbatim user request.",
            )
        }
    }

    @Test
    fun schemaOffersOneStrictBranchPerToolInRegistrationOrder() = runTest {
        checkAll(
            PROPERTY_ITERATIONS,
            Arb.list(Arb.string(1..12), 1..MAX_TOOLS),
        ) { names ->
            val tools = names.distinct().map { DescribedTool(it, "") }

            val branches = LlamatikToolMapper.schemaFor(tools).getValue("oneOf").jsonArray

            assertEquals(tools.size, branches.size)
            tools.zip(branches).forEach { (tool, branch) ->
                val properties = branch.jsonObject.getValue("properties").jsonObject
                assertEquals(
                    tool.specification.name,
                    properties.getValue("tool").jsonObject.getValue("const").jsonPrimitive.content,
                )
                assertEquals(
                    tool.specification.parameters.toJsonObject(),
                    properties.getValue("arguments"),
                )
            }
        }
    }
}

private class DescribedTool(
    toolName: String,
    toolDescription: String,
) : Tool {
    override val specification: ToolSpecification = tool(toolName) {
        description(toolDescription)
        parameters { stringRequired("value") }
    }

    override suspend fun execute(arguments: ToolArguments): ToolResult =
        ToolResult.Failure("Mapper property tests never execute tools.")
}

private const val PROPERTY_ITERATIONS = 300
private const val MAX_TOOLS = 5
