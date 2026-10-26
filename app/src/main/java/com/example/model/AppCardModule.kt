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
    CARRIER("carrier", "Carrier Frequency & Waveform", "1–1000 Hz, 4 waveforms (Sine, Square, Saw, Tri)"),
    LAYERING("layering", "Harmonic Chords & Multi-Tone", "Stack harmonic intervals, chords & Solfeggio layers"),
    BINAURAL("binaural", "Binaural Beats Generator", "Independent L/R stereo carriers & entrainment"),
    MODULATION("modulation", "Modulation & Entrainment", "Isochronic pulses, AM depth & frequency");

    companion object {
        val DEFAULT_ORDER = listOf(
            OSCILLOSCOPE,
            TRANSPORT,
            CARRIER,
            LAYERING,
            BINAURAL,
            MODULATION
        )

        fun fromId(id: String): AppCardModule? = values().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
