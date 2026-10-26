package com.example.model

enum class HarmonicInterval(
    val id: String,
    val displayName: String,
    val ratio: Float,
    val description: String
) {
    CUSTOM("custom", "Custom Hz", 1.0f, "Free independent frequency control"),
    UNISON("unison", "Unison (1:1)", 1.0f, "Matched fundamental with detune/phasing"),
    OCTAVE_UP("octave_up", "Octave (+1)", 2.0f, "2x frequency (+12 semitones)"),
    OCTAVE_DOWN("octave_down", "Sub-Bass (-1)", 0.5f, "0.5x frequency (-12 semitones deep anchor)"),
    PERFECT_FIFTH("perfect_fifth", "Perfect 5th (3:2)", 1.5f, "Harmonic 5th (+7 semitones)"),
    PERFECT_FOURTH("perfect_fourth", "Perfect 4th (4:3)", 1.3333334f, "Harmonic 4th (+5 semitones)"),
    MAJOR_THIRD("major_third", "Major 3rd (5:4)", 1.25f, "Harmonic major 3rd (+4 semitones)"),
    MINOR_THIRD("minor_third", "Minor 3rd (6:5)", 1.2f, "Harmonic minor 3rd (+3 semitones)");

    companion object {
        fun fromId(id: String): HarmonicInterval = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CUSTOM
    }
}

enum class ChordPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val layer2Interval: HarmonicInterval,
    val layer3Interval: HarmonicInterval,
    val layer2Gain: Float,
    val layer3Gain: Float
) {
    OFF("off", "Single Tone", "Fundamental carrier only", HarmonicInterval.CUSTOM, HarmonicInterval.CUSTOM, 0.45f, 0.35f),
    OCTAVE_STACK("octaves", "Octave Shimmer", "Root + Sub-Bass (-1) + High Octave (+1)", HarmonicInterval.OCTAVE_DOWN, HarmonicInterval.OCTAVE_UP, 0.45f, 0.40f),
    POWER_FIFTH("fifth", "Fifth Power Drone", "Root + Perfect 5th (1.5x) + Octave (+1)", HarmonicInterval.PERFECT_FIFTH, HarmonicInterval.OCTAVE_UP, 0.45f, 0.35f),
    MAJOR_TRIAD("major", "Major Triad", "Root + Major 3rd (1.25x) + Perfect 5th (1.5x)", HarmonicInterval.MAJOR_THIRD, HarmonicInterval.PERFECT_FIFTH, 0.40f, 0.40f),
    MINOR_TRIAD("minor", "Minor Triad", "Root + Minor 3rd (1.2x) + Perfect 5th (1.5x)", HarmonicInterval.MINOR_THIRD, HarmonicInterval.PERFECT_FIFTH, 0.40f, 0.40f),
    SOLFEGGIO_TRIAD("solfeggio", "Solfeggio Triad", "432 Hz Root + 528 Hz Love + 639 Hz Harmony", HarmonicInterval.CUSTOM, HarmonicInterval.CUSTOM, 0.45f, 0.45f);

    companion object {
        fun fromId(id: String): ChordPreset = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
    }
}
