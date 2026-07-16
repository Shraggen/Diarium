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
                description(
                    "Exact identifier value only. For numeric identifiers, return " +
                        "canonical ASCII digits: hive four, Bienenstock vier, and " +
                        "košnica četiri must all produce 4. Never include words such " +
                        "as Hive or Bienenstock, use an ordinal such as peta, or invent " +
                        "a placeholder.",
                )
            }

            booleanRequired("queen_seen") {
                description(
                    "True only when the transcript explicitly says the queen was seen; " +
                        "false only when it explicitly says the queen was not seen. " +
                        "Never infer or default this observation.",
                )
            }
        }
    }

    override suspend fun execute(
        arguments: ToolArguments,
    ): ToolResult {
        val hiveId = arguments.stringOrNull("hive_id")?.trim().orEmpty()
        val queenSeen = arguments.booleanOrNull("queen_seen")

        return when {
            hiveId.isEmpty() ->
                ToolResult.Failure("Hive identifier must not be blank.")
            queenSeen == null -> ToolResult.Failure(
                "Queen observation must be supplied as a boolean.",
            )
            else -> recordInspection(hiveId, queenSeen)
        }
    }

    private suspend fun recordInspection(
        hiveId: String,
        queenSeen: Boolean,
    ): ToolResult {
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
