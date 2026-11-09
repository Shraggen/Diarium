package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.eval.ModelEvalSettings
import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.provider.llamatik.LlamatikStructuredJsonGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.time.measureTimedValue

/**
 * Scores the golden corpus against a real GGUF command model on the JVM.
 * Runs only through `./gradlew :app:sharedLogic:modelEval`, never in `jvmTest`.
 * Per-note results are measured, not asserted: only an unusable setup fails.
 */
class GoldenCorpusModelEval {

    @Test
    fun scoresTheGoldenCorpusAgainstACommandModel() {
        val settings = ModelEvalSettings.fromSystemProperties()
        val runs = runBlocking { evaluate(settings) }
        val report = ModelEvalReport(settings, runs)
        val written = report.writeTo(settings.reportDir) +
            listOfNotNull(settings.recordingsDir?.let(report::writeRecordingTo))
        println(report.summary())
        written.forEach { println("Model eval report: ${it.absolutePath}") }
    }

    private suspend fun evaluate(settings: ModelEvalSettings): List<VoiceNoteRun> {
        settings.sampling.applyToLlamaBridge()
        val model = LlamatikStructuredJsonGenerator(settings.promptMode)
        check(model.initialize(settings.llmModel.absolutePath)) {
            "Llamatik could not load ${settings.llmModel.absolutePath}"
        }
        try {
            return (1..settings.repeats).flatMap { repeat ->
                goldenCorpus.map { note -> run(model, note, repeat) }
            }
        } finally {
            model.shutdown()
        }
    }

    private suspend fun run(
        model: StructuredJsonGenerator,
        note: GoldenVoiceNote,
        repeat: Int,
    ): VoiceNoteRun {
        val observed = ObservedGenerator(model)
        val proposal = VoiceNoteJournal.withModel(observed).propose(note.transcript)
        return VoiceNoteRun(
            note = note,
            repeat = repeat,
            rawOutput = checkNotNull(observed.output) { "${note.name}: the model was not called" },
            proposal = proposal,
            latencyMillis = observed.latencyMillis,
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
