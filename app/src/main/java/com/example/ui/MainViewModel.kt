package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.AudioPlaybackService
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

    val audioEngine = AudioEngine.getInstance(application)

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

    // Binaural Beat generator states
    private val _isBinauralEnabled = MutableStateFlow(false)
    val isBinauralEnabled = _isBinauralEnabled.asStateFlow()

    private val _leftCarrierFreq = MutableStateFlow(432.0f)
    val leftCarrierFreq = _leftCarrierFreq.asStateFlow()

    private val _rightCarrierFreq = MutableStateFlow(472.0f)
    val rightCarrierFreq = _rightCarrierFreq.asStateFlow()

    // Real-Time Cross-Card Frequency Linking State (links Carrier, Binaural L/R, and Modulation)
    private val _isFrequenciesLinked = MutableStateFlow(true)
    val isFrequenciesLinked = _isFrequenciesLinked.asStateFlow()

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

        // Check if database needs factory presets population or deduplication
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.populateFactoryPresets(presetDao)
        }

        // Keep foreground service and sticky notification in sync with playback
        viewModelScope.launch {
            audioEngine.isPlaying.collectLatest { playing ->
                if (playing) {
                    AudioPlaybackService.start(application)
                }
            }
        }
    }

    private fun updateAudioEngineParams() {
        audioEngine.carrierFreq = _carrierFreq.value
        audioEngine.carrierWave = _carrierWave.value
        audioEngine.isBinauralEnabled = _isBinauralEnabled.value
        audioEngine.leftCarrierFreq = _leftCarrierFreq.value
        audioEngine.rightCarrierFreq = _rightCarrierFreq.value
        audioEngine.modFreq = _modFreq.value
        audioEngine.modIntensity = _modIntensity.value
        audioEngine.modWave = _modWave.value
        audioEngine.modMode = _modMode.value
        audioEngine.isWhiteNoiseEnabled = _isWhiteNoiseEnabled.value
        audioEngine.whiteNoiseLevel = _whiteNoiseLevel.value
        audioEngine.volume = _volume.value
        audioEngine.isMuted = _isMuted.value
        audioEngine.sampleRate = _sampleRate.value
        audioEngine.bitDepth = _bitDepth.value
    }

    fun toggleFrequencyLink() {
        val next = !_isFrequenciesLinked.value
        _isFrequenciesLinked.value = next
        _userFeedbackMessage.value = if (next) "Cross-card frequency linking active" else "Frequency linking unlinked"
    }

    fun setFrequencyLink(linked: Boolean) {
        _isFrequenciesLinked.value = linked
    }

    fun setCarrierFreq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _carrierFreq.value = clamped
        audioEngine.carrierFreq = clamped

        if (_isFrequenciesLinked.value) {
            // Shift Left and Right carriers preserving current beat offset
            val offset = _rightCarrierFreq.value - _leftCarrierFreq.value
            _leftCarrierFreq.value = clamped
            audioEngine.leftCarrierFreq = clamped

            val newRight = (clamped + offset).coerceIn(1.0f, 1000.0f)
            _rightCarrierFreq.value = newRight
            audioEngine.rightCarrierFreq = newRight
        } else if (!_isBinauralEnabled.value) {
            _leftCarrierFreq.value = clamped
            audioEngine.leftCarrierFreq = clamped
        }
    }

    fun setCarrierWave(wave: WaveformType) {
        _carrierWave.value = wave
        audioEngine.carrierWave = wave
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

        if (_isFrequenciesLinked.value) {
            // Synchronize master carrier with Left ear
            _carrierFreq.value = clamped
            audioEngine.carrierFreq = clamped

            // Update modulation frequency with resulting beat difference if within range
            val diff = kotlin.math.abs(_rightCarrierFreq.value - clamped)
            if (diff in 0.1f..100.0f) {
                _modFreq.value = diff
                audioEngine.modFreq = diff
            }
        }
    }

    fun setRightCarrierFreq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _rightCarrierFreq.value = clamped
        audioEngine.rightCarrierFreq = clamped

        if (_isFrequenciesLinked.value) {
            val diff = kotlin.math.abs(clamped - _leftCarrierFreq.value)
            if (diff in 0.1f..100.0f) {
                _modFreq.value = diff
                audioEngine.modFreq = diff
            }
        }
    }

    fun setBinauralBeatDiff(diffHz: Float) {
        val clampedDiff = diffHz.coerceIn(0.1f, 100.0f)
        val newRight = (_leftCarrierFreq.value + clampedDiff).coerceIn(1.0f, 1000.0f)
        _rightCarrierFreq.value = newRight
        audioEngine.rightCarrierFreq = newRight

        if (_isFrequenciesLinked.value) {
            _modFreq.value = clampedDiff
            audioEngine.modFreq = clampedDiff
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

        if (_isFrequenciesLinked.value) {
            // Synchronize Binaural right carrier so that beat difference equals modFreq
            val newRight = (_leftCarrierFreq.value + clamped).coerceIn(1.0f, 1000.0f)
            _rightCarrierFreq.value = newRight
            audioEngine.rightCarrierFreq = newRight
        }
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
        if (audioEngine.isPlaying.value) {
            AudioPlaybackService.start(getApplication())
        }
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
        setBinauralEnabled(preset.isBinauralEnabled)
        setLeftCarrierFreq(preset.leftCarrierFreq)
        setRightCarrierFreq(preset.rightCarrierFreq)
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
                isBinauralEnabled = _isBinauralEnabled.value,
                leftCarrierFreq = _leftCarrierFreq.value,
                rightCarrierFreq = _rightCarrierFreq.value,
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

    fun restoreFactoryPresets() {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.populateFactoryPresets(presetDao)
            _userFeedbackMessage.value = "Restored built-in factory presets"
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
                    isBinauralEnabled = _isBinauralEnabled.value,
                    leftCarrierFreq = _leftCarrierFreq.value,
                    rightCarrierFreq = _rightCarrierFreq.value,
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
