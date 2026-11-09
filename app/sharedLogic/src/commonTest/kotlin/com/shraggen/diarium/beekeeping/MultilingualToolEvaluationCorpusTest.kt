package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.DiariumKernel
import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.tool.ToolResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MultilingualToolEvaluationCorpusTest {

    @Test
    fun evaluatesPlanningSafetyAndConfirmationDecisions() = runTest {
        evaluationCorpus.forEach { case ->
            evaluate(case)
        }
    }

    private suspend fun evaluate(case: EvaluationCase) {
        val repository = EvaluationRepository()
        val kernel = DiariumKernel(
            registeredTools = listOf(RecordInspectionTool(repository)),
            generator = FixedResponseGenerator(case.modelResponse),
        )
        val planned = runCatching { kernel.plan(case.transcript) }

        if (case.expectedHiveConsistency == ExpectedConsistency.MALFORMED) {
            assertTrue(planned.isFailure, case.name)
            assertTrue(repository.records.isEmpty(), case.name)
            return
        }

        val call = planned.getOrThrow()
        val hiveConsistency = HiveIdentifierConsistencyChecker().check(
            transcript = case.transcript,
            call = call,
        )
        val queenConsistency = QueenObservationConsistencyChecker().check(
            transcript = case.transcript,
            call = call,
        )
        assertHiveConsistency(case, hiveConsistency)
        assertQueenConsistency(case, queenConsistency)

        if (case.decision == Decision.CONFIRM &&
            hiveConsistency is HiveIdentifierConsistency.Verified &&
            queenConsistency is QueenObservationConsistency.Verified
        ) {
            assertIs<ToolResult.Success>(kernel.execute(call), case.name)
        }

        assertEquals(case.expectedRecords, repository.records.size, case.name)
        case.expectedPersistedHive?.let { expectedHive ->
            assertEquals(expectedHive, repository.records.single().hiveId, case.name)
        }
    }
}

private fun assertHiveConsistency(
    case: EvaluationCase,
    consistency: HiveIdentifierConsistency,
) {
    val matches = when (case.expectedHiveConsistency) {
        ExpectedConsistency.VERIFIED ->
            consistency is HiveIdentifierConsistency.Verified
        ExpectedConsistency.MISMATCH ->
            consistency is HiveIdentifierConsistency.Mismatch
        ExpectedConsistency.CANNOT_VERIFY ->
            consistency is HiveIdentifierConsistency.CannotVerify
        ExpectedConsistency.MALFORMED -> false
    }
    assertTrue(
        matches,
        "${case.name}: expected hive ${case.expectedHiveConsistency}, got $consistency",
    )
}

private fun assertQueenConsistency(
    case: EvaluationCase,
    consistency: QueenObservationConsistency,
) {
    val matches = when (case.expectedQueenConsistency) {
        ExpectedConsistency.VERIFIED ->
            consistency is QueenObservationConsistency.Verified
        ExpectedConsistency.MISMATCH ->
            consistency is QueenObservationConsistency.Mismatch
        ExpectedConsistency.CANNOT_VERIFY ->
            consistency is QueenObservationConsistency.CannotVerify
        ExpectedConsistency.MALFORMED -> false
    }
    assertTrue(
        matches,
        "${case.name}: expected queen ${case.expectedQueenConsistency}, got $consistency",
    )
}

private data class EvaluationCase(
    val name: String,
    val transcript: String,
    val modelResponse: String,
    val expectedHiveConsistency: ExpectedConsistency,
    val expectedQueenConsistency: ExpectedConsistency,
    val decision: Decision = Decision.NONE,
    val expectedRecords: Int = 0,
    val expectedPersistedHive: String? = null,
)

private enum class ExpectedConsistency {
    VERIFIED,
    MISMATCH,
    CANNOT_VERIFY,
    MALFORMED,
}

private enum class Decision {
    NONE,
    CANCEL,
    CONFIRM,
}

