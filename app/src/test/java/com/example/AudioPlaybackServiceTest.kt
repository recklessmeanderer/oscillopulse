package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.audio.AudioPlaybackService
import com.example.model.ModulationMode
import com.example.model.WaveformType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioPlaybackServiceTest {

    @Test
    fun `audioEngine singleton returns same instance across calls`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance1 = AudioEngine.getInstance(context)
        val instance2 = AudioEngine.getInstance(context)
        assertSame(instance1, instance2)
    }

    @Test
    fun `audioEngine summary formatting for standard carrier and entrainment`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine.getInstance(context)

        engine.isBinauralEnabled = false
        engine.carrierFreq = 432.0f
        engine.carrierWave = WaveformType.SINE
        engine.modFreq = 40.0f
        engine.modMode = ModulationMode.ISOCHRONIC_PULSE

        val summary = engine.getSummaryDescription()
        assertTrue(summary.contains("432 Hz Sine"))
        assertTrue(summary.contains("40 Hz Isochronic Pulse"))
    }

    @Test
    fun `audioEngine summary formatting for binaural beat mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine.getInstance(context)

        engine.isBinauralEnabled = true
        engine.leftCarrierFreq = 432.0f
        engine.rightCarrierFreq = 472.0f

        val summary = engine.getSummaryDescription()
        assertTrue(summary.contains("Binaural 432Hz / 472Hz"))
        assertTrue(summary.contains("40Hz beat"))
    }

    @Test
    fun `audioPlaybackService service start and intent handling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(AudioPlaybackService::class.java)
        val service = controller.create().get()
        assertNotNull(service)

        val startIntent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_START
        }
        service.onStartCommand(startIntent, 0, 1)

        val playIntent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_PLAY
        }
        service.onStartCommand(playIntent, 0, 2)

        val pauseIntent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_PAUSE
        }
        service.onStartCommand(pauseIntent, 0, 3)

        val stopIntent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_STOP
        }
        service.onStartCommand(stopIntent, 0, 4)

        controller.destroy()
    }
}
