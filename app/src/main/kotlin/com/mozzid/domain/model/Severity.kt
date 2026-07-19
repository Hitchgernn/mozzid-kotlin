package com.mozzid.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Disease-risk severity. Colourblind-safe by contract: every level pairs a
 * distinct glyph shape AND a text label with its colour, so meaning never rides
 * on hue alone.
 */
enum class Severity(val glyph: String, val color: Color) {
    HIGH("▲", Color(0xFFFF8A7A)),      // ▲
    MODERATE("●", Color(0xFFFFCF6B)),  // ●
    LOW("■", Color(0xFF7FD0FF));       // ■

    val bg: Color get() = color.copy(alpha = 0.10f)
    val border: Color get() = color.copy(alpha = 0.28f)
    val iconBg: Color get() = color.copy(alpha = 0.16f)

    companion object {
        fun fromName(name: String): Severity =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LOW
    }
}
