package xyz.metiq

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import xyz.metiq.audio.PcmStore
import xyz.metiq.ui.HomeScreen
import xyz.metiq.ui.theme.MetiqTheme

private const val SPLASH_MAX_HOLD_MS = 2000L

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashShownAt = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !PcmStore.noiseReady &&
                SystemClock.uptimeMillis() - splashShownAt < SPLASH_MAX_HOLD_MS
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MetiqApp
        setContent {
            val settings by app.settings.flow.collectAsState(initial = DEFAULT_SETTINGS)
            val ratePromptVisible by app.settings.ratePromptVisible.collectAsState(initial = false)
            val scope = rememberCoroutineScope()
            val repo = app.settings
            MetiqTheme {
                HomeScreen(
                    settings = settings,
                    onParticlesEnabled = { scope.launch { repo.setParticlesEnabled(it) } },
                    onWarmth = { scope.launch { repo.setWarmth(it) } },
                    onTimerPresets = { scope.launch { repo.setTimerPresetsSeconds(it) } },
                    onCustomMixes = { scope.launch { repo.setCustomMixes(it) } },
                    onLanguageTag = { scope.launch { repo.setLanguageTag(it) } },
                    ratePromptVisible = ratePromptVisible,
                    onRatePromptRate = { scope.launch { repo.snoozeRatePrompt() } },
                    onRatePromptDismiss = { scope.launch { repo.snoozeRatePrompt() } },
                )
            }
        }
    }
}
