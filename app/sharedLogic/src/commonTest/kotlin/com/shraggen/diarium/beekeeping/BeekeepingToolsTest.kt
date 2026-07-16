package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.schema.JsonBooleanSchema
import com.shraggen.diarium.schema.JsonObjectSchema
import com.shraggen.diarium.schema.JsonStringSchema
import com.shraggen.diarium.tool.ToolArguments
import com.shraggen.diarium.tool.ToolResult
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class BeekeepingToolsTest {

    @Test
    fun recordInspectionSchemaRequiresCanonicalExplicitArguments() {
        val parameters = assertIs<JsonObjectSchema>(
            RecordInspectionTool(FakeInspectionRepository()).specification.parameters,
        )
        val hiveSchema = assertIs<JsonStringSchema>(parameters.properties["hive_id"])
        val queenSchema = assertIs<JsonBooleanSchema>(parameters.properties["queen_seen"])

        assertTrue("hive_id" in parameters.required)
        assertTrue("queen_seen" in parameters.required)
        assertContains(checkNotNull(hiveSchema.description), "canonical ASCII digits")
        assertContains(checkNotNull(hiveSchema.description), "Never include words")
        assertContains(checkNotNull(queenSchema.description), "explicitly says")
        assertContains(checkNotNull(queenSchema.description), "Never infer")
    }

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

    @Test
    fun missingQueenObservationCannotBePersisted() = runTest {
        val repository = FakeInspectionRepository()
        val result = RecordInspectionTool(repository).execute(
            ToolArguments(
                buildJsonObject {
                    put("hive_id", "4")
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
