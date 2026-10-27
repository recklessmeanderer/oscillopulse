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
import com.example.model.ChordPreset
import com.example.model.HarmonicInterval
import com.example.model.ModulationMode
import com.example.model.NoiseColor
import com.example.model.ScanDirection
import com.example.model.ScopeTheme
import com.example.model.WaveformType
import android.graphics.Bitmap
import android.net.Uri
import com.example.audio.ImageSonificationHelper
import com.example.audio.TexturePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val presetRepository = AppDatabase.getPresetRepository(application)
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

    // Noise Floor states (White, Pink, Brown)
    private val _isWhiteNoiseEnabled = MutableStateFlow(false)
    val isWhiteNoiseEnabled = _isWhiteNoiseEnabled.asStateFlow()

    private val _whiteNoiseLevel = MutableStateFlow(0.15f)
    val whiteNoiseLevel = _whiteNoiseLevel.asStateFlow()

    private val _noiseColor = MutableStateFlow(NoiseColor.WHITE)
    val noiseColor = _noiseColor.asStateFlow()

    // Optical Image Sonification & Oscilloscope Wallpaper
    val opticalScanProgress = audioEngine.opticalScanProgress

    private val _scopeBackgroundImage = MutableStateFlow<Bitmap?>(null)
    val scopeBackgroundImage = _scopeBackgroundImage.asStateFlow()

    private val _scopeBackgroundOpacity = MutableStateFlow(0.35f)
    val scopeBackgroundOpacity = _scopeBackgroundOpacity.asStateFlow()

    private val _isImageNoiseActive = MutableStateFlow(false)
    val isImageNoiseActive = _isImageNoiseActive.asStateFlow()

    private val _imageNoiseDepth = MutableStateFlow(0.65f)
    val imageNoiseDepth = _imageNoiseDepth.asStateFlow()

    private val _imageScanSpeedHz = MutableStateFlow(1.0f)
    val imageScanSpeedHz = _imageScanSpeedHz.asStateFlow()

    private val _selectedImageName = MutableStateFlow<String?>("Ra in the Abyss")
    val selectedImageName = _selectedImageName.asStateFlow()

    private val _imageAnalysisInfo = MutableStateFlow<String?>(null)
    val imageAnalysisInfo = _imageAnalysisInfo.asStateFlow()

    private val _scanDirection = MutableStateFlow(ScanDirection.VERTICAL_WATERFALL)
    val scanDirection = _scanDirection.asStateFlow()

    // Audio Focus Ducking
    private val _isDuckingEnabled = MutableStateFlow(true)
    val isDuckingEnabled = _isDuckingEnabled.asStateFlow()

    // Multi-Tone Layering & Harmonic Chords
    private val _isLayeringEnabled = MutableStateFlow(false)
    val isLayeringEnabled = _isLayeringEnabled.asStateFlow()

    private val _activeChordPreset = MutableStateFlow(ChordPreset.OFF)
    val activeChordPreset = _activeChordPreset.asStateFlow()

    private val _layer2Enabled = MutableStateFlow(true)
    val layer2Enabled = _layer2Enabled.asStateFlow()

    private val _layer2Freq = MutableStateFlow(648.0f)
    val layer2Freq = _layer2Freq.asStateFlow()

    private val _layer2Wave = MutableStateFlow(WaveformType.SINE)
    val layer2Wave = _layer2Wave.asStateFlow()

    private val _layer2Gain = MutableStateFlow(0.45f)
    val layer2Gain = _layer2Gain.asStateFlow()

    private val _layer2Interval = MutableStateFlow(HarmonicInterval.PERFECT_FIFTH)
    val layer2Interval = _layer2Interval.asStateFlow()

    private val _layer3Enabled = MutableStateFlow(true)
    val layer3Enabled = _layer3Enabled.asStateFlow()

    private val _layer3Freq = MutableStateFlow(864.0f)
    val layer3Freq = _layer3Freq.asStateFlow()

    private val _layer3Wave = MutableStateFlow(WaveformType.SINE)
    val layer3Wave = _layer3Wave.asStateFlow()

    private val _layer3Gain = MutableStateFlow(0.35f)
    val layer3Gain = _layer3Gain.asStateFlow()

    private val _layer3Interval = MutableStateFlow(HarmonicInterval.OCTAVE_UP)
    val layer3Interval = _layer3Interval.asStateFlow()

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
    val allPresets: StateFlow<List<PresetEntity>> = presetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPresets: StateFlow<List<PresetEntity>> = presetRepository.customPresets
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

        // Initialize presets repository
        viewModelScope.launch(Dispatchers.IO) {
            presetRepository.restoreFactoryPresets()
        }

        // Keep foreground service and sticky notification in sync with playback
        viewModelScope.launch {
            audioEngine.isPlaying.collectLatest { playing ->
                if (playing) {
                    AudioPlaybackService.start(application)
                }
            }
        }

        // Export bundled featured image to device Pictures so Photo Picker finds it
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                if (picturesDir != null) {
                    if (!picturesDir.exists()) picturesDir.mkdirs()
                    val destFile = File(picturesDir, "ra_abyss.jpg")
                    if (!destFile.exists()) {
                        application.assets.open("ra_abyss_cedd8734.jpg").use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(destFile)
                        application.sendBroadcast(mediaScanIntent)
                    }
                }
            } catch (e: Throwable) {
                // Safe to ignore if permissions or environment differ
            }
        }

        // Preload default featured Ra in the Abyss texture
        viewModelScope.launch(Dispatchers.Default) {
            val featuredTexture = ImageSonificationHelper.getPresetBitmap(getApplication(), TexturePreset.RA_ABYSS)
            applyAnalyzedImage(featuredTexture, TexturePreset.RA_ABYSS.title)
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
        audioEngine.noiseColor = _noiseColor.value
        audioEngine.isImageNoiseEnabled = _isImageNoiseActive.value
        audioEngine.imageNoiseDepth = _imageNoiseDepth.value
        audioEngine.imageScanSpeedHz = _imageScanSpeedHz.value
        audioEngine.isDuckingEnabled = _isDuckingEnabled.value
        audioEngine.isLayeringEnabled = _isLayeringEnabled.value
        audioEngine.activeChordPreset = _activeChordPreset.value
        audioEngine.layer2Enabled = _layer2Enabled.value
        audioEngine.layer2Freq = _layer2Freq.value
        audioEngine.layer2Wave = _layer2Wave.value
        audioEngine.layer2Gain = _layer2Gain.value
        audioEngine.layer2Interval = _layer2Interval.value
        audioEngine.layer3Enabled = _layer3Enabled.value
        audioEngine.layer3Freq = _layer3Freq.value
        audioEngine.layer3Wave = _layer3Wave.value
        audioEngine.layer3Gain = _layer3Gain.value
        audioEngine.layer3Interval = _layer3Interval.value
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

        // Harmonically scale chord layer frequencies if locked to intervals
        if (_isLayeringEnabled.value) {
            if (_layer2Interval.value != HarmonicInterval.CUSTOM) {
                val newL2 = (clamped * _layer2Interval.value.ratio).coerceIn(1.0f, 1000.0f)
                _layer2Freq.value = newL2
                audioEngine.layer2Freq = newL2
            }
            if (_layer3Interval.value != HarmonicInterval.CUSTOM) {
                val newL3 = (clamped * _layer3Interval.value.ratio).coerceIn(1.0f, 1000.0f)
                _layer3Freq.value = newL3
                audioEngine.layer3Freq = newL3
            }
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

    fun setNoiseColor(color: NoiseColor) {
        _noiseColor.value = color
        audioEngine.noiseColor = color
        _userFeedbackMessage.value = "Noise color set to ${color.displayName}"
    }

    fun setDuckingEnabled(enabled: Boolean) {
        _isDuckingEnabled.value = enabled
        audioEngine.isDuckingEnabled = enabled
        _userFeedbackMessage.value = if (enabled) "Auto audio ducking enabled" else "Audio ducking disabled"
    }

    // Optical Image Sonification & Scope Wallpaper Controls
    fun loadScopeImageUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = ImageSonificationHelper.loadBitmapFromUri(getApplication(), uri)
                if (bitmap != null) {
                    applyAnalyzedImage(bitmap, "Custom Photo")
                    _userFeedbackMessage.value = "Image wallpaper loaded & sonified"
                } else {
                    _userFeedbackMessage.value = "Could not decode selected image"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _userFeedbackMessage.value = "Failed to load image: ${e.message}"
            }
        }
    }

    fun loadPresetTexture(preset: TexturePreset) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = ImageSonificationHelper.getPresetBitmap(getApplication(), preset)
                applyAnalyzedImage(bitmap, preset.title)
                setNoiseColor(preset.noiseColor)
                if (!_isWhiteNoiseEnabled.value) {
                    setWhiteNoiseEnabled(true)
                }
                _userFeedbackMessage.value = "${preset.title} loaded (${preset.noiseColor.displayName} noise)"
            } catch (e: Throwable) {
                e.printStackTrace()
                _userFeedbackMessage.value = "Error loading preset: ${e.message}"
            }
        }
    }

    private fun applyAnalyzedImage(bitmap: Bitmap, title: String) {
        val analysis = ImageSonificationHelper.extractAudioProfile(bitmap, direction = _scanDirection.value)
        _scopeBackgroundImage.value = bitmap
        _selectedImageName.value = title
        val dirLabel = if (_scanDirection.value == ScanDirection.VERTICAL_WATERFALL) "Waterfall ↓" else "H-Scan →"
        _imageAnalysisInfo.value = String.format("Lum: %.0f • %s • %s", analysis.avgLuminance, dirLabel, analysis.recommendedNoiseColor.displayName)
        _isImageNoiseActive.value = true

        audioEngine.imageAudioProfile = analysis.audioProfile
        audioEngine.isImageNoiseEnabled = true
        audioEngine.imageNoiseDepth = _imageNoiseDepth.value
        audioEngine.imageScanSpeedHz = _imageScanSpeedHz.value
    }

    fun setScanDirection(direction: ScanDirection) {
        _scanDirection.value = direction
        val currentBitmap = _scopeBackgroundImage.value
        if (currentBitmap != null) {
            val analysis = ImageSonificationHelper.extractAudioProfile(currentBitmap, direction = direction)
            audioEngine.imageAudioProfile = analysis.audioProfile
            val dirLabel = if (direction == ScanDirection.VERTICAL_WATERFALL) "Waterfall ↓" else "H-Scan →"
            _imageAnalysisInfo.value = String.format("Lum: %.0f • %s • %s", analysis.avgLuminance, dirLabel, analysis.recommendedNoiseColor.displayName)
        }
        _userFeedbackMessage.value = "Image scanning set to ${direction.displayName}"
    }

    fun clearScopeImage() {
        _scopeBackgroundImage.value = null
        _selectedImageName.value = null
        _imageAnalysisInfo.value = null
        _isImageNoiseActive.value = false
        audioEngine.isImageNoiseEnabled = false
        audioEngine.imageAudioProfile = null
        _userFeedbackMessage.value = "Scope wallpaper removed"
    }

    fun setScopeBackgroundOpacity(opacity: Float) {
        _scopeBackgroundOpacity.value = opacity.coerceIn(0.05f, 0.95f)
    }

    fun setImageNoiseActive(enabled: Boolean) {
        _isImageNoiseActive.value = enabled
        audioEngine.isImageNoiseEnabled = enabled
        if (enabled && !_isWhiteNoiseEnabled.value) {
            setWhiteNoiseEnabled(true)
        }
        _userFeedbackMessage.value = if (enabled) "Optical noise modulation enabled" else "Optical noise disabled"
    }

    fun toggleImageNoise() {
        setImageNoiseActive(!_isImageNoiseActive.value)
    }

    fun setImageNoiseDepth(depth: Float) {
        val clamped = depth.coerceIn(0.0f, 1.0f)
        _imageNoiseDepth.value = clamped
        audioEngine.imageNoiseDepth = clamped
    }

    fun setImageScanSpeedHz(speed: Float) {
        val clamped = speed.coerceIn(0.1f, 10.0f)
        _imageScanSpeedHz.value = clamped
        audioEngine.imageScanSpeedHz = clamped
    }

    // Multi-Tone Layering & Harmonic Chord Controls
    fun toggleLayering() {
        setLayeringEnabled(!_isLayeringEnabled.value)
    }

    fun setLayeringEnabled(enabled: Boolean) {
        _isLayeringEnabled.value = enabled
        audioEngine.isLayeringEnabled = enabled
        if (enabled && _activeChordPreset.value == ChordPreset.OFF) {
            selectChordPreset(ChordPreset.POWER_FIFTH)
        }
        _userFeedbackMessage.value = if (enabled) "Multi-tone layering active" else "Multi-tone layering disabled"
    }

    fun selectChordPreset(preset: ChordPreset) {
        _activeChordPreset.value = preset
        audioEngine.applyChordPreset(preset, _carrierFreq.value)
        _isLayeringEnabled.value = (preset != ChordPreset.OFF)
        _layer2Enabled.value = audioEngine.layer2Enabled
        _layer3Enabled.value = audioEngine.layer3Enabled
        _layer2Freq.value = audioEngine.layer2Freq
        _layer3Freq.value = audioEngine.layer3Freq
        _layer2Gain.value = audioEngine.layer2Gain
        _layer3Gain.value = audioEngine.layer3Gain
        _layer2Interval.value = audioEngine.layer2Interval
        _layer3Interval.value = audioEngine.layer3Interval
        _userFeedbackMessage.value = "Chord preset: ${preset.displayName}"
    }

    fun setLayer2Enabled(enabled: Boolean) {
        _layer2Enabled.value = enabled
        audioEngine.layer2Enabled = enabled
    }

    fun setLayer2Freq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _layer2Freq.value = clamped
        audioEngine.layer2Freq = clamped
    }

    fun setLayer2Wave(wave: WaveformType) {
        _layer2Wave.value = wave
        audioEngine.layer2Wave = wave
    }

    fun setLayer2Gain(gain: Float) {
        val clamped = gain.coerceIn(0.0f, 1.0f)
        _layer2Gain.value = clamped
        audioEngine.layer2Gain = clamped
    }

    fun setLayer2Interval(interval: HarmonicInterval) {
        _layer2Interval.value = interval
        audioEngine.layer2Interval = interval
        if (interval != HarmonicInterval.CUSTOM) {
            val calculated = (_carrierFreq.value * interval.ratio).coerceIn(1.0f, 1000.0f)
            _layer2Freq.value = calculated
            audioEngine.layer2Freq = calculated
        }
    }

    fun setLayer3Enabled(enabled: Boolean) {
        _layer3Enabled.value = enabled
        audioEngine.layer3Enabled = enabled
    }

    fun setLayer3Freq(freq: Float) {
        val clamped = freq.coerceIn(1.0f, 1000.0f)
        _layer3Freq.value = clamped
        audioEngine.layer3Freq = clamped
    }

    fun setLayer3Wave(wave: WaveformType) {
        _layer3Wave.value = wave
        audioEngine.layer3Wave = wave
    }

    fun setLayer3Gain(gain: Float) {
        val clamped = gain.coerceIn(0.0f, 1.0f)
        _layer3Gain.value = clamped
        audioEngine.layer3Gain = clamped
    }

    fun setLayer3Interval(interval: HarmonicInterval) {
        _layer3Interval.value = interval
        audioEngine.layer3Interval = interval
        if (interval != HarmonicInterval.CUSTOM) {
            val calculated = (_carrierFreq.value * interval.ratio).coerceIn(1.0f, 1000.0f)
            _layer3Freq.value = calculated
            audioEngine.layer3Freq = calculated
        }
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
        setNoiseColor(preset.getNoiseColorType())
        setLayeringEnabled(preset.isLayeringEnabled)
        selectChordPreset(preset.getChordPresetType())
        setLayer2Freq(preset.layer2Freq)
        setLayer2Gain(preset.layer2Gain)
        setLayer2Wave(preset.getLayer2WaveType())
        setLayer3Freq(preset.layer3Freq)
        setLayer3Gain(preset.layer3Gain)
        setLayer3Wave(preset.getLayer3WaveType())
        setModFreq(preset.modFreq)
        setModIntensity(preset.modIntensity)
        setModWave(preset.getModWaveType())
        setModMode(preset.getModulationMode())
        _userFeedbackMessage.value = "Loaded preset: ${preset.name}"
    }

    fun savePreset(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            presetRepository.saveCustomPreset(
                name = name,
                description = description,
                carrierFreq = _carrierFreq.value,
                carrierWave = _carrierWave.value.name,
                isBinauralEnabled = _isBinauralEnabled.value,
                leftCarrierFreq = _leftCarrierFreq.value,
                rightCarrierFreq = _rightCarrierFreq.value,
                modFreq = _modFreq.value,
                modIntensity = _modIntensity.value,
                modWave = _modWave.value.name,
                modMode = _modMode.value.name,
                isWhiteNoiseEnabled = _isWhiteNoiseEnabled.value,
                whiteNoiseLevel = _whiteNoiseLevel.value,
                noiseColor = _noiseColor.value.name,
                isLayeringEnabled = _isLayeringEnabled.value,
                chordPreset = _activeChordPreset.value.name,
                layer2Freq = _layer2Freq.value,
                layer2Gain = _layer2Gain.value,
                layer2Wave = _layer2Wave.value.name,
                layer3Freq = _layer3Freq.value,
                layer3Gain = _layer3Gain.value,
                layer3Wave = _layer3Wave.value.name,
                sampleRate = _sampleRate.value,
                bitDepth = _bitDepth.value
            )
            _userFeedbackMessage.value = "Preset '$name' saved"
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            presetRepository.deletePreset(preset)
            _userFeedbackMessage.value = "Deleted preset: ${preset.name}"
        }
    }

    fun restoreFactoryPresets() {
        viewModelScope.launch(Dispatchers.IO) {
            presetRepository.restoreFactoryPresets()
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
                    noiseColor = _noiseColor.value,
                    isLayeringEnabled = _isLayeringEnabled.value,
                    layer2Enabled = _layer2Enabled.value,
                    layer2Freq = _layer2Freq.value,
                    layer2Wave = _layer2Wave.value,
                    layer2Gain = _layer2Gain.value,
                    layer3Enabled = _layer3Enabled.value,
                    layer3Freq = _layer3Freq.value,
                    layer3Wave = _layer3Wave.value,
                    layer3Gain = _layer3Gain.value,
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
