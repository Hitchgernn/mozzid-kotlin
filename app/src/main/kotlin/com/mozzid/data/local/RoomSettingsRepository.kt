package com.mozzid.data.local

import com.mozzid.domain.model.AppLanguage
import com.mozzid.domain.model.AppSettings
import com.mozzid.domain.model.ThemeBrightness
import com.mozzid.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [SettingsRepository] over the `settings` key/value table.
 *
 * Rows are sparse: a key is only written once the user changes it, and anything
 * absent falls back to the [AppSettings] default. That keeps a fresh install and
 * a future added setting behaving identically — no migration needed to introduce
 * one.
 */
class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {

    // Serialises read-modify-write so two concurrent updates cannot clobber
    // each other's keys.
    private val writeLock = Mutex()

    override fun watch(): Flow<AppSettings> = dao.watchAll().map { it.toSettings() }

    override suspend fun current(): AppSettings = dao.all().toSettings()

    override suspend fun update(transform: (AppSettings) -> AppSettings) = writeLock.withLock {
        val before = dao.all().toSettings()
        val after = transform(before)
        val changed = after.toRows().filterNot { (key, value) ->
            before.toRows()[key] == value
        }
        if (changed.isNotEmpty()) {
            dao.putAll(changed.map { (key, value) -> SettingEntity(key, value) })
        }
    }

    private fun List<SettingEntity>.toSettings(): AppSettings {
        val map = associate { it.key to it.value }
        val defaults = AppSettings()
        return AppSettings(
            language = AppLanguage.fromTag(map[KEY_LANGUAGE] ?: defaults.language.tag),
            brightness = ThemeBrightness.fromName(map[KEY_BRIGHTNESS] ?: defaults.brightness.name),
            accentId = map[KEY_ACCENT] ?: defaults.accentId,
            voiceOutput = map[KEY_VOICE].toBooleanOr(defaults.voiceOutput),
            backgroundListening = map[KEY_BG_LISTEN].toBooleanOr(defaults.backgroundListening),
            notificationsEnabled = map[KEY_NOTIFICATIONS].toBooleanOr(defaults.notificationsEnabled),
            onboardingComplete = map[KEY_ONBOARDED].toBooleanOr(defaults.onboardingComplete),
        )
    }

    private fun AppSettings.toRows(): Map<String, String> = mapOf(
        KEY_LANGUAGE to language.tag,
        KEY_BRIGHTNESS to brightness.name,
        KEY_ACCENT to accentId,
        KEY_VOICE to voiceOutput.toString(),
        KEY_BG_LISTEN to backgroundListening.toString(),
        KEY_NOTIFICATIONS to notificationsEnabled.toString(),
        KEY_ONBOARDED to onboardingComplete.toString(),
    )

    /** A malformed row falls back to the default rather than crashing the app. */
    private fun String?.toBooleanOr(fallback: Boolean) = this?.toBooleanStrictOrNull() ?: fallback

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_BRIGHTNESS = "brightness"
        const val KEY_ACCENT = "accent"
        const val KEY_VOICE = "voice_output"
        const val KEY_BG_LISTEN = "background_listening"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_ONBOARDED = "onboarding_complete"
    }
}
