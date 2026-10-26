package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChordPreset
import com.example.model.HarmonicInterval
import com.example.model.WaveformType

@Composable
fun HarmonicLayeringSection(
    isLayeringEnabled: Boolean,
    activeChordPreset: ChordPreset,
    baseCarrierFreq: Float,
    layer2Enabled: Boolean,
    layer2Freq: Float,
    layer2Wave: WaveformType,
    layer2Gain: Float,
    layer2Interval: HarmonicInterval,
    layer3Enabled: Boolean,
    layer3Freq: Float,
    layer3Wave: WaveformType,
    layer3Gain: Float,
    layer3Interval: HarmonicInterval,
    onToggleLayering: () -> Unit,
    onSelectChordPreset: (ChordPreset) -> Unit,
    onToggleLayer2: (Boolean) -> Unit,
    onLayer2FreqChange: (Float) -> Unit,
    onLayer2WaveChange: (WaveformType) -> Unit,
    onLayer2GainChange: (Float) -> Unit,
    onLayer2IntervalChange: (HarmonicInterval) -> Unit,
    onToggleLayer3: (Boolean) -> Unit,
    onLayer3FreqChange: (Float) -> Unit,
    onLayer3WaveChange: (WaveformType) -> Unit,
    onLayer3GainChange: (Float) -> Unit,
    onLayer3IntervalChange: (HarmonicInterval) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("harmonic_layering_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Description & Master Switch
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
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isLayeringEnabled) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Harmonic Chords & Multi-Tone",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLayeringEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "3-TONE CHORD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA855F7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Stack harmonic intervals, chords & Solfeggio layers over fundamental",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isLayeringEnabled,
                    onCheckedChange = { onToggleLayering() },
                    modifier = Modifier.testTag("layering_master_switch")
                )
            }

            // Quick Chord Presets Strip
            Column {
                Text(
                    text = "CHORD HARMONIC PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChordPreset.values().take(3).forEach { preset ->
                        val isSelected = isLayeringEnabled && activeChordPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChordPreset(preset) },
                            label = { Text(preset.displayName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChordPreset.values().drop(3).forEach { preset ->
                        val isSelected = isLayeringEnabled && activeChordPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChordPreset(preset) },
                            label = { Text(preset.displayName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isLayeringEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Fundamental Anchor Display
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Layer 1 (Fundamental Anchor):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${baseCarrierFreq.toInt()} Hz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Layer 2 Section
                    LayerRowCard(
                        layerNumber = 2,
                        label = "Harmonic 2nd Voice",
                        isEnabled = layer2Enabled,
                        frequency = layer2Freq,
                        waveform = layer2Wave,
                        gain = layer2Gain,
                        currentInterval = layer2Interval,
                        accentColor = Color(0xFF38BDF8),
                        onToggle = onToggleLayer2,
                        onFreqChange = onLayer2FreqChange,
                        onWaveChange = onLayer2WaveChange,
                        onGainChange = onLayer2GainChange,
                        onIntervalChange = onLayer2IntervalChange
                    )

                    // Layer 3 Section
                    LayerRowCard(
                        layerNumber = 3,
                        label = "Harmonic 3rd Voice",
                        isEnabled = layer3Enabled,
                        frequency = layer3Freq,
                        waveform = layer3Wave,
                        gain = layer3Gain,
                        currentInterval = layer3Interval,
                        accentColor = Color(0xFFA855F7),
                        onToggle = onToggleLayer3,
                        onFreqChange = onLayer3FreqChange,
                        onWaveChange = onLayer3WaveChange,
                        onGainChange = onLayer3GainChange,
                        onIntervalChange = onLayer3IntervalChange
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerRowCard(
    layerNumber: Int,
    label: String,
    isEnabled: Boolean,
    frequency: Float,
    waveform: WaveformType,
    gain: Float,
    currentInterval: HarmonicInterval,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    onFreqChange: (Float) -> Unit,
    onWaveChange: (WaveformType) -> Unit,
    onGainChange: (Float) -> Unit,
    onIntervalChange: (HarmonicInterval) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textInput by remember(frequency) { mutableStateOf(frequency.toInt().toString()) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Layer Header: Title, Enable Switch, and Frequency Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Layer $layerNumber: $label",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEnabled) "${frequency.toInt()} Hz • ${waveform.name.lowercase().replaceFirstChar { it.uppercase() }} (${(gain * 100).toInt()}%)" else "Muted",
                            fontSize = 11.sp,
                            color = if (isEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Numeric input field for exact frequency
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { newText ->
                        textInput = newText.filter { it.isDigit() }.take(4)
                        val num = textInput.toFloatOrNull()
                        if (num != null && num in 1.0f..1000.0f) {
                            onFreqChange(num)
                        }
                    },
                    modifier = Modifier.width(86.dp).height(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    enabled = isEnabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            if (isEnabled) {
                // Interval quick buttons (Octave, 5th, 4th, Maj 3rd, Min 3rd, Custom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val intervals = listOf(
                        HarmonicInterval.OCTAVE_DOWN,
                        HarmonicInterval.OCTAVE_UP,
                        HarmonicInterval.PERFECT_FIFTH,
                        HarmonicInterval.MAJOR_THIRD,
                        HarmonicInterval.CUSTOM
                    )
                    intervals.forEach { interval ->
                        val isSelected = currentInterval == interval
                        Surface(
                            color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onIntervalChange(interval) }
                        ) {
                            Text(
                                text = when (interval) {
                                    HarmonicInterval.OCTAVE_DOWN -> "-1 Oct"
                                    HarmonicInterval.OCTAVE_UP -> "+1 Oct"
                                    HarmonicInterval.PERFECT_FIFTH -> "5th"
                                    HarmonicInterval.MAJOR_THIRD -> "Maj3"
                                    HarmonicInterval.CUSTOM -> "Free"
                                    else -> interval.displayName
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Frequency Slider (1 - 1000 Hz)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Frequency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${frequency.toInt()} Hz", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = frequency,
                        onValueChange = {
                            onFreqChange(it)
                            onIntervalChange(HarmonicInterval.CUSTOM)
                        },
                        valueRange = 1.0f..1000.0f,
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    )
                }

                // Waveform Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WaveformType.values().forEach { wave ->
                        val isSelected = waveform == wave
                        FilterChip(
                            selected = isSelected,
                            onClick = { onWaveChange(wave) },
                            label = { Text(wave.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(28.dp)
                        )
                    }
                }

                // Balance/Gain Slider (0% to 100%)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mix Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(gain * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = gain,
                        onValueChange = onGainChange,
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    )
                }
            }
        }
    }
}
