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

/** The `<timestamp>.md` and `<timestamp>.json` report of one model evaluation. */
class ModelEvalReport(
    private val settings: ModelEvalSettings,
    private val runs: List<VoiceNoteRun>,
    private val createdAt: Instant = Instant.now(),
) {
    private val score = CorpusScore.of(runs)
    private val modelSha256 = settings.llmModel.sha256()

    fun writeTo(directory: File): List<File> {
        directory.mkdirs()
        val stem = FILE_TIMESTAMP.format(createdAt)
        val markdown = directory.resolve("$stem.md").apply { writeText(markdown()) }
        val json = directory.resolve("$stem.json").apply { writeText(prettyJson.encodeToString(JsonObject.serializer(), json())) }
        return listOf(markdown, json)
    }

    fun summary(): String =
        "${settings.llmModel.name}: journal accuracy ${score.journalAccuracy}, " +
            "false accepts ${score.falseAccepts}, exact calls ${score.exactCall}, " +
            "latency p50 ${score.latencyP50Millis} ms / p95 ${score.latencyP95Millis} ms"

    private fun markdown(): String = buildString {
        appendLine("# Model evaluation ${createdAt}")
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
        appendLine("| Measure | Result |")
        appendLine("| --- | --- |")
        totals().forEach { (measure, result) -> appendLine("| $measure | $result |") }
        appendLine()
        appendLine("## Voice notes")
        appendLine()
        appendLine("| # | Voice note | Reference transcript | Raw model output | Parsed call | Outcome | Verdict | Latency |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        runs.forEach { run ->
            val cells = listOf(
                run.repeat.toString(),
                run.note.name,
                run.note.transcript,
                "`${run.rawOutput}`",
                run.parsedCall(),
                run.proposal.outcome.toString(),
                run.verdict.name,
                "${run.latencyMillis} ms",
            )
            appendLine(cells.joinToString(" | ", "| ", " |") { it.asTableCell() })
        }
    }

    private fun totals(): List<Pair<String, String>> = listOf(
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
        putJsonObject("totals") {
            totals().forEach { (measure, result) -> put(measure, result) }
        }
        putJsonArray("voiceNotes") {
            runs.forEach { run ->
                addJsonObject {
                    put("repeat", run.repeat)
                    put("name", run.note.name)
                    put("referenceTranscript", run.note.transcript)
                    put("expectedInspection", run.note.expectedInspection.toString())
                    put("rawModelOutput", run.rawOutput)
                    put("parsedCall", run.parsedCall())
                    put("outcome", run.proposal.outcome.toString())
                    put("verdict", run.verdict.name)
                    put("latencyMillis", run.latencyMillis)
                }
            }
        }
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
