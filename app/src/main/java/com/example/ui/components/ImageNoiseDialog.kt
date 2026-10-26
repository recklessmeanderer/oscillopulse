package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TexturePreset

@Composable
fun ImageNoiseDialog(
    currentImage: Bitmap?,
    imageTitle: String?,
    imageAnalysisInfo: String?,
    isImageNoiseActive: Boolean,
    imageNoiseDepth: Float,
    imageScanSpeedHz: Float,
    scopeBackgroundOpacity: Float,
    isWhiteNoiseEnabled: Boolean,
    onPickImage: () -> Unit,
    onToggleImageNoise: (Boolean) -> Unit,
    onDepthChange: (Float) -> Unit,
    onScanSpeedChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onSelectPreset: (TexturePreset) -> Unit,
    onClearImage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Image Sonification",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("close_image_noise_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Display any photo as the oscilloscope background and scan its optical luminance into white, pink, or brown noise.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Current Active Image Preview Card
                if (currentImage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)),
                            width = 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        bitmap = currentImage.asImageBitmap(),
                                        contentDescription = "Loaded Image Thumbnail",
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(
                                            text = imageTitle ?: "Custom Photo",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        imageAnalysisInfo?.let {
                                            Text(
                                                text = it,
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "${currentImage.width}x${currentImage.height} px",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onClearImage,
                                    modifier = Modifier.testTag("clear_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Wallpaper",
                                        tint = Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action: Pick Custom Image from Photo Picker
                Button(
                    onClick = onPickImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_device_photo_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentImage != null) "Choose Another Photo" else "Select Photo from Device",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Preset Textures Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FEATURED & PROCEDURAL TEXTURES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TexturePreset.values().forEach { preset ->
                        val isCurrent = imageTitle == preset.title
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) Color(0xFF1E293B) else Color(0xFF0F172A),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        preset.isAsset -> Color(0xFFD97706)
                                        else -> Color(0xFF1E293B)
                                    }
                                ),
                                width = if (isCurrent || preset.isAsset) 1.5.dp else 1.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPreset(preset) }
                                .testTag("preset_texture_${preset.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = preset.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        if (preset.isAsset) {
                                            Surface(
                                                color = Color(0xFF78350F),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "FEATURED",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE68A),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = preset.noiseColor.displayName,
                                        fontSize = 9.sp,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Optical Noise Modulation Controls
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Optical Scan Modulation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Modulate noise floor with column luminance",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isImageNoiseActive,
                            onCheckedChange = onToggleImageNoise,
                            modifier = Modifier.testTag("image_noise_switch")
                        )
                    }

                    if (!isWhiteNoiseEnabled && isImageNoiseActive) {
                        Surface(
                            color = Color(0xFF451A03),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Noise floor is currently OFF. Turn Noise ON to hear optical modulation.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isImageNoiseActive,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Modulation Depth
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Modulation Depth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(imageNoiseDepth * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = imageNoiseDepth,
                                    onValueChange = onDepthChange,
                                    valueRange = 0.05f..1.0f,
                                    modifier = Modifier.testTag("image_noise_depth_slider")
                                )
                            }

                            // Scan Speed Hz
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Optical Scan Speed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        String.format("%.2f Hz (%.1fs / scan)", imageScanSpeedHz, 1.0f / imageScanSpeedHz),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Slider(
                                    value = imageScanSpeedHz,
                                    onValueChange = onScanSpeedChange,
                                    valueRange = 0.1f..4.0f,
                                    modifier = Modifier.testTag("image_scan_speed_slider")
                                )
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Scope Wallpaper Opacity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scope Wallpaper Dimming / Opacity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${(scopeBackgroundOpacity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = scopeBackgroundOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.05f..0.85f,
                        modifier = Modifier.testTag("scope_opacity_slider")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_image_noise_dialog")
            ) {
                Text("Done")
            }
        }
    )
}
