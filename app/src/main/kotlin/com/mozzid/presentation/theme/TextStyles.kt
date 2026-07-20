package com.mozzid.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The named type ramp from the design. Sizes are the design's px values read as
 * sp so they scale with the user's font-size preference.
 *
 * Three families carry three jobs and never swap: Spectral for display and
 * species names, IBM Plex Sans for interface copy, IBM Plex Mono for anything
 * numeric or instrument-like (percentages, frequencies, timestamps).
 */
object MozzText {
    /** Screen headline — "Heard a buzz?", onboarding titles. */
    val display = TextStyle(
        fontFamily = MozzType.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.2).sp,
    )

    /** Section/tab title — "History", "Settings". */
    val title = TextStyle(
        fontFamily = MozzType.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    )

    /** Scientific names, always italic serif — the app's one editorial flourish. */
    val species = TextStyle(
        fontFamily = MozzType.Serif,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Italic,
        fontSize = 26.sp,
        lineHeight = 29.sp,
    )

    val speciesSmall = TextStyle(
        fontFamily = MozzType.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 15.5.sp,
    )

    val body = TextStyle(fontFamily = MozzType.Sans, fontSize = 14.5.sp, lineHeight = 21.sp)
    val bodySmall = TextStyle(fontFamily = MozzType.Sans, fontSize = 13.5.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontFamily = MozzType.Sans, fontSize = 12.5.sp, lineHeight = 18.sp)
    val tiny = TextStyle(fontFamily = MozzType.Sans, fontSize = 11.5.sp)

    /** Row/setting titles. */
    val rowTitle = TextStyle(
        fontFamily = MozzType.Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
    )

    /** Uppercase section label — "IDENTIFIED AS", "PREVENTION". */
    val overline = TextStyle(
        fontFamily = MozzType.Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )

    /** Big instrument readout — confidence %, frequency, totals. */
    val metric = TextStyle(
        fontFamily = MozzType.Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    )

    val metricLarge = TextStyle(
        fontFamily = MozzType.Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
    )

    val mono = TextStyle(fontFamily = MozzType.Mono, fontSize = 13.sp)
    val monoSmall = TextStyle(fontFamily = MozzType.Mono, fontSize = 11.sp)

    /** Primary button label. */
    val button = TextStyle(
        fontFamily = MozzType.Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.5.sp,
    )
}
