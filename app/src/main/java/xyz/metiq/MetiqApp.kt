package xyz.metiq

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MetiqApp : Application() {
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        settings = SettingsRepository(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val snapshot = settings.flow.first()
            applyLanguageTag(snapshot.languageTag)
        }
    }
}
