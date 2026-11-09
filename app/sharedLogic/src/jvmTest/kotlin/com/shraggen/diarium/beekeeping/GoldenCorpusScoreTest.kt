package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.eval.WordErrors
import com.shraggen.diarium.speech.SpeechLanguage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenCorpusScoreTest {

    private val hiveFourQueenSeen = note(
        transcript = "I inspected hive 4 and saw the queen.",
        expected = ExpectedInspection.Recorded("4", queenSeen = true),
    )
    private val missingHive = note(
        transcript = "I inspected the hive and saw the queen.",
        expected = ExpectedInspection.Abstention,
    )

    // The beekeeper meant hive 5, but the transcript says 4, so the harness
    // can verify a wrong proposal: the false accept that scoring must expose.
    private val misheardHive = note(
        transcript = "I inspected hive 4 and saw the queen.",
        expected = ExpectedInspection.Recorded("5", queenSeen = true),
    )

    @Test
    fun verdictSeparatesCorrectMissedAndFalselyAcceptedProposals() {
        assertEquals(Verdict.CORRECT_RECORD, run(hiveFourQueenSeen, record("4", true)).verdict)
        assertEquals(Verdict.MISSED, run(hiveFourQueenSeen, record("5", true)).verdict)
        assertEquals(Verdict.MISSED, run(hiveFourQueenSeen, "not JSON").verdict)
        assertEquals(Verdict.CORRECT_ABSTENTION, run(missingHive, record("8", true)).verdict)
        assertEquals(Verdict.FALSE_ACCEPT, run(misheardHive, record("4", true)).verdict)
    }

    @Test
    fun scoresCallShapeFieldsAndJournalOutcomes() {
        val score = CorpusScore.of(
            listOf(
                run(hiveFourQueenSeen, record("4", true), latencyMillis = 100),
                run(hiveFourQueenSeen, record("4", false), latencyMillis = 200),
                run(hiveFourQueenSeen, "```json\n${record("4", true)}\n```", latencyMillis = 300),
                run(missingHive, "not JSON", latencyMillis = 400),
                run(misheardHive, record("4", true), latencyMillis = 500),
            ),
        )

        assertEquals(Ratio(3, 5), score.validJson)
        assertEquals(Ratio(2, 4), score.exactCall)
        assertEquals(Ratio(3, 4), score.hiveIdAccuracy)
        assertEquals(Ratio(3, 4), score.queenSeenAccuracy)
        assertEquals(Ratio(1, 1), score.correctRejection)
        assertEquals(Ratio(1, 5), score.falseAccepts)
        assertEquals(Ratio(3, 5), score.journalAccuracy)
        assertEquals(300, score.latencyP50Millis)
        assertEquals(500, score.latencyP95Millis)
    }

    @Test
    fun repeatsReportTheSpreadOfJournalAccuracy() {
        val score = CorpusScore.of(
            listOf(
                run(hiveFourQueenSeen, record("4", true), repeat = 1),
                run(hiveFourQueenSeen, record("5", true), repeat = 2),
            ),
        )

        assertEquals(listOf(Ratio(1, 1), Ratio(0, 1)), score.journalAccuracyPerRepeat)
    }

    @Test
    fun speechScoreTotalsWordErrorsAndRealTimeFactorOverAllClips() {
        val score = SpeechScore.of(
            listOf(
                SpeechRun(hiveFourQueenSeen, "I inspected hive four and saw the queen.", 1_000, 2_000),
                SpeechRun(missingHive, "I inspected the hive and saw the queen.", 2_000, 2_000),
            ),
        )

        assertEquals(WordErrors(1, 16), score.wordErrors)
        assertEquals(0.75, score.realTimeFactor)
    }

    private fun note(transcript: String, expected: ExpectedInspection) = GoldenVoiceNote(
        name = transcript,
        language = SpeechLanguage.ENGLISH,
        transcript = transcript,
        expectedInspection = expected,
        scriptedModelOutput = "",
        expectedOutcome = JournalOutcome.Malformed,
    )

    private fun run(
        note: GoldenVoiceNote,
        output: String,
        latencyMillis: Long = 1,
        repeat: Int = 1,
    ): VoiceNoteRun = runBlocking {
        VoiceNoteRun(
            note = note,
            repeat = repeat,
            rawOutput = output,
            proposal = VoiceNoteJournal.replaying(output).propose(note.transcript),
            latencyMillis = latencyMillis,
        )
    }

    private fun record(hiveId: String, queenSeen: Boolean): String =
        """{"tool":"record_inspection","arguments":{"hive_id":"$hiveId","queen_seen":$queenSeen}}"""
}
