package tool

import com.shraggen.diarium.tool.ToolArguments
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolArgumentsPropertyTest {

    @Test
    fun typedAccessorsReturnTheValuesTheyWereGiven() = runTest {
        checkAll(
            PROPERTY_ITERATIONS,
            Arb.string(0..16),
            Arb.int(),
            Arb.boolean(),
        ) { text, number, flag ->
            val arguments = ToolArguments(
                buildJsonObject {
                    put(TEXT, text)
                    put(NUMBER, number)
                    put(FLAG, flag)
                },
            )

            assertEquals(text, arguments.string(TEXT))
            assertEquals(text, arguments.stringOrNull(TEXT))
            assertEquals(number, arguments.int(NUMBER))
            assertEquals(number, arguments.intOrNull(NUMBER))
            assertEquals(flag, arguments.boolean(FLAG))
            assertEquals(flag, arguments.booleanOrNull(FLAG))
            assertNull(arguments.stringOrNull(MISSING))
            assertNull(arguments.intOrNull(MISSING))
            assertNull(arguments.booleanOrNull(MISSING))
        }
    }

    @Test
    fun quotedValuesNeverBecomeBooleans() = runTest {
        val booleanLookalikes = Arb.element("true", "false", "TRUE", "False", " true ")

        checkAll(
            PROPERTY_ITERATIONS,
            Arb.choice(booleanLookalikes, Arb.string(0..8)),
        ) { text ->
            val arguments = ToolArguments(buildJsonObject { put(FLAG, text) })

            assertNull(arguments.booleanOrNull(FLAG))
        }
    }
}

private const val PROPERTY_ITERATIONS = 300
private const val TEXT = "text"
private const val NUMBER = "number"
private const val FLAG = "flag"
private const val MISSING = "missing"
