package com.shraggen.diarium.beekeeping

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Executable specification of the voice-to-journal harness: replays each
 * golden voice note's scripted model output and checks only what the journal
 * does with it. No model is involved.
 */
class GoldenCorpusSpecTest {

    @Test
    fun scriptedModelOutputsReachTheirExpectedJournalOutcome() = runTest {
        val failures = goldenCorpus.mapNotNull { note ->
            val journal = VoiceNoteJournal.replaying(note.scriptedModelOutput)
            val outcome = journal.propose(note.transcript).outcome
            "${note.name}: expected ${note.expectedOutcome}, got $outcome"
                .takeIf { outcome != note.expectedOutcome }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun planningAloneNeverRecordsAnInspection() = runTest {
        goldenCorpus.forEach { note ->
            val journal = VoiceNoteJournal.replaying(note.scriptedModelOutput)
            journal.propose(note.transcript)
            assertTrue(journal.recorded().isEmpty(), note.name)
        }
    }

    @Test
    fun confirmingARecordableOutcomeRecordsExactlyItsFields() = runTest {
        goldenCorpus.forEach { note ->
            val journal = VoiceNoteJournal.replaying(note.scriptedModelOutput)
            val proposal = journal.propose(note.transcript)
            val expected = when (val outcome = proposal.outcome) {
                is JournalOutcome.Recordable -> listOf(outcome.hiveId to outcome.queenSeen)
                else -> emptyList()
            }

            journal.confirm(proposal)

            assertEquals(
                expected,
                journal.recorded().map { it.hiveId to it.queenSeen },
                note.name,
            )
        }
    }

    @Test
    fun productionPlannerMatchesTheGroundTruthOfEveryVoiceNote() = runTest {
        val failures = goldenCorpus.mapNotNull { note ->
            val outcome = VoiceNoteJournal.withPlanner(RecordInspectionPlanner())
                .propose(note.transcript)
                .outcome
            "${note.name}: expected ${note.expectedInspection}, got $outcome"
                .takeUnless { note.expectedInspection.isMetBy(outcome) }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
