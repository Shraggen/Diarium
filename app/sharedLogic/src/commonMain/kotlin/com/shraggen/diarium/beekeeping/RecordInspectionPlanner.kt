package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolCallPlanner
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RecordInspectionPlanner(
    private val hiveIdentifierExtractor: HiveIdentifierExtractor =
        HiveIdentifierExtractor(),
    private val queenObservationExtractor: QueenObservationExtractor =
        QueenObservationExtractor(),
) : ToolCallPlanner {

    override suspend fun plan(userInput: String): ToolCall {
        val hiveIdentifier = hiveIdentifierExtractor.extract(userInput).singleOrNull()
        val queenSeen = queenObservationExtractor.extract(userInput).singleOrNull()

        return ToolCall(
            id = DETERMINISTIC_CALL_ID,
            toolName = RECORD_INSPECTION_TOOL_NAME,
            arguments = buildJsonObject {
                hiveIdentifier?.let { put(HIVE_ID_ARGUMENT_NAME, it) }
                queenSeen?.let { put(QUEEN_SEEN_ARGUMENT_NAME, it) }
            },
        )
    }

    private companion object {
        const val DETERMINISTIC_CALL_ID = "deterministic-record-inspection"
    }
}
