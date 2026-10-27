package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.PresetEntity

private enum class PresetFilterTab(val label: String) {
    ALL("All"),
    CUSTOM("Custom"),
    FACTORY("Built-In")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsSheet(
    presets: List<PresetEntity>,
    onSelectPreset: (PresetEntity) -> Unit,
    onDeletePreset: (PresetEntity) -> Unit,
    onRestoreFactoryPresets: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(PresetFilterTab.ALL) }

    val customCount = presets.count { !it.isFactory }
    val factoryCount = presets.count { it.isFactory }

    val filteredPresets = remember(presets, selectedTab) {
        when (selectedTab) {
            PresetFilterTab.ALL -> presets
            PresetFilterTab.CUSTOM -> presets.filter { !it.isFactory }
            PresetFilterTab.FACTORY -> presets.filter { it.isFactory }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("presets_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Preset Library",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Store & retrieve carrier and entrainment waveforms",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${presets.size} Total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs: All, Custom, Built-In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == PresetFilterTab.ALL,
                    onClick = { selectedTab = PresetFilterTab.ALL },
                    label = { Text("All (${presets.size})") },
                    modifier = Modifier.testTag("preset_filter_all")
                )
                FilterChip(
                    selected = selectedTab == PresetFilterTab.CUSTOM,
                    onClick = { selectedTab = PresetFilterTab.CUSTOM },
                    label = { Text("Custom ($customCount)") },
                    modifier = Modifier.testTag("preset_filter_custom")
                )
                FilterChip(
                    selected = selectedTab == PresetFilterTab.FACTORY,
                    onClick = { selectedTab = PresetFilterTab.FACTORY },
                    label = { Text("Built-In ($factoryCount)") },
                    modifier = Modifier.testTag("preset_filter_factory")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == PresetFilterTab.CUSTOM) {
                            "No custom presets saved yet.\nUse 'Save Preset' to capture your active carrier and modulation settings."
                        } else {
                            "No presets available."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    items(filteredPresets, key = { it.id }) { preset ->
                        PresetItemCard(
                            preset = preset,
                            onSelect = {
                                onSelectPreset(preset)
                                onDismiss()
                            },
                            onDelete = { onDeletePreset(preset) }
                        )
                    }
                }
            }

            if (onRestoreFactoryPresets != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRestoreFactoryPresets,
                        modifier = Modifier.testTag("restore_factory_presets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Built-In Presets", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetItemCard(
    preset: PresetEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("preset_card_${preset.id}"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (preset.isFactory) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (preset.isFactory) "BUILT-IN" else "CUSTOM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (preset.isFactory) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (preset.description.isNotBlank()) {
                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Carrier Frequency Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (preset.isBinauralEnabled) {
                            "Carrier: Binaural L ${preset.leftCarrierFreq.toInt()}Hz / R ${preset.rightCarrierFreq.toInt()}Hz (${preset.carrierWave.lowercase()})"
                        } else {
                            "Carrier: ${preset.carrierFreq.toInt()}Hz (${preset.carrierWave.lowercase()})"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Entrainment Modulation Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Entrainment: ${preset.modFreq} Hz ${preset.getEntrainmentBand()} • ${preset.getModulationMode().displayName} (${(preset.modIntensity * 100).toInt()}% ${preset.getModWaveType().displayName})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF59E0B)
                    )
                }

                // Soundscape tags: Noise / Layering
                if (preset.isWhiteNoiseEnabled || preset.isLayeringEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (preset.isWhiteNoiseEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${preset.getNoiseColorType().displayName} Noise ${(preset.whiteNoiseLevel * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (preset.isLayeringEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Chord: ${preset.getChordPresetType().displayName}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_preset_${preset.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Preset",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
