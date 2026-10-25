package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransportBar(
    isPlaying: Boolean,
    isRecording: Boolean,
    isMuted: Boolean,
    volume: Float,
    peakLevel: Float,
    onTogglePlay: () -> Unit,
    onToggleRecord: () -> Unit,
    onToggleMute: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val recPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transport_bar_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)),
            width = 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Level VU Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "OUTPUT LEVEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // VU Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF030712))
                ) {
                    val clampedLevel = peakLevel.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(clampedLevel)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF10B981), // Green
                                        Color(0xFFF59E0B), // Amber
                                        Color(0xFFEF4444)  // Red peak
                                    )
                                )
                            )
                    )
                }

                if (peakLevel > 0.95f) {
                    Text(
                        text = "CLIP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Stop Master Button
                Button(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("play_stop_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isPlaying) Color.White else Color(0xFF090D16)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "STOP" else "START AUDIO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Record Live WAV Button
                FilledTonalButton(
                    onClick = onToggleRecord,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("record_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isRecording) Color(0xFFEF4444).copy(alpha = recPulseAlpha) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.White else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "RECORDING" else "RECORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Export WAV modal button
                OutlinedButton(
                    onClick = onOpenExport,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("export_wav_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Export WAV",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Volume & Mute Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute Toggle",
                        tint = if (isMuted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider")
                )

                Text(
                    text = "${if (isMuted) 0 else (volume * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}
