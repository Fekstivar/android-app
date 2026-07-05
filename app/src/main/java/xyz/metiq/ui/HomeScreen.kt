package xyz.metiq.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.metiq.BuildConfig
import xyz.metiq.CustomMix
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_CUSTOM_MIXES
import xyz.metiq.MAX_CUSTOM_MIX_NAME_LENGTH
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.audio.PlaybackService
import xyz.metiq.ui.components.ParticleField
import xyz.metiq.ui.components.RatePromptBanner
import xyz.metiq.ui.components.WaveRings
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqColors
import xyz.metiq.ui.theme.MetiqTheme
import xyz.metiq.ui.theme.NoisePalette
import xyz.metiq.ui.theme.Inter
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private enum class HomeTab { NOISE, AMBIENT, SETTINGS }

private data class AmbientSound(
    val id: String,
    @param:StringRes val labelRes: Int,
    val accent: Color,
    val iconVector: ImageVector? = null,
    @param:DrawableRes val iconResId: Int? = null,
)

private val AMBIENT_SOUNDS = listOf(
    AmbientSound("seawaves", R.string.ambient_seawaves, Color(0xFF3A7BD5), iconVector = Icons.Outlined.Waves),
    AmbientSound("thunderstorm", R.string.ambient_thunderstorm, Color(0xFF6C5CE7), iconVector = Icons.Outlined.Thunderstorm),
    AmbientSound("fire", R.string.ambient_fire, Color(0xFFE8662B), iconVector = Icons.Outlined.LocalFireDepartment),
    AmbientSound("birds", R.string.ambient_birds, Color(0xFF4CAF7D), iconResId = R.drawable.ic_ambient_birds),
    AmbientSound("cafe", R.string.ambient_cafe, Color(0xFFB8862B), iconVector = Icons.Outlined.Storefront),
)

private const val AMBIENT_DEFAULT_VOLUME = 0.7f
private val AMBIENT_TILE_ARGB = Color(0xFF3A7BD5).toArgb()

private data class MixPreset(
    val id: String,
    @param:StringRes val labelRes: Int,
    val layers: Map<String, Float>,
)

private val PREMADE_MIXES = listOf(
    MixPreset("cabin", R.string.mix_cabin, mapOf("fire" to 0.8f, "thunderstorm" to 0.45f)),
    MixPreset("beach", R.string.mix_beach, mapOf("seawaves" to 0.7f, "birds" to 0.5f)),
    MixPreset("bar", R.string.mix_bar, mapOf("cafe" to 0.8f, "thunderstorm" to 0.35f)),
)

private data class NoiseColor(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val noiseTitleRes: Int,
)

private val NOISE_COLORS = listOf(
    NoiseColor("pink", R.string.color_pink, R.string.noise_title_pink),
    NoiseColor("brown", R.string.color_brown, R.string.noise_title_brown),
    NoiseColor("white", R.string.color_white, R.string.noise_title_white),
    NoiseColor("grey", R.string.color_grey, R.string.noise_title_grey),
)

private val NOISE_IDS = NOISE_COLORS.map { it.id }.toSet()
private val AMBIENT_IDS = AMBIENT_SOUNDS.map { it.id }.toSet()

// A preset counts as "playing" only when the active mix matches it exactly:
// same sounds, same volumes (small epsilon for float round-trips).
private fun mixMatches(active: Map<String, Float>, layers: Map<String, Float>): Boolean {
    if (active.keys != layers.keys) return false
    return active.all { (id, v) -> abs(v - (layers[id] ?: return false)) < 0.01f }
}

@Composable
private fun paletteFor(id: String): NoisePalette {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "pink" -> tokens.noisePink
        "brown" -> tokens.noiseBrown
        "white" -> tokens.noiseWhite
        "grey" -> tokens.noiseGrey
        else -> tokens.noiseWhite
    }
}

private val BUTTON_HEIGHT: Dp = 94.dp
private val AMBIENT_CELL_WIDTH: Dp = 104.dp
private const val WAVE_OVERSHOOT = 1.8f

// Mixer bar morphs out of the "Off" pill: pill thins into a slim track, widens to the full
// cell, and a large light thumb scales in. Container height = thumb so it never clips.
private val MIXER_THUMB_SIZE: Dp = 16.dp
private val MIXER_TRACK_HEIGHT: Dp = 6.dp
private val MIXER_PILL_HEIGHT: Dp = 26.dp
private const val MIXER_OFF_WIDTH_FRACTION = 0.42f

