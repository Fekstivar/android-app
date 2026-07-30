package xyz.metiq

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.metiq.audio.PcmStore

class MetiqApp : Application() {
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        PcmStore.preloadAll(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val snapshot = settings.flow.first()
            applyLanguageTag(snapshot.languageTag)
            settings.registerLaunch()
        }
    }
}
