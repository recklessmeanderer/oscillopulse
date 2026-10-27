package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.ModulationMode
import com.example.model.WaveformType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PresetDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var presetDao: PresetDao
    private lateinit var repository: PresetRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        presetDao = database.presetDao()
        repository = PresetRepositoryImpl(presetDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveCustomCarrierAndEntrainmentPreset() = runBlocking {
        // Save a custom preset with specific carrier and entrainment settings
        val customId = repository.saveCustomPreset(
            name = "Deep Theta Meditation",
            description = "108 Hz Sine Carrier + 6.0 Hz Isochronic Pulse",
            carrierFreq = 108.0f,
            carrierWave = WaveformType.SINE.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 108.0f,
            rightCarrierFreq = 114.0f,
            modFreq = 6.0f,
            modIntensity = 0.85f,
            modWave = WaveformType.SINE.name,
            modMode = ModulationMode.ISOCHRONIC_PULSE.name
        )

        assertTrue("Generated ID must be > 0", customId > 0)

        // Retrieve preset directly by ID
        val retrieved = repository.findPresetById(customId)
        assertNotNull(retrieved)
        assertEquals("Deep Theta Meditation", retrieved!!.name)
        assertEquals(108.0f, retrieved.carrierFreq, 0.001f)
        assertEquals(WaveformType.SINE, retrieved.getCarrierWaveType())
        assertFalse(retrieved.isBinauralEnabled)

        // Verify entrainment modulation parameters
        assertEquals(6.0f, retrieved.modFreq, 0.001f)
        assertEquals(0.85f, retrieved.modIntensity, 0.001f)
        assertEquals(WaveformType.SINE, retrieved.getModWaveType())
        assertEquals(ModulationMode.ISOCHRONIC_PULSE, retrieved.getModulationMode())
        assertEquals("Theta (4-8 Hz)", retrieved.getEntrainmentBand())
        assertFalse("Must be marked as custom (not factory)", retrieved.isFactory)
    }

    @Test
    fun testBinauralCarrierSettingsPersistence() = runBlocking {
        // Save binaural preset with distinct left and right carrier frequencies
        val binauralId = repository.saveCustomPreset(
            name = "Binaural 40Hz Gamma Focus",
            description = "Stereo 40Hz entrainment beat (Left 412Hz, Right 452Hz)",
            carrierFreq = 432.0f,
            carrierWave = WaveformType.SINE.name,
            isBinauralEnabled = true,
            leftCarrierFreq = 412.0f,
            rightCarrierFreq = 452.0f,
            modFreq = 40.0f,
            modIntensity = 1.0f,
            modWave = WaveformType.SINE.name,
            modMode = ModulationMode.BINAURAL_BEAT.name
        )

        val retrieved = repository.findPresetById(binauralId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isBinauralEnabled)
        assertEquals(412.0f, retrieved.leftCarrierFreq, 0.001f)
        assertEquals(452.0f, retrieved.rightCarrierFreq, 0.001f)
        assertEquals(40.0f, retrieved.rightCarrierFreq - retrieved.leftCarrierFreq, 0.001f)
        assertEquals(ModulationMode.BINAURAL_BEAT, retrieved.getModulationMode())
        assertEquals("Gamma (30-100 Hz)", retrieved.getEntrainmentBand())
    }

    @Test
    fun testCustomPresetsReactiveFlow() = runBlocking {
        // Initially empty
        val initialCustom = repository.customPresets.first()
        assertTrue(initialCustom.isEmpty())

        // Insert two custom presets
        repository.saveCustomPreset(
            name = "Alpha Relaxation 10Hz",
            description = "Gentle flow",
            carrierFreq = 432.0f,
            carrierWave = WaveformType.TRIANGLE.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 432.0f,
            rightCarrierFreq = 442.0f,
            modFreq = 10.0f,
            modIntensity = 0.70f,
            modWave = WaveformType.SINE.name,
            modMode = ModulationMode.AMPLITUDE_MODULATION.name
        )

        repository.saveCustomPreset(
            name = "Delta Sleep 2.5Hz",
            description = "Restorative sleep",
            carrierFreq = 100.0f,
            carrierWave = WaveformType.SINE.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 100.0f,
            rightCarrierFreq = 102.5f,
            modFreq = 2.5f,
            modIntensity = 0.90f,
            modWave = WaveformType.SINE.name,
            modMode = ModulationMode.ISOCHRONIC_PULSE.name
        )

        val updatedCustom = repository.customPresets.first()
        assertEquals(2, updatedCustom.size)
        assertEquals("Delta Sleep 2.5Hz", updatedCustom[0].name) // Newest first
        assertEquals("Alpha Relaxation 10Hz", updatedCustom[1].name)
    }

    @Test
    fun testUpdatePresetEntrainmentSettings() = runBlocking {
        val id = repository.saveCustomPreset(
            name = "Modulation Test",
            description = "Initial",
            carrierFreq = 200.0f,
            carrierWave = WaveformType.SQUARE.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 200.0f,
            rightCarrierFreq = 220.0f,
            modFreq = 20.0f,
            modIntensity = 0.5f,
            modWave = WaveformType.SQUARE.name,
            modMode = ModulationMode.ISOCHRONIC_PULSE.name
        )

        val original = repository.findPresetById(id)!!
        val modified = original.copy(
            modFreq = 14.0f,
            modIntensity = 0.95f,
            carrierFreq = 528.0f
        )
        repository.updatePreset(modified)

        val reloaded = repository.findPresetById(id)!!
        assertEquals(14.0f, reloaded.modFreq, 0.001f)
        assertEquals(0.95f, reloaded.modIntensity, 0.001f)
        assertEquals(528.0f, reloaded.carrierFreq, 0.001f)
        assertEquals("Beta (14-30 Hz)", reloaded.getEntrainmentBand())
    }

    @Test
    fun testDeleteCustomPreset() = runBlocking {
        val id = repository.saveCustomPreset(
            name = "Temp Preset",
            description = "To be deleted",
            carrierFreq = 300.0f,
            carrierWave = WaveformType.SINE.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 300.0f,
            rightCarrierFreq = 310.0f,
            modFreq = 10.0f,
            modIntensity = 1.0f,
            modWave = WaveformType.SINE.name,
            modMode = ModulationMode.ISOCHRONIC_PULSE.name
        )

        val inserted = repository.findPresetById(id)
        assertNotNull(inserted)

        repository.deletePreset(inserted!!)
        val afterDelete = repository.findPresetById(id)
        assertNull(afterDelete)
    }

    @Test
    fun testRestoreFactoryPresetsPreservesCustomPresets() = runBlocking {
        // Seed factory presets first (as happens on DB creation)
        repository.restoreFactoryPresets()

        // Save a custom preset (autoincrement will assign ID after factory presets)
        val customId = repository.saveCustomPreset(
            name = "My Personal Gamma",
            description = "Custom research wave",
            carrierFreq = 440.0f,
            carrierWave = WaveformType.SAWTOOTH.name,
            isBinauralEnabled = false,
            leftCarrierFreq = 440.0f,
            rightCarrierFreq = 480.0f,
            modFreq = 40.0f,
            modIntensity = 1.0f,
            modWave = WaveformType.SQUARE.name,
            modMode = ModulationMode.ISOCHRONIC_PULSE.name
        )
        assertTrue("Custom ID should be after factory presets", customId > 11L)

        // Re-run restoreFactoryPresets to test idempotency and custom preservation
        repository.restoreFactoryPresets()

        // Verify factory presets exist
        val factoryPresets = repository.factoryPresets.first()
        assertTrue("Factory presets must be populated", factoryPresets.isNotEmpty())

        // Verify custom preset was NOT deleted or overwritten by restoring factory presets
        val custom = repository.findPresetById(customId)
        assertNotNull(custom)
        assertEquals("My Personal Gamma", custom!!.name)
        assertFalse(custom.isFactory)
    }
}
