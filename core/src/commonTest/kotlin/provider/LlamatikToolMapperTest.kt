package provider

import com.shraggen.diarium.provider.llamatik.LlamatikToolMapper
import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.tool
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class LlamatikToolMapperTest {

    @Test
    fun createsOneStrictBranchPerTool() {
        val schema = LlamatikToolMapper.schemaFor(
            listOf(TestTool),
        )

        val branch = schema["oneOf"]
            ?.jsonArray
            ?.single()
            ?.jsonObject
            ?: error("Missing tool branch.")

        val properties = branch["properties"]!!.jsonObject

        assertEquals(
            "test_action",
            properties["tool"]!!
                .jsonObject["const"]!!
                .jsonPrimitive
                .content,
        )

        assertEquals(
            "object",
            properties["arguments"]!!
                .jsonObject["type"]!!
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun promptExplicitlySupportsGermanAndBothSerbianScripts() {
        val prompt = LlamatikToolMapper.promptFor(
            userInput = "Прегледао сам кошницу 4 и видео матицу.",
            tools = listOf(TestTool),
        )

        assertContains(prompt, "Bienenstock 4")
        assertContains(prompt, "košnicu 4")
        assertContains(prompt, "кошницу 4")
        assertContains(prompt, "preserve identifier meaning exactly")
        assertContains(prompt, "never prefix an identifier")
        assertContains(prompt, "Never invent")
    }

    private object TestTool : Tool {

        override val specification = tool("test_action") {
            description("Executes an action.")

            parameters {
                stringRequired("value")
            }
        }

        override suspend fun execute(
            arguments: ToolArguments,
        ): ToolResult =
            ToolResult.Success(JsonNull)
    }
}