private val evaluationCorpus = listOf(
    EvaluationCase(
        name = "English valid command persists after confirmation",
        transcript = "I inspected hive 4 and saw the queen.",
        modelResponse = recordResponse("4", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.VERIFIED,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
        decision = Decision.CONFIRM,
        expectedRecords = 1,
        expectedPersistedHive = "4",
    ),
    EvaluationCase(
        name = "German valid command remains unpersisted after cancel",
        transcript =
            "Bienenstock Nummer vier wurde kontrolliert und die Königin nicht gesehen.",
        modelResponse = recordResponse("4", queenSeen = false),
        expectedHiveConsistency = ExpectedConsistency.VERIFIED,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
        decision = Decision.CANCEL,
    ),
    EvaluationCase(
        name = "Noisy Serbian Latin command is verified",
        transcript = "Hm... košnica, pet; matica je viđena.",
        modelResponse = recordResponse("5", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.VERIFIED,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Serbian Cyrillic number words are verified",
        transcript = "Прегледао сам кошницу четрдесет две и матица није виђена.",
        modelResponse = recordResponse("42", queenSeen = false),
        expectedHiveConsistency = ExpectedConsistency.VERIFIED,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Missing transcript identifier blocks invented proposal",
        transcript = "I inspected the hive and saw the queen.",
        modelResponse = recordResponse("8", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.CANNOT_VERIFY,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Serbian transcript and proposal contradiction is blocked",
        transcript = "Pregledao sam košnicu, pet, i video maticu.",
        modelResponse = recordResponse("4", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.MISMATCH,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
        decision = Decision.CONFIRM,
    ),
    EvaluationCase(
        name = "Multiple explicit identifiers cannot be guessed",
        transcript = "I inspected hive 4, correction, hive 5, and saw the queen.",
        modelResponse = recordResponse("5", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.CANNOT_VERIFY,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Malformed model JSON cannot produce a proposal",
        transcript = "I inspected hive 4.",
        modelResponse = "not JSON",
        expectedHiveConsistency = ExpectedConsistency.MALFORMED,
        expectedQueenConsistency = ExpectedConsistency.MALFORMED,
    ),
    EvaluationCase(
        name = "Physical English run rejects field label inside identifier",
        transcript = "I inspected Hive 4 and saw the queen.",
        modelResponse = recordResponse("Hive 4", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.MISMATCH,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Physical Serbian four run rejects hallucinated fields",
        transcript = "Прегледово, sam košnicu, četri, i video, maticu.",
        modelResponse = recordResponse("trapeze_1", queenSeen = false),
        expectedHiveConsistency = ExpectedConsistency.MISMATCH,
        expectedQueenConsistency = ExpectedConsistency.MISMATCH,
    ),
    EvaluationCase(
        name = "Physical German run rejects translated field label",
        transcript = "Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.",
        modelResponse = recordResponse("Bienenstock 4", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.MISMATCH,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Physical German five run rejects omitted model identifier",
        transcript = "Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.",
        modelResponse =
            """{"tool":"record_inspection","arguments":{"queen_seen":true}}""",
        expectedHiveConsistency = ExpectedConsistency.CANNOT_VERIFY,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
    EvaluationCase(
        name = "Physical Serbian five run rejects inflected identifier",
        transcript = "Прегледово, sam košnicu, пет и видео, sam, maticu.",
        modelResponse = recordResponse("peta", queenSeen = true),
        expectedHiveConsistency = ExpectedConsistency.MISMATCH,
        expectedQueenConsistency = ExpectedConsistency.VERIFIED,
    ),
)

private fun recordResponse(
    hiveId: String,
    queenSeen: Boolean,
): String =
    """{"tool":"record_inspection","arguments":{"hive_id":"$hiveId","queen_seen":$queenSeen}}"""

private class FixedResponseGenerator(
    private val response: String,
) : StructuredJsonGenerator {
    override suspend fun generate(prompt: String, schema: JsonObject): String = response
}

private class EvaluationRepository : InspectionRepository {
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
