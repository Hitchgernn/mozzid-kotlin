package com.mozzid.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Convenience accessor: `MozzTheme.colors` inside any composable. */
object MozzTheme {
    val colors: MozzColors
        @Composable get() = LocalMozzColors.current
}

@Composable
fun MozzTheme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: AppAccent = AppAccent.TEAL,
    content: @Composable () -> Unit,
) {
    val colors = remember(dark, accent) { MozzColors.of(dark, accent) }

    val material = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    }

    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = MozzType.Sans, fontWeight = FontWeight.Normal),
        titleLarge = TextStyle(fontFamily = MozzType.Serif, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontFamily = MozzType.Mono, fontWeight = FontWeight.Medium),
    )

    CompositionLocalProvider(LocalMozzColors provides colors) {
        MaterialTheme(colorScheme = material, typography = typography, content = content)
    }
}
