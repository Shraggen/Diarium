package tool

import com.shraggen.diarium.tool.ToolArguments
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolArgumentsTest {

    @Test
    fun booleanOrNullRejectsMissingAndNonBooleanValues() {
        val arguments = ToolArguments(
            buildJsonObject {
                put("valid", true)
                put("invalid", "true")
            },
        )

        assertEquals(true, arguments.booleanOrNull("valid"))
        assertNull(arguments.booleanOrNull("invalid"))
        assertNull(arguments.booleanOrNull("missing"))
    }
}
