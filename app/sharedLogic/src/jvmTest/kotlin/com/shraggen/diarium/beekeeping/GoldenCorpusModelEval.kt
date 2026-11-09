package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.eval.ModelEvalSettings
import com.shraggen.diarium.eval.Pcm16WavClip
import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.provider.llamatik.LlamatikStructuredJsonGenerator
import com.shraggen.diarium.speech.LlamatikSpeechTranscriber
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import java.io.File
import kotlin.test.Test
import kotlin.time.measureTimedValue

/**
 * Scores the golden corpus against a real GGUF command model on the JVM and,
 * with a Whisper model, transcribes the corpus audio clips first. Runs only
 * through `./gradlew :app:sharedLogic:modelEval`, never in `jvmTest`.
 * Per-note results are measured, not asserted: only an unusable setup fails.
 */
class GoldenCorpusModelEval {

    @Test
    fun scoresTheGoldenCorpusAgainstACommandModel() {
        val settings = ModelEvalSettings.fromSystemProperties()
        // Llamatik keeps one global model per bridge, so Whisper is released
        // before the command model loads.
        val speechRuns = settings.whisperModel?.let { transcribeClips(it, settings.audioDir) }.orEmpty()
        val (referenceRuns, speechPlanRuns) = runBlocking { plan(settings, speechRuns) }
        val report = ModelEvalReport(settings, referenceRuns, speechRuns, speechPlanRuns)
        val written = report.writeTo(settings.reportDir) +
            listOfNotNull(settings.recordingsDir?.let(report::writeRecordingTo))
        println(report.summary())
        written.forEach { println("Model eval report: ${it.absolutePath}") }
    }

    private fun transcribeClips(whisperModel: File, audioDir: File): List<SpeechRun> {
        val clips = goldenCorpus.mapNotNull { note ->
            audioDir.resolve(note.audioClipName).takeIf(File::isFile)?.let { note to it }
        }
        require(clips.isNotEmpty()) {
            "No golden voice-note clips in ${audioDir.absolutePath}. Expected 16 kHz mono PCM16 WAV files " +
                "named after the notes, for example ${goldenCorpus.first().audioClipName}."
        }
        val transcriber = LlamatikSpeechTranscriber()
        check(transcriber.initialize(whisperModel.absolutePath)) {
            "Llamatik could not load Whisper model ${whisperModel.absolutePath}"
        }
        try {
            return clips.map { (note, clip) ->
                val audioMillis = runCatching { Pcm16WavClip.durationMillis(clip.readBytes()) }.getOrElse {
                    throw IllegalArgumentException(
                        "$clip: ${it.message}. A Git LFS pointer needs `git lfs pull` first.",
                        it,
                    )
                }
                val (transcript, duration) = measureTimedValue {
                    transcriber.transcribe(clip.absolutePath, note.language)
                }
                SpeechRun(note, transcript.text, duration.inWholeMilliseconds, audioMillis)
            }
        } finally {
            transcriber.release()
        }
    }

    private suspend fun plan(
        settings: ModelEvalSettings,
        speechRuns: List<SpeechRun>,
    ): Pair<List<VoiceNoteRun>, List<VoiceNoteRun>> {
        settings.sampling.applyToLlamaBridge()
        val model = LlamatikStructuredJsonGenerator(settings.promptMode)
        check(model.initialize(settings.llmModel.absolutePath)) {
            "Llamatik could not load ${settings.llmModel.absolutePath}"
        }
        try {
            val repeats = 1..settings.repeats
            val referenceRuns = repeats.flatMap { repeat ->
                goldenCorpus.map { note -> run(model, note, note.transcript, repeat) }
            }
            val speechPlanRuns = repeats.flatMap { repeat ->
                speechRuns.map { speech -> run(model, speech.note, speech.transcript, repeat) }
            }
            return referenceRuns to speechPlanRuns
        } finally {
            model.shutdown()
        }
    }

    private suspend fun run(
        model: StructuredJsonGenerator,
        note: GoldenVoiceNote,
        transcript: String,
        repeat: Int,
    ): VoiceNoteRun {
        val observed = ObservedGenerator(model)
        val proposal = VoiceNoteJournal.withModel(observed).propose(transcript)
        return VoiceNoteRun(
            note = note,
            repeat = repeat,
            rawOutput = checkNotNull(observed.output) { "${note.name}: the model was not called" },
            proposal = proposal,
            latencyMillis = observed.latencyMillis,
            transcript = transcript,
        )
    }
}

/** Captures the raw output and generation latency of one model call. */
private class ObservedGenerator(
    private val delegate: StructuredJsonGenerator,
) : StructuredJsonGenerator {
    var output: String? = null
        private set
    var latencyMillis: Long = 0
        private set

    override suspend fun generate(prompt: String, schema: JsonObject): String {
        val (generated, duration) = measureTimedValue { delegate.generate(prompt, schema) }
        output = generated
        latencyMillis = duration.inWholeMilliseconds
        return generated
    }
}
