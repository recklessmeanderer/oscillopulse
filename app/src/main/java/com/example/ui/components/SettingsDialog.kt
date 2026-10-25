package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectScopeTheme: (ScopeTheme) -> Unit,
    onSelectSampleRate: (Int) -> Unit,
    onSelectBitDepth: (Int) -> Unit,
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
