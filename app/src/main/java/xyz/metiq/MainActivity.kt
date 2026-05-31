package xyz.metiq

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import xyz.metiq.ui.HomeScreen
import xyz.metiq.ui.LicensesScreen
import xyz.metiq.ui.SettingsScreen
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

private sealed interface Route {
    val depth: Int

    data object Home : Route { override val depth = 0 }
    data object Settings : Route { override val depth = 1 }
    data object Licenses : Route { override val depth = 2 }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MetiqApp
        setContent {
            val settings by app.settings.flow.collectAsState(initial = DEFAULT_SETTINGS)
            val scope = rememberCoroutineScope()
            val repo = app.settings
            MetiqTheme {
                var route by remember { mutableStateOf<Route>(Route.Home) }
                val tokens = LocalMetiqColors.current

                BackHandler(enabled = route !is Route.Home) {
                    route = when (route) {
                        Route.Licenses -> Route.Settings
                        else -> Route.Home
                    }
                }

                AnimatedContent(
                    targetState = route,
                    modifier = Modifier.fillMaxSize().background(tokens.background),
                    transitionSpec = {
                        val forward = targetState.depth > initialState.depth
                        if (forward) {
                            slideInHorizontally { it } togetherWith
                                slideOutHorizontally { -it / 4 }
                        } else {
                            slideInHorizontally { -it } togetherWith
                                slideOutHorizontally { it / 4 }
                        }
                    },
                    label = "route",
                ) { current ->
                    when (current) {
                        Route.Home -> HomeScreen(
                            settings = settings,
                            onOpenSettings = { route = Route.Settings },
                        )

                        Route.Settings -> SettingsScreen(
                            settings = settings,
                            onParticlesEnabled = { scope.launch { repo.setParticlesEnabled(it) } },
                            onDefaultColorId = { scope.launch { repo.setDefaultColorId(it) } },
                            onTimerPresets = { scope.launch { repo.setTimerPresetsSeconds(it) } },
                            onLanguageTag = { scope.launch { repo.setLanguageTag(it) } },
                            onBack = { route = Route.Home },
                            onOpenLicenses = { route = Route.Licenses },
                        )

                        Route.Licenses -> LicensesScreen(
                            onBack = { route = Route.Settings },
                        )
                    }
                }
            }
        }
    }
}
