package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.schema.toJsonObject
import com.shraggen.diarium.tool.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object LlamatikToolMapper {

    fun promptFor(
        userInput: String,
        tools: List<Tool>,
    ): String {
        require(tools.isNotEmpty()) {
            "At least one tool must be registered."
        }

        val toolDescriptions = tools.joinToString(separator = "\n") { tool ->
            "- ${tool.specification.name}: ${tool.specification.description}"
        }

        return """
            Select exactly one available tool for the user's request.
            Return only the JSON object required by the supplied schema.

            Available tools:
            $toolDescriptions

            User request:
            $userInput
        """.trimIndent()
    }

    fun schemaFor(tools: List<Tool>): JsonObject {
        require(tools.isNotEmpty()) {
            "At least one tool must be registered."
        }

        return buildJsonObject {
            put(
                "oneOf",
                buildJsonArray {
                    tools.forEach { tool ->
                        add(branchFor(tool))
                    }
                },
            )
        }
    }

    private fun branchFor(tool: Tool): JsonObject =
        buildJsonObject {
            put("type", "object")

            put(
                "properties",
                buildJsonObject {
                    put(
                        "tool",
                        buildJsonObject {
                            put("const", tool.specification.name)
                        },
                    )

                    put(
                        "arguments",
                        tool.specification.parameters.toJsonObject(),
                    )
                },
            )

            put(
                "required",
                buildJsonArray {
                    add("tool")
                    add("arguments")
                },
            )

            put("additionalProperties", false)
        }
}
