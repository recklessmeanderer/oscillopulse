package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.*
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppCardModule
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
    val isBinauralEnabled by viewModel.isBinauralEnabled.collectAsStateWithLifecycle()
    val leftCarrierFreq by viewModel.leftCarrierFreq.collectAsStateWithLifecycle()
    val rightCarrierFreq by viewModel.rightCarrierFreq.collectAsStateWithLifecycle()
    val isFrequenciesLinked by viewModel.isFrequenciesLinked.collectAsStateWithLifecycle()
    val isWhiteNoiseEnabled by viewModel.isWhiteNoiseEnabled.collectAsStateWithLifecycle()
    val whiteNoiseLevel by viewModel.whiteNoiseLevel.collectAsStateWithLifecycle()
    val noiseColor by viewModel.noiseColor.collectAsStateWithLifecycle()
    val isDuckingEnabled by viewModel.isDuckingEnabled.collectAsStateWithLifecycle()

    val isLayeringEnabled by viewModel.isLayeringEnabled.collectAsStateWithLifecycle()
    val activeChordPreset by viewModel.activeChordPreset.collectAsStateWithLifecycle()
    val layer2Enabled by viewModel.layer2Enabled.collectAsStateWithLifecycle()
    val layer2Freq by viewModel.layer2Freq.collectAsStateWithLifecycle()
    val layer2Wave by viewModel.layer2Wave.collectAsStateWithLifecycle()
    val layer2Gain by viewModel.layer2Gain.collectAsStateWithLifecycle()
    val layer2Interval by viewModel.layer2Interval.collectAsStateWithLifecycle()
    val layer3Enabled by viewModel.layer3Enabled.collectAsStateWithLifecycle()
    val layer3Freq by viewModel.layer3Freq.collectAsStateWithLifecycle()
    val layer3Wave by viewModel.layer3Wave.collectAsStateWithLifecycle()
    val layer3Gain by viewModel.layer3Gain.collectAsStateWithLifecycle()
    val layer3Interval by viewModel.layer3Interval.collectAsStateWithLifecycle()

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

    val cardOrder by viewModel.cardOrder.collectAsStateWithLifecycle()
    val isReorderMode by viewModel.isReorderMode.collectAsStateWithLifecycle()

    val scopeBackgroundImage by viewModel.scopeBackgroundImage.collectAsStateWithLifecycle()
    val scopeBackgroundOpacity by viewModel.scopeBackgroundOpacity.collectAsStateWithLifecycle()
    val isImageNoiseActive by viewModel.isImageNoiseActive.collectAsStateWithLifecycle()
    val imageNoiseDepth by viewModel.imageNoiseDepth.collectAsStateWithLifecycle()
    val imageScanSpeedHz by viewModel.imageScanSpeedHz.collectAsStateWithLifecycle()
    val selectedImageName by viewModel.selectedImageName.collectAsStateWithLifecycle()
    val imageAnalysisInfo by viewModel.imageAnalysisInfo.collectAsStateWithLifecycle()
    val opticalScanProgress by viewModel.opticalScanProgress.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.loadScopeImageUri(it) }
    }

    // Dialog & Sheet States
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showImageNoiseDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showRecordingsSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }
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
                        KeyEvent.KEYCODE_B -> {
                            viewModel.toggleBinauralBeat()
                            true
                        }
                        KeyEvent.KEYCODE_N -> {
                            viewModel.toggleWhiteNoise()
                            true
                        }
                        KeyEvent.KEYCODE_L -> {
                            viewModel.toggleReorderMode()
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
                    // Customize Card Layout Mode Button
                    IconButton(
                        onClick = { viewModel.toggleReorderMode() },
                        modifier = Modifier.testTag("customize_layout_button")
                    ) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Done else Icons.Default.DashboardCustomize,
                            contentDescription = if (isReorderMode) "Done Reordering" else "Reorder Cards Layout",
                            tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
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
                    FilterChip(
                        selected = isWhiteNoiseEnabled,
                        onClick = { viewModel.toggleWhiteNoise() },
                        label = {
                            Text(
                                text = if (isWhiteNoiseEnabled) "${noiseColor.displayName}: ${(whiteNoiseLevel * 100).toInt()}%" else "+ Noise Floor",
                                fontSize = 11.sp,
                                fontWeight = if (isWhiteNoiseEnabled) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Grain,
                                contentDescription = "Noise Floor",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_white_noise_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = isLayeringEnabled,
                        onClick = { viewModel.toggleLayering() },
                        label = {
                            Text(
                                text = if (isLayeringEnabled) "Chords: ${activeChordPreset.displayName}" else "+ Chords",
                                fontSize = 11.sp,
                                fontWeight = if (isLayeringEnabled) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Chords Layering",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_chords_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = scopeBackgroundImage != null || isImageNoiseActive,
                        onClick = { showImageNoiseDialog = true },
                        label = {
                            Text(
                                text = if (scopeBackgroundImage != null) "Wallpaper: ${selectedImageName ?: "Active"}" else "+ Image Wallpaper",
                                fontSize = 11.sp,
                                fontWeight = if (scopeBackgroundImage != null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Image Sonification",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_image_noise_chip")
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

                item {
                    AssistChip(
                        onClick = { showReorderSheet = true },
                        label = { Text("Reorder Cards", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Reorder Modules",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_reorder_chip")
                    )
                }
            }

            // Reorder Mode Active Banner
            AnimatedVisibility(
                visible = isReorderMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reorder_mode_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Card Layout Mode: Use up/down arrows or open reorder sheet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { showReorderSheet = true },
                                modifier = Modifier.testTag("banner_reorder_sheet_button")
                            ) {
                                Text("Sheet", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.setReorderMode(false) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("banner_done_button")
                            ) {
                                Text("Done", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Movable & Reorderable Cards
            cardOrder.forEachIndexed { index, module ->
                key(module.id) {
                    ReorderableCardWrapper(
                        module = module,
                        index = index,
                        totalCount = cardOrder.size,
                        isReorderMode = isReorderMode,
                        onMoveUp = { viewModel.moveCardUp(module) },
                        onMoveDown = { viewModel.moveCardDown(module) },
                        onOpenReorderSheet = { showReorderSheet = true }
                    ) {
                        when (module) {
                            AppCardModule.OSCILLOSCOPE -> {
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
                                    backgroundImage = scopeBackgroundImage,
                                    backgroundOpacity = scopeBackgroundOpacity,
                                    isImageNoiseActive = isImageNoiseActive,
                                    opticalScanProgress = opticalScanProgress,
                                    onOpenImageNoiseDialog = { showImageNoiseDialog = true },
                                    onToggleCollapse = { isScopeCollapsed = !isScopeCollapsed }
                                )
                            }
                            AppCardModule.TRANSPORT -> {
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
                            }
                            AppCardModule.CARRIER -> {
                                CarrierFrequencySection(
                                    frequency = carrierFreq,
                                    waveform = carrierWave,
                                    onFrequencyChange = { viewModel.setCarrierFreq(it) },
                                    onWaveformChange = { viewModel.setCarrierWave(it) },
                                    isFrequenciesLinked = isFrequenciesLinked,
                                    onToggleFrequencyLink = { viewModel.toggleFrequencyLink() }
                                )
                            }
                            AppCardModule.LAYERING -> {
                                HarmonicLayeringSection(
                                    isLayeringEnabled = isLayeringEnabled,
                                    activeChordPreset = activeChordPreset,
                                    baseCarrierFreq = carrierFreq,
                                    layer2Enabled = layer2Enabled,
                                    layer2Freq = layer2Freq,
                                    layer2Wave = layer2Wave,
                                    layer2Gain = layer2Gain,
                                    layer2Interval = layer2Interval,
                                    layer3Enabled = layer3Enabled,
                                    layer3Freq = layer3Freq,
                                    layer3Wave = layer3Wave,
                                    layer3Gain = layer3Gain,
                                    layer3Interval = layer3Interval,
                                    onToggleLayering = { viewModel.toggleLayering() },
                                    onSelectChordPreset = { viewModel.selectChordPreset(it) },
                                    onToggleLayer2 = { viewModel.setLayer2Enabled(it) },
                                    onLayer2FreqChange = { viewModel.setLayer2Freq(it) },
                                    onLayer2WaveChange = { viewModel.setLayer2Wave(it) },
                                    onLayer2GainChange = { viewModel.setLayer2Gain(it) },
                                    onLayer2IntervalChange = { viewModel.setLayer2Interval(it) },
                                    onToggleLayer3 = { viewModel.setLayer3Enabled(it) },
                                    onLayer3FreqChange = { viewModel.setLayer3Freq(it) },
                                    onLayer3WaveChange = { viewModel.setLayer3Wave(it) },
                                    onLayer3GainChange = { viewModel.setLayer3Gain(it) },
                                    onLayer3IntervalChange = { viewModel.setLayer3Interval(it) }
                                )
                            }
                            AppCardModule.BINAURAL -> {
                                BinauralBeatSection(
                                    isBinauralEnabled = isBinauralEnabled,
                                    leftCarrierFreq = leftCarrierFreq,
                                    rightCarrierFreq = rightCarrierFreq,
                                    onToggleBinaural = { viewModel.setBinauralEnabled(it) },
                                    onLeftFreqChange = { viewModel.setLeftCarrierFreq(it) },
                                    onRightFreqChange = { viewModel.setRightCarrierFreq(it) },
                                    onSetBeatDiff = { viewModel.setBinauralBeatDiff(it) },
                                    isFrequenciesLinked = isFrequenciesLinked,
                                    onToggleFrequencyLink = { viewModel.toggleFrequencyLink() }
                                )
                            }
                            AppCardModule.MODULATION -> {
                                ModulationSection(
                                    frequency = modFreq,
                                    intensity = modIntensity,
                                    waveform = modWave,
                                    mode = modMode,
                                    onFrequencyChange = { viewModel.setModFreq(it) },
                                    onIntensityChange = { viewModel.setModIntensity(it) },
                                    onWaveformChange = { viewModel.setModWave(it) },
                                    onModeChange = { viewModel.setModMode(it) },
                                    isFrequenciesLinked = isFrequenciesLinked,
                                    onToggleFrequencyLink = { viewModel.toggleFrequencyLink() }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }

    // Modal Dialogs & Sheets
    if (showShortcutsDialog) {
        KeyboardShortcutsDialog(onDismiss = { showShortcutsDialog = false })
    }

    if (showImageNoiseDialog) {
        ImageNoiseDialog(
            currentImage = scopeBackgroundImage,
            imageTitle = selectedImageName,
            imageAnalysisInfo = imageAnalysisInfo,
            isImageNoiseActive = isImageNoiseActive,
            imageNoiseDepth = imageNoiseDepth,
            imageScanSpeedHz = imageScanSpeedHz,
            scopeBackgroundOpacity = scopeBackgroundOpacity,
            isWhiteNoiseEnabled = isWhiteNoiseEnabled,
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onToggleImageNoise = { viewModel.setImageNoiseActive(it) },
            onDepthChange = { viewModel.setImageNoiseDepth(it) },
            onScanSpeedChange = { viewModel.setImageScanSpeedHz(it) },
            onOpacityChange = { viewModel.setScopeBackgroundOpacity(it) },
            onSelectPreset = { viewModel.loadPresetTexture(it) },
            onClearImage = { viewModel.clearScopeImage() },
            onDismiss = { showImageNoiseDialog = false }
        )
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
            isWhiteNoiseEnabled = isWhiteNoiseEnabled,
            whiteNoiseLevel = whiteNoiseLevel,
            noiseColor = noiseColor,
            isDuckingEnabled = isDuckingEnabled,
            onToggleDarkMode = { viewModel.toggleDarkMode(it) },
            onSelectScopeTheme = { viewModel.setScopeTheme(it) },
            onSelectSampleRate = { viewModel.setSampleRate(it) },
            onSelectBitDepth = { viewModel.setBitDepth(it) },
            onToggleWhiteNoise = { viewModel.setWhiteNoiseEnabled(it) },
            onWhiteNoiseLevelChange = { viewModel.setWhiteNoiseLevel(it) },
            onSelectNoiseColor = { viewModel.setNoiseColor(it) },
            onToggleDucking = { viewModel.setDuckingEnabled(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPresetsSheet) {
        PresetsSheet(
            presets = allPresets,
            onSelectPreset = { viewModel.applyPreset(it) },
            onDeletePreset = { viewModel.deletePreset(it) },
            onRestoreFactoryPresets = { viewModel.restoreFactoryPresets() },
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

    if (showReorderSheet) {
        ReorderModulesSheet(
            cardOrder = cardOrder,
            onMoveUp = { viewModel.moveCardUp(it) },
            onMoveDown = { viewModel.moveCardDown(it) },
            onResetOrder = { viewModel.resetCardOrder() },
            onDismiss = { showReorderSheet = false }
        )
    }
}
