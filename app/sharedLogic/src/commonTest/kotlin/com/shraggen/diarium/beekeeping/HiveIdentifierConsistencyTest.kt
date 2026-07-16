package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HiveIdentifierConsistencyTest {

    private val checker = HiveIdentifierConsistencyChecker()

    @Test
    fun verifiesContextualDigitsAndMultilingualNumberWords() {
        val cases = listOf(
            "I inspected hive 4 and saw the queen." to "4",
            "Hive number twenty one was inspected." to "21",
            "Bienenstock Nummer vier wurde kontrolliert." to "4",
            "Bienenstock neunundneunzig wurde kontrolliert." to "99",
            "Pregledao sam košnicu, pet, i video maticu." to "5",
            "Pregledao sam košnicu dvadeset pet." to "25",
            "Прегледао сам кошницу пет и видео матицу." to "5",
            "Прегледао сам кошницу четрдесет две." to "42",
        )

        cases.forEach { (transcript, identifier) ->
            val result = checker.check(transcript, recordCall(identifier))

            val verified = assertIs<HiveIdentifierConsistency.Verified>(result)
            assertEquals(identifier, verified.transcriptIdentifier)
            assertEquals(identifier, verified.proposedIdentifier)
        }
    }

    @Test
    fun reportsBothIdentifiersWhenTranscriptAndProposalDisagree() {
        val result = checker.check(
            transcript = "Pregledao sam košnicu, pet, i video maticu.",
            call = recordCall("4"),
        )

        val mismatch = assertIs<HiveIdentifierConsistency.Mismatch>(result)
        assertEquals("5", mismatch.transcriptIdentifier)
        assertEquals("4", mismatch.proposedIdentifier)
    }

    @Test
    fun cannotVerifyMissingOrContradictoryTranscriptIdentifiers() {
        val cases = listOf(
            "I inspected the hive and saw the queen." to emptySet(),
            "I inspected hive 4, correction, hive 5." to setOf("4", "5"),
            "I saw 5 queen cells while inspecting the hive." to emptySet(),
        )

        cases.forEach { (transcript, identifiers) ->
            val result = checker.check(transcript, recordCall("5"))

            val cannotVerify = assertIs<HiveIdentifierConsistency.CannotVerify>(result)
            assertEquals(identifiers, cannotVerify.transcriptIdentifiers)
        }
    }
}

private fun recordCall(hiveId: String) = ToolCall(
    id = "test-call",
    toolName = RECORD_INSPECTION_TOOL_NAME,
    arguments = buildJsonObject {
        put(HIVE_ID_ARGUMENT_NAME, hiveId)
        put("queen_seen", true)
    },
)
