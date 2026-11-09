package com.shraggen.diarium.eval

import com.shraggen.diarium.speech.WHISPER_SAMPLE_RATE

/** Word-level edit distance between a reference transcript and a speech-recognition hypothesis. */
data class WordErrors(
    val errors: Int,
    val referenceWords: Int,
) {
    operator fun plus(other: WordErrors): WordErrors =
        WordErrors(errors + other.errors, referenceWords + other.referenceWords)

    override fun toString(): String =
        if (referenceWords == 0) {
            "n/a"
        } else {
            "${"%.1f".format(PERCENT * errors / referenceWords)}% ($errors/$referenceWords words)"
        }

    companion object {
        private const val PERCENT = 100.0
        private val WORD = Regex("[\\p{L}\\p{N}]+")

        val NONE = WordErrors(0, 0)

        /** Substitutions, deletions, and insertions, ignoring case and punctuation. */
        fun between(reference: String, hypothesis: String): WordErrors {
            val expected = words(reference)
            val actual = words(hypothesis)
            var previous = IntArray(actual.size + 1) { it }
            expected.forEachIndexed { row, expectedWord ->
                val current = IntArray(actual.size + 1)
                current[0] = row + 1
                actual.forEachIndexed { column, actualWord ->
                    val substitution = previous[column] + if (expectedWord == actualWord) 0 else 1
                    current[column + 1] = minOf(substitution, previous[column + 1] + 1, current[column] + 1)
                }
                previous = current
            }
            return WordErrors(errors = previous[actual.size], referenceWords = expected.size)
        }

        private fun words(text: String): List<String> =
            WORD.findAll(text.lowercase()).map { it.value }.toList()
    }
}

/** Reads the RIFF header of a voice-note clip in the format Whisper requires: 16 kHz mono PCM16. */
@Suppress("MagicNumber")
object Pcm16WavClip {

    private const val PCM_FORMAT = 1
    private const val MILLIS_PER_SECOND = 1_000L

    fun durationMillis(wav: ByteArray): Long {
        require(wav.size >= 12 && wav.ascii(0) == "RIFF" && wav.ascii(8) == "WAVE") { "Not a RIFF WAVE file" }
        var format: Format? = null
        var offset = 12
        while (offset + 8 <= wav.size) {
            val id = wav.ascii(offset)
            val size = wav.int32(offset + 4)
            val body = offset + 8
            when (id) {
                "fmt " -> format = Format(
                    audioFormat = wav.int16(body),
                    channels = wav.int16(body + 2),
                    sampleRate = wav.int32(body + 4),
                    bitsPerSample = wav.int16(body + 14),
                )
                "data" -> {
                    val fmt = requireNotNull(format) { "WAV data chunk precedes its fmt chunk" }
                    require(fmt.isWhisperCompatible()) {
                        "Expected $WHISPER_SAMPLE_RATE Hz mono PCM16 WAV, got ${fmt.sampleRate} Hz, " +
                            "${fmt.channels} channel(s), ${fmt.bitsPerSample}-bit, format ${fmt.audioFormat}"
                    }
                    return size.toLong() * MILLIS_PER_SECOND / (WHISPER_SAMPLE_RATE * 2)
                }
            }
            offset = body + size + (size and 1)
        }
        throw IllegalArgumentException("WAV file has no data chunk")
    }

    private data class Format(
        val audioFormat: Int,
        val channels: Int,
        val sampleRate: Int,
        val bitsPerSample: Int,
    ) {
        fun isWhisperCompatible(): Boolean =
            audioFormat == PCM_FORMAT && channels == 1 && sampleRate == WHISPER_SAMPLE_RATE && bitsPerSample == 16
    }

    private fun ByteArray.ascii(offset: Int): String =
        if (offset + 4 > size) "" else String(this, offset, 4, Charsets.US_ASCII)

    private fun ByteArray.int16(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)

    private fun ByteArray.int32(offset: Int): Int =
        int16(offset) or (int16(offset + 2) shl 16)
}
