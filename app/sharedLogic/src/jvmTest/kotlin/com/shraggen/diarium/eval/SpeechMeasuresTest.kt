package com.shraggen.diarium.eval

import com.shraggen.diarium.speech.pcm16MonoWav
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpeechMeasuresTest {

    @Test
    fun wordErrorsCountSubstitutionsDeletionsAndInsertionsIgnoringCaseAndPunctuation() {
        assertEquals(
            WordErrors(errors = 0, referenceWords = 8),
            WordErrors.between("I inspected hive 4 and saw the queen.", "i inspected Hive 4, and saw the queen"),
        )
        assertEquals(
            WordErrors(errors = 3, referenceWords = 8),
            // "4" -> "four" (substitution), "the" deleted, "today" inserted
            WordErrors.between("I inspected hive 4 and saw the queen.", "I inspected hive four and saw queen today"),
        )
        assertEquals(
            WordErrors(errors = 0, referenceWords = 6),
            WordErrors.between("Прегледао сам кошницу 4 и видео", "прегледао сам кошницу 4 и видео"),
        )
    }

    @Test
    fun corpusWordErrorRateWeighsNotesByReferenceLength() {
        val total = listOf(WordErrors(1, 10), WordErrors(3, 30)).fold(WordErrors.NONE, WordErrors::plus)
        assertEquals(WordErrors(4, 40), total)
        assertEquals("10.0% (4/40 words)", total.toString())
    }

    @Test
    fun clipDurationComesFromTheWhisperWavFormat() {
        val twoSeconds = pcm16MonoWav(ShortArray(32_000))
        assertEquals(2_000, Pcm16WavClip.durationMillis(twoSeconds))
    }

    @Test
    fun clipsWhisperCannotReadAreRejectedWithTheReason() {
        val wrongRate = pcm16MonoWav(ShortArray(100), sampleRate = 22_050)
        val failure = assertFailsWith<IllegalArgumentException> { Pcm16WavClip.durationMillis(wrongRate) }
        assertEquals("Expected 16000 Hz mono PCM16 WAV, got 22050 Hz, 1 channel(s), 16-bit, format 1", failure.message)
        assertFailsWith<IllegalArgumentException> { Pcm16WavClip.durationMillis("not audio".encodeToByteArray()) }
    }
}
