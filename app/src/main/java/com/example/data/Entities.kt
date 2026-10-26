package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ChordPreset
import com.example.model.ModulationMode
import com.example.model.NoiseColor
import com.example.model.WaveformType

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val carrierFreq: Float = 432.0f,
    val carrierWave: String = WaveformType.SINE.name,
    val modFreq: Float = 40.0f,
    val modIntensity: Float = 1.0f,
    val modWave: String = WaveformType.SQUARE.name,
    val modMode: String = ModulationMode.ISOCHRONIC_PULSE.name,
    val isBinauralEnabled: Boolean = false,
    val leftCarrierFreq: Float = 432.0f,
    val rightCarrierFreq: Float = 472.0f,
    val isWhiteNoiseEnabled: Boolean = false,
    val whiteNoiseLevel: Float = 0.15f,
    val noiseColor: String = NoiseColor.WHITE.name,
    val isLayeringEnabled: Boolean = false,
    val chordPreset: String = ChordPreset.OFF.name,
    val layer2Freq: Float = 648.0f,
    val layer2Gain: Float = 0.45f,
    val layer3Freq: Float = 864.0f,
    val layer3Gain: Float = 0.35f,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val isFactory: Boolean = false
) {
    fun getCarrierWaveType(): WaveformType = runCatching {
        WaveformType.valueOf(carrierWave)
    }.getOrDefault(WaveformType.SINE)

    fun getModWaveType(): WaveformType = runCatching {
        WaveformType.valueOf(modWave)
    }.getOrDefault(WaveformType.SQUARE)

    fun getModulationMode(): ModulationMode = runCatching {
        ModulationMode.valueOf(modMode)
    }.getOrDefault(ModulationMode.ISOCHRONIC_PULSE)

    fun getNoiseColorType(): NoiseColor = runCatching {
        NoiseColor.valueOf(noiseColor)
    }.getOrDefault(NoiseColor.WHITE)

    fun getChordPresetType(): ChordPreset = runCatching {
        ChordPreset.valueOf(chordPreset)
    }.getOrDefault(ChordPreset.OFF)
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val sampleRate: Int,
    val bitDepth: Int,
    val carrierFreq: Float,
    val modFreq: Float,
    val createdAt: Long = System.currentTimeMillis()
)
