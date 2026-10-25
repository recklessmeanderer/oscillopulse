package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioConfig

@Composable
fun WavExportDialog(
    carrierFreq: Float,
    modFreq: Float,
    currentSampleRate: Int,
    currentBitDepth: Int,
    isExporting: Boolean,
    exportProgress: Float,
    onDismiss: () -> Unit,
    onExport: (filename: String, durationSec: Float, sampleRate: Int, bitDepth: Int) -> Unit
) {
    var filename by remember {
        mutableStateOf("wave_${carrierFreq.toInt()}Hz_${modFreq.toInt()}Hz_${System.currentTimeMillis() % 10000}.wav")
    }
    var selectedDuration by remember { mutableStateOf(30f) } // 30 seconds default
    var selectedSampleRate by remember { mutableStateOf(currentSampleRate) }
    var selectedBitDepth by remember { mutableStateOf(currentBitDepth) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Text(
                text = "Export Audio to WAV",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Filename Input
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("File Name (.wav)") },
                    singleLine = true,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration Selector
                Column {
                    Text(
                        text = "Export Duration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            10f to "10s",
                            30f to "30s",
                            60f to "1m",
                            300f to "5m",
                            900f to "15m"
                        ).forEach { (dur, label) ->
                            FilterChip(
                                selected = selectedDuration == dur,
                                onClick = { if (!isExporting) selectedDuration = dur },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                // Sample Rate Selector
                Column {
                    Text(
                        text = "Sample Rate (Hz)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioConfig.SUPPORTED_SAMPLE_RATES.forEach { rate ->
                            FilterChip(
                                selected = selectedSampleRate == rate,
                                onClick = { if (!isExporting) selectedSampleRate = rate },
                                label = { Text("${rate / 1000}k", fontSize = 11.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                // Bit Depth Selector
                Column {
                    Text(
                        text = "Bit Depth Quality",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioConfig.SUPPORTED_BIT_DEPTHS.forEach { depth ->
                            FilterChip(
                                selected = selectedBitDepth == depth,
                                onClick = { if (!isExporting) selectedBitDepth = depth },
                                label = {
                                    Text(
                                        when (depth) {
                                            16 -> "16-bit PCM"
                                            24 -> "24-bit HD"
                                            32 -> "32-bit Float"
                                            else -> "$depth-bit"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                // Estimated File Size Info
                val estimatedBytes = (selectedSampleRate * selectedDuration * 2 * (selectedBitDepth / 8)).toLong()
                val estimatedMb = estimatedBytes / (1024f * 1024f)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format("Format: 2-ch Stereo • Estimated Size: %.2f MB", estimatedMb),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Export Progress if running
                if (isExporting) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("Synthesizing audio: %d%%", (exportProgress * 100).toInt()),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = if (filename.endsWith(".wav", ignoreCase = true)) filename else "$filename.wav"
                    onExport(cleanName, selectedDuration, selectedSampleRate, selectedBitDepth)
                },
                enabled = !isExporting && filename.isNotBlank()
            ) {
                Text(if (isExporting) "Exporting..." else "Render WAV")
            }
        },
        dismissButton = {
            if (!isExporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
