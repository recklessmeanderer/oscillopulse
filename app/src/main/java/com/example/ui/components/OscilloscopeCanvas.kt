package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScopeTheme
import kotlin.math.abs
import kotlin.math.sqrt

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Composable
fun OscilloscopeCanvas(
    samples: FloatArray,
    isPlaying: Boolean,
    isRecording: Boolean,
    recordingDurationMs: Long,
    carrierFreq: Float,
    modFreq: Float,
    scopeTheme: ScopeTheme,
    isBinauralEnabled: Boolean = false,
    leftCarrierFreq: Float = 0f,
    rightCarrierFreq: Float = 0f,
    isCollapsed: Boolean = false,
    backgroundImage: Bitmap? = null,
    backgroundOpacity: Float = 0.35f,
    isImageNoiseActive: Boolean = false,
    opticalScanProgress: Float = 0f,
    onOpenImageNoiseDialog: (() -> Unit)? = null,
    onToggleCollapse: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(backgroundImage) { backgroundImage?.asImageBitmap() }
    var internalCollapsed by remember { mutableStateOf(isCollapsed) }
    val effectiveCollapsed = if (onToggleCollapse != null) isCollapsed else internalCollapsed
    val toggleCollapse: () -> Unit = {
        if (onToggleCollapse != null) onToggleCollapse()
        else internalCollapsed = !internalCollapsed
    }

    var timebaseZoom by remember { mutableStateOf(2f) } // 1x, 2f, 4f
    var amplitudeGain by remember { mutableStateOf(1.0f) }
    var triggerEnabled by remember { mutableStateOf(true) }
    var isFrozen by remember { mutableStateOf(false) }

    // Pulsing animation for Recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )

    val (traceColor, glowColor) = when (scopeTheme) {
        ScopeTheme.CYAN -> Pair(Color(0xFF38BDF8), Color(0x5538BDF8))
        ScopeTheme.GREEN -> Pair(Color(0xFF22C55E), Color(0x5522C55E))
        ScopeTheme.AMBER -> Pair(Color(0xFFFBBF24), Color(0x55FBBF24))
        ScopeTheme.WHITE -> Pair(Color(0xFFE2E8F0), Color(0x55E2E8F0))
    }

    val gridColor = Color(0xFF1E293B)
    val gridSubColor = Color(0xFF0F172A)

    // Calculate real-time measurements from current sample window
    val (vpp, rms) = remember(samples) {
        if (samples.isEmpty()) Pair(0f, 0f)
        else {
            var minV = Float.MAX_VALUE
            var maxV = -Float.MAX_VALUE
            var sumSq = 0.0
            for (s in samples) {
                if (s < minV) minV = s
                if (s > maxV) maxV = s
                sumSq += (s * s)
            }
            val calculatedVpp = if (minV > maxV) 0f else (maxV - minV)
            val calculatedRms = sqrt(sumSq / samples.size).toFloat()
            Pair(calculatedVpp, calculatedRms)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("oscilloscope_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF060913)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isRecording) Color(0xFFEF4444) else Color(0xFF1E293B)),
            width = if (isRecording) 1.5.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Oscilloscope Header Bar (Tap header to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { toggleCollapse() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "OSCILLOSCOPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = traceColor
                    )

                    // Trigger badge
                    Surface(
                        color = if (triggerEnabled) traceColor.copy(alpha = 0.15f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { triggerEnabled = !triggerEnabled }
                    ) {
                        Text(
                            text = if (triggerEnabled) "TRIG: AUTO" else "TRIG: FREE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (triggerEnabled) traceColor else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Freeze badge
                    if (isFrozen) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "HOLD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Recording Indicator if active
                    if (isRecording) {
                        val sec = (recordingDurationMs / 1000) % 60
                        val min = (recordingDurationMs / 60000)
                        val ms = (recordingDurationMs % 1000) / 100
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(Color(0xFF3B0712), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = recAlpha))
                            )
                            Text(
                                text = String.format("REC %02d:%02d.%d", min, sec, ms),
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (effectiveCollapsed) {
                        // Compact preview when collapsed
                        Text(
                            text = if (isBinauralEnabled) {
                                String.format("L:%.0f R:%.0f", leftCarrierFreq, rightCarrierFreq)
                            } else {
                                String.format("%.0fHz • Vpp:%.2fV", carrierFreq, vpp)
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = traceColor
                        )
                    } else {
                        Text(
                            text = if (isPlaying) "LIVE" else "STANDBY",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isPlaying) Color(0xFF10B981) else Color(0xFF64748B)
                        )
                    }

                    // Optical Sonification & Wallpaper Dialog Button
                    if (onOpenImageNoiseDialog != null) {
                        IconButton(
                            onClick = onOpenImageNoiseDialog,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("oscilloscope_image_button")
                        ) {
                            Icon(
                                imageVector = if (imageBitmap != null) Icons.Default.Wallpaper else Icons.Default.AddPhotoAlternate,
                                contentDescription = "Oscilloscope Wallpaper & Optical Noise",
                                tint = if (imageBitmap != null || isImageNoiseActive) traceColor else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Chevron Expand/Collapse icon
                    IconButton(
                        onClick = { toggleCollapse() },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("collapse_oscilloscope_button")
                    ) {
                        Icon(
                            imageVector = if (effectiveCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (effectiveCollapsed) "Expand Oscilloscope" else "Collapse Oscilloscope",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !effectiveCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Scope Screen Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF030712))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .clickable { isFrozen = !isFrozen }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerY = h / 2f

                            // 1. Draw custom background wallpaper image if loaded
                            if (imageBitmap != null) {
                                drawImage(
                                    image = imageBitmap,
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(w.toInt(), h.toInt()),
                                    alpha = backgroundOpacity
                                )
                                // Dark scrim for trace readability
                                drawRect(Color(0xFF030712).copy(alpha = 0.25f))
                            }

                            // 2. Optical Scanline Progress Cursor
                            if (isImageNoiseActive) {
                                val scanX = w * (opticalScanProgress % 1.0f)
                                // Soft wide glow beam
                                drawLine(
                                    color = traceColor.copy(alpha = 0.18f),
                                    start = Offset(scanX, 0f),
                                    end = Offset(scanX, h),
                                    strokeWidth = 14f
                                )
                                // Core scan cursor line
                                drawLine(
                                    color = traceColor.copy(alpha = 0.9f),
                                    start = Offset(scanX, 0f),
                                    end = Offset(scanX, h),
                                    strokeWidth = 2f
                                )
                                // Scanline reticle ticks
                                drawLine(
                                    color = Color.White,
                                    start = Offset(scanX - 4f, 0f),
                                    end = Offset(scanX + 4f, 0f),
                                    strokeWidth = 3f
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(scanX - 4f, h),
                                    end = Offset(scanX + 4f, h),
                                    strokeWidth = 3f
                                )
                            }

                            // Draw Grid
                            val numCols = 10
                            val numRows = 8
                            val colWidth = w / numCols
                            val rowHeight = h / numRows

                            // Subtle background lines
                            for (c in 1 until numCols) {
                                val x = c * colWidth
                                drawLine(
                                    color = if (c == numCols / 2) gridColor else gridSubColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, h),
                                    strokeWidth = if (c == numCols / 2) 1.5f else 0.8f
                                )
                            }
                            for (r in 1 until numRows) {
                                val y = r * rowHeight
                                drawLine(
                                    color = if (r == numRows / 2) gridColor else gridSubColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = if (r == numRows / 2) 1.5f else 0.8f
                                )
                            }

                            // Center Crosshair ticks
                            val centerTickSize = 4f
                            for (c in 0..numCols * 5) {
                                val x = c * (colWidth / 5)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x, centerY - centerTickSize),
                                    end = Offset(x, centerY + centerTickSize),
                                    strokeWidth = 1f
                                )
                            }
                            val centerX = w / 2f
                            for (r in 0..numRows * 5) {
                                val y = r * (rowHeight / 5)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(centerX - centerTickSize, y),
                                    end = Offset(centerX + centerTickSize, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Render Waveform trace
                            if (samples.isNotEmpty()) {
                                // Find trigger zero-crossing (rising slope from <= 0 to > 0)
                                var startIndex = 0
                                if (triggerEnabled && !isFrozen) {
                                    val maxSearch = (samples.size / 3).coerceAtLeast(1)
                                    for (i in 1 until maxSearch) {
                                        if (samples[i - 1] <= 0f && samples[i] > 0f) {
                                            startIndex = i
                                            break
                                        }
                                    }
                                }

                                // Determine points to draw based on timebase zoom
                                val effectiveSamples = (samples.size / timebaseZoom).toInt().coerceIn(32, samples.size - startIndex)
                                val stepX = w / (effectiveSamples - 1).toFloat()

                                val path = Path()
                                val glowPath = Path()

                                for (i in 0 until effectiveSamples) {
                                    val sampleIdx = startIndex + i
                                    if (sampleIdx >= samples.size) break
                                    val sampleVal = samples[sampleIdx] * amplitudeGain
                                    val clampedSample = sampleVal.coerceIn(-1.5f, 1.5f)
                                    // Map -1..1 to screen height with 0 at centerY
                                    val y = centerY - (clampedSample * (h * 0.42f))
                                    val x = i * stepX

                                    if (i == 0) {
                                        path.moveTo(x, y)
                                        glowPath.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        glowPath.lineTo(x, y)
                                    }
                                }

                                // Draw outer glow
                                drawPath(
                                    path = glowPath,
                                    color = glowColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                // Draw core trace
                                drawPath(
                                    path = path,
                                    color = traceColor,
                                    style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        // Overlay technical metrics at top-right of canvas
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color(0x99030712), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format("Vpp: %.2f V", vpp),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = traceColor
                            )
                            Text(
                                text = String.format("RMS: %.2f", rms),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = String.format("Zoom: %.1fx", timebaseZoom),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            if (isImageNoiseActive) {
                                Text(
                                    text = String.format("SCAN: %02d%%", (opticalScanProgress * 100).toInt()),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }

                        // Overlay Frequency info at bottom-left of canvas
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color(0x99030712), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isBinauralEnabled) {
                                Text(
                                    text = String.format("L: %.0fHz | R: %.0fHz", leftCarrierFreq, rightCarrierFreq),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFA855F7)
                                )
                                Text(
                                    text = String.format("BEAT: %.1f Hz", abs(rightCarrierFreq - leftCarrierFreq)),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            } else {
                                Text(
                                    text = String.format("CARRIER: %.1f Hz", carrierFreq),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = traceColor
                                )
                                Text(
                                    text = String.format("MOD: %.1f Hz", modFreq),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Scope Controls (Timebase zoom, Amplitude scale, Image sonification)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Horizontal Timebase buttons
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TIME:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            listOf(1f to "1x", 2f to "2x", 4f to "4x", 8f to "8x").forEach { (zoom, label) ->
                                FilterChip(
                                    selected = timebaseZoom == zoom,
                                    onClick = { timebaseZoom = zoom },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Right side: Gain buttons + Image Wallpaper button
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("GAIN:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            listOf(0.5f to "0.5x", 1.0f to "1x", 1.5f to "1.5x").forEach { (gain, label) ->
                                FilterChip(
                                    selected = amplitudeGain == gain,
                                    onClick = { amplitudeGain = gain },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }

                            if (onOpenImageNoiseDialog != null) {
                                FilterChip(
                                    selected = imageBitmap != null || isImageNoiseActive,
                                    onClick = onOpenImageNoiseDialog,
                                    label = { Text(if (imageBitmap != null) "BG" else "+BG", fontSize = 10.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    modifier = Modifier.height(28.dp).testTag("quick_scope_bg_chip")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