// Total animated dots shared across all active ambient force fields (split evenly per sound).
private const val AMBIENT_PARTICLE_TOTAL = 90

private enum class TimerField {
    HOURS, MINUTES, SECONDS
}

private fun formatDecimal(n: Int): String = n.toString().padStart(2, '0')
private fun hoursFor(seconds: Long): Int = (seconds / 3600L).toInt()
private fun minutesFor(seconds: Long): Int = ((seconds / 60L) % 60L).toInt()
private fun secondsFor(seconds: Long): Int = (seconds % 60L).toInt()

@Composable
private fun presetLabel(seconds: Long): String {
    val totalMinutes = (seconds / 60L).toInt()
    val hours = totalMinutes / 60
    val remainder = totalMinutes % 60
    return when {
        hours > 0 && remainder == 0 -> stringResource(R.string.timer_preset_hours, hours)
        hours > 0 -> stringResource(R.string.timer_preset_hours_minutes, hours, remainder)
        else -> stringResource(R.string.timer_preset_minutes, totalMinutes)
    }
}

@Composable
fun HomeScreen(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onTimerPresets: (List<Long>) -> Unit,
    onCustomMixes: (List<CustomMix>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    ratePromptVisible: Boolean = false,
    onRatePromptRate: () -> Unit = {},
    onRatePromptDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLicenses by remember { mutableStateOf(false) }
    var binder by remember { mutableStateOf<PlaybackService.EngineBinder?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var startJob by remember { mutableStateOf<Job?>(null) }
    var tab by remember { mutableStateOf(HomeTab.NOISE) }
    val ambientLevels = remember { mutableStateMapOf<String, Float>() }

    var showSaveMixDialog by remember { mutableStateOf(false) }
    var pendingMixDelete by remember { mutableStateOf<CustomMix?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    var timerRemaining by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf<TimerField?>(null) }
    var editBuffer by remember { mutableStateOf("") }
    var editInitial by remember { mutableStateOf("") }

    val noiseTitleById = NOISE_COLORS.associate { it.id to stringResource(it.noiseTitleRes) }
    val resolvedTokens = LocalMetiqColors.current
    val noiseArgbById = remember(resolvedTokens) {
        mapOf(
            "pink" to resolvedTokens.noisePink.fill.toArgb(),
            "brown" to resolvedTokens.noiseBrown.fill.toArgb(),
            "white" to resolvedTokens.noiseWhite.fill.toArgb(),
            "grey" to resolvedTokens.noiseGrey.fill.toArgb(),
        )
    }

    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var powerSave by remember { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                powerSave = powerManager?.isPowerSaveMode == true
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val resetTimer: () -> Unit = {
        timerRemaining = 0L
        timerRunning = false
        editField = null
        editBuffer = ""
        editInitial = ""
    }

    val commitTimerEdit: (TimerField) -> Unit = { field ->
        if (editField == field && editBuffer.isNotBlank()) {
            val parsed = editBuffer.toIntOrNull() ?: 0
            val maxVal = if (field == TimerField.HOURS) 99 else 59
            val clamped = parsed.coerceIn(0, maxVal)
            val h = if (field == TimerField.HOURS) clamped else hoursFor(timerRemaining)
            val m = if (field == TimerField.MINUTES) clamped else minutesFor(timerRemaining)
            val s = if (field == TimerField.SECONDS) clamped else secondsFor(timerRemaining)
            timerRemaining = h * 3600L + m * 60L + s
        }
        if (editField == field) {
            editField = null
            editBuffer = ""
            editInitial = ""
        }
    }

    DisposableEffect(Unit) {
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, b: IBinder?) {
                val bound = b as PlaybackService.EngineBinder
                binder = bound
                val ids = bound.engine.activeLayerIds()
                val existingNoise = ids.firstOrNull { it in NOISE_IDS }
                if (existingNoise != null && activeId == null) {
                    activeId = existingNoise
                }
                ids.filter { it in AMBIENT_IDS }.forEach { aid ->
                    if (!ambientLevels.containsKey(aid)) {
                        ambientLevels[aid] = bound.engine.layerVolume(aid) ?: AMBIENT_DEFAULT_VOLUME
                    }
                }
                if (ambientLevels.isNotEmpty() && activeId == null) tab = HomeTab.AMBIENT
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
            }
        }
        val bindIntent = Intent(
            context, PlaybackService::class.java
        ).setAction(PlaybackService.ENGINE_BIND_ACTION)
        context.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE)

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        future.addListener({
            val c = future.get()
            controller = c
            playing = c.isPlaying
            c.addListener(listener)
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            controller?.run {
                removeListener(listener)
                release()
            }
            controller = null
            context.unbindService(conn)
        }
    }

    LaunchedEffect(timerRunning) {
        if (!timerRunning) return@LaunchedEffect
        while (timerRunning && timerRemaining > 0L) {
            delay(1000L)
            if (timerRunning) timerRemaining -= 1L
        }
        if (timerRunning && timerRemaining == 0L) {
            val c = controller ?: return@LaunchedEffect
            val b = binder ?: return@LaunchedEffect
            b.engine.stopAll()
            b.setActiveColor(null, null)
            c.stop()
            activeId = null
            ambientLevels.clear()
            resetTimer()
        }
    }

    val activeColor = activeId?.let { id -> NOISE_COLORS.firstOrNull { it.id == id } }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val onNoiseTab = tab == HomeTab.NOISE
    val particlesOn =
        onNoiseTab && activeColor != null && playing && settings.particlesEnabled && !powerSave &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val ambientWavesOn =
        !onNoiseTab && playing && !powerSave &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val ambientParticlesOn =
        !onNoiseTab && playing && settings.particlesEnabled && !powerSave &&
                ambientLevels.isNotEmpty() &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val wavesOn =
        onNoiseTab && activeColor != null && playing && !powerSave &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val timerEnabled = (activeId != null || ambientLevels.isNotEmpty()) && playing
    val tokens = LocalMetiqColors.current
    val ambientNowPlaying = stringResource(R.string.ambient_now_playing)

    val applyMix: (Map<String, Float>) -> Unit = { layers ->
        val b = binder
        val c = controller
        val valid = layers.filterKeys { it in AMBIENT_IDS }.filterValues { it > 0f }
        if (b != null && c != null && valid.isNotEmpty()) {
            activeId?.let { b.engine.stopLayer(it) }
            activeId = null
            ambientLevels.keys.toList().forEach { b.engine.stopLayer(it) }
            ambientLevels.clear()
            resetTimer()
            b.setActiveColor(ambientNowPlaying, AMBIENT_TILE_ARGB)
            b.requestAudioFocusNow()
            c.play()
            valid.forEach { (id, vol) ->
                ambientLevels[id] = vol
                scope.launch {
                    runCatching { b.engine.startLayer(id, "audio/ambient/$id.ogg", vol) }
                }
            }
        }
    }

    // Turn an ambient sound on at the given volume, switching off any active noise color
    // and starting playback if this is the first active layer.
    val activateAmbient: (String, Float) -> Unit = { id, vol ->
        val b = binder
        val c = controller
        if (b != null && c != null) {
            activeId?.let { noise ->
                b.engine.stopLayer(noise)
                activeId = null
            }
            if (ambientLevels.isEmpty()) {
                resetTimer()
                b.setActiveColor(ambientNowPlaying, AMBIENT_TILE_ARGB)
                b.requestAudioFocusNow()
                c.play()
            }
            ambientLevels[id] = vol
            scope.launch {
                runCatching { b.engine.startLayer(id, "audio/ambient/$id.ogg", vol) }
            }
        }
    }

    // Turn an ambient sound off, stopping playback when it was the last active layer.
    val disableAmbient: (String) -> Unit = { id ->
        val b = binder
        if (b != null) {
            b.engine.stopLayer(id)
            ambientLevels.remove(id)
            if (ambientLevels.isEmpty()) {
                b.setActiveColor(null, null)
                controller?.stop()
                resetTimer()
            }
        }
    }

    // Retapping the active preset: stop every sound in the mix at once.
    val stopAmbient: () -> Unit = {
        val b = binder
        if (b != null) {
            ambientLevels.keys.toList().forEach { b.engine.stopLayer(it) }
            ambientLevels.clear()
            b.setActiveColor(null, null)
            controller?.stop()
            resetTimer()
        }
    }

    // Tapping an orb starts it at the default volume, or turns it off if already active.
    // Fine level control lives in each active orb's slider (sliding to zero also turns it off).
    val tapAmbient: (String) -> Unit = { id ->
        if (ambientLevels.containsKey(id)) {
            disableAmbient(id)
        } else {
            activateAmbient(id, AMBIENT_DEFAULT_VOLUME)
        }
    }

    BackHandler(enabled = showLicenses) { showLicenses = false }
    BackHandler(enabled = !showLicenses && tab != HomeTab.NOISE) { tab = HomeTab.NOISE }

    Scaffold(
        containerColor = tokens.background,
        bottomBar = {
            if (!showLicenses) MetiqBottomBar(selected = tab, onSelect = { tab = it })
        },
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize()) {
        if (tab == HomeTab.SETTINGS) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = tokens.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                SettingsContent(
                    settings = settings,
                    onParticlesEnabled = onParticlesEnabled,
                    onTimerPresets = onTimerPresets,
                    onLanguageTag = onLanguageTag,
                    onOpenLicenses = { showLicenses = true },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Sticky note rides above the logo + app box, pushing them down rather
            // than overlaying the timer. Swiping it away gives the space back.
            AnimatedVisibility(
                visible = ratePromptVisible && !showLicenses,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                RatePromptBanner(
                    showRate = BuildConfig.STORE_SUPPORTS_RATING,
                    storeName = BuildConfig.STORE_NAME,
                    onRate = {
                        openStoreRating(context)
                        onRatePromptRate()
                    },
                    onFeedback = {
                        openUrl(context, FEEDBACK_URL)
                        onRatePromptRate()
                    },
                    onDonate = {
                        openUrl(context, KOFI_URL)
                        onRatePromptRate()
                    },
                    onDismiss = onRatePromptDismiss,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(tokens.foreground),
        ) {
            if (particlesOn) {
                ParticleField(
                    color = paletteFor(activeColor.id).fill,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (ambientParticlesOn) {
                // One force field per active sound, in its own accent. Total dot budget is
                // split evenly so N sounds each render 1/N of the dots — never more total.
                val activeAmbient = AMBIENT_SOUNDS.filter { ambientLevels.containsKey(it.id) }
                val perField = (AMBIENT_PARTICLE_TOTAL / activeAmbient.size).coerceAtLeast(1)
                activeAmbient.forEach { sound ->
                    ParticleField(
                        color = lerp(sound.accent, Color.White, 0.40f),
                        count = perField,
                        seed = sound.id.hashCode().toLong(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_metiq),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(32.dp),
                )
                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.help_cd),
                        tint = tokens.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (tab) {
                    HomeTab.NOISE -> {
                        ColorGrid(
                            activeId = activeId,
                            wavesOn = wavesOn,
                            onSelect = { id ->
                                val title = noiseTitleById[id] ?: return@ColorGrid
                                val argb = noiseArgbById[id] ?: return@ColorGrid
                                ambientLevels.clear()
                                startJob?.cancel()
                                startJob = scope.launch {
                                    selectColor(
                                        id, title, argb, activeId, binder, controller, resetTimer,
                                    ) { activeId = it }
                                }
                            },
                        )
                    }

                    HomeTab.AMBIENT -> {
                        val activeMix = ambientLevels.filterValues { it > 0f }
                        val mixIsSaved = PREMADE_MIXES.any { mixMatches(activeMix, it.layers) } ||
                                settings.customMixes.any { mixMatches(activeMix, it.layers) }
                        MixPresets(
                            customMixes = settings.customMixes,
                            activeMix = activeMix,
                            onApply = applyMix,
                            onStop = stopAmbient,
                            onDelete = { pendingMixDelete = it },
                        )
                        Spacer(Modifier.height(24.dp))
                        AmbientGrid(
                            levels = ambientLevels,
                            wavesOn = ambientWavesOn,
                            onTap = tapAmbient,
                            onVolume = { id, v ->
                                ambientLevels[id] = v
                                binder?.engine?.setLayerVolume(id, v)
                            },
                            onVolumeSettled = { id ->
                                if ((ambientLevels[id] ?: 0f) <= 0f) disableAmbient(id)
                            },
                        )
                        Spacer(Modifier.height(24.dp))
                        SaveMixButton(
                            enabled = activeMix.isNotEmpty() && !mixIsSaved &&
                                    settings.customMixes.size < MAX_CUSTOM_MIXES,
                            onClick = { showSaveMixDialog = true },
                        )
                    }

                    HomeTab.SETTINGS -> Unit
                }
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = tokens.divider,
                    thickness = 1.dp,
                )
                Spacer(Modifier.height(24.dp))
                TimerBlock(
                    enabled = timerEnabled,
                    remainingSeconds = timerRemaining,
                    running = timerRunning,
                    editField = editField,
                    editBuffer = editBuffer,
                    presetsSeconds = settings.timerPresetsSeconds,
                    onBeginEdit = { field ->
                        if (timerEnabled && !timerRunning) {
                            editField?.let { commitTimerEdit(it) }
                            editBuffer = ""
                            editInitial = ""
                            editField = field
                        }
                    },
                    onBufferChange = { editBuffer = it },
                    onCommitField = commitTimerEdit,
                    onPresetSelect = { seconds ->
                        if (timerEnabled && !timerRunning) {
                            timerRemaining = seconds
                            timerRunning = true
                            editField = null
                            editBuffer = ""
                            editInitial = ""
                        }
                    },
                    onToggleRunning = {
                        if (timerRunning) {
                            timerRunning = false
                            timerRemaining = 0L
                        } else if (timerRemaining > 0L) {
                            timerRunning = true
                        }
                    },
                )
            }
        }
        }
        }
        if (showLicenses) {
            LicensesScreen(onBack = { showLicenses = false })
        }
        pendingMixDelete?.let { mix ->
            AlertDialog(
                onDismissRequest = { pendingMixDelete = null },
                title = { Text(stringResource(R.string.mix_delete_title), fontFamily = Inter) },
                text = {
                    Text(
                        stringResource(R.string.mix_delete_message, mix.name),
                        fontFamily = Inter,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onCustomMixes(settings.customMixes - mix)
                        pendingMixDelete = null
                    }) { Text(stringResource(R.string.mix_delete_confirm), fontFamily = Inter) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingMixDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
                    }
                },
            )
        }
        if (showSaveMixDialog) {
            SaveMixDialog(
                onDismiss = { showSaveMixDialog = false },
                onSave = { name ->
                    val snapshot = ambientLevels.filterValues { it > 0f }
                    if (snapshot.isNotEmpty()) {
                        val others = settings.customMixes
                            .filterNot { it.name.equals(name, ignoreCase = true) }
                        onCustomMixes((others + CustomMix(name, snapshot)).take(MAX_CUSTOM_MIXES))
                    }
                    showSaveMixDialog = false
                },
            )
        }
        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text(stringResource(R.string.help_title), fontFamily = Inter) },
                text = {
                    Text(
                        stringResource(
                            if (tab == HomeTab.AMBIENT) R.string.ambient_helper
                            else R.string.home_helper
                        ),
                        fontFamily = Inter,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) {
                        Text(stringResource(R.string.dialog_ok), fontFamily = Inter)
                    }
                },
            )
        }
      }
    }
}

