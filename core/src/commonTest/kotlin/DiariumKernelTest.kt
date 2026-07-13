import com.shraggen.diarium.DiariumKernel
import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DiariumKernelTest {

    @Test
    fun generatesParsesAndExecutesToolCall() {
        val generator = FakeGenerator(
            response = """
                {
                    "tool": "test_action",
                    "arguments": {
                        "value": "expected"
                    }
                }
            """.trimIndent(),
        )

        val kernel = DiariumKernel(
            registeredTools = listOf(TestTool),
            generator = generator,
        )

        val result = runSuspend {
            kernel.process("Perform the test action.")
        }

        val success = assertIs<ToolResult.Success>(result)

        assertEquals(
            "expected",
            success.content.jsonPrimitive.content,
        )

        assertEquals(
            1,
            generator.generatedCount,
        )

        assertEquals(
            1,
            generator.lastSchema?.get("oneOf")?.jsonArray?.size,
        )
    }

    @Test
    fun rejectsBlankInputWithoutCallingGenerator() {
        val generator = FakeGenerator(response = "{}")

        val kernel = DiariumKernel(
            registeredTools = listOf(TestTool),
            generator = generator,
        )

        val result = runSuspend {
            kernel.process(" ")
        }

        assertIs<ToolResult.Failure>(result)
        assertEquals(0, generator.generatedCount)
    }

    @Test
    fun planningDoesNotExecuteToolBeforeConfirmation() {
        val generator = FakeGenerator(
            response = """
                {
                    "tool": "counted_action",
                    "arguments": {"value": "pending"}
                }
            """.trimIndent(),
        )
        val tool = CountingTool()
        val kernel = DiariumKernel(
            registeredTools = listOf(tool),
            generator = generator,
        )

        val call = runSuspend {
            kernel.plan("Plan an action.")
        }

        assertEquals(0, tool.executionCount)

        runSuspend {
            kernel.execute(call)
        }

        assertEquals(1, tool.executionCount)
    }

    private class FakeGenerator(
        private val response: String,
    ) : StructuredJsonGenerator {

        var generatedCount: Int = 0
            private set

        var lastSchema: JsonObject? = null
            private set

        override suspend fun generate(
            prompt: String,
            schema: JsonObject,
        ): String {
            generatedCount += 1
            lastSchema = schema
            return response
        }
    }

    private object TestTool : Tool {

        override val specification = tool("test_action") {
            description("Executes the test action.")

            parameters {
                stringRequired("value")
            }
        }

        override suspend fun execute(
            arguments: ToolArguments,
        ): ToolResult =
            ToolResult.Success(
                JsonPrimitive(arguments.string("value")),
            )
    }

    private class CountingTool : Tool {

        var executionCount = 0
            private set

        override val specification = tool("counted_action") {
            description("Executes a counted action.")
            parameters {
                stringRequired("value")
            }
        }

        override suspend fun execute(arguments: ToolArguments): ToolResult {
            executionCount += 1
            return ToolResult.Success(JsonPrimitive(arguments.string("value")))
        }
    }
}

private fun <T> runSuspend(block: suspend () -> T): T {
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
