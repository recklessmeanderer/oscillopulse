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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageSonificationTest {

    @Test
    fun `generate all preset textures and verify dimensions`() {
        TexturePreset.values().forEach { preset ->
            val bitmap = ImageSonificationHelper.generatePresetTexture(preset, width = 320, height = 180)
            assertNotNull("Bitmap should not be null for preset ${preset.name}", bitmap)
            assertEquals(320, bitmap.width)
            assertEquals(180, bitmap.height)

            val analysis = ImageSonificationHelper.extractAudioProfile(bitmap, profileSize = 256)
            assertEquals(256, analysis.audioProfile.size)
            assertTrue("Average luminance should be non-negative", analysis.avgLuminance >= 0f)

            // Verify audio profile values stay normalized between -1.0 and +1.0
            for (v in analysis.audioProfile) {
                assertTrue("Profile sample should be >= -1.0f", v >= -1.0f)
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
