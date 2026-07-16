package com.shraggen.diarium

import com.shraggen.diarium.beekeeping.HiveIdentifierConsistency
import com.shraggen.diarium.beekeeping.QueenObservationConsistency
import com.shraggen.diarium.speech.SpeechLanguage
import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedUICommonTest {

    @Test
    fun pendingCallKeepsExactDiagnosticsAndBlocksMismatch() {
        val call = ToolCall(
            id = "call-7",
            toolName = "record_inspection",
            arguments = buildJsonObject {
                put("hive_id", "4")
                put("queen_seen", true)
            },
        )

        val pending = pendingToolCall(
            transcript = "Pregledao sam košnicu pet i video maticu.",
            call = call,
        )

        assertEquals(call, pending.call)
        assertEquals(call.arguments.toString(), pending.arguments)
        assertEquals("4", pending.hiveId)
        assertEquals(true, pending.queenSeen)
        assertFalse(pending.confirmationAllowed)
        assertTrue(pending.identifierConsistency is HiveIdentifierConsistency.Mismatch)
        assertTrue(pending.queenConsistency is QueenObservationConsistency.Verified)
    }

    @Test
    fun queenMismatchBlocksOtherwiseVerifiedHiveCall() {
        val pending = pendingToolCall(
            transcript = "Pregledao sam košnicu 4 i video maticu.",
            call = ToolCall(
                id = "call-queen-mismatch",
                toolName = "record_inspection",
                arguments = buildJsonObject {
                    put("hive_id", "4")
                    put("queen_seen", false)
                },
            ),
        )

        assertTrue(pending.identifierConsistency is HiveIdentifierConsistency.Verified)
        assertTrue(pending.queenConsistency is QueenObservationConsistency.Mismatch)
        assertFalse(pending.confirmationAllowed)
    }

    @Test
    fun onlyCompleteVerifiedProposalsCanBeConfirmed() {
        val transcript = "I inspected hive 4 and saw the queen."
        val completeCall = ToolCall(
            id = "complete",
            toolName = "record_inspection",
            arguments = buildJsonObject {
                put("hive_id", "4")
                put("queen_seen", true)
            },
        )
        val missingHiveCall = completeCall.copy(
            id = "missing-hive",
            arguments = buildJsonObject {
                put("queen_seen", true)
            },
        )
        val missingQueenCall = completeCall.copy(
            id = "missing-queen",
            arguments = buildJsonObject {
                put("hive_id", "4")
            },
        )

        assertTrue(pendingToolCall(transcript, completeCall).confirmationAllowed)
        assertFalse(pendingToolCall(transcript, missingHiveCall).confirmationAllowed)
        assertFalse(pendingToolCall(transcript, missingQueenCall).confirmationAllowed)
    }

    @Test
    fun allLanguagesProvideLocalizedGuardrailCopy() {
        SpeechLanguage.entries.forEach { language ->
            val copy = copyFor(language)

            assertTrue(copy.identifierMismatch.isNotBlank())
            assertTrue(copy.identifierCannotVerify.isNotBlank())
            assertTrue(copy.spokenHive.isNotBlank())
            assertTrue(copy.proposedHive.isNotBlank())
            assertTrue(copy.queenMismatch.isNotBlank())
            assertTrue(copy.queenCannotVerify.isNotBlank())
        }
    }
}