@Composable
private fun SaveMixDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_save_title), fontFamily = Inter) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_CUSTOM_MIX_NAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.mix_save_name_hint), fontFamily = Inter) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim().replace('|', ' ')) },
            ) { Text(stringResource(R.string.mix_save_confirm), fontFamily = Inter) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
            }
        },
    )
}

@Composable
private fun ColorGrid(
    activeId: String?,
    wavesOn: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NOISE_COLORS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { color ->
                    val active = activeId == color.id
                    ColorButton(
                        color = color,
                        active = active,
                        waveOn = active && wavesOn,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: NoiseColor,
    active: Boolean,
    waveOn: Boolean,
    onSelect: (String) -> Unit,
) {
    val palette = paletteFor(color.id)
    Box(
        modifier = Modifier.size(BUTTON_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        if (waveOn) {
            WaveRings(
                color = palette.fill,
                diameter = BUTTON_HEIGHT,
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .size(BUTTON_HEIGHT * WAVE_OVERSHOOT),
            )
        }
        ColorCircle(
            palette = palette,
            active = active,
            contentDescription = stringResource(color.labelRes),
            onClick = { onSelect(color.id) },
        )
    }
}

@Composable
private fun ColorCircle(
    palette: NoisePalette,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "circleBorder",
    )
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(BUTTON_HEIGHT)
            .clip(shape)
            .background(palette.fill)
            .border(borderWidth, palette.outline, shape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
    )
}

@Composable
private fun TimerBlock(
    enabled: Boolean,
    remainingSeconds: Long,
    running: Boolean,
    editField: TimerField?,
    editBuffer: String,
    presetsSeconds: List<Long>,
    onBeginEdit: (TimerField) -> Unit,
    onBufferChange: (String) -> Unit,
    onCommitField: (TimerField) -> Unit,
    onPresetSelect: (Long) -> Unit,
    onToggleRunning: () -> Unit,
) {
    val blockAlpha = if (enabled) 1f else MetiqColors.DisabledAlpha
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier.alpha(blockAlpha).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerCell(
                label = stringResource(R.string.timer_hours),
                field = TimerField.HOURS,
                liveValue = hoursFor(remainingSeconds),
                isEditing = editField == TimerField.HOURS,
                editBuffer = editBuffer,
                onBeginEdit = { onBeginEdit(TimerField.HOURS) },
                onBufferChange = onBufferChange,
                onCommit = { onCommitField(TimerField.HOURS) },
                enabled = enabled && !running,
            )
            TimerCell(
                label = stringResource(R.string.timer_minutes),
                field = TimerField.MINUTES,
                liveValue = minutesFor(remainingSeconds),
                isEditing = editField == TimerField.MINUTES,
                editBuffer = editBuffer,
                onBeginEdit = { onBeginEdit(TimerField.MINUTES) },
                onBufferChange = onBufferChange,
                onCommit = { onCommitField(TimerField.MINUTES) },
                enabled = enabled && !running,
            )
            TimerCell(
                label = stringResource(R.string.timer_seconds),
                field = TimerField.SECONDS,
                liveValue = secondsFor(remainingSeconds),
                isEditing = editField == TimerField.SECONDS,
                editBuffer = editBuffer,
                onBeginEdit = { onBeginEdit(TimerField.SECONDS) },
                onBufferChange = onBufferChange,
                onCommit = { onCommitField(TimerField.SECONDS) },
                enabled = enabled && !running,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presetsSeconds.forEach { seconds ->
                PresetChip(
                    label = presetLabel(seconds),
                    enabled = enabled && !running,
                    onClick = { onPresetSelect(seconds) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        StartStopButton(
            running = running,
            enabled = enabled && (running || remainingSeconds > 0L),
            onClick = onToggleRunning,
        )
    }
}

@Composable
private fun StartStopButton(running: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    val bg = if (running) tokens.textPrimary else tokens.textPrimary.copy(alpha = 0.12f)
    val fg = if (running) tokens.background else tokens.textPrimary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(if (running) R.string.timer_stop else R.string.timer_start),
            color = fg,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun TimerCell(
    label: String,
    @Suppress("UNUSED_PARAMETER") field: TimerField,
    liveValue: Int,
    isEditing: Boolean,
    editBuffer: String,
    onBeginEdit: () -> Unit,
    onBufferChange: (String) -> Unit,
    onCommit: () -> Unit,
    enabled: Boolean,
) {
    val tokens = LocalMetiqColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.cellBackground)
                .clickable(enabled = enabled && !isEditing) { onBeginEdit() },
            contentAlignment = Alignment.Center,
        ) {
            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                var hadFocus by remember { mutableStateOf(false) }
                BasicTextField(
                    value = editBuffer,
                    onValueChange = { txt ->
                        if (txt.length <= 2 && txt.all { it.isDigit() }) onBufferChange(txt)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Inter,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(tokens.textPrimary),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) hadFocus = true
                            else if (hadFocus) onCommit()
                        },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            } else {
                Text(
                    text = formatDecimal(liveValue),
                    color = tokens.textPrimary,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = tokens.textPrimary.copy(alpha = 0.7f),
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
    }
}

@Composable
private fun PresetChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.cellBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
    }
}

private suspend fun selectColor(
    id: String,
    title: String,
    notificationArgb: Int,
    current: String?,
    binder: PlaybackService.EngineBinder?,
    controller: MediaController?,
    resetTimer: () -> Unit,
    setActive: (String?) -> Unit,
) {
    val b = binder ?: return
    val c = controller ?: return
    if (current == id) {
        b.setActiveColor(null, null)
        setActive(null)
        resetTimer()
        b.engine.stopAll()
        c.stop()
        return
    }
    b.setActiveColor(title, notificationArgb)
    setActive(id)
    resetTimer()
    b.requestAudioFocusNow()
    c.play()
    b.engine.switchTo(id, "audio/noise/$id.ogg")
}

@Composable
private fun MetiqBottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    val tokens = LocalMetiqColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = tokens.background,
        selectedTextColor = tokens.textPrimary,
        unselectedIconColor = tokens.textPrimary.copy(alpha = 0.6f),
        unselectedTextColor = tokens.textPrimary.copy(alpha = 0.6f),
        indicatorColor = tokens.textPrimary,
    )
    NavigationBar(containerColor = tokens.background) {
        NavigationBarItem(
            selected = selected == HomeTab.NOISE,
            onClick = { onSelect(HomeTab.NOISE) },
            icon = { Icon(Icons.Outlined.GraphicEq, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_noise), fontFamily = Inter) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == HomeTab.AMBIENT,
            onClick = { onSelect(HomeTab.AMBIENT) },
            icon = { Icon(Icons.Outlined.Waves, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_ambient), fontFamily = Inter) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == HomeTab.SETTINGS,
            onClick = { onSelect(HomeTab.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_settings), fontFamily = Inter) },
            colors = itemColors,
        )
    }
}

