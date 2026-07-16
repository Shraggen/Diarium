package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QueenObservationConsistencyTest {

    private val checker = QueenObservationConsistencyChecker()

    @Test
    fun verifiesExplicitPositiveObservationsAcrossLanguages() {
        val transcripts = listOf(
            "I inspected hive 4 and saw the queen.",
            "Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.",
            "Pregledao sam košnicu 4 i video maticu.",
            "Прегледао сам кошницу 4 и видео матицу.",
            "Прегледово, sam košnicu, пет и видео, sam, maticu.",
        )

        transcripts.forEach { transcript ->
            val result = checker.check(transcript, queenCall(queenSeen = true))

            val verified = assertIs<QueenObservationConsistency.Verified>(result)
            assertEquals(true, verified.transcriptQueenSeen)
            assertEquals(true, verified.proposedQueenSeen)
        }
    }

    @Test
    fun verifiesExplicitNegativeObservationsAcrossLanguages() {
        val transcripts = listOf(
            "I did not see the queen in hive 4.",
            "Ich habe Bienenstock 4 kontrolliert und die Königin nicht gesehen.",
            "Nisam video maticu u košnici 4.",
            "Нисам видео матицу у кошници 4.",
            "maticu nisam video u košnici 4.",
        )

        transcripts.forEach { transcript ->
            val result = checker.check(transcript, queenCall(queenSeen = false))

            val verified = assertIs<QueenObservationConsistency.Verified>(result)
            assertEquals(false, verified.transcriptQueenSeen)
            assertEquals(false, verified.proposedQueenSeen)
        }
    }

    @Test
    fun reportsTranscriptAndProposalWhenObservationDisagrees() {
        val result = checker.check(
            transcript = "Прегледово, sam košnicu, četri, i video, maticu.",
            call = queenCall(queenSeen = false),
        )

        val mismatch = assertIs<QueenObservationConsistency.Mismatch>(result)
        assertEquals(true, mismatch.transcriptQueenSeen)
        assertEquals(false, mismatch.proposedQueenSeen)
    }

    @Test
    fun cannotVerifyMissingOrContradictoryObservations() {
        val cases = listOf(
            "I inspected hive 4." to emptySet(),
            "I did not see the queen, but later I saw the queen." to
                setOf(false, true),
            "Прегледово, sam košnica, пет и видеоматица." to emptySet(),
        )

        cases.forEach { (transcript, observations) ->
            val result = checker.check(transcript, queenCall(queenSeen = true))

            val cannotVerify = assertIs<QueenObservationConsistency.CannotVerify>(result)
            assertEquals(observations, cannotVerify.transcriptObservations)
        }
    }
}

private fun queenCall(queenSeen: Boolean) = ToolCall(
    id = "queen-test-call",
    toolName = RECORD_INSPECTION_TOOL_NAME,
    arguments = buildJsonObject {
        put(HIVE_ID_ARGUMENT_NAME, "4")
        put(QUEEN_SEEN_ARGUMENT_NAME, queenSeen)
    },
)
