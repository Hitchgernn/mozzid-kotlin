package com.mozzid.presentation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mozzid.domain.model.AppLanguage
import java.util.Locale

/**
 * Applies the user's chosen [AppLanguage] to everything composed inside.
 *
 * The in-app switch overrides the system locale, and it has to take effect the
 * instant it is tapped. `AppCompatDelegate.setApplicationLocales` would mean
 * depending on AppCompatActivity and recreating the Activity on our minSdk, so
 * instead this overrides the Context and Configuration the composition reads —
 * `stringResource` then resolves against the chosen locale and the whole tree
 * relabels on recomposition, with no restart and no flicker.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(language, configuration) {
        // Locale("in") is deliberate: Android's legacy tag for Indonesian, which
        // is also what the values-in/ resource folder is keyed on.
        val locale = Locale(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(configuration).apply { setLocale(locale) }
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}
