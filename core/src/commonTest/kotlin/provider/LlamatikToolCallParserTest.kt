package provider

import com.shraggen.diarium.provider.llamatik.LlamatikToolCallParser
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
