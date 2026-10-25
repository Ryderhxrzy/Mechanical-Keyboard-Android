// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.BackButton
import helium314.keyboard.latin.utils.MechanicalKeycapRenderer
import helium314.keyboard.latin.utils.MechanicalSoundManager
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.screens.MECHANICAL_PRESETS

/**
 * Quick settings opened from the settings icon on the keyboard.
 * Everything can be changed here without opening the main app, and the test field at the top
 * shows the result immediately with the real keyboard.
 */
class MechQuickSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cv = ComposeView(this)
        setContentView(cv)
        cv.setContent {
            Theme {
                Surface {
                    QuickSettingsScreen(
                        onClose = { finish() },
                        onOpenAll = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
        enableEdgeToEdge()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSettingsScreen(onClose: () -> Unit, onOpenAll: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val view = LocalView.current
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    val sound = remember { MechanicalSoundManager.getInstance().apply { init(ctx) } }
    val focusRequester = remember { FocusRequester() }

    /** make the real keyboard pick up the change right now */
    fun applyNow() {
        KeyboardSwitcher.getInstance().setThemeNeedsReload()
        imm.restartInput(view)
    }

    var testText by remember { mutableStateOf("") }
    var themeId by remember { mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_MECH_AMBER) ?: KeyboardTheme.THEME_MECH_AMBER) }
    var themeMode by remember { mutableStateOf(prefs.getString(KeyboardTheme.PREF_MECH_THEME_MODE, KeyboardTheme.MECH_MODE_AUTO) ?: KeyboardTheme.MECH_MODE_AUTO) }
    var selectedSwitch by remember { mutableStateOf(sound.getSelectedSwitchType()) }
    var volume by remember { mutableFloatStateOf(sound.getVolume()) }
    var soundOn by remember { mutableStateOf(sound.isReadyAndEnabled()) }
    var keycaps3D by remember { mutableStateOf(prefs.getBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, true)) }
    var rgbOn by remember { mutableStateOf(prefs.getBoolean(MechanicalKeycapRenderer.PREF_MECH_RGB_ENABLED, false)) }
    var vibrate by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_VIBRATE_ON, false)) }
    val heightKey = remember { createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, 0, 2) }
    val gapKey = remember { createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, 0, 2) }
    var heightScale by remember { mutableFloatStateOf(prefs.getFloat(heightKey, 1.0f)) }
    var gapScale by remember { mutableFloatStateOf(prefs.getFloat(gapKey, 1.0f)) }

    val subtypes = remember { SubtypeSettings.getEnabledSubtypes(true) }
    var selectedSubtype by remember { mutableStateOf(SubtypeSettings.getSelectedSubtype(prefs)) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Quick Keyboard Settings") },
                windowInsets = WindowInsets(0),
                navigationIcon = { BackButton { onClose() } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- live test field ----
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text("Test field - type here to try your keyboard") },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_lucide_keyboard), null, Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(14.dp),
                minLines = 2
            )

            // ---- layout / language ----
            QuickCard("Layout & Language") {
                if (subtypes.size > 1) {
                    ChipRow(
                        items = subtypes.map { it.displayName() },
                        selectedIndex = subtypes.indexOfFirst { it == selectedSubtype },
                        onSelect = { i ->
                            RichInputMethodManager.getInstance().onSubtypeChanged(subtypes[i])
                            selectedSubtype = subtypes[i]
                            applyNow()
                        }
                    )
                } else {
                    Text(
                        "Only one layout is enabled. Open all settings > Languages & Layouts to add more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- theme ----
            QuickCard("Theme") {
                ChipRow(
                    items = listOf("Auto", "Dark", "Light"),
                    selectedIndex = listOf(KeyboardTheme.MECH_MODE_AUTO, KeyboardTheme.MECH_MODE_DARK, KeyboardTheme.MECH_MODE_LIGHT).indexOf(themeMode),
                    onSelect = { i ->
                        val mode = listOf(KeyboardTheme.MECH_MODE_AUTO, KeyboardTheme.MECH_MODE_DARK, KeyboardTheme.MECH_MODE_LIGHT)[i]
                        themeMode = mode
                        prefs.edit {
                            putString(KeyboardTheme.PREF_MECH_THEME_MODE, mode)
                            putBoolean(Settings.PREF_THEME_DAY_NIGHT, true)
                        }
                        applyNow()
                    }
                )
                ChipRow(
                    items = MECHANICAL_PRESETS.map { it.name },
                    selectedIndex = MECHANICAL_PRESETS.indexOfFirst { it.id == themeId },
                    onSelect = { i ->
                        val preset = MECHANICAL_PRESETS[i]
                        themeId = preset.id
                        selectedSwitch = preset.recommendedSwitch
                        prefs.edit {
                            putString(Settings.PREF_THEME_COLORS, preset.id)
                            putString(Settings.PREF_THEME_COLORS_NIGHT, preset.id)
                            putString(Settings.PREF_THEME_STYLE, KeyboardTheme.STYLE_ROUNDED)
                            putBoolean(Settings.PREF_THEME_KEY_BORDERS, true)
                            putBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, true)
                            putString(MechanicalSoundManager.PREF_MECH_SWITCH_TYPE, preset.recommendedSwitch.id)
                        }
                        applyNow()
                    }
                )
            }

            // ---- size ----
            QuickCard("Size") {
                SliderRow("Keyboard height", "${(heightScale * 100).toInt()}%", heightScale, 0.70f..1.35f,
                    onChange = { v ->
                        heightScale = v
                        for (i in 0..3) prefs.edit { putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, i, 2), v) }
                    },
                    onFinished = { applyNow() })
                SliderRow("Key spacing", "${(gapScale * 100).toInt()}%", gapScale, 0.65f..1.35f,
                    onChange = { v ->
                        gapScale = v
                        for (i in 0..3) prefs.edit { putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, i, 2), v) }
                    },
                    onFinished = { applyNow() })
            }

            // ---- sound ----
            QuickCard("Sound") {
                ToggleRow("Mechanical switch sound", soundOn) {
                    soundOn = it
                    sound.setSoundEnabled(it)
                }
                if (soundOn) {
                    ChipRow(
                        items = MechanicalSoundManager.SwitchType.entries.map { it.title },
                        selectedIndex = MechanicalSoundManager.SwitchType.entries.indexOf(selectedSwitch),
                        onSelect = { i ->
                            val type = MechanicalSoundManager.SwitchType.entries[i]
                            selectedSwitch = type
                            sound.setSwitchType(type)
                            sound.testSwitchSound(type)
                        }
                    )
                    SliderRow("Volume", if (volume <= 0.01f) "MUTE" else "${(volume * 100).toInt()}%", volume, 0f..1f,
                        onChange = { volume = it; sound.setVolume(it) },
                        onFinished = { sound.testSwitchSound(selectedSwitch) })
                }
            }

            // ---- look & feel ----
            QuickCard("Look & Feel") {
                ToggleRow("3D keycaps", keycaps3D) {
                    keycaps3D = it
                    prefs.edit { putBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, it) }
                    applyNow()
                }
                ToggleRow("RGB underglow", rgbOn) {
                    rgbOn = it
                    prefs.edit { putBoolean(MechanicalKeycapRenderer.PREF_MECH_RGB_ENABLED, it) }
                    applyNow()
                }
                ToggleRow("Vibrate on keypress", vibrate) {
                    vibrate = it
                    prefs.edit { putBoolean(Settings.PREF_VIBRATE_ON, it) }
                }
            }

            Button(onClick = onOpenAll, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(painterResource(R.drawable.ic_lucide_settings), null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Open all settings")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun ChipRow(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit, onFinished: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(valueText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, onValueChangeFinished = onFinished)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

