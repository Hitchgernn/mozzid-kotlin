package com.mozzid.domain.model

/**
 * UI language. [tag] is the resource qualifier Android expects — Indonesian is
 * `in`, the legacy code Android still uses for what ISO now calls `id`.
 */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    INDONESIAN("in");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
            // Accept the modern code too, so a value written by any other layer
            // (or a future migration off the legacy tag) still resolves.
                ?: if (tag.equals("id", ignoreCase = true)) INDONESIAN else ENGLISH
    }
}

enum class ThemeBrightness { DARK, LIGHT;

    companion object {
        fun fromName(name: String?): ThemeBrightness =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DARK
    }
}

/**
 * Everything persisted in the `settings` key/value table, read as one value.
 *
 * [accentId] is a plain string rather than the presentation-layer `AppAccent`
 * enum — domain must not depend on presentation. `presentation/theme` resolves it,
 * falling back to the default accent for anything it does not recognise.
 */
data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val brightness: ThemeBrightness = ThemeBrightness.DARK,
    val accentId: String = "TEAL",
    val voiceOutput: Boolean = true,
    val backgroundListening: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
)
