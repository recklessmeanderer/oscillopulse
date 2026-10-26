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
    assertEquals(com.example.model.AppCardModule.LAYERING, defaultList[3])

    // Test reorder swap
    val mutableList = defaultList.toMutableList()
    val carrier = mutableList[2]
    mutableList.removeAt(2)
    mutableList.add(0, carrier)
    assertEquals(com.example.model.AppCardModule.CARRIER, mutableList[0])
    assertEquals(com.example.model.AppCardModule.OSCILLOSCOPE, mutableList[1])
  }

  @Test
  fun testDeterministicPresetIds() {
    val p1 = com.example.data.PresetEntity(id = 1L, name = "40 Hz Gamma Focus", isFactory = true)
    val p2 = com.example.data.PresetEntity(id = 2L, name = "Alpha 10 Hz Relaxation", isFactory = true)
    assertEquals(1L, p1.id)
    assertEquals(2L, p2.id)
    assertTrue(p1.isFactory)
    assertNotEquals(p1.id, p2.id)
  }

  @Test
  fun testFrequencyLinkingMath() {
    // When carrier changes from 432 to 528 with 40Hz beat diff
    val leftInitial = 432f
    val rightInitial = 472f
    val beatOffset = rightInitial - leftInitial
    assertEquals(40f, beatOffset, 0.001f)

    val newCarrier = 528f
    val newLeft = newCarrier
    val newRight = newCarrier + beatOffset
    assertEquals(568f, newRight, 0.001f)
    assertEquals(40f, newRight - newLeft, 0.001f)
  }

  @Test
  fun testNoiseColors() {
    val pinkPreset = com.example.data.PresetEntity(
      name = "Pink Rain",
      isWhiteNoiseEnabled = true,
      whiteNoiseLevel = 0.25f,
      noiseColor = "PINK"
    )
    assertEquals(com.example.model.NoiseColor.PINK, pinkPreset.getNoiseColorType())

    val brownPreset = com.example.data.PresetEntity(
      name = "Brown Waterfall",
      isWhiteNoiseEnabled = true,
      whiteNoiseLevel = 0.30f,
      noiseColor = "BROWN"
    )
    assertEquals(com.example.model.NoiseColor.BROWN, brownPreset.getNoiseColorType())
  }

  @Test
  fun testChordPresetCalculations() {
    val base = 432.0f
    val fifthRatio = com.example.model.HarmonicInterval.PERFECT_FIFTH.ratio
    val octaveRatio = com.example.model.HarmonicInterval.OCTAVE_UP.ratio
    assertEquals(648.0f, base * fifthRatio, 0.01f)
    assertEquals(864.0f, base * octaveRatio, 0.01f)

    val chord = com.example.model.ChordPreset.POWER_FIFTH
    assertEquals(com.example.model.HarmonicInterval.PERFECT_FIFTH, chord.layer2Interval)
    assertEquals(com.example.model.HarmonicInterval.OCTAVE_UP, chord.layer3Interval)
  }
}
