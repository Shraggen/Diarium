package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.conformance.MutatingToolContract
import com.shraggen.diarium.conformance.PlannerExample
import com.shraggen.diarium.conformance.assertDeterministicPlannerConformance
import com.shraggen.diarium.conformance.assertMutatingToolConformance
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordInspectionConformanceTest {

    @Test
    fun plannerConformsToDeterministicContract() = runTest {
        assertDeterministicPlannerConformance(
            planner = RecordInspectionPlanner(),
            expectedToolName = RECORD_INSPECTION_TOOL_NAME,
            examples = listOf(
                plannerExample(
                    name = "English",
                    input = "I inspected hive 4 and saw the queen.",
                    hiveId = "4",
                    queenSeen = true,
                ),
                plannerExample(
                    name = "German",
                    input =
                        "Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.",
                    hiveId = "5",
                    queenSeen = true,
                ),
                plannerExample(
                    name = "Serbian Cyrillic",
                    input = "Прегледао сам кошницу 6 и нисам видео матицу.",
                    hiveId = "6",
                    queenSeen = false,
                ),
            ),
        )
    }

    @Test
    fun toolConformsToValidatedSingleEffectContract() = runTest {
        val repository = ConformanceInspectionRepository()

        assertMutatingToolConformance(
            MutatingToolContract(
                tool = RecordInspectionTool(repository),
                validArguments = inspectionArguments(
                    hiveId = "4",
                    queenSeen = true,
                ),
                invalidArguments = listOf(
                    buildJsonObject {},
                    buildJsonObject { put(HIVE_ID_ARGUMENT_NAME, "4") },
                    buildJsonObject { put(QUEEN_SEEN_ARGUMENT_NAME, true) },
                    inspectionArguments(hiveId = " ", queenSeen = true),
                    buildJsonObject {
                        put(HIVE_ID_ARGUMENT_NAME, "4")
                        put(QUEEN_SEEN_ARGUMENT_NAME, "true")
                    },
                ),
                effectCount = repository.records::size,
            ),
        )

        assertEquals("4", repository.records.single().hiveId)
    }
}

private fun plannerExample(
    name: String,
    input: String,
    hiveId: String,
    queenSeen: Boolean,
): PlannerExample = PlannerExample(
    name = name,
    input = input,
    expectedArguments = inspectionArguments(hiveId, queenSeen),
)

private fun inspectionArguments(
    hiveId: String,
    queenSeen: Boolean,
) = buildJsonObject {
    put(HIVE_ID_ARGUMENT_NAME, hiveId)
    put(QUEEN_SEEN_ARGUMENT_NAME, queenSeen)
}

private class ConformanceInspectionRepository : InspectionRepository {
    val records = mutableListOf<InspectionRecord>()

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val record = InspectionRecord(
            id = records.size.toLong() + 1,
            hiveId = draft.hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = records.size.toLong(),
        )
        records += record
        return record
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> =
        records.takeLast(limit).reversed()
}
