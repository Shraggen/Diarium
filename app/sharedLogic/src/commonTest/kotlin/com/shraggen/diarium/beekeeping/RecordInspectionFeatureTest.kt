package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.DiariumKernel
import com.shraggen.diarium.tool.ToolResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class RecordInspectionFeatureTest {

    @Test
    fun supportedLanguagesPlanWithoutWriting() = runTest {
        val scenarios = listOf(
            FeatureScenario(
                command = "I inspected hive 4 and saw the queen.",
                hiveId = "4",
                queenSeen = true,
            ),
            FeatureScenario(
                command =
                    "Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.",
                hiveId = "5",
                queenSeen = true,
            ),
            FeatureScenario(
                command = "Pregledao sam košnicu 6 i nisam video maticu.",
                hiveId = "6",
                queenSeen = false,
            ),
            FeatureScenario(
                command = "Прегледао сам кошницу 7 и видео сам матицу.",
                hiveId = "7",
                queenSeen = true,
            ),
        )

        scenarios.forEach { scenario ->
            val repository = FeatureInspectionRepository()
            val kernel = inspectionKernel(repository)

            val call = kernel.plan(scenario.command)

            assertEquals(
                scenario.hiveId,
                call.arguments.getValue(HIVE_ID_ARGUMENT_NAME).jsonPrimitive.content,
                scenario.command,
            )
            assertEquals(
                scenario.queenSeen,
                call.arguments.getValue(QUEEN_SEEN_ARGUMENT_NAME).jsonPrimitive.boolean,
                scenario.command,
            )
            assertEquals(emptyList(), repository.records, scenario.command)
        }
    }

    @Test
    fun confirmationWritesExactlyOnce() = runTest {
        val repository = FeatureInspectionRepository()
        val kernel = inspectionKernel(repository)
        val call = kernel.plan("I inspected hive 4 and saw the queen.")

        assertIs<ToolResult.Success>(kernel.execute(call))

        assertEquals(1, repository.records.size)
        assertEquals("4", repository.records.single().hiveId)
    }

    @Test
    fun cancellationWritesNothing() = runTest {
        val repository = FeatureInspectionRepository()
        val kernel = inspectionKernel(repository)

        kernel.plan("I inspected hive 4 and saw the queen.")

        assertEquals(emptyList(), repository.records)
    }

    @Test
    fun ambiguousFactsCannotBePersisted() = runTest {
        val repository = FeatureInspectionRepository()
        val kernel = inspectionKernel(repository)
        val calls = listOf(
            kernel.plan(
                "I inspected hive 4, correction, hive 5, and saw the queen.",
            ),
            kernel.plan("I inspected hive 4 and maybe saw the queen."),
        )

        calls.forEach { call ->
            assertIs<ToolResult.Failure>(kernel.execute(call))
        }

        assertEquals(emptyList(), repository.records)
    }

    @Test
    fun recentInspectionsAreNewestFirst() = runTest {
        val repository = FeatureInspectionRepository()
        val kernel = inspectionKernel(repository)

        kernel.execute(kernel.plan("I inspected hive 4 and saw the queen."))
        kernel.execute(kernel.plan("I inspected hive 5 and did not see the queen."))

        assertEquals(listOf("5", "4"), repository.recent().map { it.hiveId })
    }
}

private fun inspectionKernel(
    repository: InspectionRepository,
): DiariumKernel = DiariumKernel(
    registeredTools = listOf(RecordInspectionTool(repository)),
    planner = RecordInspectionPlanner(),
)

private data class FeatureScenario(
    val command: String,
    val hiveId: String,
    val queenSeen: Boolean,
)

private class FeatureInspectionRepository : InspectionRepository {
    val records = mutableListOf<InspectionRecord>()

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val record = InspectionRecord(
            id = records.size.toLong() + 1,
            hiveId = draft.hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = records.size.toLong(),
        )
        records.add(0, record)
        return record
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> =
        records.take(limit)
}
