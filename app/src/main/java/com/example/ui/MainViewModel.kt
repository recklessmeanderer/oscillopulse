package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.WavExporter
import com.example.data.AppDatabase
import com.example.data.PresetEntity
import com.example.data.RecordingEntity
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

    val audioEngine = AudioEngine(application.filesDir)

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
