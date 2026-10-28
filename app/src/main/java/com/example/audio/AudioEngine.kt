package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.ChordPreset
import com.example.model.HarmonicInterval
import com.example.model.ModulationMode
import com.example.model.NoiseColor
import com.example.model.WaveformType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

class AudioEngine(private val filesDir: File) {

    companion object {
        @Volatile
        private var instance: AudioEngine? = null

        fun getInstance(context: android.content.Context): AudioEngine {
            return instance ?: synchronized(this) {
                instance ?: AudioEngine(context.applicationContext.filesDir).also { instance = it }
            }
        }

        fun getInstance(filesDir: File): AudioEngine {
            return instance ?: synchronized(this) {
                instance ?: AudioEngine(filesDir).also { instance = it }
            }
        }
    }

    fun getSummaryDescription(): String {
        val baseDesc = if (isBinauralEnabled) {
            val beat = kotlin.math.abs(rightCarrierFreq - leftCarrierFreq)
            "Binaural ${leftCarrierFreq.toInt()}Hz / ${rightCarrierFreq.toInt()}Hz (${beat.toInt()}Hz beat)"
        } else {
            val carrierDesc = "${carrierFreq.toInt()} Hz ${carrierWave.name.lowercase().replaceFirstChar { it.uppercase() }}"
            val modDesc = "${modFreq.toInt()} Hz ${modMode.displayName}"
            "$carrierDesc • $modDesc"
        }
        val layerDesc = if (isLayeringEnabled && (layer2Enabled || layer3Enabled)) " • ${activeChordPreset.displayName}" else ""
        val noiseDesc = if (isWhiteNoiseEnabled && whiteNoiseLevel > 0f) " • ${noiseColor.displayName}" else ""
        return "$baseDesc$layerDesc$noiseDesc"
    }

