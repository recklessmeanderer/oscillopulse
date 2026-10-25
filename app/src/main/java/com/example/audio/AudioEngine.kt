package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.ModulationMode
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

    // Volatile parameters for real-time thread safety
    @Volatile var carrierFreq: Float = 432.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }

    @Volatile var carrierWave: WaveformType = WaveformType.SINE
    @Volatile var carrierPhaseOffsetDeg: Float = 0.0f
        set(value) { field = ((value % 360f) + 360f) % 360f }

    // Binaural Beat configuration (independent Left and Right carriers)
    @Volatile var isBinauralEnabled: Boolean = false
    @Volatile var leftCarrierFreq: Float = 432.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }
    @Volatile var rightCarrierFreq: Float = 472.0f
        set(value) { field = value.coerceIn(1.0f, 1000.0f) }

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
                snapshot[i] = scopeRingBuffer[readIdx % scopeBufferSize]
                readIdx++
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
            val twoPi = 2.0 * Math.PI

            // Reusable byte buffer for writing to recording stream if recording
            val recByteBuffer = ByteBuffer.allocate(bufferSamples * numChannels * 4).order(ByteOrder.LITTLE_ENDIAN)

            while (isActive && _isPlaying.value) {
                val targetVol = if (isMuted) 0.0f else volume
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
                val cPhaseOffsetRad = Math.toRadians(carrierPhaseOffsetDeg.toDouble())

                var maxMagnitude = 0f
                recByteBuffer.clear()

                for (i in 0 until bufferSamples) {
                    val rawCarrierL = cWave.calculate(carrierPhaseL + cPhaseOffsetRad)
                    val rawCarrierR = cWave.calculate(carrierPhaseR + cPhaseOffsetRad)
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

                    val sampleL = (rawCarrierL * envL * targetVol).coerceIn(-1.0f, 1.0f)
                    val sampleR = (rawCarrierR * envR * targetVol).coerceIn(-1.0f, 1.0f)

                    // Average mono sample for oscilloscope visualization
                    val monoScopeSample = (sampleL + sampleR) * 0.5f
                    synchronized(scopeLock) {
                        scopeRingBuffer[scopeWriteIndex % scopeBufferSize] = monoScopeSample
                        scopeWriteIndex++
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
