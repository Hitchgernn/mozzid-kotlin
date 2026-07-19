package com.mozzid.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The full design-token colour set, derived from (dark?, accent). Mirrors the CSS
 * custom properties in the original design (--bg, --surface, --text, --accent, …).
 * Provided via [LocalMozzColors] so any composable reads them and recolours live
 * when brightness or accent changes.
 */
@Immutable
data class MozzColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val text4: Color,
    val faint: Color,
    val line: Color,
    val line2: Color,
    val fill: Color,
    val accent: Color,
    val accent2: Color,
    val accentHi: Color,
    val accentInk: Color,
    val accentSoftText: Color,
) {
    /** Translucent tint of the accent (design's `color-mix(... N%, transparent)`). */
    fun accentMix(percent: Float): Color = accent.copy(alpha = percent / 100f)

    companion object {
        fun of(dark: Boolean, accent: AppAccent): MozzColors = MozzColors(
            bg = if (dark) Color(0xFF080B11) else Color(0xFFE7EBF1),
            surface = if (dark) Color(0xFF0F141D) else Color(0xFFFFFFFF),
            surface2 = if (dark) Color(0xFF0D121A) else Color(0xFFEEF2F7),
            surface3 = if (dark) Color(0xFF0C1119) else Color(0xFFFFFFFF),
            text = if (dark) Color(0xFFEEF3F9) else Color(0xFF0F1720),
            text2 = if (dark) Color(0xFFC6D0DD) else Color(0xFF33414F),
            text3 = if (dark) Color(0xFF93A1B3) else Color(0xFF5A6675),
            text4 = if (dark) Color(0xFF6B7788) else Color(0xFF8592A1),
            faint = if (dark) Color(0xFF4A5567) else Color(0xFFAEB8C4),
            line = if (dark) Color(0x0FFFFFFF) else Color(0x1A101823),
            line2 = if (dark) Color(0x1FFFFFFF) else Color(0x2E101823),
            fill = if (dark) Color(0x0AFFFFFF) else Color(0x0D101823),
            accent = accent.color,
            accent2 = accent.deep,
            accentHi = accent.bright,
            accentInk = accent.ink,
            accentSoftText = if (dark) accent.bright else accent.deep,
        )
    }
}

val LocalMozzColors = staticCompositionLocalOf { MozzColors.of(dark = true, accent = AppAccent.TEAL) }
