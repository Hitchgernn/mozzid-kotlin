package com.mozzid.domain.model

/**
 * Disease-risk severity. Colourblind-safe by contract: every level pairs a
 * distinct glyph shape AND a text label with its colour, so meaning never rides
 * on hue alone.
 *
 * The colour is a plain ARGB value, not a Compose `Color` — `domain/` stays pure
 * Kotlin. `presentation/theme` turns it into a paintable colour along with the
 * derived bg/border/iconBg tints.
 */
enum class Severity(val glyph: String, val colorArgb: Long) {
    HIGH("▲", 0xFFFF8A7A),
    MODERATE("●", 0xFFFFCF6B),
    LOW("■", 0xFF7FD0FF);

    companion object {
        fun fromName(name: String): Severity =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LOW
    }
}
