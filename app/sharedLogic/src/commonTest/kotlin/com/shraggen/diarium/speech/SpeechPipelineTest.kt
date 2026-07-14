package com.shraggen.diarium.speech

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpeechPipelineTest {

    @Test
    fun parsesGermanSegmentsWithoutTranslatingThem() {
        val transcript = WhisperTranscriptParser.parseSegments(
            """{"language":"de","segments":[{"text":" Bienenstock vier.","t0":0,"t1":900},{"text":" Königin gesehen.","t0":900,"t1":1700}]}""",
        )

        assertEquals("de", transcript.detectedLanguage)
        assertEquals("Bienenstock vier. Königin gesehen.", transcript.text)
    }

    @Test
    fun preservesSerbianCyrillic() {
        val transcript = WhisperTranscriptParser.parseSegments(
            """{"language":"sr","segments":[{"text":" Прегледао сам кошницу 4"},{"text":" и видео матицу."}]}""",
        )

        assertEquals("Прегледао сам кошницу 4 и видео матицу.", transcript.text)
    }

    @Test
    fun rejectsEmptyWhisperOutput() {
        assertFailsWith<IllegalArgumentException> {
            WhisperTranscriptParser.parseSegments(
                """{"language":"sr","segments":[]}""",
            )
        }
    }

    @Test
    fun buildsWhisperCompatiblePcmWav() {
        val wav = pcm16MonoWav(shortArrayOf(0, 1, -1))

        assertEquals("RIFF", wav.decodeToString(startIndex = 0, endIndex = 4))
        assertEquals("WAVE", wav.decodeToString(startIndex = 8, endIndex = 12))
        assertEquals("data", wav.decodeToString(startIndex = 36, endIndex = 40))
        assertEquals(50, wav.size)
        assertEquals(6, wav.readInt32LittleEndian(40))
        assertEquals(0xFF, wav[48].toInt() and 0xFF)
        assertEquals(0xFF, wav[49].toInt() and 0xFF)
    }

    @Test
    fun mapsSupportedLocalesAndIncludesBothSerbianScripts() {
        assertEquals(SpeechLanguage.GERMAN, SpeechLanguage.fromLanguageTag("de-CH"))
        assertEquals(SpeechLanguage.SERBIAN, SpeechLanguage.fromLanguageTag("sr-Latn"))
        assertEquals(SpeechLanguage.ENGLISH, SpeechLanguage.fromLanguageTag("fr"))
        assertTrue(SpeechLanguage.SERBIAN.whisperPrompt.contains("košnice"))
        assertTrue(SpeechLanguage.SERBIAN.whisperPrompt.contains("кошнице"))
    }
}

private fun ByteArray.readInt32LittleEndian(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
