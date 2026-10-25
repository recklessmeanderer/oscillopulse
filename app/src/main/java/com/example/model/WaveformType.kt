package com.example.model

enum class WaveformType(val displayName: String, val iconLabel: String) {
    SINE("Sine", "∿"),
    SQUARE("Square", "⊓"),
    SAWTOOTH("Sawtooth", "∧"),
    TRIANGLE("Triangle", "△");

    fun calculate(phase: Double): Float {
        val normalizedPhase = (phase % (2.0 * Math.PI) + 2.0 * Math.PI) % (2.0 * Math.PI)
        return when (this) {
            SINE -> kotlin.math.sin(normalizedPhase).toFloat()
            SQUARE -> if (normalizedPhase < Math.PI) 1.0f else -1.0f
            SAWTOOTH -> (1.0 - (normalizedPhase / Math.PI)).toFloat() // 1.0 to -1.0
            TRIANGLE -> {
                val p = normalizedPhase / Math.PI
                if (p < 1.0) {
                    (-1.0 + 2.0 * p).toFloat()
                } else {
                    (3.0 - 2.0 * p).toFloat()
                }
            }
        }
    }
}
