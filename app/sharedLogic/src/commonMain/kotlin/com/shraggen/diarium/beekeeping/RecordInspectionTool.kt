package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.Tool
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RecordInspectionTool(
    private val repository: InspectionRepository,
) : Tool {

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
        val hiveId = arguments.string("hive_id").trim()
        val queenSeen = arguments.booleanOrNull("queen_seen") ?: false

        if (hiveId.isEmpty()) {
            return ToolResult.Failure("Hive identifier must not be blank.")
        }

        val inspection = repository.record(
            InspectionDraft(
                hiveId = hiveId,
                queenSeen = queenSeen,
            ),
        )

        return ToolResult.Success(
            buildJsonObject {
                put("id", inspection.id)
                put("hive_id", inspection.hiveId)
                put("queen_seen", inspection.queenSeen)
                put("recorded_at_epoch_millis", inspection.recordedAtEpochMillis)
                put("recorded", true)
            },
        )
    }
}
