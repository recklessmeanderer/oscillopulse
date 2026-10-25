package com.example.model

enum class ModulationMode(val displayName: String, val description: String) {
    ISOCHRONIC_PULSE(
        "Isochronic Pulse",
        "Rhythmic amplitude gating for brainwave entrainment (e.g. 40 Hz Gamma)"
    ),
    AMPLITUDE_MODULATION(
        "Amplitude Mod (AM)",
        "Continuous tremolo / synthesizer carrier modulation"
    ),
    BINAURAL_BEAT(
        "Binaural Beat",
        "Stereo frequency separation between left and right channels"
    )
}
