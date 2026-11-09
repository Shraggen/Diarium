package com.shraggen.diarium.beekeeping

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Replays raw outputs saved from real model evaluations (`modelEval
 * -Pdiarium.recordOutputs=true`). A harness change that alters what the
 * journal does with any recorded output fails here without a model.
 */
class RecordedModelOutputSpecTest {

    @Test
    fun recordedModelOutputsStillReachTheirRecordedJournalOutcome() = runTest {
        val recordings = ModelRecording.loadCommitted()
        assertTrue(recordings.isNotEmpty(), "No model recordings found under /$RECORDINGS_RESOURCE")

        val failures = recordings.flatMap { recording ->
            recording.voiceNotes.mapNotNull { recorded ->
                val outcome = VoiceNoteJournal.replaying(recorded.rawModelOutput)
                    .propose(recorded.transcript)
                    .outcome
                "${recording.source} / ${recorded.name}: recorded ${recorded.outcome}, now $outcome"
                    .takeIf { outcome != recorded.outcome }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun journalOutcomesSurviveTheRecordingFormat() {
        listOf(
            JournalOutcome.Malformed,
            JournalOutcome.Blocked(FieldCheck.MISMATCH, FieldCheck.CANNOT_VERIFY),
            JournalOutcome.Recordable("42", queenSeen = false),
        ).forEach { outcome ->
            kotlin.test.assertEquals(outcome, outcome.toRecordingJson().toJournalOutcome())
        }
    }
}
