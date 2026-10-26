package com.example.audio

import com.example.model.ModulationMode
import com.example.model.WaveformType
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavExporter {

    /**
     * Renders synthesized audio offline to a clean WAV file for the specified duration.
     */
    fun renderWav(
        outputFile: File,
        carrierFreq: Float,
        carrierWave: WaveformType,
        modFreq: Float,
        modIntensity: Float,
        modWave: WaveformType,
        modMode: ModulationMode,
        isBinauralEnabled: Boolean = false,
        leftCarrierFreq: Float = 432f,
        rightCarrierFreq: Float = 472f,
        isWhiteNoiseEnabled: Boolean = false,
        whiteNoiseLevel: Float = 0.15f,
        noiseColor: com.example.model.NoiseColor = com.example.model.NoiseColor.WHITE,
        isLayeringEnabled: Boolean = false,
        layer2Enabled: Boolean = false,
        layer2Freq: Float = 648f,
        layer2Wave: WaveformType = WaveformType.SINE,
        layer2Gain: Float = 0.45f,
        layer3Enabled: Boolean = false,
        layer3Freq: Float = 864f,
        layer3Wave: WaveformType = WaveformType.SINE,
        layer3Gain: Float = 0.35f,
        durationSeconds: Float,
        sampleRate: Int,
        bitDepth: Int,
        volume: Float = 0.8f,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        val numChannels = 2 // Stereo
        val totalSamplesPerChannel = (sampleRate * durationSeconds).toInt()
        val bytesPerSample = bitDepth / 8
        val blockAlign = numChannels * bytesPerSample
        val dataChunkSize = totalSamplesPerChannel * blockAlign

        var carrierPhaseL = 0.0
        var carrierPhaseR = 0.0
        var modPhase = 0.0
        var layer2Phase = 0.0
        var layer3Phase = 0.0

        // Noise filter states
        var p0L = 0f; var p1L = 0f; var p2L = 0f; var p3L = 0f; var p4L = 0f; var p5L = 0f; var p6L = 0f
        var p0R = 0f; var p1R = 0f; var p2R = 0f; var p3R = 0f; var p4R = 0f; var p5R = 0f; var p6R = 0f
        var brownL = 0f
        var brownR = 0f

        val twoPi = 2.0 * Math.PI

        val (carrierFreqL, carrierFreqR) = if (isBinauralEnabled) {
            Pair(leftCarrierFreq.toDouble().coerceAtLeast(0.1), rightCarrierFreq.toDouble().coerceAtLeast(0.1))
        } else if (modMode == ModulationMode.BINAURAL_BEAT) {
            val halfMod = (modFreq / 2.0).toDouble()
            Pair((carrierFreq - halfMod).coerceAtLeast(0.1), (carrierFreq + halfMod).coerceAtLeast(0.1))
        } else {
            Pair(carrierFreq.toDouble(), carrierFreq.toDouble())
        }

        val phaseIncCL = (twoPi * carrierFreqL) / sampleRate
        val phaseIncCR = (twoPi * carrierFreqR) / sampleRate
        val phaseIncM = (twoPi * modFreq.toDouble()) / sampleRate

        val phaseIncL2 = (twoPi * layer2Freq.toDouble()) / sampleRate
        val phaseIncL3 = (twoPi * layer3Freq.toDouble()) / sampleRate
        val rng = java.util.Random()

        FileOutputStream(outputFile).use { fos ->
            // Write placeholder 44-byte WAV header
            val header = createWavHeader(
                sampleRate = sampleRate,
                numChannels = numChannels,
                bitDepth = bitDepth,
                totalAudioLen = dataChunkSize.toLong()
            )
            fos.write(header)

            val bufferSize = 4096
            val byteBuffer = ByteBuffer.allocate(bufferSize * blockAlign).order(ByteOrder.LITTLE_ENDIAN)

            var written = 0
            while (written < totalSamplesPerChannel) {
                val chunkSize = minOf(bufferSize, totalSamplesPerChannel - written)
                byteBuffer.clear()

                for (i in 0 until chunkSize) {
                    var rawCarrierL = carrierWave.calculate(carrierPhaseL)
                    var rawCarrierR = carrierWave.calculate(carrierPhaseR)

                    var totalCarrierGain = 1.0f
                    if (isLayeringEnabled) {
                        if (layer2Enabled && layer2Gain > 0f) {
                            val s2 = layer2Wave.calculate(layer2Phase) * layer2Gain
                            rawCarrierL += s2
                            rawCarrierR += s2
                            totalCarrierGain += layer2Gain
                            layer2Phase = (layer2Phase + phaseIncL2) % twoPi
                        }
                        if (layer3Enabled && layer3Gain > 0f) {
                            val s3 = layer3Wave.calculate(layer3Phase) * layer3Gain
                            rawCarrierL += s3
                            rawCarrierR += s3
                            totalCarrierGain += layer3Gain
                            layer3Phase = (layer3Phase + phaseIncL3) % twoPi
                        }
                    }

                    val carrierNorm = if (totalCarrierGain > 1.0f) 1.0f / totalCarrierGain else 1.0f
                    val carrierSigL = rawCarrierL * carrierNorm
                    val carrierSigR = rawCarrierR * carrierNorm

                    val rawMod = modWave.calculate(modPhase)

                    val (envL, envR) = if (isBinauralEnabled) Pair(1.0f, 1.0f) else calculateEnvelope(rawMod, modIntensity, modMode)

                    var sL = carrierSigL * envL * volume
                    var sR = carrierSigR * envR * volume

                    if (isWhiteNoiseEnabled && whiteNoiseLevel > 0f) {
                        val whiteL = rng.nextFloat() * 2f - 1f
                        val whiteR = rng.nextFloat() * 2f - 1f

                        val (noiseSampleL, noiseSampleR) = when (noiseColor) {
                            com.example.model.NoiseColor.WHITE -> Pair(whiteL, whiteR)
                            com.example.model.NoiseColor.PINK -> {
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
                            com.example.model.NoiseColor.BROWN -> {
                                brownL = (brownL + (0.025f * whiteL)) / 1.02f
                                brownR = (brownR + (0.025f * whiteR)) / 1.02f
                                Pair(brownL * 3.5f, brownR * 3.5f)
                            }
                        }

                        sL += noiseSampleL * whiteNoiseLevel * volume
                        sR += noiseSampleR * whiteNoiseLevel * volume
                    }

                    val sampleL = sL.coerceIn(-1.0f, 1.0f)
                    val sampleR = sR.coerceIn(-1.0f, 1.0f)

                    writeSample(byteBuffer, sampleL, bitDepth)
                    writeSample(byteBuffer, sampleR, bitDepth)

                    carrierPhaseL = (carrierPhaseL + phaseIncCL) % twoPi
                    carrierPhaseR = (carrierPhaseR + phaseIncCR) % twoPi
                    modPhase = (modPhase + phaseIncM) % twoPi
                }

                fos.write(byteBuffer.array(), 0, chunkSize * blockAlign)
                written += chunkSize

                if (totalSamplesPerChannel > 0) {
                    onProgress(written.toFloat() / totalSamplesPerChannel.toFloat())
                }
            }
        }
        return true
    }

    /**
     * Finalizes raw PCM stream into a standard WAV audio file with RIFF header.
     */
    fun finalizeRawPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        numChannels: Int = 2,
        bitDepth: Int = 16
    ): Boolean {
        if (!pcmFile.exists() || pcmFile.length() == 0L) return false

        val rawPcmLength = pcmFile.length()
        val header = createWavHeader(
            sampleRate = sampleRate,
            numChannels = numChannels,
            bitDepth = bitDepth,
            totalAudioLen = rawPcmLength
        )

        FileOutputStream(wavFile).use { fos ->
            fos.write(header)
            pcmFile.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                }
            }
        }
        return true
    }

    private fun calculateEnvelope(
        rawMod: Float,
        modIntensity: Float,
        modMode: ModulationMode
    ): Pair<Float, Float> {
        return when (modMode) {
            ModulationMode.ISOCHRONIC_PULSE -> {
                // Pulse envelope: 1 - intensity at bottom, 1 at peak
                val unipolarMod = (rawMod + 1.0f) * 0.5f // 0 to 1
                val env = (1.0f - modIntensity) + (modIntensity * unipolarMod)
                Pair(env, env)
            }
            ModulationMode.AMPLITUDE_MODULATION -> {
                // Classic AM depth: 1 + m * intensity
                val env = (1.0f - modIntensity * 0.5f) + (rawMod * modIntensity * 0.5f)
                Pair(env.coerceIn(0.0f, 1.0f), env.coerceIn(0.0f, 1.0f))
            }
            ModulationMode.BINAURAL_BEAT -> {
                // Binaural relies on carrier frequency difference in left & right ear
                Pair(1.0f, 1.0f)
            }
        }
    }

    fun writeSample(byteBuffer: ByteBuffer, sample: Float, bitDepth: Int) {
        when (bitDepth) {
            16 -> {
                val pcm16 = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(pcm16)
            }
            24 -> {
                val pcm24 = (sample * 8388607.0f).toInt().coerceIn(-8388608, 8388607)
                byteBuffer.put((pcm24 and 0xFF).toByte())
                byteBuffer.put(((pcm24 shr 8) and 0xFF).toByte())
                byteBuffer.put(((pcm24 shr 16) and 0xFF).toByte())
            }
            32 -> {
                // IEEE 754 Float
                byteBuffer.putFloat(sample)
            }
            else -> {
                val pcm16 = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(pcm16)
            }
        }
    }

    fun createWavHeader(
        sampleRate: Int,
        numChannels: Int,
        bitDepth: Int,
        totalAudioLen: Long
    ): ByteArray {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * numChannels * (bitDepth / 8)
        val blockAlign = numChannels * (bitDepth / 8)
        val audioFormat: Short = if (bitDepth == 32) 3 else 1 // 1 = PCM, 3 = IEEE Float

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        bb.put('R'.code.toByte())
        bb.put('I'.code.toByte())
        bb.put('F'.code.toByte())
        bb.put('F'.code.toByte())
        bb.putInt(totalDataLen.toInt())
        bb.put('W'.code.toByte())
        bb.put('A'.code.toByte())
        bb.put('V'.code.toByte())
        bb.put('E'.code.toByte())

        // "fmt " sub-chunk
        bb.put('f'.code.toByte())
        bb.put('m'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put(' '.code.toByte())
        bb.putInt(16) // SubChunk1Size (16 for PCM)
        bb.putShort(audioFormat)
        bb.putShort(numChannels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign.toShort())
        bb.putShort(bitDepth.toShort())

        // "data" sub-chunk
        bb.put('d'.code.toByte())
        bb.put('a'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put('a'.code.toByte())
        bb.putInt(totalAudioLen.toInt())

        return header
    }
}
