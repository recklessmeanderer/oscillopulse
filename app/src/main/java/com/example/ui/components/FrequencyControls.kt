package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModulationMode
import com.example.model.WaveformType

@Composable
fun WaveformSelectorRow(
    title: String,
    selected: WaveformType,
    onSelect: (WaveformType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WaveformType.values().forEach { wave ->
                val isSelected = selected == wave
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(wave) }
                        .testTag("waveform_${wave.name.lowercase()}"),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        width = 1.5.dp
                    ) else null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = wave.iconLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = wave.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarrierFrequencySection(
    frequency: Float,
    waveform: WaveformType,
    phaseOffset: Float,
    onFrequencyChange: (Float) -> Unit,
    onWaveformChange: (WaveformType) -> Unit,
    onPhaseOffsetChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDirectInput by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("carrier_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)),
            width = 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Frequency Value and Quick Step Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CARRIER WAVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1 Hz – 1000 Hz",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Digital Frequency Readout (Tappable for direct numeric input)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF030712),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        width = 1.dp
                    ),
                    modifier = Modifier.clickable { showDirectInput = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1f", frequency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hz",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Slider (1 Hz to 1000 Hz)
            Slider(
                value = frequency,
                onValueChange = onFrequencyChange,
                valueRange = 1.0f..1000.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("carrier_slider")
            )

            // Fine tuning +/- step buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency - 10f).coerceAtLeast(1f)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("-10", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency - 1f).coerceAtLeast(1f)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("-1", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency - 0.1f).coerceAtLeast(1f)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("-0.1", fontSize = 10.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency + 0.1f).coerceAtMost(1000f)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+0.1", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency + 1f).coerceAtMost(1000f)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+1", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { onFrequencyChange((frequency + 10f).coerceAtMost(1000f)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+10", fontSize = 10.sp)
                    }
                }
            }

            // Quick frequency anchor buttons (40Hz, 108Hz, 216Hz, 432Hz, 528Hz, 1000Hz)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(40f, 108f, 216f, 432f, 528f, 800f).forEach { targetFreq ->
                    SuggestionChip(
                        onClick = { onFrequencyChange(targetFreq) },
                        label = { Text("${targetFreq.toInt()}Hz", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Selector for Carrier
            WaveformSelectorRow(
                title = "Carrier Waveform",
                selected = waveform,
                onSelect = onWaveformChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Carrier Phase Shift Section
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(10.dp))

            val radVal = (phaseOffset * (Math.PI / 180.0)).toFloat()
            val radLabel = when {
                kotlin.math.abs(phaseOffset - 0f) < 1f -> "0 rad"
                kotlin.math.abs(phaseOffset - 90f) < 1f -> "π/2 rad"
                kotlin.math.abs(phaseOffset - 180f) < 1f -> "π rad"
                kotlin.math.abs(phaseOffset - 270f) < 1f -> "3π/2 rad"
                kotlin.math.abs(phaseOffset - 360f) < 1f -> "2π rad"
                else -> String.format("%.2f rad", radVal)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Carrier Phase Shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-time waveform offset ($radLabel)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF030712),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        width = 1.dp
                    )
                ) {
                    Text(
                        text = "${phaseOffset.toInt()}°",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Slider(
                value = phaseOffset,
                onValueChange = onPhaseOffsetChange,
                valueRange = 0.0f..360.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("carrier_phase_slider")
            )

            // Phase quick anchors & fine-tune
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0f to "0°", 90f to "90°", 180f to "180°", 270f to "270°").forEach { (deg, label) ->
                        val isSelected = kotlin.math.abs(phaseOffset - deg) < 2f
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPhaseOffsetChange(deg) },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onPhaseOffsetChange(((phaseOffset - 15f) % 360f + 360f) % 360f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("-15°", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { onPhaseOffsetChange(((phaseOffset + 15f) % 360f + 360f) % 360f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("+15°", fontSize = 10.sp)
                    }
                }
            }
        }
    }

    if (showDirectInput) {
        DirectFrequencyInputDialog(
            currentFreq = frequency,
            min = 1f,
            max = 1000f,
            title = "Enter Carrier Frequency (1 - 1000 Hz)",
            onDismiss = { showDirectInput = false },
            onConfirm = {
                onFrequencyChange(it)
                showDirectInput = false
            }
        )
    }
}

@Composable
fun ModulationSection(
    frequency: Float,
    intensity: Float,
    waveform: WaveformType,
    mode: ModulationMode,
    onFrequencyChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onWaveformChange: (WaveformType) -> Unit,
    onModeChange: (ModulationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("modulation_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)),
            width = 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MODULATION & ENTRAINMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFF59E0B) // Amber
                    )
                    Text(
                        text = when {
                            frequency in 30.0f..100.0f -> "Gamma Brainwave Range (Peak Focus / 40 Hz)"
                            frequency in 13.0f..30.0f -> "Beta Brainwave Range (Alertness)"
                            frequency in 8.0f..13.0f -> "Alpha Brainwave Range (Relaxation)"
                            frequency in 4.0f..8.0f -> "Theta Brainwave Range (Deep State)"
                            else -> "Delta Brainwave Range (Deep Sleep)"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Modulation Frequency Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF030712),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        width = 1.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1f", frequency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hz",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modulation Frequency Slider (0.1 Hz to 100 Hz)
            Slider(
                value = frequency,
                onValueChange = onFrequencyChange,
                valueRange = 0.1f..100.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mod_frequency_slider")
            )

            // Quick Brainwave Frequency Chips (Delta 2.5Hz, Theta 6Hz, Alpha 10Hz, Beta 20Hz, Gamma 40Hz)
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
                ).forEach { (f, label) ->
                    val isSelected = kotlin.math.abs(frequency - f) < 0.2f
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFrequencyChange(f) },
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Modulation Intensity / Depth Slider (0% to 100%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modulation Intensity (Depth)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(intensity * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 0.0f..1.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mod_intensity_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Modulation Waveform
            WaveformSelectorRow(
                title = "Modulation Waveform",
                selected = waveform,
                onSelect = onWaveformChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Modulation Mode Tabs (Isochronic Pulse vs Amplitude Modulation vs Binaural Beat)
            Text(
                text = "Modulation Engine Mode",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModulationMode.values().forEach { m ->
                    val isSelected = mode == m
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onModeChange(m) },
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (m) {
                                    ModulationMode.ISOCHRONIC_PULSE -> "Isochronic"
                                    ModulationMode.AMPLITUDE_MODULATION -> "AM Tremolo"
                                    ModulationMode.BINAURAL_BEAT -> "Binaural"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectFrequencyInputDialog(
    currentFreq: Float,
    min: Float,
    max: Float,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textVal by remember { mutableStateOf(currentFreq.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = textVal,
                    onValueChange = {
                        textVal = it
                        val parsed = it.toFloatOrNull()
                        errorText = when {
                            parsed == null -> "Please enter a valid number"
                            parsed < min || parsed > max -> "Value must be between $min and $max"
                            else -> null
                        }
                    },
                    isError = errorText != null,
                    singleLine = true,
                    label = { Text("Frequency (Hz)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textVal.toFloatOrNull()
                    if (parsed != null && parsed in min..max) {
                        onConfirm(parsed)
                    }
                },
                enabled = errorText == null && textVal.toFloatOrNull() != null
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