    // Volatile parameters for real-time thread safety
    @Volatile var carrierFreq: Float = 432.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }

    @Volatile var carrierWave: WaveformType = WaveformType.SINE

    // Binaural Beat configuration (independent Left and Right carriers)
    @Volatile var isBinauralEnabled: Boolean = false
    @Volatile var leftCarrierFreq: Float = 432.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }
    @Volatile var rightCarrierFreq: Float = 472.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }

    @Volatile var isWhiteNoiseEnabled: Boolean = false
    @Volatile var whiteNoiseLevel: Float = 0.15f // 0.0f to 1.0f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }
    @Volatile var noiseColor: NoiseColor = NoiseColor.WHITE

    // Image Optical Noise Sonification
    @Volatile var isImageNoiseEnabled: Boolean = false
    @Volatile var imageNoiseDepth: Float = 0.5f // 0.0 to 1.0 modulation intensity
        set(value) { field = value.coerceIn(0.0f, 1.0f) }
    @Volatile var imageScanSpeedHz: Float = 1.0f // 0.1 to 10.0 Hz scan rate
        set(value) { field = value.coerceIn(0.1f, 10.0f) }
    @Volatile var imageAudioProfile: FloatArray? = null

    private val _opticalScanProgress = MutableStateFlow(0f)
    val opticalScanProgress = _opticalScanProgress.asStateFlow()

    // Audio Focus Ducking
    @Volatile var isDuckingEnabled: Boolean = true
    @Volatile var duckTarget: Float = 1.0f // 1.0f full volume, 0.25f when ducked
    private var smoothedDuckFactor: Float = 1.0f

    // Multi-Tone Layering & Harmonic Chords
    @Volatile var isLayeringEnabled: Boolean = false
    @Volatile var activeChordPreset: ChordPreset = ChordPreset.OFF

    @Volatile var layer2Enabled: Boolean = true
    @Volatile var layer2Freq: Float = 648.0f // 432 * 1.5 (Perfect 5th)
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }
    @Volatile var layer2Wave: WaveformType = WaveformType.SINE
    @Volatile var layer2Gain: Float = 0.45f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }
    @Volatile var layer2Interval: HarmonicInterval = HarmonicInterval.PERFECT_FIFTH

    @Volatile var layer3Enabled: Boolean = true
    @Volatile var layer3Freq: Float = 864.0f // 432 * 2.0 (Octave)
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }
    @Volatile var layer3Wave: WaveformType = WaveformType.SINE
    @Volatile var layer3Gain: Float = 0.35f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }
    @Volatile var layer3Interval: HarmonicInterval = HarmonicInterval.OCTAVE_UP

    fun applyChordPreset(preset: ChordPreset, baseFreq: Float = carrierFreq) {
        activeChordPreset = preset
        if (preset == ChordPreset.OFF) {
            isLayeringEnabled = false
            return
        }
        isLayeringEnabled = true
        layer2Enabled = true
        layer3Enabled = true
        layer2Interval = preset.layer2Interval
        layer3Interval = preset.layer3Interval
        layer2Gain = preset.layer2Gain
        layer3Gain = preset.layer3Gain

        if (preset == ChordPreset.SOLFEGGIO_TRIAD) {
            layer2Freq = 528.0f
            layer3Freq = 639.0f
        } else {
            layer2Freq = (baseFreq * preset.layer2Interval.ratio).coerceIn(1.0f, 1000.0f)
            layer3Freq = (baseFreq * preset.layer3Interval.ratio).coerceIn(1.0f, 1000.0f)
        }
    }

    fun syncLayerFrequenciesToBase(baseFreq: Float) {
        if (!isLayeringEnabled) return
        if (layer2Interval != HarmonicInterval.CUSTOM) {
            layer2Freq = (baseFreq * layer2Interval.ratio).coerceIn(1.0f, 1000.0f)
        }
        if (layer3Interval != HarmonicInterval.CUSTOM) {
            layer3Freq = (baseFreq * layer3Interval.ratio).coerceIn(1.0f, 1000.0f)
        }
    }

    @Volatile var modFreq: Float = 40.0f
        set(value) { field = value.coerceIn(0.1f, 200.0f) }

    @Volatile var modIntensity: Float = 1.0f // 0.0 to 1.0 (0% to 100%)
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    @Volatile var modWave: WaveformType = WaveformType.SQUARE
    @Volatile var modMode: ModulationMode = ModulationMode.ISOCHRONIC_PULSE

    @Volatile var volume: Float = 0.5f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    @Volatile var isMuted: Boolean = false

    @Volatile var sampleRate: Int = 44100
    @Volatile var bitDepth: Int = 16

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs = _recordingDurationMs.asStateFlow()

    private val _peakLevel = MutableStateFlow(0f)
    val peakLevel = _peakLevel.asStateFlow()

    // Circular buffer for oscilloscope rendering (displaying 1024 latest samples)
    val scopeBufferSize = 1024
    private val scopeRingBuffer = FloatArray(scopeBufferSize)
    private var scopeWriteIndex = 0
    private val scopeLock = Any()

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Recording state
    private var recordingPcmFile: File? = null
    private var recordingOutputStream: BufferedOutputStream? = null
    private var recordingStartTime = 0L
    private var recordingBytesWritten = 0L

    fun getOscilloscopeSnapshot(): FloatArray {
        synchronized(scopeLock) {
            val snapshot = FloatArray(scopeBufferSize)
            var readIdx = scopeWriteIndex
            for (i in 0 until scopeBufferSize) {
                snapshot[i] = scopeRingBuffer[readIdx]
                readIdx = (readIdx + 1) % scopeBufferSize
            }
            return snapshot
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        audioJob?.cancel()
        audioJob = scope.launch(Dispatchers.IO) {
            runAudioLoop()
        }
    }

    fun stop() {
        if (_isRecording.value) {
            stopRecording()
        }
        _isPlaying.value = false
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null
        _peakLevel.value = 0f
    }

    fun toggleRecording(onCompleted: (File?) -> Unit = {}) {
        if (_isRecording.value) {
            val wavFile = stopRecording()
            onCompleted(wavFile)
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        if (_isRecording.value) return

        val recDir = File(filesDir, "recordings")
        if (!recDir.exists()) recDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val tempPcm = File(recDir, "rec_temp_$timestamp.pcm")
        recordingPcmFile = tempPcm
        recordingOutputStream = BufferedOutputStream(FileOutputStream(tempPcm))
        recordingStartTime = System.currentTimeMillis()
        recordingBytesWritten = 0L
        _recordingDurationMs.value = 0L
        _isRecording.value = true

        // Ensure playback is active so recording captures the live waveform
        if (!_isPlaying.value) {
            start()
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        _isRecording.value = false

        try {
            recordingOutputStream?.flush()
            recordingOutputStream?.close()
        } catch (_: Exception) {}
        recordingOutputStream = null

        val pcm = recordingPcmFile
        if (pcm == null || !pcm.exists() || pcm.length() == 0L) {
            return null
        }

        val recDir = File(filesDir, "recordings")
        val timestamp = System.currentTimeMillis()
        val carrierPart = carrierFreq.toInt()
        val modPart = modFreq.toInt()
        val finalWavFile = File(recDir, "wave_${carrierPart}Hz_${modPart}Hz_$timestamp.wav")

        val success = WavExporter.finalizeRawPcmToWav(
            pcmFile = pcm,
            wavFile = finalWavFile,
            sampleRate = sampleRate,
            numChannels = 2,
            bitDepth = bitDepth
        )
        pcm.delete()

        return if (success) finalWavFile else null
    }

    private fun CoroutineScope.runAudioLoop() {
        val currentSampleRate = sampleRate
        val currentBitDepth = bitDepth
        val numChannels = 2

        val minBufSize = AudioTrack.getMinBufferSize(
            currentSampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSamples = max(minBufSize / 4, 1024)
        val shortBuffer = ShortArray(bufferSamples * numChannels)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(currentSampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSamples * numChannels * 2 * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            var carrierPhaseL = 0.0
            var carrierPhaseR = 0.0
            var modPhase = 0.0
            var layer2Phase = 0.0
            var layer3Phase = 0.0
            var opticalScanPhase = 0.0

            // Pink noise Paul Kellet filter state
            var p0L = 0f; var p1L = 0f; var p2L = 0f; var p3L = 0f; var p4L = 0f; var p5L = 0f; var p6L = 0f
            var p0R = 0f; var p1R = 0f; var p2R = 0f; var p3R = 0f; var p4R = 0f; var p5R = 0f; var p6R = 0f

            // Brown noise leaky integrator state
            var brownL = 0f
            var brownR = 0f

            val twoPi = 2.0 * Math.PI

            // Reusable byte buffer for writing to recording stream if recording
            val recByteBuffer = ByteBuffer.allocate(bufferSamples * numChannels * 4).order(ByteOrder.LITTLE_ENDIAN)

            while (isActive && _isPlaying.value) {
                // Smoothly interpolate duck factor
                val targetDuck = if (isDuckingEnabled) duckTarget else 1.0f
                smoothedDuckFactor += (targetDuck - smoothedDuckFactor) * 0.15f
                val effectiveVol = (if (isMuted) 0.0f else volume) * smoothedDuckFactor

                val isImgNoise = isImageNoiseEnabled
                val imgProfile = imageAudioProfile
                val imgDepth = imageNoiseDepth
                val scanSpeed = imageScanSpeedHz
                val scanPhaseInc = if (isImgNoise && imgProfile != null) (scanSpeed.toDouble() / currentSampleRate) else 0.0

                val cWave = carrierWave
                val mWave = modWave
                val mMode = modMode
                val cFreq = carrierFreq
                val mFreq = modFreq
                val mInt = modIntensity

                val (freqL, freqR) = if (isBinauralEnabled) {
                    Pair(leftCarrierFreq.toDouble().coerceAtLeast(0.1), rightCarrierFreq.toDouble().coerceAtLeast(0.1))
                } else if (mMode == ModulationMode.BINAURAL_BEAT) {
                    val half = (mFreq / 2.0).toDouble()
                    Pair((cFreq - half).coerceAtLeast(0.1), (cFreq + half).coerceAtLeast(0.1))
                } else {
                    Pair(cFreq.toDouble(), cFreq.toDouble())
                }

                val phaseIncCL = (twoPi * freqL) / currentSampleRate
                val phaseIncCR = (twoPi * freqR) / currentSampleRate
                val phaseIncM = (twoPi * mFreq.toDouble()) / currentSampleRate

                // Layering phase increments
                val layeringOn = isLayeringEnabled
                val l2On = layer2Enabled
                val l2Freq = layer2Freq
                val l2Wave = layer2Wave
                val l2Gain = layer2Gain
                val phaseIncL2 = (twoPi * l2Freq.toDouble()) / currentSampleRate

                val l3On = layer3Enabled
                val l3Freq = layer3Freq
                val l3Wave = layer3Wave
                val l3Gain = layer3Gain
                val phaseIncL3 = (twoPi * l3Freq.toDouble()) / currentSampleRate

                val noiseEnabled = isWhiteNoiseEnabled
                val noiseLvl = whiteNoiseLevel
                val nColor = noiseColor
                val rng = java.util.Random()

                var maxMagnitude = 0f
                recByteBuffer.clear()

                for (i in 0 until bufferSamples) {
                    var rawCarrierL = cWave.calculate(carrierPhaseL)
                    var rawCarrierR = cWave.calculate(carrierPhaseR)

                    var totalCarrierGain = 1.0f
                    if (layeringOn) {
                        if (l2On && l2Gain > 0f) {
                            val sampleL2 = l2Wave.calculate(layer2Phase) * l2Gain
                            rawCarrierL += sampleL2
                            rawCarrierR += sampleL2
                            totalCarrierGain += l2Gain
                            layer2Phase = (layer2Phase + phaseIncL2) % twoPi
                        }
                        if (l3On && l3Gain > 0f) {
                            val sampleL3 = l3Wave.calculate(layer3Phase) * l3Gain
                            rawCarrierL += sampleL3
                            rawCarrierR += sampleL3
                            totalCarrierGain += l3Gain
                            layer3Phase = (layer3Phase + phaseIncL3) % twoPi
                        }
                    }

                    val carrierNorm = if (totalCarrierGain > 1.0f) 1.0f / totalCarrierGain else 1.0f
                    val carrierSigL = rawCarrierL * carrierNorm
                    val carrierSigR = rawCarrierR * carrierNorm

                    val rawMod = mWave.calculate(modPhase)

                    val (envL, envR) = when {
                        isBinauralEnabled -> Pair(1.0f, 1.0f)
                        mMode == ModulationMode.ISOCHRONIC_PULSE -> {
                            val unipolarMod = (rawMod + 1.0f) * 0.5f
                            val env = (1.0f - mInt) + (mInt * unipolarMod)
                            Pair(env, env)
                        }
                        mMode == ModulationMode.AMPLITUDE_MODULATION -> {
                            val env = (1.0f - mInt * 0.5f) + (rawMod * mInt * 0.5f)
                            Pair(env.coerceIn(0.0f, 1.0f), env.coerceIn(0.0f, 1.0f))
                        }
                        else -> Pair(1.0f, 1.0f)
                    }

                    var sL = carrierSigL * envL * effectiveVol
                    var sR = carrierSigR * envR * effectiveVol

                    if (noiseEnabled && noiseLvl > 0f) {
                        val whiteL = rng.nextFloat() * 2f - 1f
                        val whiteR = rng.nextFloat() * 2f - 1f

                        val (noiseSampleL, noiseSampleR) = when (nColor) {
                            NoiseColor.WHITE -> Pair(whiteL, whiteR)
                            NoiseColor.PINK -> {
                                p0L = 0.99886f * p0L + whiteL * 0.0555179f
                                p1L = 0.99332f * p1L + whiteL * 0.0750759f
                                p2L = 0.96900f * p2L + whiteL * 0.1538520f
                                p3L = 0.86650f * p3L + whiteL * 0.3104856f
                                p4L = 0.55000f * p4L + whiteL * 0.5329522f
                                p5L = -0.7616f * p5L - whiteL * 0.0168980f
                                val pinkL = (p0L + p1L + p2L + p3L + p4L + p5L + p6L + whiteL * 0.5362f) * 0.11f
                                p6L = whiteL * 0.115926f

                                p0R = 0.99886f * p0R + whiteR * 0.0555179f
                                p1R = 0.99332f * p1R + whiteR * 0.0750759f
                                p2R = 0.96900f * p2R + whiteR * 0.1538520f
                                p3R = 0.86650f * p3R + whiteR * 0.3104856f
                                p4R = 0.55000f * p4R + whiteR * 0.5329522f
                                p5R = -0.7616f * p5R - whiteR * 0.0168980f
                                val pinkR = (p0R + p1R + p2R + p3R + p4R + p5R + p6R + whiteR * 0.5362f) * 0.11f
                                p6R = whiteR * 0.115926f

                                Pair(pinkL * 1.5f, pinkR * 1.5f)
                            }
                            NoiseColor.BROWN -> {
                                brownL = (brownL + (0.025f * whiteL)) / 1.02f
                                brownR = (brownR + (0.025f * whiteR)) / 1.02f
                                Pair(brownL * 3.5f, brownR * 3.5f)
                            }
                        }

                        // Optical image modulation if enabled
                        val opticalMod = if (isImgNoise && imgProfile != null && imgProfile.isNotEmpty()) {
                            val profIdx = ((opticalScanPhase % 1.0) * (imgProfile.size - 1)).toInt().coerceIn(0, imgProfile.size - 1)
                            val lum = imgProfile[profIdx] // 0.0f to 1.0f
                            // Modulation factor: depth modulates volume based on column luminance
                            (1.0f - imgDepth * 0.7f) + (lum * imgDepth * 1.4f)
                        } else {
                            1.0f
                        }

                        sL += noiseSampleL * noiseLvl * opticalMod * effectiveVol
                        sR += noiseSampleR * noiseLvl * opticalMod * effectiveVol
                    }

                    if (isImgNoise && imgProfile != null) {
                        opticalScanPhase = (opticalScanPhase + scanPhaseInc) % 1.0
                    }

                    val sampleL = sL.coerceIn(-1.0f, 1.0f)
                    val sampleR = sR.coerceIn(-1.0f, 1.0f)

                    // Average mono sample for oscilloscope visualization
                    val monoScopeSample = (sampleL + sampleR) * 0.5f
                    synchronized(scopeLock) {
                        scopeRingBuffer[scopeWriteIndex] = monoScopeSample
                        scopeWriteIndex = (scopeWriteIndex + 1) % scopeBufferSize
                    }

                    val mag = max(abs(sampleL), abs(sampleR))
                    if (mag > maxMagnitude) maxMagnitude = mag

                    // 16-bit PCM for live AudioTrack
                    val pcmL = (sampleL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    val pcmR = (sampleR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()

                    shortBuffer[i * 2] = pcmL
                    shortBuffer[i * 2 + 1] = pcmR

                    if (_isRecording.value) {
                        WavExporter.writeSample(recByteBuffer, sampleL, currentBitDepth)
                        WavExporter.writeSample(recByteBuffer, sampleR, currentBitDepth)
                    }

                    carrierPhaseL = (carrierPhaseL + phaseIncCL) % twoPi
                    carrierPhaseR = (carrierPhaseR + phaseIncCR) % twoPi
                    modPhase = (modPhase + phaseIncM) % twoPi
                }

                _peakLevel.value = maxMagnitude
                if (isImgNoise) {
                    _opticalScanProgress.value = (opticalScanPhase % 1.0).toFloat()
                }

                // Write to live audio output
                track.write(shortBuffer, 0, bufferSamples * numChannels)

                // Write to recording stream if enabled
                if (_isRecording.value && recordingOutputStream != null) {
                    val bytesToWrite = bufferSamples * numChannels * (currentBitDepth / 8)
                    recordingOutputStream?.write(recByteBuffer.array(), 0, bytesToWrite)
                    recordingBytesWritten += bytesToWrite
                    val elapsed = System.currentTimeMillis() - recordingStartTime
                    _recordingDurationMs.value = elapsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null
            _isPlaying.value = false
        }
    }
}
