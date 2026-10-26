package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import com.example.model.NoiseColor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

data class ImageAnalysisResult(
    val audioProfile: FloatArray,
    val recommendedNoiseColor: NoiseColor,
    val avgLuminance: Float,
    val warmCoolBalance: Float // > 1.0 = warm/brown, < 0.9 = cool/white, ~1.0 = pink
)

enum class TexturePreset(val title: String, val subtitle: String, val noiseColor: NoiseColor) {
    COSMIC_NEBULA("Cosmic Nebula", "Deep space starfield & ultraviolet gas", NoiseColor.WHITE),
    OCEAN_SURF("Ocean Surf", "Rhythmic rolling waves & sea spray", NoiseColor.BROWN),
    FOREST_CANOPY("Forest Canopy", "Sunlight filtering through pine trees", NoiseColor.PINK),
    GOLDEN_SUNSET("Golden Sunset", "Warm gradient horizon & earth resonance", NoiseColor.BROWN),
    AURORA_BOREALIS("Aurora Borealis", "Polar geomagnetic light ribbons", NoiseColor.PINK)
}

object ImageSonificationHelper {

    /**
     * Loads a downsampled Bitmap safely from a content Uri using the Android Photo Picker.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 800): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts an optical audio waveform profile (luminance and chromatic balance)
     * by scanning image columns left-to-right like an optical sound film track.
     */
    fun extractAudioProfile(bitmap: Bitmap, profileSize: Int = 2048): ImageAnalysisResult {
        val w = bitmap.width
        val h = bitmap.height
        val profile = FloatArray(profileSize)

        var totalR = 0.0
        var totalG = 0.0
        var totalB = 0.0
        var totalLum = 0.0
        val vSteps = 16

        for (i in 0 until profileSize) {
            val col = ((i.toFloat() / profileSize) * w).toInt().coerceIn(0, w - 1)
            var colLum = 0.0

            for (v in 0 until vSteps) {
                val row = ((v.toFloat() / vSteps) * h).toInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(col, row)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                colLum += lum

                if (i % 16 == 0) {
                    totalR += r
                    totalG += g
                    totalB += b
                    totalLum += lum
                }
            }

            // Normalize column luminance to -1.0 .. +1.0 AC audio range
            val normLum = (colLum / (vSteps * 255.0)).toFloat()
            profile[i] = (normLum * 2.0f) - 1.0f
        }

        val samplePoints = (profileSize / 16) * vSteps
        val avgR = (totalR / samplePoints).toFloat()
        val avgG = (totalG / samplePoints).toFloat()
        val avgB = (totalB / samplePoints).toFloat()
        val avgLum = (totalLum / samplePoints).toFloat()

        val warmCoolRatio = if (avgB > 0f) (avgR + 1f) / (avgB + 1f) else 1.0f

        val recommendedColor = when {
            avgLum > 175f || avgB > avgR * 1.2f -> NoiseColor.WHITE
            warmCoolRatio > 1.25f || avgLum < 85f -> NoiseColor.BROWN
            else -> NoiseColor.PINK
        }

        return ImageAnalysisResult(
            audioProfile = profile,
            recommendedNoiseColor = recommendedColor,
            avgLuminance = avgLum,
            warmCoolBalance = warmCoolRatio
        )
    }

    /**
     * Generates artistic procedural texture bitmaps that sound rich and look stunning as oscilloscope backgrounds.
     */
    fun generatePresetTexture(preset: TexturePreset, width: Int = 640, height: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (preset) {
            TexturePreset.COSMIC_NEBULA -> {
                // Deep space gradient
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(10, 5, 25), Color.rgb(35, 10, 65), Color.rgb(12, 28, 56)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Glowing radial nebula cloud
                paint.shader = RadialGradient(
                    width * 0.45f, height * 0.5f, width * 0.4f,
                    intArrayOf(Color.argb(120, 168, 85, 247), Color.argb(60, 56, 189, 248), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawCircle(width * 0.45f, height * 0.5f, width * 0.4f, paint)

                // Star specks for granular high-frequency optical texture
                val rng = java.util.Random(1337)
                paint.shader = null
                paint.color = Color.WHITE
                for (s in 0 until 180) {
                    val sx = rng.nextFloat() * width
                    val sy = rng.nextFloat() * height
                    val r = rng.nextFloat() * 1.8f + 0.5f
                    paint.alpha = rng.nextInt(180) + 75
                    canvas.drawCircle(sx, sy, r, paint)
                }
            }

            TexturePreset.OCEAN_SURF -> {
                // Ocean depth gradient
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(15, 32, 67), Color.rgb(20, 60, 85), Color.rgb(10, 20, 35)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Rolling wave bands
                paint.shader = null
                for (wIdx in 0..4) {
                    val waveY = height * (0.35f + wIdx * 0.14f)
                    paint.color = Color.argb(45 - wIdx * 6, 56, 189, 248)
                    for (x in 0 until width step 8) {
                        val waveOffset = (sin(x * 0.015f + wIdx * 1.2f) * 16f)
                        canvas.drawCircle(x.toFloat(), waveY + waveOffset, 20f, paint)
                    }
                }
            }

            TexturePreset.FOREST_CANOPY -> {
                // Emerald canopy gradient
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(12, 36, 24), Color.rgb(22, 65, 40), Color.rgb(8, 24, 16)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Sun ray streaks
                paint.shader = LinearGradient(
                    width * 0.2f, 0f, width * 0.8f, height.toFloat(),
                    intArrayOf(Color.argb(80, 250, 204, 21), Color.argb(20, 34, 197, 94), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            TexturePreset.GOLDEN_SUNSET -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(75, 20, 45), Color.rgb(180, 70, 30), Color.rgb(230, 140, 40), Color.rgb(30, 15, 20)),
                    floatArrayOf(0f, 0.35f, 0.65f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            TexturePreset.AURORA_BOREALIS -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(5, 10, 25), Color.rgb(15, 30, 45), Color.rgb(8, 16, 28)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Luminous aurora wave ribbon
                paint.shader = LinearGradient(
                    0f, height * 0.2f, width.toFloat(), height * 0.7f,
                    intArrayOf(Color.argb(120, 34, 197, 94), Color.argb(100, 56, 189, 248), Color.argb(80, 168, 85, 247)),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
                for (x in 0 until width step 6) {
                    val y1 = height * 0.35f + (sin(x * 0.02f) * 28f)
                    val y2 = y1 + 55f
                    canvas.drawLine(x.toFloat(), y1, x.toFloat(), y2, paint)
                }
            }
        }

        return bitmap
    }
}
