package com.mozzid.presentation.theme

import androidx.compose.ui.graphics.Color
import com.mozzid.domain.model.Severity
import com.mozzid.domain.model.Species

/**
 * Paints the ARGB values carried by the pure-Kotlin domain models.
 *
 * These tints are fixed rather than accent-derived on purpose: severity must read
 * the same no matter which accent is active, so risk never changes appearance
 * with a theme swap. Always pair them with the glyph and text label — colour
 * alone is never allowed to carry the meaning.
 */
val Severity.color: Color get() = Color(colorArgb)

/** Panel fill behind a severity banner. */
val Severity.bg: Color get() = color.copy(alpha = 0.10f)

/** Hairline around a severity banner. */
val Severity.border: Color get() = color.copy(alpha = 0.28f)

/** Fill behind the severity glyph itself. */
val Severity.iconBg: Color get() = color.copy(alpha = 0.16f)

/** Species dot used in the log list and on map pins. */
val Species.dotColor: Color get() = Color(dotColorArgb)
