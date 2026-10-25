package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioConfig
import com.example.model.ScopeTheme

@Composable
fun SettingsDialog(
    isDarkMode: Boolean,
    scopeTheme: ScopeTheme,
    sampleRate: Int,
    bitDepth: Int,
    isWhiteNoiseEnabled: Boolean,
    whiteNoiseLevel: Float,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectScopeTheme: (ScopeTheme) -> Unit,
    onSelectSampleRate: (Int) -> Unit,
    onSelectBitDepth: (Int) -> Unit,
    onToggleWhiteNoise: (Boolean) -> Unit,
    onWhiteNoiseLevelChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Audio & Studio Settings",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dark Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Low-Light Dark Studio Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "High-contrast dark palette for dark labs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        thumbContent = {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                HorizontalDivider()

                // Oscilloscope Phosphor Color Theme
                Column {
                    Text(
                        text = "Oscilloscope Phosphor Color",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScopeTheme.values().forEach { theme ->
                            FilterChip(
                                selected = scopeTheme == theme,
                                onClick = { onSelectScopeTheme(theme) },
                                label = { Text(theme.title, fontSize = 10.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Default Sample Rate
                Column {
                    Text(
                        text = "Real-time Sample Rate",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Hardware synthesis frequency",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioConfig.SUPPORTED_SAMPLE_RATES.forEach { rate ->
                            FilterChip(
                                selected = sampleRate == rate,
                                onClick = { onSelectSampleRate(rate) },
                                label = { Text("${rate / 1000} kHz", fontSize = 10.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                // Bit Depth
                Column {
                    Text(
                        text = "Synthesis Bit Depth",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dynamic range & resolution",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioConfig.SUPPORTED_BIT_DEPTHS.forEach { depth ->
                            FilterChip(
                                selected = bitDepth == depth,
                                onClick = { onSelectBitDepth(depth) },
                                label = {
                                    Text(
                                        when (depth) {
                                            16 -> "16-bit PCM"
                                            24 -> "24-bit PCM"
                                            32 -> "32-bit Float"
                                            else -> "$depth-bit"
                                        },
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Sound Configuration: White Noise Floor Generator
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_config_white_noise_section")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grain,
                                contentDescription = "White Noise",
                                tint = if (isWhiteNoiseEnabled) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "White Noise Floor",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isWhiteNoiseEnabled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "BLENDED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Blend white noise floor for acoustic masking & relaxation",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isWhiteNoiseEnabled,
                            onCheckedChange = onToggleWhiteNoise,
                            modifier = Modifier.testTag("white_noise_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = isWhiteNoiseEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Noise Floor Blend Level",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(whiteNoiseLevel * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Slider(
                                value = whiteNoiseLevel,
                                onValueChange = onWhiteNoiseLevelChange,
                                valueRange = 0.01f..0.60f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("white_noise_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            // Quick noise floor blend presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Subtle" to 0.08f,
                                    "Relax" to 0.18f,
                                    "Deep Mask" to 0.35f
                                ).forEach { (label, lvl) ->
                                    val isSelected = kotlin.math.abs(whiteNoiseLevel - lvl) < 0.04f
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onWhiteNoiseLevelChange(lvl) },
                                        label = { Text("$label (${(lvl * 100).toInt()}%)", fontSize = 10.sp) },
                                        modifier = Modifier.height(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
