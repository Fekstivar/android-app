package xyz.metiq

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class Settings(
    val particlesEnabled: Boolean,
    val timerPresetsSeconds: List<Long>,
    val languageTag: String?,
)

val DEFAULT_SETTINGS = Settings(
    particlesEnabled = true,
    timerPresetsSeconds = listOf(
        15L * 60, 30L * 60, 45L * 60, 60L * 60
    ),
    languageTag = null,
)

const val MAX_TIMER_PRESETS = 4

val SUPPORTED_LANGUAGE_TAGS: List<String> = listOf("en", "it", "es", "fr", "pt")

private val Context.dataStore by preferencesDataStore(name = "metiq_settings")

private object Keys {
    val PARTICLES_ENABLED = booleanPreferencesKey("particles_enabled")
    val TIMER_PRESETS = stringPreferencesKey("timer_presets")
    val LANGUAGE_TAG = stringPreferencesKey("language_tag")
}

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val flow: Flow<Settings> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs.toSettings() }

    suspend fun setParticlesEnabled(enabled: Boolean) {
        store.edit { it[Keys.PARTICLES_ENABLED] = enabled }
    }

    suspend fun setTimerPresetsSeconds(presets: List<Long>) {
        val capped = presets.take(MAX_TIMER_PRESETS).filter { it > 0L }
        store.edit { it[Keys.TIMER_PRESETS] = capped.joinToString(",") }
    }

    suspend fun setLanguageTag(tag: String?) {
        store.edit {
            if (tag == null) it.remove(Keys.LANGUAGE_TAG)
            else it[Keys.LANGUAGE_TAG] = tag
        }
        applyLanguageTag(tag)
    }

    private fun Preferences.toSettings(): Settings {
        val particles = this[Keys.PARTICLES_ENABLED] ?: DEFAULT_SETTINGS.particlesEnabled
        val presets = this[Keys.TIMER_PRESETS]?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?.filter { it > 0L }?.take(MAX_TIMER_PRESETS)?.ifEmpty { null }
            ?: DEFAULT_SETTINGS.timerPresetsSeconds
        val languageTag = this[Keys.LANGUAGE_TAG]?.takeIf { it in SUPPORTED_LANGUAGE_TAGS }
        return Settings(
            particlesEnabled = particles,
            timerPresetsSeconds = presets,
            languageTag = languageTag,
        )
    }
}

fun applyLanguageTag(tag: String?) {
    val locales = if (tag.isNullOrBlank()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
