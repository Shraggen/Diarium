package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.eval.WordErrors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.ceil

/**
 * One golden voice note planned once by a command model, from [transcript]:
 * the reference transcript, or a speech-recognition transcript of its clip.
 */
data class VoiceNoteRun(
    val note: GoldenVoiceNote,
    val repeat: Int,
    val rawOutput: String,
    val proposal: JournalProposal,
    val latencyMillis: Long,
    val transcript: String = note.transcript,
) {
    val verdict: Verdict
        get() {
            val met = note.expectedInspection.isMetBy(proposal.outcome)
            return when {
                proposal.outcome is JournalOutcome.Recordable ->
                    if (met) Verdict.CORRECT_RECORD else Verdict.FALSE_ACCEPT
                met -> Verdict.CORRECT_ABSTENTION
                else -> Verdict.MISSED
            }
        }
}

/** How the journal outcome of one run compares with the note's ground truth. */
enum class Verdict {
    /** Confirmation offered with exactly the expected inspection. */
    CORRECT_RECORD,

    /** Confirmation withheld where the note calls for abstention. */
    CORRECT_ABSTENTION,

    /** Confirmation withheld although an inspection was expected. */
    MISSED,

    /** Confirmation offered for an inspection the beekeeper did not state. Safety-critical. */
    FALSE_ACCEPT,
}

data class Ratio(val hits: Int, val total: Int) {
    override fun toString(): String =
        if (total == 0) "n/a" else "$hits/$total (${"%.1f".format(PERCENT * hits / total)}%)"

    private companion object {
        const val PERCENT = 100.0
    }
}

/** Totals over evaluated runs. Field and exact-call scores count only notes expecting a record. */
data class CorpusScore(
    val validJson: Ratio,
    val exactCall: Ratio,
    val hiveIdAccuracy: Ratio,
    val queenSeenAccuracy: Ratio,
    val correctRejection: Ratio,
    val falseAccepts: Ratio,
    val journalAccuracy: Ratio,
    val journalAccuracyPerRepeat: List<Ratio>,
    val latencyP50Millis: Long,
    val latencyP95Millis: Long,
) {
    companion object {
        private const val P50 = 0.50
        private const val P95 = 0.95

        fun of(runs: List<VoiceNoteRun>): CorpusScore {
            val recordRuns = runs.filter { it.note.expectedInspection is ExpectedInspection.Recorded }
            val abstentionRuns = runs.filter { it.note.expectedInspection is ExpectedInspection.Abstention }
            val latencies = runs.map { it.latencyMillis }.sorted()
            return CorpusScore(
                validJson = runs.ratio { it.rawOutput.isJsonObject() },
                exactCall = recordRuns.ratio { it.proposedArguments() == it.expectedArguments() },
                hiveIdAccuracy = recordRuns.ratio {
                    it.proposedField(HIVE_ID_ARGUMENT_NAME) == it.expectedArguments()[HIVE_ID_ARGUMENT_NAME]
                },
                queenSeenAccuracy = recordRuns.ratio {
                    it.proposedField(QUEEN_SEEN_ARGUMENT_NAME)?.booleanOrNull ==
                        (it.note.expectedInspection as ExpectedInspection.Recorded).queenSeen
                },
                correctRejection = abstentionRuns.ratio { it.verdict == Verdict.CORRECT_ABSTENTION },
                falseAccepts = runs.ratio { it.verdict == Verdict.FALSE_ACCEPT },
                journalAccuracy = runs.ratio { it.isCorrect() },
                journalAccuracyPerRepeat = runs.groupBy { it.repeat }
                    .toSortedMap()
                    .values
                    .map { repeatRuns -> repeatRuns.ratio { it.isCorrect() } },
                latencyP50Millis = latencies.nearestRank(P50),
                latencyP95Millis = latencies.nearestRank(P95),
            )
        }
    }
}

/** Speech recognition of one golden voice note's audio clip. */
data class SpeechRun(
    val note: GoldenVoiceNote,
    val transcript: String,
    val processingMillis: Long,
    val audioMillis: Long,
) {
    val wordErrors: WordErrors
        get() = WordErrors.between(note.transcript, transcript)

    val realTimeFactor: Double
        get() = processingMillis.toDouble() / audioMillis.coerceAtLeast(1)
}

/** Corpus word error rate, and real-time factor as total processing time over total audio length. */
data class SpeechScore(
    val wordErrors: WordErrors,
    val realTimeFactor: Double,
) {
    companion object {
        fun of(runs: List<SpeechRun>): SpeechScore = SpeechScore(
            wordErrors = runs.map { it.wordErrors }.fold(WordErrors.NONE, WordErrors::plus),
            realTimeFactor = runs.sumOf { it.processingMillis }.toDouble() /
                runs.sumOf { it.audioMillis }.coerceAtLeast(1),
        )
    }
}

/** The tool call a perfect model would propose for a note expecting a record. */
fun VoiceNoteRun.expectedArguments(): JsonObject {
    val expected = note.expectedInspection as ExpectedInspection.Recorded
    return buildJsonObject {
        put(HIVE_ID_ARGUMENT_NAME, expected.hiveId)
        put(QUEEN_SEEN_ARGUMENT_NAME, expected.queenSeen)
    }
}

private fun VoiceNoteRun.isCorrect(): Boolean =
    verdict == Verdict.CORRECT_RECORD || verdict == Verdict.CORRECT_ABSTENTION

private fun VoiceNoteRun.proposedArguments(): JsonObject? =
    proposal.call?.takeIf { it.toolName == RECORD_INSPECTION_TOOL_NAME }?.arguments

private fun VoiceNoteRun.proposedField(name: String): JsonPrimitive? =
    proposedArguments()?.get(name) as? JsonPrimitive

private fun String.isJsonObject(): Boolean =
    runCatching { Json.parseToJsonElement(trim()) }.getOrNull() is JsonObject

private fun <T> List<T>.ratio(hit: (T) -> Boolean): Ratio = Ratio(count(hit), size)

private fun List<Long>.nearestRank(percentile: Double): Long =
    if (isEmpty()) 0 else this[(ceil(percentile * size).toInt() - 1).coerceIn(indices)]
