package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class BeekeepingToolsTest {

    @Test
    fun recordInspectionPersistsValidatedArguments() = runTest {
        val repository = FakeInspectionRepository()
        val result = RecordInspectionTool(repository).execute(
            ToolArguments(
                buildJsonObject {
                    put("hive_id", " 4 ")
                    put("queen_seen", true)
                },
            ),
        )

        val success = assertIs<ToolResult.Success>(result)
        assertEquals("4", repository.inspections.single().hiveId)
        assertEquals(
            "4",
            success.content.jsonObject["hive_id"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun blankHiveIdentifierCannotBePersisted() = runTest {
        val repository = FakeInspectionRepository()
        val result = RecordInspectionTool(repository).execute(
            ToolArguments(
                buildJsonObject {
                    put("hive_id", "   ")
                },
            ),
        )

        assertIs<ToolResult.Failure>(result)
        assertEquals(emptyList(), repository.inspections)
    }

}

private class FakeInspectionRepository : InspectionRepository {

    val inspections = mutableListOf<InspectionRecord>()

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val inspection = InspectionRecord(
            id = inspections.size.toLong() + 1,
            hiveId = draft.hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = inspections.size.toLong(),
        )
        inspections.add(0, inspection)
        return inspection
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> =
        inspections.take(limit)
}
