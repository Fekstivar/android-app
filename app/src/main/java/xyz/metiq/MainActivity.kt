package xyz.metiq

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import xyz.metiq.ui.HomeScreen
import xyz.metiq.ui.theme.MetiqTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
