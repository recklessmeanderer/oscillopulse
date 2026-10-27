package com.example.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing audio waveform presets.
 * Exposes reactive Kotlin Flows and safe asynchronous suspend functions.
 */
interface PresetRepository {
    val allPresets: Flow<List<PresetEntity>>
    val customPresets: Flow<List<PresetEntity>>
    val factoryPresets: Flow<List<PresetEntity>>

    fun getPresetById(id: Long): Flow<PresetEntity?>
    suspend fun findPresetById(id: Long): PresetEntity?

    suspend fun savePreset(preset: PresetEntity): Long

    suspend fun saveCustomPreset(
        name: String,
        description: String,
        carrierFreq: Float,
        carrierWave: String,
        isBinauralEnabled: Boolean,
        leftCarrierFreq: Float,
        rightCarrierFreq: Float,
        modFreq: Float,
        modIntensity: Float,
        modWave: String,
        modMode: String,
        isWhiteNoiseEnabled: Boolean = false,
        whiteNoiseLevel: Float = 0.15f,
        noiseColor: String = "WHITE",
        isLayeringEnabled: Boolean = false,
        chordPreset: String = "OFF",
        layer2Freq: Float = 648.0f,
        layer2Gain: Float = 0.45f,
        layer2Wave: String = "SINE",
        layer3Freq: Float = 864.0f,
        layer3Gain: Float = 0.35f,
        layer3Wave: String = "SINE",
        sampleRate: Int = 44100,
        bitDepth: Int = 16
    ): Long

    suspend fun updatePreset(preset: PresetEntity)
    suspend fun deletePreset(preset: PresetEntity)
    suspend fun deletePresetById(id: Long)
    suspend fun restoreFactoryPresets()
}

/**
 * Room implementation of [PresetRepository].
 * Directs database calls to IO Dispatcher and encapsulates PresetDao interactions.
 */
class PresetRepositoryImpl(
    private val presetDao: PresetDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PresetRepository {

    override val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    override val customPresets: Flow<List<PresetEntity>> = presetDao.getCustomPresets()

    override val factoryPresets: Flow<List<PresetEntity>> = presetDao.getFactoryPresets()

    override fun getPresetById(id: Long): Flow<PresetEntity?> = presetDao.getPresetById(id)

    override suspend fun findPresetById(id: Long): PresetEntity? = withContext(ioDispatcher) {
        presetDao.findPresetById(id)
    }

    override suspend fun savePreset(preset: PresetEntity): Long = withContext(ioDispatcher) {
        presetDao.insertPreset(preset)
    }

    override suspend fun saveCustomPreset(
        name: String,
        description: String,
        carrierFreq: Float,
        carrierWave: String,
        isBinauralEnabled: Boolean,
        leftCarrierFreq: Float,
        rightCarrierFreq: Float,
        modFreq: Float,
        modIntensity: Float,
        modWave: String,
        modMode: String,
        isWhiteNoiseEnabled: Boolean,
        whiteNoiseLevel: Float,
        noiseColor: String,
        isLayeringEnabled: Boolean,
        chordPreset: String,
        layer2Freq: Float,
        layer2Gain: Float,
        layer2Wave: String,
        layer3Freq: Float,
        layer3Gain: Float,
        layer3Wave: String,
        sampleRate: Int,
        bitDepth: Int
    ): Long = withContext(ioDispatcher) {
        val entity = PresetEntity(
            name = name,
            description = description,
            carrierFreq = carrierFreq,
            carrierWave = carrierWave,
            isBinauralEnabled = isBinauralEnabled,
            leftCarrierFreq = leftCarrierFreq,
            rightCarrierFreq = rightCarrierFreq,
            modFreq = modFreq,
            modIntensity = modIntensity,
            modWave = modWave,
            modMode = modMode,
            isWhiteNoiseEnabled = isWhiteNoiseEnabled,
            whiteNoiseLevel = whiteNoiseLevel,
            noiseColor = noiseColor,
            isLayeringEnabled = isLayeringEnabled,
            chordPreset = chordPreset,
            layer2Freq = layer2Freq,
            layer2Gain = layer2Gain,
            layer2Wave = layer2Wave,
            layer3Freq = layer3Freq,
            layer3Gain = layer3Gain,
            layer3Wave = layer3Wave,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            isFactory = false,
            createdAt = System.currentTimeMillis()
        )
        presetDao.insertPreset(entity)
    }

    override suspend fun updatePreset(preset: PresetEntity): Unit = withContext(ioDispatcher) {
        presetDao.updatePreset(preset)
    }

    override suspend fun deletePreset(preset: PresetEntity): Unit = withContext(ioDispatcher) {
        presetDao.deletePreset(preset)
    }

    override suspend fun deletePresetById(id: Long): Unit = withContext(ioDispatcher) {
        presetDao.deletePresetById(id)
    }

    override suspend fun restoreFactoryPresets(): Unit = withContext(ioDispatcher) {
        AppDatabase.populateFactoryPresets(presetDao)
    }
}