// Horizontal padding the parent scroll column applies to its children; the preset
// row escapes it so its scroll viewport reaches the foreground card edges.
private val CONTENT_HORIZONTAL_PADDING = 20.dp
private val MIX_EDGE_FADE_WIDTH = 24.dp

@Composable
private fun MixPresets(
    customMixes: List<CustomMix>,
    activeMix: Map<String, Float>,
    onApply: (Map<String, Float>) -> Unit,
    onStop: () -> Unit,
    onDelete: (CustomMix) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val expanded =
                    constraints.maxWidth + (CONTENT_HORIZONTAL_PADDING * 2).roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = expanded, maxWidth = expanded)
                )
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Fade chips out at the strip edges, only on the side(s) with
                // more content to scroll to.
                val fade = MIX_EDGE_FADE_WIDTH.toPx()
                if (scroll.value > 0) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent, 1f to Color.Black,
                            startX = 0f, endX = fade,
                        ),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
                if (scroll.value < scroll.maxValue) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black, 1f to Color.Transparent,
                            startX = size.width - fade, endX = size.width,
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
            .horizontalScroll(scroll)
            .padding(horizontal = CONTENT_HORIZONTAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        customMixes.forEach { mix ->
            key(mix.name) {
                val active = mixMatches(activeMix, mix.layers)
                MixChip(
                    label = mix.name,
                    active = active,
                    onClick = { if (active) onStop() else onApply(mix.layers) },
                    trailingIcon = Icons.Outlined.Delete,
                    onTrailingClick = { onDelete(mix) },
                )
            }
        }
        if (customMixes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(tokens.divider),
            )
        }
        PREMADE_MIXES.forEach { preset ->
            val active = mixMatches(activeMix, preset.layers)
            MixChip(
                label = stringResource(preset.labelRes),
                active = active,
                onClick = { if (active) onStop() else onApply(preset.layers) },
            )
        }
    }
}

