package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ChordPreset
import com.example.model.ModulationMode
import com.example.model.NoiseColor
import com.example.model.WaveformType

/**
 * Room Entity representing an audio waveform preset.
 * Stores complete carrier frequency specifications, entrainment modulation settings,
 * multi-tone layering, and noise floor parameters.
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",

    // Carrier Frequency Settings
    val carrierFreq: Float = 432.0f,
    val carrierWave: String = WaveformType.SINE.name,
    val isBinauralEnabled: Boolean = false,
    val leftCarrierFreq: Float = 432.0f,
    val rightCarrierFreq: Float = 472.0f,

    // Entrainment Modulation Settings
    val modFreq: Float = 40.0f,
    val modIntensity: Float = 1.0f,
    val modWave: String = WaveformType.SQUARE.name,
    val modMode: String = ModulationMode.ISOCHRONIC_PULSE.name,

    // Noise Floor Settings
    val isWhiteNoiseEnabled: Boolean = false,
    val whiteNoiseLevel: Float = 0.15f,
    val noiseColor: String = NoiseColor.WHITE.name,

    // Multi-Tone Layering & Harmonic Chord Settings
    val isLayeringEnabled: Boolean = false,
    val chordPreset: String = ChordPreset.OFF.name,
    val layer2Freq: Float = 648.0f,
    val layer2Gain: Float = 0.45f,
    val layer2Wave: String = WaveformType.SINE.name,
    val layer3Freq: Float = 864.0f,
    val layer3Gain: Float = 0.35f,
    val layer3Wave: String = WaveformType.SINE.name,

    // Audio Engine Config & Metadata
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val isFactory: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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

    fun getLayer2WaveType(): WaveformType = runCatching {
        WaveformType.valueOf(layer2Wave)
    }.getOrDefault(WaveformType.SINE)

    fun getLayer3WaveType(): WaveformType = runCatching {
        WaveformType.valueOf(layer3Wave)
    }.getOrDefault(WaveformType.SINE)

    /**
     * Categorizes the entrainment target brainwave band based on modulation frequency.
     */
    fun getEntrainmentBand(): String = when {
        modFreq < 0.5f -> "Sub-Delta"
        modFreq < 4.0f -> "Delta (0.5-4 Hz)"
        modFreq < 8.0f -> "Theta (4-8 Hz)"
        modFreq < 14.0f -> "Alpha (8-14 Hz)"
        modFreq < 30.0f -> "Beta (14-30 Hz)"
        modFreq <= 100.0f -> "Gamma (30-100 Hz)"
        else -> "High-Frequency (${modFreq.toInt()} Hz)"
    }
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
