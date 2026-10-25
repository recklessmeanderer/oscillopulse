package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppCardModule(
    val id: String,
    val title: String,
    val subtitle: String
) {
    OSCILLOSCOPE("oscilloscope", "Oscilloscope Monitor", "Real-time waveform visualizer & triggers"),
    TRANSPORT("transport", "Master Transport Bar", "Play, record, VU level meter & master volume"),
    CARRIER("carrier", "Carrier Frequency & Waveform", "1–1000 Hz, 4 waveforms, phase shift slider"),
    BINAURAL("binaural", "Binaural Beats Generator", "Independent L/R stereo carriers & entrainment"),
    HARMONICS("harmonics", "Additive Harmonics", "Up to 3 sine wave overtones with volume sliders"),
    MODULATION("modulation", "Modulation & Entrainment", "Isochronic pulses, AM depth & frequency");

    companion object {
        val DEFAULT_ORDER = listOf(
            OSCILLOSCOPE,
            TRANSPORT,
            CARRIER,
            BINAURAL,
            HARMONICS,
            MODULATION
        )

        fun fromId(id: String): AppCardModule? = values().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
