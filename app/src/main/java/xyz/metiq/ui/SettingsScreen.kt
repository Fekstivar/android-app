package xyz.metiq.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.metiq.BuildConfig
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_TIMER_PRESETS
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme
import xyz.metiq.ui.theme.Inter

private data class LanguageOption(
    val tag: String?,
    @param:StringRes val labelRes: Int,
)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption(null, R.string.settings_language_system),
    LanguageOption("en", R.string.settings_language_english),
    LanguageOption("it", R.string.settings_language_italian),
    LanguageOption("es", R.string.settings_language_spanish),
    LanguageOption("fr", R.string.settings_language_french),
    LanguageOption("pt", R.string.settings_language_portuguese),
)

private const val KOFI_URL = "https://ko-fi.com/metiq"
private const val GH_SPONSORS_URL = "https://github.com/sponsors/metiq-xyz"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onTimerPresets: (List<Long>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    Scaffold(
        containerColor = tokens.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_cd),
                            tint = tokens.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.background,
                ),
            )
        },
    ) { padding ->
        SettingsContent(
            settings = settings,
            onParticlesEnabled = onParticlesEnabled,
            onTimerPresets = onTimerPresets,
            onLanguageTag = onLanguageTag,
            onOpenLicenses = onOpenLicenses,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onTimerPresets: (List<Long>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section(stringResource(R.string.settings_section_appearance)) {
            ToggleRow(
                label = stringResource(R.string.settings_particles_label),
                description = stringResource(R.string.settings_particles_description),
                checked = settings.particlesEnabled,
                onToggle = onParticlesEnabled,
            )
        }
        Section(stringResource(R.string.settings_section_language)) {
            DropdownPickerRow(
                label = stringResource(R.string.settings_language_label),
                options = LANGUAGE_OPTIONS,
                current = LANGUAGE_OPTIONS.first { it.tag == settings.languageTag },
                labelFor = { stringResource(it.labelRes) },
                onPick = { onLanguageTag(it.tag) },
            )
        }
        Section(stringResource(R.string.settings_section_timer_presets)) {
            TimerPresetsEditor(
                presetsSeconds = settings.timerPresetsSeconds,
                onChange = onTimerPresets,
            )
        }
        Section(stringResource(R.string.settings_section_support)) {
            LinkRow(
                label = stringResource(R.string.settings_rate_label, BuildConfig.STORE_NAME),
                onClick = {
                    openUrlWithFallback(
                        context,
                        BuildConfig.STORE_RATE_URL,
                        BuildConfig.STORE_RATE_FALLBACK_URL,
                    )
                },
            )
            LinkRow(
                label = stringResource(R.string.settings_donate_kofi_label),
                description = stringResource(R.string.settings_donate_description),
                onClick = { openUrl(context, KOFI_URL) },
            )
            LinkRow(
                label = stringResource(R.string.settings_donate_github_label),
                onClick = { openUrl(context, GH_SPONSORS_URL) },
            )
        }
        Section(stringResource(R.string.settings_section_about)) {
            LabeledValue(stringResource(R.string.settings_about_version), version)
            LinkRow(
                label = stringResource(R.string.settings_about_open_licenses),
                onClick = onOpenLicenses,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.foreground)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            color = tokens.textPrimary,
            modifier = Modifier.padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
            style = TextStyle(
                fontFamily = Inter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        content()
    }
}

private val SECTION_HORIZONTAL_PADDING = 16.dp

@Composable
private fun <T> DropdownPickerRow(
    label: String,
    options: List<T>,
    current: T,
    labelFor: @Composable (T) -> String,
    onPick: (T) -> Unit,
    leadingFor: (@Composable (T) -> Unit)? = null,
) {
    val tokens = LocalMetiqColors.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingFor != null) {
                    leadingFor(current)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = labelFor(current),
                    color = tokens.textPrimary.copy(alpha = 0.7f),
                    style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = tokens.textPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = tokens.foreground,
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = labelFor(opt),
                                style = TextStyle(fontFamily = Inter, fontSize = 16.sp),
                            )
                        },
                        leadingIcon = leadingFor?.let { { it(opt) } },
                        onClick = {
                            onPick(opt)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(textColor = tokens.textPrimary),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = tokens.textPrimary,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
            )
            if (description != null) {
                Text(
                    text = description,
                    color = tokens.textPrimary.copy(alpha = 0.6f),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.background,
                checkedTrackColor = tokens.textPrimary,
                uncheckedThumbColor = tokens.textPrimary.copy(alpha = 0.6f),
                uncheckedTrackColor = tokens.textPrimary.copy(alpha = 0.15f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun TimerPresetsEditor(
    presetsSeconds: List<Long>,
    onChange: (List<Long>) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val focusManager = LocalFocusManager.current
    val buffers = remember(presetsSeconds) {
        mutableStateOf(
            List(MAX_TIMER_PRESETS) { idx ->
                presetsSeconds.getOrNull(idx)?.let { (it / 60L).toString() } ?: ""
            }
        )
    }
    val commit: () -> Unit = {
        focusManager.clearFocus()
        val newPresets = buffers.value.mapNotNull { it.toLongOrNull()?.takeIf { v -> v > 0 } }
            .map { it * 60L }
        if (newPresets != presetsSeconds) onChange(newPresets)
    }
    Column(
        modifier = Modifier.padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buffers.value.forEachIndexed { idx, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "#${idx + 1}",
                    color = tokens.textPrimary.copy(alpha = 0.5f),
                    style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                    modifier = Modifier.width(28.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.background)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { txt ->
                            if (txt.length <= 4 && txt.all { it.isDigit() }) {
                                val updated = buffers.value.toMutableList()
                                updated[idx] = txt
                                buffers.value = updated
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = Inter,
                            fontSize = 15.sp,
                            color = tokens.textPrimary,
                        ),
                        cursorBrush = SolidColor(tokens.textPrimary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_timer_unit_min),
                    color = tokens.textPrimary.copy(alpha = 0.6f),
                    style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(tokens.textPrimary.copy(alpha = 0.12f))
                .clickable { commit() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_timer_save),
                color = tokens.textPrimary,
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
            )
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
        if (description != null) {
            Text(
                text = description,
                color = tokens.textPrimary.copy(alpha = 0.6f),
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    val tokens = LocalMetiqColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = tokens.textPrimary.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp),
        )
        Text(
            text = value,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, textAlign = TextAlign.End),
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private fun openUrlWithFallback(
    context: android.content.Context,
    primary: String,
    fallback: String
) {
    val intent = Intent(Intent.ACTION_VIEW, primary.toUri())
    val ok = runCatching { context.startActivity(intent) }.isSuccess
    if (!ok) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, fallback.toUri())) }
}

@Preview(name = "Settings", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun SettingsScreenPreview() {
    MetiqTheme {
        SettingsScreen(
            settings = DEFAULT_SETTINGS,
            onParticlesEnabled = {},
            onTimerPresets = {},
            onLanguageTag = {},
            onBack = {},
            onOpenLicenses = {},
        )
    }
}
