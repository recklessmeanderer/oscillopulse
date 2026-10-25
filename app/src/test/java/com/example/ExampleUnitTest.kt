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
    assertEquals(5, defaultList.size)
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
}