@Composable
private fun MixChip(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val tokens = LocalMetiqColors.current
    val background = if (active) tokens.textPrimary else tokens.cellBackground
    val content = if (active) tokens.background else tokens.textPrimary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = content,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
        if (trailingIcon != null && onTrailingClick != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = stringResource(R.string.mix_delete_confirm),
                tint = content.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTrailingClick),
            )
        }
    }
}

@Composable
private fun SaveMixButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val contentAlpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.textPrimary.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = tokens.textPrimary.copy(alpha = contentAlpha),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.mix_save_chip),
            color = tokens.textPrimary.copy(alpha = contentAlpha),
            style = TextStyle(
                fontFamily = Inter,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun AmbientGrid(
    levels: Map<String, Float>,
    wavesOn: Boolean,
    onTap: (String) -> Unit,
    onVolume: (String, Float) -> Unit,
    onVolumeSettled: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AMBIENT_SOUNDS.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                row.forEach { sound ->
                    val level = levels[sound.id]
                    AmbientOrb(
                        sound = sound,
                        active = level != null,
                        wavesOn = wavesOn,
                        volume = level ?: AMBIENT_DEFAULT_VOLUME,
                        onTap = { onTap(sound.id) },
                        onVolume = { onVolume(sound.id, it) },
                        onVolumeSettled = { onVolumeSettled(sound.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientOrb(
    sound: AmbientSound,
    active: Boolean,
    wavesOn: Boolean,
    volume: Float,
    onTap: () -> Unit,
    onVolume: (Float) -> Unit,
    onVolumeSettled: () -> Unit,
) {
    val label = stringResource(sound.labelRes)
    val orbFill = lerp(sound.accent, Color.White, 0.40f)
    val iconTint = lerp(sound.accent, Color.Black, 0.55f)
    Column(
        modifier = Modifier.width(AMBIENT_CELL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(BUTTON_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            if (active && wavesOn) {
                WaveRings(
                    color = orbFill,
                    diameter = BUTTON_HEIGHT,
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                        .size(BUTTON_HEIGHT * WAVE_OVERSHOOT),
                )
            }
            Box(
                modifier = Modifier
                    .size(BUTTON_HEIGHT)
                    .clip(CircleShape)
                    .background(orbFill)
                    .clickable(onClick = onTap)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                val iconModifier = Modifier.size(34.dp)
                when {
                    sound.iconVector != null -> Icon(
                        imageVector = sound.iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = iconModifier,
                    )

                    sound.iconResId != null -> Icon(
                        painter = painterResource(sound.iconResId),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = iconModifier,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MixerControl(
            active = active,
            volume = volume,
            accent = sound.accent,
            onVolume = onVolume,
            onVolumeSettled = onVolumeSettled,
        )
    }
}

// "Off" pill and active mixer bar are one morphing control: when a sound turns on the pill
// widens to the full cell, its label fades out, the outline thins, and a small thumb scales in.
@Composable
private fun MixerControl(
    active: Boolean,
    volume: Float,
    accent: Color,
    onVolume: (Float) -> Unit,
    onVolumeSettled: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val trackColor = tokens.cellBackground
    val thumbColor = lerp(accent, Color.White, 0.40f)
    val t by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "mixerMorph",
    )
    // Thumb pops in only after the pill has thinned and widened, and snaps out when turning off.
    val thumbScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (active) 200 else 120,
            delayMillis = if (active) 140 else 0,
        ),
        label = "mixerThumb",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MIXER_THUMB_SIZE)
            .pointerInput(active) {
                if (!active) return@pointerInput
                detectTapGestures { offset ->
                    onVolume((offset.x / size.width).coerceIn(0f, 1f))
                    onVolumeSettled()
                }
            }
            .pointerInput(active) {
                if (!active) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { onVolumeSettled() },
                ) { change, _ ->
                    change.consume()
                    onVolume((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val cy = size.height / 2f
            // Pill (off) thins down to a slim track (on) and widens to the full cell.
            val barH = MIXER_PILL_HEIGHT.toPx() +
                (MIXER_TRACK_HEIGHT.toPx() - MIXER_PILL_HEIGHT.toPx()) * t
            val barW = (MIXER_OFF_WIDTH_FRACTION + (1f - MIXER_OFF_WIDTH_FRACTION) * t) * w
            val barLeft = (w - barW) / 2f
            val corner = CornerRadius(barH / 2f, barH / 2f)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(barLeft, cy - barH / 2f),
                size = Size(barW, barH),
                cornerRadius = corner,
            )
            val thumbR = MIXER_THUMB_SIZE.toPx() / 2f
            val thumbX = (barLeft + volume * barW).coerceIn(barLeft + thumbR, barLeft + barW - thumbR)
            if (t > 0f) {
                drawRoundRect(
                    color = accent.copy(alpha = t),
                    topLeft = Offset(barLeft, cy - barH / 2f),
                    size = Size(thumbX - barLeft, barH),
                    cornerRadius = corner,
                )
            }
            val r = thumbR * thumbScale
            if (r > 0f) {
                drawCircle(color = thumbColor, radius = r, center = Offset(thumbX, cy))
            }
        }
        if (t < 0.999f) {
            Text(
                text = stringResource(R.string.ambient_off),
                color = tokens.textPrimary.copy(alpha = 0.5f * (1f - t)),
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
            )
        }
    }
}

@Preview(name = "Home", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun HomeScreenPreview() {
    MetiqTheme {
        HomeScreen(
            settings = DEFAULT_SETTINGS,
            onParticlesEnabled = {},
            onTimerPresets = {},
            onCustomMixes = {},
            onLanguageTag = {},
        )
    }
}
