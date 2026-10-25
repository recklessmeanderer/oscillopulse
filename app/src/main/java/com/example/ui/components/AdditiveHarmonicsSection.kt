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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AdditiveHarmonicsSection(
    isHarmonicsEnabled: Boolean,
    carrierFreq: Float,
    h1Multiplier: Int,
    h1Volume: Float,
    h2Multiplier: Int,
    h2Volume: Float,
    h3Multiplier: Int,
    h3Volume: Float,
    onToggleHarmonics: (Boolean) -> Unit,
    onH1MultiplierChange: (Int) -> Unit,
    onH1VolumeChange: (Float) -> Unit,
    onH2MultiplierChange: (Int) -> Unit,
    onH2VolumeChange: (Float) -> Unit,
    onH3MultiplierChange: (Int) -> Unit,
    onH3VolumeChange: (Float) -> Unit,
    onSelectProfile: (h1M: Int, h1V: Float, h2M: Int, h2V: Float, h3M: Int, h3V: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFFF59E0B) // Warm Amber / Harmonic Gold accent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("harmonics_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isHarmonicsEnabled) accentColor.copy(alpha = 0.6f) else Color(0xFF1E293B)
            ),
            width = if (isHarmonicsEnabled) 1.5.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Title & Master Toggle
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
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Additive Harmonics",
                        tint = if (isHarmonicsEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ADDITIVE HARMONICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isHarmonicsEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isHarmonicsEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "3 OVERTONES READY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Layer sine wave overtones onto carrier wave",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dedicated Toggle Switch
                Switch(
                    checked = isHarmonicsEnabled,
                    onCheckedChange = onToggleHarmonics,
                    modifier = Modifier.testTag("harmonics_switch")
                )
            }

            AnimatedVisibility(
                visible = isHarmonicsEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Quick harmonic profile presets
                    Text(
                        text = "HARMONIC PROFILES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HarmonicProfileChip(
                            title = "Natural",
                            subtitle = "2x, 3x, 4x",
                            onClick = { onSelectProfile(2, 0.50f, 3, 0.30f, 4, 0.15f) },
                            modifier = Modifier.weight(1f)
                        )
                        HarmonicProfileChip(
                            title = "Octaves",
                            subtitle = "2x, 4x, 8x",
                            onClick = { onSelectProfile(2, 0.60f, 4, 0.35f, 8, 0.20f) },
                            modifier = Modifier.weight(1f)
                        )
                        HarmonicProfileChip(
                            title = "Odd / Hollow",
                            subtitle = "3x, 5x, 7x",
                            onClick = { onSelectProfile(3, 0.45f, 5, 0.25f, 7, 0.15f) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overtone 1
                    OvertoneStrip(
                        index = 1,
                        name = "1st Overtone",
                        badgeColor = Color(0xFFF97316), // Orange
                        carrierFreq = carrierFreq,
                        multiplier = h1Multiplier,
                        volume = h1Volume,
                        onMultiplierChange = onH1MultiplierChange,
                        onVolumeChange = onH1VolumeChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overtone 2
                    OvertoneStrip(
                        index = 2,
                        name = "2nd Overtone",
                        badgeColor = Color(0xFFF59E0B), // Amber
                        carrierFreq = carrierFreq,
                        multiplier = h2Multiplier,
                        volume = h2Volume,
                        onMultiplierChange = onH2MultiplierChange,
                        onVolumeChange = onH2VolumeChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overtone 3
                    OvertoneStrip(
                        index = 3,
                        name = "3rd Overtone",
                        badgeColor = Color(0xFF10B981), // Emerald
                        carrierFreq = carrierFreq,
                        multiplier = h3Multiplier,
                        volume = h3Volume,
                        onMultiplierChange = onH3MultiplierChange,
                        onVolumeChange = onH3VolumeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun OvertoneStrip(
    index: Int,
    name: String,
    badgeColor: Color,
    carrierFreq: Float,
    multiplier: Int,
    volume: Float,
    onMultiplierChange: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val overtoneFreq = (carrierFreq * multiplier).roundToInt()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (volume > 0.01f) badgeColor.copy(alpha = 0.4f) else Color(0xFF1E293B)
            ),
            width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth().testTag("overtone_strip_$index")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top row: Title, Multiplier chips, and Calculated Frequency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Frequency readout
                Text(
                    text = "${multiplier}x ($overtoneFreq Hz)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multiplier selector chips (2x to 8x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val multipliers = listOf(2, 3, 4, 5, 6, 7, 8)
                multipliers.forEach { mult ->
                    val isSelected = mult == multiplier
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) badgeColor.copy(alpha = 0.25f) else Color(0xFF1E293B))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) badgeColor else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onMultiplierChange(mult) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mult}x",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) badgeColor else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Volume Slider row with readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (volume > 0.01f) badgeColor else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("h${index}_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = badgeColor,
                        activeTrackColor = badgeColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (volume > 0.01f) Color.White else Color(0xFF64748B),
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
private fun HarmonicProfileChip(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)),
            width = 1.dp
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
