package com.example

import com.example.audio.ImageSonificationHelper
import com.example.audio.TexturePreset
import com.example.model.NoiseColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import android.content.Context
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageSonificationTest {

    @Test
    fun `generate and load all preset textures including Ra in the Abyss`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TexturePreset.values().forEach { preset ->
            val bitmap = ImageSonificationHelper.getPresetBitmap(context, preset)
            assertNotNull("Bitmap should not be null for preset ${preset.name}", bitmap)
            assertTrue("Width should be > 0", bitmap.width > 0)
            assertTrue("Height should be > 0", bitmap.height > 0)

            val analysis = ImageSonificationHelper.extractAudioProfile(bitmap, profileSize = 256)
            assertEquals(256, analysis.audioProfile.size)
            assertTrue("Average luminance should be non-negative", analysis.avgLuminance >= 0f)

            // Verify audio profile values stay normalized between 0.0 and 1.0
            for (v in analysis.audioProfile) {
                assertTrue("Profile sample should be >= 0.0f", v >= 0.0f)
                assertTrue("Profile sample should be <= 1.0f", v <= 1.0f)
            }
        }
    }

    @Test
    fun `cosmic nebula and ocean surf noise color matching`() {
        val cosmicBitmap = ImageSonificationHelper.generatePresetTexture(TexturePreset.COSMIC_NEBULA, 200, 100)
        val cosmicAnalysis = ImageSonificationHelper.extractAudioProfile(cosmicBitmap, 128)
        assertNotNull(cosmicAnalysis.recommendedNoiseColor)

        val oceanBitmap = ImageSonificationHelper.generatePresetTexture(TexturePreset.OCEAN_SURF, 200, 100)
        val oceanAnalysis = ImageSonificationHelper.extractAudioProfile(oceanBitmap, 128)
        assertNotNull(oceanAnalysis.recommendedNoiseColor)
    }
}
