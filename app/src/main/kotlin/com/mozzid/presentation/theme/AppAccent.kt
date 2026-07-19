package com.mozzid.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * The four brand accents from the design. The accent swaps live and recolours the
 * whole app via [MozzColors]; nothing else in the palette changes.
 *
 * Quad = base (--accent), deep (--accent2), bright (--accent-hi), ink (--accent-ink).
 */
enum class AppAccent(
    val color: Color,
    val deep: Color,
    val bright: Color,
    val ink: Color,
) {
    TEAL(Color(0xFF2DD4BF), Color(0xFF12A695), Color(0xFF34E6D1), Color(0xFF04211D)),
    LIME(Color(0xFF84CC16), Color(0xFF4D7C0F), Color(0xFFBEF264), Color(0xFF152400)),
    AMBER(Color(0xFFF5A623), Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFF2A1600)),
    INDIGO(Color(0xFF8B93F8), Color(0xFF4F46E5), Color(0xFFA5B4FC), Color(0xFF0A0F2A));

    companion object {
        fun fromName(name: String?): AppAccent =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TEAL
    }
}
