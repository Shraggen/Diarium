package provider

import com.shraggen.diarium.provider.llamatik.LlamatikToolCallParser
import com.shraggen.diarium.tool.ToolCall
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LlamatikToolCallParserPropertyTest {

    private val parser = LlamatikToolCallParser()

    @Test
    fun wellFormedResponsesRoundTripWithOrWithoutMarkdownFences() = runTest {
        checkAll(
            PROPERTY_ITERATIONS,
            Arb.string(0..16),
            jsonObjectArb(MAX_JSON_DEPTH),
            Arb.element(responseWrappers),
            Arb.string(1..8),
        ) { toolName, arguments, wrap, callId ->
            val response = JsonObject(
                mapOf("tool" to JsonPrimitive(toolName), "arguments" to arguments),
            ).toString()

            assertEquals(
                ToolCall(id = callId, toolName = toolName, arguments = arguments),
                parser.parse(wrap(response), callId),
            )
        }
    }

    @Test
    fun responsesAreAcceptedExactlyWhenToolIsTextAndArgumentsAreAnObject() = runTest {
        checkAll(PROPERTY_ITERATIONS, candidateResponseArb) { root ->
            val response = root.toString()
            val tool = (root as? JsonObject)?.get("tool") as? JsonPrimitive
            val arguments = (root as? JsonObject)?.get("arguments") as? JsonObject

            if (tool != null && tool.isString && arguments != null) {
                val call = parser.parse(response)
                assertEquals(tool.content, call.toolName)
                assertEquals(arguments, call.arguments)
            } else {
                assertFailsWith<IllegalArgumentException> { parser.parse(response) }
            }
        }
    }

    @Test
    fun arbitraryModelOutputIsRejectedOnlyAsIllegalArgument() = runTest {
        checkAll(PROPERTY_ITERATIONS, Arb.string(0..64)) { response ->
            runCatching { parser.parse(response) }
                .exceptionOrNull()
                ?.let { assertIs<IllegalArgumentException>(it) }
        }
    }
}

private val responseWrappers: List<(String) -> String> = listOf(
    { it },
    { " \n\t$it\n " },
    { "```json\n$it\n```" },
    { "```\n$it\n```" },
)

private val primitiveArb: Arb<JsonElement> = Arb.choice(
    Arb.string(0..8).map { JsonPrimitive(it) },
    Arb.int().map { JsonPrimitive(it) },
    Arb.boolean().map { JsonPrimitive(it) },
    Arb.constant(JsonNull),
)

private fun jsonObjectArb(depth: Int): Arb<JsonObject> = arbitrary {
    val values = if (depth == 0) primitiveArb else jsonElementArb(depth - 1)
    JsonObject(Arb.map(Arb.string(0..6), values, minSize = 0, maxSize = 4).bind())
}

private fun jsonElementArb(depth: Int): Arb<JsonElement> {
    if (depth == 0) return primitiveArb
    return Arb.choice(
        primitiveArb,
        jsonObjectArb(depth),
        Arb.list(jsonElementArb(depth - 1), 0..3).map { JsonArray(it) },
    )
}

/** Mostly near-miss tool calls: right keys with arbitrary value types, plus extra keys. */
private val candidateResponseArb: Arb<JsonElement> = Arb.choice(
    jsonElementArb(MAX_JSON_DEPTH),
    arbitrary {
        val extra = jsonObjectArb(1).bind()
        JsonObject(
            extra + mapOf(
                "tool" to jsonElementArb(1).bind(),
                "arguments" to jsonElementArb(MAX_JSON_DEPTH).bind(),
            ),
        )
    },
)

private const val PROPERTY_ITERATIONS = 300
private const val MAX_JSON_DEPTH = 2
