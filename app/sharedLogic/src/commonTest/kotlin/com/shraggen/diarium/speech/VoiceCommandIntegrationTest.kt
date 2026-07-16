package com.shraggen.diarium.speech

import com.shraggen.diarium.DiariumKernel
import com.shraggen.diarium.beekeeping.InspectionDraft
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.beekeeping.InspectionRepository
import com.shraggen.diarium.beekeeping.RecordInspectionPlanner
import com.shraggen.diarium.beekeeping.RecordInspectionTool
import com.shraggen.diarium.tool.ToolResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VoiceCommandIntegrationTest {

    @Test
    fun serbianTranscriptIsPlannedThenPersistedOnlyAfterExecution() = runTest {
        val transcript = WhisperTranscriptParser.parseSegments(
            """{"language":"sr","segments":[{"text":"Прегледао сам кошницу 4 и видео матицу."}]}""",
        )
        val repository = InMemoryInspectionRepository()
        val kernel = DiariumKernel(
            registeredTools = listOf(RecordInspectionTool(repository)),
            planner = RecordInspectionPlanner(),
        )

        val plannedCall = kernel.plan(transcript.text)

        assertEquals("record_inspection", plannedCall.toolName)
        assertTrue(repository.records.isEmpty(), "Planning must never persist data.")

        val result = kernel.execute(plannedCall)

        assertIs<ToolResult.Success>(result)
        assertEquals(1, repository.records.size)
        assertEquals("4", repository.records.single().hiveId)
        assertTrue(repository.records.single().queenSeen)
    }
}

private class InMemoryInspectionRepository : InspectionRepository {
    val records = mutableListOf<InspectionRecord>()

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val record = InspectionRecord(
            id = records.size.toLong() + 1,
            hiveId = draft.hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = 1_000,
        )
        records += record
        return record
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> =
        records.takeLast(limit).reversed()
}
