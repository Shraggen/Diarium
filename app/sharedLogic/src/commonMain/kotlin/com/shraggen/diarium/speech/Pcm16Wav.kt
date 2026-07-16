package com.shraggen.diarium.speech

fun pcm16MonoWav(
    samples: ShortArray,
    sampleRate: Int = WHISPER_SAMPLE_RATE,
): ByteArray {
    require(sampleRate > 0) {
        "Sample rate must be positive."
    }

    val dataSize = samples.size * PCM16_BYTES_PER_SAMPLE
    val result = ByteArray(WAV_HEADER_SIZE + dataSize)
    result.writeAscii(offset = 0, value = "RIFF")
    result.writeInt32LittleEndian(offset = 4, value = result.size - 8)
    result.writeAscii(offset = 8, value = "WAVE")
    result.writeAscii(offset = 12, value = "fmt ")
    result.writeInt32LittleEndian(offset = 16, value = PCM_FORMAT_CHUNK_SIZE)
    result.writeInt16LittleEndian(offset = 20, value = PCM_AUDIO_FORMAT)
    result.writeInt16LittleEndian(offset = 22, value = MONO_CHANNELS)
    result.writeInt32LittleEndian(offset = 24, value = sampleRate)
    result.writeInt32LittleEndian(
        offset = 28,
        value = sampleRate * PCM16_BYTES_PER_SAMPLE,
    )
    result.writeInt16LittleEndian(offset = 32, value = PCM16_BYTES_PER_SAMPLE)
    result.writeInt16LittleEndian(offset = 34, value = PCM16_BITS_PER_SAMPLE)
    result.writeAscii(offset = 36, value = "data")
    result.writeInt32LittleEndian(offset = 40, value = dataSize)

    samples.forEachIndexed { index, sample ->
        result.writeInt16LittleEndian(
            offset = WAV_HEADER_SIZE + (index * PCM16_BYTES_PER_SAMPLE),
            value = sample.toInt(),
        )
    }
    return result
}

const val WHISPER_SAMPLE_RATE = 16_000

private const val WAV_HEADER_SIZE = 44
private const val PCM_FORMAT_CHUNK_SIZE = 16
private const val PCM_AUDIO_FORMAT = 1
private const val MONO_CHANNELS = 1
private const val PCM16_BYTES_PER_SAMPLE = 2
private const val PCM16_BITS_PER_SAMPLE = 16

private fun ByteArray.writeAscii(offset: Int, value: String) {
    value.forEachIndexed { index, character ->
        this[offset + index] = character.code.toByte()
    }
}

@Suppress("MagicNumber")
private fun ByteArray.writeInt16LittleEndian(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
}

@Suppress("MagicNumber")
private fun ByteArray.writeInt32LittleEndian(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
}
