package com.shraggen.diarium.beekeeping

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/** Test-classpath directory holding committed model recordings. */
const val RECORDINGS_RESOURCE = "model-recordings"

/** One raw model output and the journal outcome the harness reached when it was recorded. */
data class RecordedVoiceNote(
    val name: String,
    val transcript: String,
    val rawModelOutput: String,
    val outcome: JournalOutcome,
)

/**
 * Raw outputs of one model evaluation, saved as a golden master for the
 * harness. The file name identifies model, prompt mode, and sampling profile.
 */
data class ModelRecording(
    val source: String,
    val voiceNotes: List<RecordedVoiceNote>,
) {
    companion object {
        private val prettyJson = Json { prettyPrint = true }

        fun loadCommitted(): List<ModelRecording> {
            val directory = ModelRecording::class.java.getResource("/$RECORDINGS_RESOURCE")
                ?.let { File(it.toURI()) }
                ?: return emptyList()
            return directory.listFiles { file -> file.extension == "json" }
                .orEmpty()
                .sortedBy { it.name }
                .map { file -> parse(file.name, file.readText()) }
        }

        fun parse(source: String, text: String): ModelRecording {
            val root = Json.parseToJsonElement(text).jsonObject
            return ModelRecording(
                source = source,
                voiceNotes = root.getValue("voiceNotes").jsonArray.map { element ->
                    val note = element.jsonObject
                    RecordedVoiceNote(
                        name = note.string("name"),
                        transcript = note.string("transcript"),
                        rawModelOutput = note.string("rawModelOutput"),
                        outcome = note.getValue("outcome").jsonObject.toJournalOutcome(),
                    )
                },
            )
        }

        /** Writes the recording of [runs], replacing an earlier one for the same setup. */
        fun write(
            directory: File,
            fileStem: String,
            header: JsonObject,
            runs: List<VoiceNoteRun>,
        ): File {
            val recording = JsonObject(
                header + ("voiceNotes" to JsonArray(
                    runs.map { run ->
                        buildJsonObject {
                            put("name", run.note.name)
                            put("transcript", run.note.transcript)
                            put("rawModelOutput", run.rawOutput)
                            put("outcome", run.proposal.outcome.toRecordingJson())
                        }
                    },
                )),
            )
            directory.mkdirs()
            return directory.resolve("$fileStem.json").apply {
                writeText(prettyJson.encodeToString(JsonObject.serializer(), recording) + "\n")
            }
        }
    }
}

fun JournalOutcome.toRecordingJson(): JsonObject = buildJsonObject {
    when (val outcome = this@toRecordingJson) {
        JournalOutcome.Malformed -> put("kind", "malformed")
        is JournalOutcome.Blocked -> {
            put("kind", "blocked")
            put("hive", outcome.hive.name)
            put("queen", outcome.queen.name)
        }
        is JournalOutcome.Recordable -> {
            put("kind", "recordable")
            put("hiveId", outcome.hiveId)
            put("queenSeen", outcome.queenSeen)
        }
    }
}

fun JsonObject.toJournalOutcome(): JournalOutcome =
    when (val kind = string("kind")) {
        "malformed" -> JournalOutcome.Malformed
        "blocked" -> JournalOutcome.Blocked(
            hive = FieldCheck.valueOf(string("hive")),
            queen = FieldCheck.valueOf(string("queen")),
        )
        "recordable" -> JournalOutcome.Recordable(
            hiveId = string("hiveId"),
            queenSeen = getValue("queenSeen").jsonPrimitive.boolean,
        )
        else -> throw IllegalArgumentException("Unknown recorded journal outcome '$kind'")
    }

private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
