package tool

import com.shraggen.diarium.tool.tool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolBuilderTest {

    @Test
    fun buildsSpecification() {

        val tool =
            tool("weather") {

                description("Weather")

                parameters {

                    stringRequired("city")
                }
            }

        assertEquals(
            "weather",
            tool.name,
        )

        assertEquals(
            "Weather",
            tool.description,
        )

        assertTrue(
            tool.parameters.properties.containsKey("city")
        )
    }
}
