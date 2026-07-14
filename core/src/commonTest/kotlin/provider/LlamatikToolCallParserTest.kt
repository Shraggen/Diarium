package provider

import com.shraggen.diarium.provider.llamatik.LlamatikToolCallParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LlamatikToolCallParserTest {

    private val parser = LlamatikToolCallParser()

    @Test
    fun parsesJsonObject() {
        val call = parser.parse(
            """{"tool":"record_inspection","arguments":{"hive_id":"4"}}""",
        )

        assertEquals("record_inspection", call.toolName)
        assertEquals("4", call.arguments["hive_id"].toString().trim('"'))
    }

    @Test
    fun unwrapsMarkdownFence() {
        val call = parser.parse(
            """
                ```json
                {"tool":"record_inspection","arguments":{"hive_id":"4"}}
                ```
            """.trimIndent(),
        )

        assertEquals("record_inspection", call.toolName)
    }

    @Test
    fun rejectsInvalidJsonWithStableError() {
        val exception = assertFailsWith<IllegalArgumentException> {
            parser.parse("not json")
        }

        assertEquals("Llamatik returned invalid JSON.", exception.message)
    }

    @Test
    fun rejectsNonObjectResponseWithStableError() {
        val exception = assertFailsWith<IllegalArgumentException> {
            parser.parse("[]")
        }

        assertEquals("Llamatik response must be a JSON object.", exception.message)
    }

    @Test
    fun rejectsNonStringToolNameWithStableError() {
        val exception = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"tool":42,"arguments":{}}""")
        }

        assertEquals(
            "Llamatik response must contain string 'tool'.",
            exception.message,
        )
    }

    @Test
    fun rejectsNonObjectArgumentsWithStableError() {
        val exception = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"tool":"record_inspection","arguments":[]}""")
        }

        assertEquals(
            "Llamatik response must contain object 'arguments'.",
            exception.message,
        )
    }
}
