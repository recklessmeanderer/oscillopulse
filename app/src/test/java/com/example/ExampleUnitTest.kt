package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPresetEntityHarmonics() {
    val preset = com.example.data.PresetEntity(
      name = "Chime Test",
      carrierFreq = 220f,
      isHarmonicsEnabled = true,
      h1Multiplier = 2,
      h1Volume = 0.5f,
      h2Multiplier = 3,
      h2Volume = 0.25f,
      h3Multiplier = 4,
      h3Volume = 0.125f
    )
    assertTrue(preset.isHarmonicsEnabled)
    assertEquals(2, preset.h1Multiplier)
    assertEquals(0.5f, preset.h1Volume, 0.001f)
    assertEquals(3, preset.h2Multiplier)
    assertEquals(0.25f, preset.h2Volume, 0.001f)
    assertEquals(4, preset.h3Multiplier)
    assertEquals(0.125f, preset.h3Volume, 0.001f)
  }

  @Test
  fun testPresetEntityWhiteNoise() {
    val preset = com.example.data.PresetEntity(
      name = "White Noise Masking",
      carrierFreq = 100f,
      isWhiteNoiseEnabled = true,
      whiteNoiseLevel = 0.20f
    )
    assertTrue(preset.isWhiteNoiseEnabled)
    assertEquals(0.20f, preset.whiteNoiseLevel, 0.001f)
  }

  @Test
  fun testAppCardModuleOrder() {
    val defaultList = com.example.model.AppCardModule.DEFAULT_ORDER
    assertEquals(6, defaultList.size)
    assertEquals(com.example.model.AppCardModule.OSCILLOSCOPE, defaultList[0])
    assertEquals(com.example.model.AppCardModule.TRANSPORT, defaultList[1])
    assertEquals(com.example.model.AppCardModule.CARRIER, defaultList[2])

    // Test reorder swap
    val mutableList = defaultList.toMutableList()
    val carrier = mutableList[2]
    mutableList.removeAt(2)
    mutableList.add(0, carrier)
    assertEquals(com.example.model.AppCardModule.CARRIER, mutableList[0])
    assertEquals(com.example.model.AppCardModule.OSCILLOSCOPE, mutableList[1])
  }
}
