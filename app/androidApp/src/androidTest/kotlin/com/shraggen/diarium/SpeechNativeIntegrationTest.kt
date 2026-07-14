package com.shraggen.diarium

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import com.shraggen.diarium.speech.LlamatikSpeechTranscriber
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SpeechNativeIntegrationTest {

    @Test
    fun bundledSileroModelClassifiesSilenceAsNonSpeech() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        VadSilero(
            context = context,
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_512,
            mode = Mode.NORMAL,
        ).use { vad ->
            assertFalse(vad.isSpeech(ShortArray(FrameSize.FRAME_SIZE_512.value)))
        }
    }

    @Test
    @LargeTest
    fun provisionedWhisperModelInitializesThroughLlamatik() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = File(context.filesDir, "whisper-models")
            .listFiles { file -> file.extension.equals("bin", ignoreCase = true) }
            ?.maxByOrNull(File::lastModified)
        assumeTrue("No private Whisper model was provisioned.", model != null)

        val transcriber = LlamatikSpeechTranscriber()
        try {
            assertTrue(transcriber.initialize(checkNotNull(model).absolutePath))
        } finally {
            transcriber.release()
        }
    }
}
