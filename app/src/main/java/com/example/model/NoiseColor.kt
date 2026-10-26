package com.example.model

enum class NoiseColor(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String
) {
    WHITE(
        "white",
        "White Noise",
        "Flat Spectrum",
        "Equal energy across all audio frequencies for crisp acoustic masking"
    ),
    PINK(
        "pink",
        "Pink Noise",
        "1/f Balanced",
        "-3 dB/octave gentle slope resembling continuous rainfall or steady wind"
    ),
    BROWN(
        "brown",
        "Brown Noise",
        "1/f² Deep Rumble",
        "-6 dB/octave deep warm roll-off resembling ocean surf or distant waterfall"
    );

    companion object {
        fun fromId(id: String): NoiseColor = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WHITE
    }
}
