package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ScopeTheme
import com.example.model.WaveformType
import com.example.ui.MainViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGeneratorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Collect ViewModel states
    val carrierFreq by viewModel.carrierFreq.collectAsStateWithLifecycle()
    val carrierWave by viewModel.carrierWave.collectAsStateWithLifecycle()
    val carrierPhaseOffset by viewModel.carrierPhaseOffset.collectAsStateWithLifecycle()
    val isBinauralEnabled by viewModel.isBinauralEnabled.collectAsStateWithLifecycle()
    val leftCarrierFreq by viewModel.leftCarrierFreq.collectAsStateWithLifecycle()
    val rightCarrierFreq by viewModel.rightCarrierFreq.collectAsStateWithLifecycle()
    val modFreq by viewModel.modFreq.collectAsStateWithLifecycle()
    val modIntensity by viewModel.modIntensity.collectAsStateWithLifecycle()
    val modWave by viewModel.modWave.collectAsStateWithLifecycle()
    val modMode by viewModel.modMode.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val sampleRate by viewModel.sampleRate.collectAsStateWithLifecycle()
    val bitDepth by viewModel.bitDepth.collectAsStateWithLifecycle()
    val scopeTheme by viewModel.scopeTheme.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()
    val isRecording by viewModel.audioEngine.isRecording.collectAsStateWithLifecycle()
    val recordingDurationMs by viewModel.audioEngine.recordingDurationMs.collectAsStateWithLifecycle()
    val peakLevel by viewModel.audioEngine.peakLevel.collectAsStateWithLifecycle()
    val scopeSamples by viewModel.scopeSamples.collectAsStateWithLifecycle()

    val allPresets by viewModel.allPresets.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showRecordingsSheet by remember { mutableStateOf(false) }
    var isScopeCollapsed by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(userFeedback) {
        userFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_SPACE -> {
                            viewModel.togglePlayback()
                            true
                        }
                        KeyEvent.KEYCODE_R -> {
                            viewModel.toggleRecording()
                            true
                        }
                        KeyEvent.KEYCODE_M -> {
                            viewModel.toggleMute()
                            true
                        }
                        KeyEvent.KEYCODE_1 -> {
                            viewModel.setCarrierWave(WaveformType.SINE)
                            true
                        }
                        KeyEvent.KEYCODE_2 -> {
                            viewModel.setCarrierWave(WaveformType.SQUARE)
                            true
                        }
                        KeyEvent.KEYCODE_3 -> {
                            viewModel.setCarrierWave(WaveformType.SAWTOOTH)
                            true
                        }
                        KeyEvent.KEYCODE_4 -> {
                            viewModel.setCarrierWave(WaveformType.TRIANGLE)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.setCarrierFreq(carrierFreq + 5f)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.setCarrierFreq(carrierFreq - 5f)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            viewModel.setModFreq(modFreq + 1f)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            viewModel.setModFreq(modFreq - 1f)
                            true
                        }
                        KeyEvent.KEYCODE_LEFT_BRACKET -> {
                            viewModel.setVolume((volume - 0.05f).coerceAtLeast(0f))
                            true
                        }
                        KeyEvent.KEYCODE_RIGHT_BRACKET -> {
                            viewModel.setVolume((volume + 0.05f).coerceAtMost(1f))
                            true
                        }
                        KeyEvent.KEYCODE_S -> {
                            showSavePresetDialog = true
                            true
                        }
                        KeyEvent.KEYCODE_P -> {
                            viewModel.setCarrierPhaseOffset((carrierPhaseOffset + 45f) % 360f)
                            true
                        }
                        KeyEvent.KEYCODE_B -> {
                            viewModel.toggleBinauralBeat()
                            true
                        }
                        KeyEvent.KEYCODE_E -> {
                            showExportDialog = true
                            true
                        }
                        KeyEvent.KEYCODE_O -> {
                            val nextTheme = when (scopeTheme) {
                                ScopeTheme.CYAN -> ScopeTheme.GREEN
                                ScopeTheme.GREEN -> ScopeTheme.AMBER
                                ScopeTheme.AMBER -> ScopeTheme.WHITE
                                ScopeTheme.WHITE -> ScopeTheme.CYAN
                            }
                            viewModel.setScopeTheme(nextTheme)
                            true
                        }
                        KeyEvent.KEYCODE_C -> {
                            isScopeCollapsed = !isScopeCollapsed
                            true
                        }
                        KeyEvent.KEYCODE_SLASH -> {
                            showShortcutsDialog = true
                            true
                        }
                        else -> false
                    }
                } else false
            },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OscilloPulse",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "1–1000 Hz",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Precision Carrier & Entrainment Synth",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Keyboard Shortcuts Button
                    IconButton(
                        onClick = { showShortcutsDialog = true },
                        modifier = Modifier.testTag("shortcuts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keyboard Shortcuts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Recordings Button (with badge if files exist)
                    IconButton(
                        onClick = { showRecordingsSheet = true },
                        modifier = Modifier.testTag("recordings_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (allRecordings.isNotEmpty()) {
                                    Badge { Text("${allRecordings.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Saved Recordings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Presets Library Button
                    IconButton(
                        onClick = { showPresetsSheet = true },
                        modifier = Modifier.testTag("presets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Presets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Audio Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Presets Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "QUICK PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Save Preset",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showSavePresetDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allPresets.take(6), key = { it.id }) { preset ->
                    val isCurrent = kotlin.math.abs(preset.carrierFreq - carrierFreq) < 1f &&
                            kotlin.math.abs(preset.modFreq - modFreq) < 0.5f
                    FilterChip(
                        selected = isCurrent,
                        onClick = { viewModel.applyPreset(preset) },
                        label = {
                            Text(
                                text = preset.name,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isCurrent) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.height(32.dp)
                    )
                }

                item {
                    AssistChip(
                        onClick = { showPresetsSheet = true },
                        label = { Text("All Presets (${allPresets.size})", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Real-Time Oscilloscope View
            OscilloscopeCanvas(
                samples = scopeSamples,
                isPlaying = isPlaying,
                isRecording = isRecording,
                recordingDurationMs = recordingDurationMs,
                carrierFreq = carrierFreq,
                modFreq = modFreq,
                scopeTheme = scopeTheme,
                isBinauralEnabled = isBinauralEnabled,
                leftCarrierFreq = leftCarrierFreq,
                rightCarrierFreq = rightCarrierFreq,
                isCollapsed = isScopeCollapsed,
                onToggleCollapse = { isScopeCollapsed = !isScopeCollapsed }
            )

            // Master Transport Controls (Play, Record, Volume, VU Level Meter)
            TransportBar(
                isPlaying = isPlaying,
                isRecording = isRecording,
                isMuted = isMuted,
                volume = volume,
                peakLevel = peakLevel,
                onTogglePlay = { viewModel.togglePlayback() },
                onToggleRecord = { viewModel.toggleRecording() },
                onToggleMute = { viewModel.toggleMute() },
                onVolumeChange = { viewModel.setVolume(it) },
                onOpenExport = { showExportDialog = true }
            )

            // Carrier Frequency Section (1 Hz to 1000 Hz + Waveforms + Phase Shift)
            CarrierFrequencySection(
                frequency = carrierFreq,
                waveform = carrierWave,
                phaseOffset = carrierPhaseOffset,
                onFrequencyChange = { viewModel.setCarrierFreq(it) },
                onWaveformChange = { viewModel.setCarrierWave(it) },
                onPhaseOffsetChange = { viewModel.setCarrierPhaseOffset(it) }
            )

            // Binaural Beat Generation Section (Independent Left & Right carriers)
            BinauralBeatSection(
                isBinauralEnabled = isBinauralEnabled,
                leftCarrierFreq = leftCarrierFreq,
                rightCarrierFreq = rightCarrierFreq,
                onToggleBinaural = { viewModel.setBinauralEnabled(it) },
                onLeftFreqChange = { viewModel.setLeftCarrierFreq(it) },
                onRightFreqChange = { viewModel.setRightCarrierFreq(it) },
                onSetBeatDiff = { viewModel.setBinauralBeatDiff(it) }
            )

            // Modulation & Entrainment Section
            ModulationSection(
                frequency = modFreq,
                intensity = modIntensity,
                waveform = modWave,
                mode = modMode,
                onFrequencyChange = { viewModel.setModFreq(it) },
                onIntensityChange = { viewModel.setModIntensity(it) },
                onWaveformChange = { viewModel.setModWave(it) },
                onModeChange = { viewModel.setModMode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Dialogs & Sheets
    if (showShortcutsDialog) {
        KeyboardShortcutsDialog(onDismiss = { showShortcutsDialog = false })
    }

    if (showExportDialog) {
        WavExportDialog(
            carrierFreq = carrierFreq,
            modFreq = modFreq,
            currentSampleRate = sampleRate,
            currentBitDepth = bitDepth,
            isExporting = isExporting,
            exportProgress = exportProgress,
            onDismiss = { showExportDialog = false },
            onExport = { fname, dur, sRate, bDepth ->
                viewModel.exportWav(fname, dur, sRate, bDepth) {
                    showExportDialog = false
                }
            }
        )
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            carrierFreq = carrierFreq,
            carrierWaveName = carrierWave.displayName,
            modFreq = modFreq,
            modWaveName = modWave.displayName,
            onDismiss = { showSavePresetDialog = false },
            onSave = { name, desc ->
                viewModel.savePreset(name, desc)
                showSavePresetDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isDarkMode = isDarkMode,
            scopeTheme = scopeTheme,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            onToggleDarkMode = { viewModel.toggleDarkMode(it) },
            onSelectScopeTheme = { viewModel.setScopeTheme(it) },
            onSelectSampleRate = { viewModel.setSampleRate(it) },
            onSelectBitDepth = { viewModel.setBitDepth(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPresetsSheet) {
        PresetsSheet(
            presets = allPresets,
            onSelectPreset = { viewModel.applyPreset(it) },
            onDeletePreset = { viewModel.deletePreset(it) },
            onDismiss = { showPresetsSheet = false }
        )
    }

    if (showRecordingsSheet) {
        RecordingsSheet(
            recordings = allRecordings,
            onDeleteRecording = { viewModel.deleteRecording(it) },
            onDismiss = { showRecordingsSheet = false }
        )
    }
}
