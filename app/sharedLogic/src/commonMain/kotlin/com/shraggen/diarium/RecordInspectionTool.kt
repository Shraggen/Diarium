package com.shraggen.diarium

import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RecordInspectionTool : Tool {

    override val specification = tool("record_inspection") {
        description("Records the status of a beehive inspection.")

        parameters {
            stringRequired("hive_id") {
                description("Identifier of the inspected hive.")
            }

            boolean("queen_seen") {
                description("Whether the queen was observed.")
            }
        }
    }

    override suspend fun execute(
        arguments: ToolArguments,
    ): ToolResult {
        val hiveId = arguments.string("hive_id")
        val queenSeen = arguments.booleanOrNull("queen_seen") ?: false

        return ToolResult.Success(
            buildJsonObject {
                put("hive_id", hiveId)
                put("queen_seen", queenSeen)
                put("recorded", true)
            },
        )
    }
}
