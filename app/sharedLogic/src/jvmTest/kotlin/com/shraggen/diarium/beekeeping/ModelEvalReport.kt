package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.eval.ModelEvalSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The `<timestamp>.md` and `<timestamp>.json` report of one model evaluation.
 *
 * [referenceRuns] plan from reference transcripts. [speechRuns] are Whisper
 * transcriptions of the available clips, and [speechPlanRuns] plan from them.
 */
class ModelEvalReport(
    private val settings: ModelEvalSettings,
    private val referenceRuns: List<VoiceNoteRun>,
    private val speechRuns: List<SpeechRun> = emptyList(),
    private val speechPlanRuns: List<VoiceNoteRun> = emptyList(),
    private val createdAt: Instant = Instant.now(),
) {
    private val score = CorpusScore.of(referenceRuns)
    private val modelSha256 = settings.llmModel.sha256()
    private val whisperSha256 = settings.whisperModel?.takeIf { speechRuns.isNotEmpty() }?.sha256()

    fun writeTo(directory: File): List<File> {
        directory.mkdirs()
        val stem = FILE_TIMESTAMP.format(createdAt)
        val markdown = directory.resolve("$stem.md").apply { writeText(markdown()) }
        val json = directory.resolve("$stem.json").apply {
            writeText(prettyJson.encodeToString(JsonObject.serializer(), json()))
        }
        return listOf(markdown, json)
    }

    /** Saves the raw outputs as a harness recording replayed by [RecordedModelOutputSpecTest]. */
    fun writeRecordingTo(directory: File): File = ModelRecording.write(
        directory = directory,
        fileStem = listOf(
            settings.llmModel.nameWithoutExtension,
            settings.promptMode.name.lowercase(),
            settings.sampling.name.lowercase(),
        ).joinToString("."),
        header = buildJsonObject {
            put("recordedAt", createdAt.toString())
            put("model", settings.llmModel.name)
            put("sha256", modelSha256)
            put("promptMode", settings.promptMode.name)
            put("sampling", settings.sampling.describe())
        },
        runs = referenceRuns + speechPlanRuns,
    )

    fun summary(): String = buildString {
        append(
            "${settings.llmModel.name}: journal accuracy ${score.journalAccuracy}, " +
                "false accepts ${score.falseAccepts}, exact calls ${score.exactCall}, " +
                "latency p50 ${score.latencyP50Millis} ms / p95 ${score.latencyP95Millis} ms",
        )
        if (speechRuns.isNotEmpty()) {
            val speech = SpeechScore.of(speechRuns)
            append(
                "; ${settings.whisperModel?.name}: WER ${speech.wordErrors}, " +
                    "real-time factor ${"%.2f".format(speech.realTimeFactor)}, " +
                    "end-to-end exact calls ${CorpusScore.of(speechPlanRuns).exactCall}",
            )
        }
    }

    private fun markdown(): String = buildString {
        appendLine("# Model evaluation $createdAt")
        appendLine()
        appendLine("- Model: `${settings.llmModel.name}`")
        appendLine("- SHA-256: `$modelSha256`")
        appendLine("- Prompt mode: `${settings.promptMode}`")
        appendLine("- Sampling: ${settings.sampling.describe()}")
        appendLine("- Repeats: ${settings.repeats}; voice notes: ${goldenCorpus.size}")
        appendLine("- Host: ${host()}")
        appendLine()
        appendLine("## Totals")
        appendLine()
        table(listOf("Measure", "Result"), totals(score).map { (measure, result) -> listOf(measure, result) })
        appendLine()
        appendLine("## Voice notes")
        appendLine()
        planTable(referenceRuns)
        if (speechRuns.isNotEmpty()) speechMarkdown()
    }

    private fun StringBuilder.speechMarkdown() {
        val speech = SpeechScore.of(speechRuns)
        appendLine()
        appendLine("## Speech recognition")
        appendLine()
        appendLine("- Whisper model: `${settings.whisperModel?.name}`")
        appendLine("- SHA-256: `$whisperSha256`")
        appendLine("- Clips: ${speechRuns.size} of ${goldenCorpus.size} voice notes")
        appendLine("- Word error rate: ${speech.wordErrors}")
        appendLine("- Real-time factor: ${"%.2f".format(speech.realTimeFactor)} (processing time ÷ audio length)")
        appendLine()
        appendLine("### Planning from reference and Whisper transcripts (notes with clips)")
        appendLine()
        val reference = totals(CorpusScore.of(referenceRunsWithClips()))
        val whisper = totals(CorpusScore.of(speechPlanRuns))
        table(
            listOf("Measure", "Reference transcripts", "Whisper transcripts"),
            reference.zip(whisper) { (measure, ref), (_, spoken) -> listOf(measure, ref, spoken) },
        )
        appendLine()
        appendLine("### Clips")
        appendLine()
        table(
            listOf("Voice note", "Reference transcript", "Whisper transcript", "WER", "Audio", "Processing", "RTF"),
            speechRuns.map { run ->
                listOf(
                    run.note.name,
                    run.note.transcript,
                    run.transcript,
                    run.wordErrors.toString(),
                    "${run.audioMillis} ms",
                    "${run.processingMillis} ms",
                    "%.2f".format(run.realTimeFactor),
                )
            },
        )
        appendLine()
        appendLine("### Planning from Whisper transcripts")
        appendLine()
        planTable(speechPlanRuns)
    }

    private fun StringBuilder.planTable(runs: List<VoiceNoteRun>) = table(
        listOf("#", "Voice note", "Transcript", "Raw model output", "Parsed call", "Outcome", "Verdict", "Latency"),
        runs.map { run ->
            listOf(
                run.repeat.toString(),
                run.note.name,
                run.transcript,
                "`${run.rawOutput}`",
                run.parsedCall(),
                run.proposal.outcome.toString(),
                run.verdict.name,
                "${run.latencyMillis} ms",
            )
        },
    )

    private fun StringBuilder.table(header: List<String>, rows: List<List<String>>) {
        appendLine(header.joinToString(" | ", "| ", " |"))
        appendLine(header.joinToString(" | ", "| ", " |") { "---" })
        rows.forEach { row -> appendLine(row.joinToString(" | ", "| ", " |") { it.asTableCell() }) }
    }

    private fun referenceRunsWithClips(): List<VoiceNoteRun> {
        val notesWithClips = speechRuns.map { it.note }.toSet()
        return referenceRuns.filter { it.note in notesWithClips }
    }

    private fun totals(score: CorpusScore): List<Pair<String, String>> = listOf(
        "Journal accuracy (expected outcome reached)" to score.journalAccuracy.toString(),
        "False accepts (safety-critical)" to score.falseAccepts.toString(),
        "Valid JSON" to score.validJson.toString(),
        "Exact call" to score.exactCall.toString(),
        "hive_id accuracy" to score.hiveIdAccuracy.toString(),
        "queen_seen accuracy" to score.queenSeenAccuracy.toString(),
        "Correct rejection" to score.correctRejection.toString(),
        "Journal accuracy per repeat" to score.journalAccuracyPerRepeat.joinToString(", "),
        "Latency p50" to "${score.latencyP50Millis} ms",
        "Latency p95" to "${score.latencyP95Millis} ms",
    )

    private fun json(): JsonObject = buildJsonObject {
        put("createdAt", createdAt.toString())
        putJsonObject("model") {
            put("file", settings.llmModel.name)
            put("sha256", modelSha256)
            put("bytes", settings.llmModel.length())
        }
        put("promptMode", settings.promptMode.name)
        putJsonObject("sampling") { putSampling() }
        put("repeats", settings.repeats)
        put("host", host())
        putJsonObject("totals") { putTotals(score) }
        putJsonArray("voiceNotes") { referenceRuns.forEach { add(it.toJson()) } }
        if (speechRuns.isNotEmpty()) {
            putJsonObject("speech") { putSpeech() }
        }
    }

    private fun JsonObjectBuilder.putSpeech() {
        val speech = SpeechScore.of(speechRuns)
        putJsonObject("whisperModel") {
            put("file", settings.whisperModel?.name)
            put("sha256", whisperSha256)
        }
        put("wordErrorRate", speech.wordErrors.toString())
        put("wordErrors", speech.wordErrors.errors)
        put("referenceWords", speech.wordErrors.referenceWords)
        put("realTimeFactor", speech.realTimeFactor)
        putJsonObject("referenceTranscriptTotals") { putTotals(CorpusScore.of(referenceRunsWithClips())) }
        putJsonObject("whisperTranscriptTotals") { putTotals(CorpusScore.of(speechPlanRuns)) }
        putJsonArray("clips") {
            speechRuns.forEach { run ->
                addJsonObject {
                    put("name", run.note.name)
                    put("referenceTranscript", run.note.transcript)
                    put("whisperTranscript", run.transcript)
                    put("wordErrors", run.wordErrors.errors)
                    put("referenceWords", run.wordErrors.referenceWords)
                    put("audioMillis", run.audioMillis)
                    put("processingMillis", run.processingMillis)
                    put("realTimeFactor", run.realTimeFactor)
                }
            }
        }
        putJsonArray("voiceNotes") { speechPlanRuns.forEach { add(it.toJson()) } }
    }

    private fun JsonObjectBuilder.putTotals(score: CorpusScore) {
        totals(score).forEach { (measure, result) -> put(measure, result) }
    }

    private fun JsonObjectBuilder.putSampling() {
        val sampling = settings.sampling
        put("profile", sampling.name.lowercase())
        put("temperature", sampling.temperature)
        put("topK", sampling.topK)
        put("topP", sampling.topP)
        put("repeatPenalty", sampling.repeatPenalty)
        put("maxTokens", sampling.maxTokens)
        put("contextLength", sampling.contextLength)
        put("numThreads", sampling.numThreads)
        put("batchSize", sampling.batchSize)
        put("gpuLayers", sampling.gpuLayers)
        putJsonArray("flags") {
            if (sampling.useMmap) add("mmap")
            if (sampling.flashAttention) add("flashAttention")
        }
    }

    private companion object {
        val FILE_TIMESTAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
        val prettyJson = Json { prettyPrint = true }
    }
}

private fun VoiceNoteRun.toJson(): JsonObject = buildJsonObject {
    put("repeat", repeat)
    put("name", note.name)
    put("referenceTranscript", note.transcript)
    put("plannedTranscript", transcript)
    put("expectedInspection", note.expectedInspection.toString())
    put("rawModelOutput", rawOutput)
    put("parsedCall", parsedCall())
    put("outcome", proposal.outcome.toString())
    put("verdict", verdict.name)
    put("latencyMillis", latencyMillis)
}

private fun VoiceNoteRun.parsedCall(): String =
    proposal.call?.let { "${it.toolName} ${it.arguments}" } ?: "none (malformed)"

private fun String.asTableCell(): String =
    replace("|", "\\|").replace("\r", "").replace("\n", " ⏎ ")

private fun host(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.arch")}, " +
        "${Runtime.getRuntime().availableProcessors()} CPUs, Java ${System.getProperty("java.version")}"

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
