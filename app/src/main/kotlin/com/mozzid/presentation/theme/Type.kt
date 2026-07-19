package com.mozzid.presentation.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mozzid.R

/**
 * The three design typefaces, bundled offline in res/font (no runtime fetch).
 * Serif display = Spectral, UI = IBM Plex Sans, numerics/labels = IBM Plex Mono.
 */
object MozzType {
    val Serif = FontFamily(
        Font(R.font.spectral_regular, FontWeight.Normal),
        Font(R.font.spectral_medium, FontWeight.Medium),
        Font(R.font.spectral_semibold, FontWeight.SemiBold),
        Font(R.font.spectral_bold, FontWeight.Bold),
        Font(R.font.spectral_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val Sans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    )
}
