package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsSheet(
    recordings: List<RecordingEntity>,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activePlayingId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            mediaPlayer?.release()
            mediaPlayer = null
            onDismiss()
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
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
                Text(
                    text = "Exported WAV Files",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${recordings.size} Files",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No exported WAV files yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Use the Record button or Export WAV to generate audio files.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    items(recordings, key = { it.id }) { rec ->
                        val isPlayingThis = activePlayingId == rec.id
                        RecordingItemCard(
                            recording = rec,
                            isPlaying = isPlayingThis,
                            onTogglePlay = {
                                if (isPlayingThis) {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    activePlayingId = null
                                } else {
                                    mediaPlayer?.release()
                                    val file = File(rec.filePath)
                                    if (file.exists()) {
                                        try {
                                            val mp = MediaPlayer().apply {
                                                setDataSource(rec.filePath)
                                                prepare()
                                                start()
                                                setOnCompletionListener {
                                                    activePlayingId = null
                                                    release()
                                                }
                                            }
                                            mediaPlayer = mp
                                            activePlayingId = rec.id
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            },
                            onShare = {
                                shareWavFile(context, rec.filePath, rec.title)
                            },
                            onDelete = {
                                if (isPlayingThis) {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    activePlayingId = null
                                }
                                onDeleteRecording(rec)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = recording.durationMs / 1000f
    val sizeMb = recording.fileSizeBytes / (1024f * 1024f)
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateStr = remember(recording.createdAt) { dateFormat.format(Date(recording.createdAt)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause circular button
            IconButton(
                onClick = onTogglePlay,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Color(0xFF090D16) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recording.carrierFreq.toInt()}Hz carrier • ${recording.modFreq}Hz mod",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format("%.1fs • %.2f MB • %d kHz/%d-bit • %s", durationSec, sizeMb, recording.sampleRate / 1000, recording.bitDepth, dateStr),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Share Button
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share WAV file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete WAV file",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun shareWavFile(context: Context, filePath: String, title: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Audio WAV"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
