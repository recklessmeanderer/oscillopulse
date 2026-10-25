package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.WavExporter
import com.example.data.AppDatabase
import com.example.data.PresetEntity
import com.example.data.RecordingEntity
import android.content.Context
import com.example.model.AppCardModule
import com.example.model.AudioConfig
import com.example.model.ModulationMode
import com.example.model.ScopeTheme
import com.example.model.WaveformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val presetDao = db.presetDao()
    private val recordingDao = db.recordingDao()
    private val layoutPrefs = application.getSharedPreferences("oscillopulse_layout_prefs", Context.MODE_PRIVATE)

    val audioEngine = AudioEngine(application.filesDir)

    // Movable Studio Cards Order State
    private val _cardOrder = MutableStateFlow(loadSavedCardOrder())
    val cardOrder = _cardOrder.asStateFlow()

    private val _isReorderMode = MutableStateFlow(false)
    val isReorderMode = _isReorderMode.asStateFlow()

    private fun loadSavedCardOrder(): List<AppCardModule> {
        val raw = layoutPrefs.getString("card_order", null) ?: return AppCardModule.DEFAULT_ORDER
        val ids = raw.split(",").map { it.trim() }
        val loaded = ids.mapNotNull { AppCardModule.fromId(it) }.distinct()
        return if (loaded.size == AppCardModule.values().size) loaded else AppCardModule.DEFAULT_ORDER
    }

    private fun saveCardOrder(order: List<AppCardModule>) {
        val raw = order.joinToString(",") { it.id }
        layoutPrefs.edit().putString("card_order", raw).apply()
    }

    // Observable states
    private val _carrierFreq = MutableStateFlow(432.0f)
    val carrierFreq = _carrierFreq.asStateFlow()

    private val _carrierWave = MutableStateFlow(WaveformType.SINE)
    val carrierWave = _carrierWave.asStateFlow()

    private val _carrierPhaseOffset = MutableStateFlow(0.0f) // 0 to 360 degrees
    val carrierPhaseOffset = _carrierPhaseOffset.asStateFlow()

    // Binaural Beat generator states
    private val _isBinauralEnabled = MutableStateFlow(false)
    val isBinauralEnabled = _isBinauralEnabled.asStateFlow()

    private val _leftCarrierFreq = MutableStateFlow(432.0f)
    val leftCarrierFreq = _leftCarrierFreq.asStateFlow()

    private val _rightCarrierFreq = MutableStateFlow(472.0f)
    val rightCarrierFreq = _rightCarrierFreq.asStateFlow()

    // Additive Harmonics states
    private val _isHarmonicsEnabled = MutableStateFlow(false)
    val isHarmonicsEnabled = _isHarmonicsEnabled.asStateFlow()

    private val _h1Multiplier = MutableStateFlow(2)
    val h1Multiplier = _h1Multiplier.asStateFlow()

    private val _h1Volume = MutableStateFlow(0.0f)
    val h1Volume = _h1Volume.asStateFlow()

    private val _h2Multiplier = MutableStateFlow(3)
    val h2Multiplier = _h2Multiplier.asStateFlow()

    private val _h2Volume = MutableStateFlow(0.0f)
    val h2Volume = _h2Volume.asStateFlow()

    private val _h3Multiplier = MutableStateFlow(4)
    val h3Multiplier = _h3Multiplier.asStateFlow()

    private val _h3Volume = MutableStateFlow(0.0f)
    val h3Volume = _h3Volume.asStateFlow()

    // White Noise Floor states
    private val _isWhiteNoiseEnabled = MutableStateFlow(false)
    val isWhiteNoiseEnabled = _isWhiteNoiseEnabled.asStateFlow()

    private val _whiteNoiseLevel = MutableStateFlow(0.15f)
    val whiteNoiseLevel = _whiteNoiseLevel.asStateFlow()

    private val _modFreq = MutableStateFlow(40.0f)
    val modFreq = _modFreq.asStateFlow()

    private val _modIntensity = MutableStateFlow(1.0f)
    val modIntensity = _modIntensity.asStateFlow()

    private val _modWave = MutableStateFlow(WaveformType.SQUARE)
    val modWave = _modWave.asStateFlow()

    private val _modMode = MutableStateFlow(ModulationMode.ISOCHRONIC_PULSE)
    val modMode = _modMode.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _sampleRate = MutableStateFlow(44100)
    val sampleRate = _sampleRate.asStateFlow()

    private val _bitDepth = MutableStateFlow(16)
    val bitDepth = _bitDepth.asStateFlow()

    private val _scopeTheme = MutableStateFlow(ScopeTheme.CYAN)
    val scopeTheme = _scopeTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    // Oscilloscope samples emitted to UI
    private val _scopeSamples = MutableStateFlow(FloatArray(1024))
    val scopeSamples = _scopeSamples.asStateFlow()

    // Batch Export state
    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress = _exportProgress.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage = _userFeedbackMessage.asStateFlow()

    // Database Flows
    val allPresets: StateFlow<List<PresetEntity>> = presetDao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordings: StateFlow<List<RecordingEntity>> = recordingDao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Synchronize initial state to AudioEngine
        updateAudioEngineParams()

        // Oscilloscope refresh loop
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (audioEngine.isPlaying.value || audioEngine.isRecording.value) {
                    _scopeSamples.value = audioEngine.getOscilloscopeSnapshot()
                } else if (_scopeSamples.value[0] != 0f) {
                    // Decay or clear smoothly when stopped
                    _scopeSamples.value = FloatArray(1024)
                }
                delay(30) // ~33 FPS for smooth oscilloscope rendering
            }
        }

        // Check if database needs factory presets population
        viewModelScope.launch(Dispatchers.IO) {
            if (presetDao.getCount() == 0) {
                AppDatabase.populateFactoryPresets(presetDao)
            }
        }
    }

    private fun updateAudioEngineParams() {
        audioEngine.carrierFreq = _carrierFreq.value
        audioEngine.carrierWave = _carrierWave.value
        audioEngine.carrierPhaseOffsetDeg = _carrierPhaseOffset.value
        audioEngine.isBinauralEnabled = _isBinauralEnabled.value
        audioEngine.leftCarrierFreq = _leftCarrierFreq.value
        audioEngine.rightCarrierFreq = _rightCarrierFreq.value
        audioEngine.modFreq = _modFreq.value
        audioEngine.modIntensity = _modIntensity.value
        audioEngine.modWave = _modWave.value
        audioEngine.modMode = _modMode.value
        audioEngine.isHarmonicsEnabled = _isHarmonicsEnabled.value
        audioEngine.h1Multiplier = _h1Multiplier.value
        audioEngine.h1Volume = _h1Volume.value
        audioEngine.h2Multiplier = _h2Multiplier.value
        audioEngine.h2Volume = _h2Volume.value
        audioEngine.h3Multiplier = _h3Multiplier.value
        audioEngine.h3Volume = _h3Volume.value
        audioEngine.isWhiteNoiseEnabled = _isWhiteNoiseEnabled.value
        audioEngine.whiteNoiseLevel = _whiteNoiseLevel.value
        audioEngine.volume = _volume.value
        audioEngine.isMuted = _isMuted.value
        audioEngine.sampleRate = _sampleRate.value
        audioEngine.bitDepth = _bitDepth.value
    }

    fun setCarrierFreq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _carrierFreq.value = clamped
        audioEngine.carrierFreq = clamped
        // If binaural is enabled, keep left synced unless independently adjusted
        if (!_isBinauralEnabled.value) {
            _leftCarrierFreq.value = clamped
            audioEngine.leftCarrierFreq = clamped
        }
    }

    fun setCarrierWave(wave: WaveformType) {
        _carrierWave.value = wave
        audioEngine.carrierWave = wave
    }

    fun setCarrierPhaseOffset(degrees: Float) {
        val normalized = ((degrees % 360f) + 360f) % 360f
        _carrierPhaseOffset.value = normalized
        audioEngine.carrierPhaseOffsetDeg = normalized
    }

    fun setBinauralEnabled(enabled: Boolean) {
        _isBinauralEnabled.value = enabled
        audioEngine.isBinauralEnabled = enabled
        _userFeedbackMessage.value = if (enabled) {
            "Binaural beat active: Left ${_leftCarrierFreq.value.toInt()}Hz, Right ${_rightCarrierFreq.value.toInt()}Hz (${kotlin.math.abs(_leftCarrierFreq.value - _rightCarrierFreq.value).toInt()}Hz beat)"
        } else {
            "Binaural beat disabled"
        }
    }

    fun toggleBinauralBeat() {
        setBinauralEnabled(!_isBinauralEnabled.value)
    }

    fun setLeftCarrierFreq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _leftCarrierFreq.value = clamped
        audioEngine.leftCarrierFreq = clamped
    }

    fun setRightCarrierFreq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _rightCarrierFreq.value = clamped
        audioEngine.rightCarrierFreq = clamped
    }

    fun setBinauralBeatDiff(diffHz: Float) {
        val newRight = (_leftCarrierFreq.value + diffHz).coerceIn(1.0f, 1000.0f)
        _rightCarrierFreq.value = newRight
        audioEngine.rightCarrierFreq = newRight
    }

    // Additive Harmonics Controls
    fun setHarmonicsEnabled(enabled: Boolean) {
        _isHarmonicsEnabled.value = enabled
        audioEngine.isHarmonicsEnabled = enabled
        _userFeedbackMessage.value = if (enabled) "Additive Harmonics enabled" else "Additive Harmonics disabled"
    }

    fun toggleHarmonics() {
        setHarmonicsEnabled(!_isHarmonicsEnabled.value)
    }

    fun setHarmonicMultiplier(harmonicIndex: Int, multiplier: Int) {
        val clamped = multiplier.coerceIn(2, 16)
        when (harmonicIndex) {
            1 -> {
                _h1Multiplier.value = clamped
                audioEngine.h1Multiplier = clamped
            }
            2 -> {
                _h2Multiplier.value = clamped
                audioEngine.h2Multiplier = clamped
            }
            3 -> {
                _h3Multiplier.value = clamped
                audioEngine.h3Multiplier = clamped
            }
        }
    }

    fun setHarmonicVolume(harmonicIndex: Int, vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        when (harmonicIndex) {
            1 -> {
                _h1Volume.value = clamped
                audioEngine.h1Volume = clamped
            }
            2 -> {
                _h2Volume.value = clamped
                audioEngine.h2Volume = clamped
            }
            3 -> {
                _h3Volume.value = clamped
                audioEngine.h3Volume = clamped
            }
        }
    }

    fun setHarmonicProfile(h1M: Int, h1V: Float, h2M: Int, h2V: Float, h3M: Int, h3V: Float) {
        setHarmonicMultiplier(1, h1M)
        setHarmonicVolume(1, h1V)
        setHarmonicMultiplier(2, h2M)
        setHarmonicVolume(2, h2V)
        setHarmonicMultiplier(3, h3M)
        setHarmonicVolume(3, h3V)
        if (!_isHarmonicsEnabled.value) {
            setHarmonicsEnabled(true)
        }
    }

    // White Noise Floor Controls
    fun setWhiteNoiseEnabled(enabled: Boolean) {
        _isWhiteNoiseEnabled.value = enabled
        audioEngine.isWhiteNoiseEnabled = enabled
        _userFeedbackMessage.value = if (enabled) {
            "White noise floor enabled (${(_whiteNoiseLevel.value * 100).toInt()}%)"
        } else {
            "White noise floor disabled"
        }
    }

    fun toggleWhiteNoise() {
        setWhiteNoiseEnabled(!_isWhiteNoiseEnabled.value)
    }

    fun setWhiteNoiseLevel(level: Float) {
        val clamped = level.coerceIn(0.0f, 1.0f)
        _whiteNoiseLevel.value = clamped
        audioEngine.whiteNoiseLevel = clamped
    }

    // Card Reordering & Layout Customization
    fun toggleReorderMode() {
        val next = !_isReorderMode.value
        _isReorderMode.value = next
        _userFeedbackMessage.value = if (next) "Layout customize mode on" else "Layout customize mode off"
    }

    fun setReorderMode(enabled: Boolean) {
        _isReorderMode.value = enabled
    }

    fun moveCardUp(module: AppCardModule) {
        val list = _cardOrder.value.toMutableList()
        val index = list.indexOf(module)
        if (index > 0) {
            val prev = list[index - 1]
            list[index - 1] = module
            list[index] = prev
            _cardOrder.value = list
            saveCardOrder(list)
            _userFeedbackMessage.value = "Moved ${module.title} up"
        }
    }

    fun moveCardDown(module: AppCardModule) {
        val list = _cardOrder.value.toMutableList()
        val index = list.indexOf(module)
        if (index in 0 until list.size - 1) {
            val next = list[index + 1]
            list[index + 1] = module
            list[index] = next
            _cardOrder.value = list
            saveCardOrder(list)
            _userFeedbackMessage.value = "Moved ${module.title} down"
        }
    }

    fun resetCardOrder() {
        _cardOrder.value = AppCardModule.DEFAULT_ORDER
        saveCardOrder(AppCardModule.DEFAULT_ORDER)
        _userFeedbackMessage.value = "Reset to default layout order"
    }

    fun setModFreq(freq: Float) {
        val clamped = freq.coerceIn(0.1f, 100.0f)
        _modFreq.value = clamped
        audioEngine.modFreq = clamped
    }

    fun setModIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.0f, 1.0f)
        _modIntensity.value = clamped
        audioEngine.modIntensity = clamped
    }

    fun setModWave(wave: WaveformType) {
        _modWave.value = wave
        audioEngine.modWave = wave
    }

    fun setModMode(mode: ModulationMode) {
        _modMode.value = mode
        audioEngine.modMode = mode
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        audioEngine.volume = clamped
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        audioEngine.isMuted = newMute
    }

    fun setSampleRate(rate: Int) {
        _sampleRate.value = rate
        audioEngine.sampleRate = rate
        if (audioEngine.isPlaying.value) {
            audioEngine.stop()
            audioEngine.start()
        }
    }

    fun setBitDepth(depth: Int) {
        _bitDepth.value = depth
        audioEngine.bitDepth = depth
    }

    fun setScopeTheme(theme: ScopeTheme) {
        _scopeTheme.value = theme
    }

    fun toggleDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun togglePlayback() {
        audioEngine.togglePlayback()
    }

    fun toggleRecording() {
        audioEngine.toggleRecording { wavFile ->
            if (wavFile != null && wavFile.exists()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val entity = RecordingEntity(
                        title = wavFile.name,
                        filePath = wavFile.absolutePath,
                        fileSizeBytes = wavFile.length(),
                        durationMs = audioEngine.recordingDurationMs.value,
                        sampleRate = _sampleRate.value,
                        bitDepth = _bitDepth.value,
                        carrierFreq = _carrierFreq.value,
                        modFreq = _modFreq.value
                    )
                    recordingDao.insertRecording(entity)
                    _userFeedbackMessage.value = "Recording saved: ${wavFile.name}"
                }
            }
        }
    }

    fun applyPreset(preset: PresetEntity) {
        setCarrierFreq(preset.carrierFreq)
        setCarrierWave(preset.getCarrierWaveType())
        setCarrierPhaseOffset(preset.carrierPhaseOffset)
        setBinauralEnabled(preset.isBinauralEnabled)
        setLeftCarrierFreq(preset.leftCarrierFreq)
        setRightCarrierFreq(preset.rightCarrierFreq)
        setHarmonicsEnabled(preset.isHarmonicsEnabled)
        setHarmonicMultiplier(1, preset.h1Multiplier)
        setHarmonicVolume(1, preset.h1Volume)
        setHarmonicMultiplier(2, preset.h2Multiplier)
        setHarmonicVolume(2, preset.h2Volume)
        setHarmonicMultiplier(3, preset.h3Multiplier)
        setHarmonicVolume(3, preset.h3Volume)
        setWhiteNoiseEnabled(preset.isWhiteNoiseEnabled)
        setWhiteNoiseLevel(preset.whiteNoiseLevel)
        setModFreq(preset.modFreq)
        setModIntensity(preset.modIntensity)
        setModWave(preset.getModWaveType())
        setModMode(preset.getModulationMode())
        _userFeedbackMessage.value = "Loaded preset: ${preset.name}"
    }

    fun savePreset(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = PresetEntity(
                name = name,
                description = description,
                carrierFreq = _carrierFreq.value,
                carrierWave = _carrierWave.value.name,
                carrierPhaseOffset = _carrierPhaseOffset.value,
                isBinauralEnabled = _isBinauralEnabled.value,
                leftCarrierFreq = _leftCarrierFreq.value,
                rightCarrierFreq = _rightCarrierFreq.value,
                isHarmonicsEnabled = _isHarmonicsEnabled.value,
                h1Multiplier = _h1Multiplier.value,
                h1Volume = _h1Volume.value,
                h2Multiplier = _h2Multiplier.value,
                h2Volume = _h2Volume.value,
                h3Multiplier = _h3Multiplier.value,
                h3Volume = _h3Volume.value,
                isWhiteNoiseEnabled = _isWhiteNoiseEnabled.value,
                whiteNoiseLevel = _whiteNoiseLevel.value,
                modFreq = _modFreq.value,
                modIntensity = _modIntensity.value,
                modWave = _modWave.value.name,
                modMode = _modMode.value.name,
                sampleRate = _sampleRate.value,
                bitDepth = _bitDepth.value,
                isFactory = false
            )
            presetDao.insertPreset(entity)
            _userFeedbackMessage.value = "Preset '$name' saved"
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            presetDao.deletePreset(preset)
            _userFeedbackMessage.value = "Deleted preset: ${preset.name}"
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(recording.filePath)
            if (file.exists()) file.delete()
            recordingDao.deleteRecording(recording)
            _userFeedbackMessage.value = "Deleted ${recording.title}"
        }
    }

    fun exportWav(
        fileName: String,
        durationSec: Float,
        exportSampleRate: Int,
        exportBitDepth: Int,
        onDone: () -> Unit
    ) {
        if (_isExporting.value) return
        _isExporting.value = true
        _exportProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recDir = File(getApplication<Application>().filesDir, "recordings")
                if (!recDir.exists()) recDir.mkdirs()

                val outFile = File(recDir, fileName)
                val success = WavExporter.renderWav(
                    outputFile = outFile,
                    carrierFreq = _carrierFreq.value,
                    carrierWave = _carrierWave.value,
                    carrierPhaseOffsetDeg = _carrierPhaseOffset.value,
                    isBinauralEnabled = _isBinauralEnabled.value,
                    leftCarrierFreq = _leftCarrierFreq.value,
                    rightCarrierFreq = _rightCarrierFreq.value,
                    isHarmonicsEnabled = _isHarmonicsEnabled.value,
                    h1Multiplier = _h1Multiplier.value,
                    h1Volume = _h1Volume.value,
                    h2Multiplier = _h2Multiplier.value,
                    h2Volume = _h2Volume.value,
                    h3Multiplier = _h3Multiplier.value,
                    h3Volume = _h3Volume.value,
                    isWhiteNoiseEnabled = _isWhiteNoiseEnabled.value,
                    whiteNoiseLevel = _whiteNoiseLevel.value,
                    modFreq = _modFreq.value,
                    modIntensity = _modIntensity.value,
                    modWave = _modWave.value,
                    modMode = _modMode.value,
                    durationSeconds = durationSec,
                    sampleRate = exportSampleRate,
                    bitDepth = exportBitDepth,
                    volume = _volume.value,
                    onProgress = { progress ->
                        _exportProgress.value = progress
                    }
                )

                if (success) {
                    val recording = RecordingEntity(
                        title = outFile.name,
                        filePath = outFile.absolutePath,
                        fileSizeBytes = outFile.length(),
                        durationMs = (durationSec * 1000).toLong(),
                        sampleRate = exportSampleRate,
                        bitDepth = exportBitDepth,
                        carrierFreq = _carrierFreq.value,
                        modFreq = _modFreq.value
                    )
                    recordingDao.insertRecording(recording)
                    _userFeedbackMessage.value = "Successfully exported ${outFile.name}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userFeedbackMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
                onDone()
            }
        }
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
