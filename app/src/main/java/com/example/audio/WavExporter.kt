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
        carrierPhaseOffsetDeg: Float = 0f,
        isBinauralEnabled: Boolean = false,
        leftCarrierFreq: Float = 432f,
        rightCarrierFreq: Float = 472f,
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
        val phaseOffsetRad = Math.toRadians(carrierPhaseOffsetDeg.toDouble())

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
                    val rawCarrierL = carrierWave.calculate(carrierPhaseL + phaseOffsetRad)
                    val rawCarrierR = carrierWave.calculate(carrierPhaseR + phaseOffsetRad)
                    val rawMod = modWave.calculate(modPhase)

                    val (envL, envR) = if (isBinauralEnabled) Pair(1.0f, 1.0f) else calculateEnvelope(rawMod, modIntensity, modMode)

                    val sampleL = (rawCarrierL * envL * volume).coerceIn(-1.0f, 1.0f)
                    val sampleR = (rawCarrierR * envR * volume).coerceIn(-1.0f, 1.0f)

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
