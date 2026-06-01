package xyz.metiq.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.audio.AudioRoute
import xyz.metiq.audio.AudioRouteObserver
import xyz.metiq.audio.PlaybackService
import xyz.metiq.ui.components.ParticleField
import xyz.metiq.ui.components.WaveRings
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqColors
import xyz.metiq.ui.theme.MetiqTheme
import xyz.metiq.ui.theme.NoisePalette
import xyz.metiq.ui.theme.Satoshi

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

private val BUTTON_HEIGHT: Dp = 88.dp
private const val WAVE_OVERSHOOT = 1.8f

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
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var binder by remember { mutableStateOf<PlaybackService.EngineBinder?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var startJob by remember { mutableStateOf<Job?>(null) }

    var timerRemaining by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf<TimerField?>(null) }
    var editBuffer by remember { mutableStateOf("") }
    var editInitial by remember { mutableStateOf("") }

    val routeObserver = remember { AudioRouteObserver(context) }
    val routeFlow: Flow<AudioRoute> = remember(routeObserver) { routeObserver.flow }
    val route by routeFlow.collectAsState(initial = null)

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
                val existing = bound.engine.activeLayerIds().firstOrNull()
                if (existing != null && activeId == null) {
                    activeId = existing
                }
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
            resetTimer()
        }
    }

    val activeColor = activeId?.let { id -> NOISE_COLORS.first { it.id == id } }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val particlesOn =
        activeColor != null && playing && settings.particlesEnabled && !powerSave &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val wavesOn =
        activeColor != null && playing && !powerSave &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val timerEnabled = activeId != null && playing
    val tokens = LocalMetiqColors.current
    val settingsIconCd = stringResource(R.string.home_settings_cd)

    Scaffold(containerColor = tokens.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_metiq),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(28.dp),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = settingsIconCd,
                        tint = tokens.textPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HelperText()
                Spacer(Modifier.height(24.dp))
                ColorGrid(
                    activeId = activeId,
                    wavesOn = wavesOn,
                    onSelect = { id ->
                        val title = noiseTitleById[id] ?: return@ColorGrid
                        val argb = noiseArgbById[id] ?: return@ColorGrid
                        startJob?.cancel()
                        startJob = scope.launch {
                            selectColor(
                                id, title, argb, activeId, binder, controller, resetTimer,
                            ) { activeId = it }
                        }
                    },
                )
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
            route?.let { r ->
                Text(
                    text = stringResource(R.string.home_playing_on, r.displayName),
                    color = tokens.textPrimary.copy(alpha = 0.5f),
                    style = TextStyle(fontFamily = Satoshi, fontSize = 14.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .alpha(if (playing) 1f else 0.5f),
                )
            }
        }
    }
}

@Composable
private fun HelperText() {
    Text(
        text = stringResource(R.string.home_helper),
        color = LocalMetiqColors.current.textPrimary,
        textAlign = TextAlign.Center,
        style = TextStyle(fontFamily = Satoshi, fontSize = 16.sp, lineHeight = 20.sp),
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
            style = TextStyle(fontFamily = Satoshi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
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
                        fontFamily = Satoshi,
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
                        fontFamily = Satoshi,
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
            style = TextStyle(fontFamily = Satoshi, fontSize = 14.sp),
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
            style = TextStyle(fontFamily = Satoshi, fontSize = 14.sp),
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
    b.engine.switchTo(id, "audio/$id.ogg")
}

@Preview(name = "Home", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun HomeScreenPreview() {
    MetiqTheme {
        HomeScreen(settings = DEFAULT_SETTINGS, onOpenSettings = {})
    }
}
