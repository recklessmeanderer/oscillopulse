package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShortcutInfo(val key: String, val description: String)

@Composable
fun KeyboardShortcutsDialog(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        ShortcutInfo("Space", "Start / Stop audio playback"),
        ShortcutInfo("R", "Toggle live WAV recording"),
        ShortcutInfo("M", "Mute / Unmute audio output"),
        ShortcutInfo("1 / 2 / 3 / 4", "Switch carrier waveform (Sine, Square, Saw, Tri)"),
        ShortcutInfo("Up / Down", "Increase / decrease carrier frequency (±5 Hz)"),
        ShortcutInfo("Left / Right", "Increase / decrease modulation frequency (±1 Hz)"),
        ShortcutInfo("B", "Toggle binaural beats (stereo channel split)"),
        ShortcutInfo("N", "Toggle white noise floor generator"),
        ShortcutInfo("L", "Toggle card reordering & layout customize mode"),
        ShortcutInfo("[ / ]", "Decrease / increase volume"),
        ShortcutInfo("S", "Open Save Custom Preset dialog"),
        ShortcutInfo("E", "Open WAV Export dialog"),
        ShortcutInfo("O", "Cycle oscilloscope theme"),
        ShortcutInfo("C", "Collapse / expand oscilloscope canvas"),
        ShortcutInfo("T", "Toggle oscilloscope trigger (Auto / Free)"),
        ShortcutInfo("?", "Show this Keyboard Shortcuts reference")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Professional Keyboard Shortcuts",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "External keyboard controls for fast lab and studio manipulation:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                shortcuts.forEach { shortcut ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = shortcut.key,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = shortcut.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
