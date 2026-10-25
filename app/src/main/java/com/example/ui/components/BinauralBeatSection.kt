package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun BinauralBeatSection(
    isBinauralEnabled: Boolean,
    leftCarrierFreq: Float,
    rightCarrierFreq: Float,
    onToggleBinaural: (Boolean) -> Unit,
    onLeftFreqChange: (Float) -> Unit,
    onRightFreqChange: (Float) -> Unit,
    onSetBeatDiff: (Float) -> Unit,
    isFrequenciesLinked: Boolean = true,
    onToggleFrequencyLink: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val beatDiff = abs(rightCarrierFreq - leftCarrierFreq)
    var showBeatDirectInput by remember { mutableStateOf(false) }

    val brainwaveBand = when {
        beatDiff >= 30f -> "Gamma Band (Peak Focus / 40Hz Entrainment)"
        beatDiff >= 13f -> "Beta Band (Active Alertness)"
        beatDiff >= 8f -> "Alpha Band (Relaxed Focus & Flow)"
        beatDiff >= 4f -> "Theta Band (Deep Meditation)"
        else -> "Delta Band (Deep Restorative Sleep)"
    }

    val accentColor = Color(0xFFA855F7) // Purple / Violet entrainment accent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("binaural_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isBinauralEnabled) accentColor.copy(alpha = 0.6f) else Color(0xFF1E293B)
            ),
            width = if (isBinauralEnabled) 1.5.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Title & Enable/Disable Toggle
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
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Binaural Beat",
                        tint = if (isBinauralEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BINAURAL BEATS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isBinauralEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isBinauralEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "STEREO ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Independent Left & Right carriers",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Link status toggle badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFrequenciesLinked) accentColor.copy(alpha = 0.18f) else Color(0xFF1E293B),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isFrequenciesLinked) accentColor.copy(alpha = 0.6f) else Color(0xFF334155)
                            ),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = onToggleFrequencyLink != null) { onToggleFrequencyLink?.invoke() }
                            .testTag("binaural_link_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isFrequenciesLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = if (isFrequenciesLinked) "Frequencies Linked" else "Frequencies Unlinked",
                                tint = if (isFrequenciesLinked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isFrequenciesLinked) "LINKED" else "UNLINKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFrequenciesLinked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dedicated Toggle Switch
                    Switch(
                        checked = isBinauralEnabled,
                        onCheckedChange = onToggleBinaural,
                        modifier = Modifier.testTag("binaural_switch")
                    )
                }
            }

            AnimatedVisibility(
                visible = isBinauralEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Headphone notification banner
                    Surface(
                        color = Color(0xFF1E1B4B), // Deep indigo
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.3f)),
                            width = 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🎧",
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Stereo headphones required: Left and Right ears receive distinct frequencies to trigger brainwave synchronization.",
                                fontSize = 11.sp,
                                color = Color(0xFFE9D5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Binaural Beat Frequency Readout (Tappable for direct numeric input)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF030712),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.5f)),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showBeatDirectInput = true }
                            .testTag("binaural_beat_readout")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PERCEIVED BINAURAL BEAT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "✎ Tap to edit",
                                        fontSize = 9.sp,
                                        color = accentColor.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = brainwaveBand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f", beatDiff),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Hz",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Left Ear Carrier Control
                    ChannelCarrierSlider(
                        label = "Left Ear Carrier (L)",
                        channelTag = "LEFT",
                        frequency = leftCarrierFreq,
                        onFrequencyChange = onLeftFreqChange,
                        accentColor = Color(0xFF38BDF8) // Cyan for Left
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Right Ear Carrier Control
                    ChannelCarrierSlider(
                        label = "Right Ear Carrier (R)",
                        channelTag = "RIGHT",
                        frequency = rightCarrierFreq,
                        onFrequencyChange = onRightFreqChange,
                        accentColor = Color(0xFFF43F5E) // Rose/Coral for Right
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Beat Presets & L/R Swap
                    Text(
                        text = "Quick Entrainment Target (Right = Left + Δf)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            2.5f to "Delta 2.5",
                            6.0f to "Theta 6",
                            10.0f to "Alpha 10",
                            20.0f to "Beta 20",
                            40.0f to "Gamma 40★"
                        ).forEach { (diff, label) ->
                            val isSelected = abs(beatDiff - diff) < 0.2f
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetBeatDiff(diff) },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Swap L/R channels button
                        AssistChip(
                            onClick = {
                                val temp = leftCarrierFreq
                                onLeftFreqChange(rightCarrierFreq)
                                onRightFreqChange(temp)
                            },
                            label = { Text("Swap", fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap channels",
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            if (!isBinauralEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Binaural beat synthesis is currently disabled. Enable to send distinct carrier frequencies to Left and Right channels for cortical frequency-following entrainment.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }

    if (showBeatDirectInput) {
        DirectFrequencyInputDialog(
            currentFreq = beatDiff,
            min = 0.1f,
            max = 100f,
            title = "Enter Perceived Beat Difference Δf (0.1 - 100 Hz)",
            onDismiss = { showBeatDirectInput = false },
            onConfirm = {
                onSetBeatDiff(it)
                showBeatDirectInput = false
            }
        )
    }
}

@Composable
fun ChannelCarrierSlider(
    label: String,
    channelTag: String,
    frequency: Float,
    onFrequencyChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var showDirectInput by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = channelTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF030712),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.4f)),
                    width = 1.dp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showDirectInput = true }
                    .testTag("carrier_slider_${channelTag.lowercase()}_readout")
            ) {
                Text(
                    text = "${frequency.toInt()} Hz",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = frequency,
            onValueChange = onFrequencyChange,
            valueRange = 1.0f..1000.0f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("carrier_slider_${channelTag.lowercase()}")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = { onFrequencyChange((frequency - 5f).coerceAtLeast(1f)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("-5", fontSize = 10.sp)
                }
                FilledTonalButton(
                    onClick = { onFrequencyChange((frequency - 1f).coerceAtLeast(1f)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("-1", fontSize = 10.sp)
                }
                FilledTonalButton(
                    onClick = { onFrequencyChange((frequency + 1f).coerceAtMost(1000f)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("+1", fontSize = 10.sp)
                }
                FilledTonalButton(
                    onClick = { onFrequencyChange((frequency + 5f).coerceAtMost(1000f)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("+5", fontSize = 10.sp)
                }
            }
        }
    }

    if (showDirectInput) {
        DirectFrequencyInputDialog(
            currentFreq = frequency,
            min = 1f,
            max = 1000f,
            title = "Enter $label (1 - 1000 Hz)",
            onDismiss = { showDirectInput = false },
            onConfirm = {
                onFrequencyChange(it)
                showDirectInput = false
            }
        )
    }
}
