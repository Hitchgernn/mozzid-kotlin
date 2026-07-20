package com.mozzid.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
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
 * The in-app switch overrides the system locale and has to take effect the
 * instant it is tapped. `AppCompatDelegate.setApplicationLocales` would mean
 * depending on AppCompatActivity and recreating the Activity on our minSdk, so
 * instead this overrides the Context and Configuration the composition reads.
 * `stringResource` then resolves against the chosen locale and the tree relabels
 * on recomposition, with no restart and no flicker.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(context, configuration, language) {
        // Locale("in") is deliberate: Android's legacy tag for Indonesian, which
        // is also what the values-in/ resource folder is keyed on.
        LocalizedContext(context, Locale(language.tag), configuration)
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}

/**
 * A Context that serves resources in [locale] while still wrapping [base].
 *
 * Wrapping matters more than it looks. Compose resolves the Activity-scoped
 * owners — ActivityResultRegistryOwner, ViewModelStoreOwner,
 * OnBackPressedDispatcherOwner — by walking up the ContextWrapper chain from
 * LocalContext. `createConfigurationContext()` alone returns a *detached*
 * context, which severs that chain and makes anything needing an owner throw
 * (rememberLauncherForActivityResult dies with "No ActivityResultRegistryOwner
 * was provided"). Keeping [base] underneath preserves the walk; only
 * [getResources] is redirected.
 */
private class LocalizedContext(
    base: Context,
    locale: Locale,
    configuration: Configuration,
) : ContextWrapper(base) {

    private val localizedResources: Resources =
        base.createConfigurationContext(
            Configuration(configuration).apply { setLocale(locale) },
        ).resources

    override fun getResources(): Resources = localizedResources
}
