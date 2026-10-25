package com.example.model

data class AudioConfig(
    val sampleRate: Int = 44100, // 22050, 44100, 48000, 96000
    val bitDepth: Int = 16       // 16-bit PCM, 24-bit PCM, 32-bit Float
) {
    companion object {
        val SUPPORTED_SAMPLE_RATES = listOf(22050, 44100, 48000, 96000)
        val SUPPORTED_BIT_DEPTHS = listOf(16, 24, 32)
    }
}

enum class ScopeTheme(val title: String) {
    CYAN("Neon Cyan"),
    GREEN("Phosphor Green"),
    AMBER("Vintage Amber"),
    WHITE("Monochrome")
}
