// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.*
import helium314.keyboard.latin.utils.MechanicalSoundManager.SwitchType
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.settings.SettingsActivity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onClickAbout: () -> Unit,
    onClickTextCorrection: () -> Unit,
    onClickPreferences: () -> Unit,
    onClickToolbar: () -> Unit,
    onClickGestureTyping: () -> Unit,
    onClickDataGathering: () -> Unit,
    onClickAdvanced: () -> Unit,
    onClickAppearance: () -> Unit,
    onClickLanguage: () -> Unit,
    onClickLayouts: () -> Unit,
    onClickDictionaries: () -> Unit,
    onClickBack: () -> Unit,
    onClickMechanicalKeyboard: () -> Unit = {},
) {
    var selectedBottomNav by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val activity = ctx.getActivity() as? SettingsActivity
    val prefTrigger = activity?.prefChanged?.collectAsState()
    if ((prefTrigger?.value ?: 0) < 0) { /* trigger recomposition */ }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                NavigationBarItem(
                    selected = selectedBottomNav == 0,
                    onClick = { selectedBottomNav = 0 },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_keyboard),
                            contentDescription = "Mech Studio",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Mech Studio",
                            fontWeight = if (selectedBottomNav == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedBottomNav == 1,
                    onClick = { selectedBottomNav = 1 },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_settings),
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = if (selectedBottomNav == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedBottomNav == 0) {
                MechanicalStudioHub(
                    ctx = ctx,
                    prefs = prefs,
                    activity = activity,
                    onClickAppearance = onClickAppearance,
                    onClickMechanicalKeyboard = onClickMechanicalKeyboard
                )
            } else {
                SettingsOverviewHub(
                    onClickAbout = onClickAbout,
                    onClickTextCorrection = onClickTextCorrection,
                    onClickPreferences = onClickPreferences,
                    onClickToolbar = onClickToolbar,
                    onClickGestureTyping = onClickGestureTyping,
                    onClickDataGathering = onClickDataGathering,
                    onClickAdvanced = onClickAdvanced,
                    onClickAppearance = onClickAppearance,
                    onClickLanguage = onClickLanguage,
                    onClickDictionaries = onClickDictionaries,
                    onClickMechanicalKeyboard = onClickMechanicalKeyboard
                )
            }
        }
    }
}

/**
 * Mech Studio Hub:
 * 1. Persistent Real-Time Mobile 3D Keyboard Preview at the top.
 * 2. 3 Clean, categorized sub-tabs below (Themes & Colors, Switch Acoustics, Size & 3D).
 */
@Composable
private fun MechanicalStudioHub(
    ctx: Context,
    prefs: android.content.SharedPreferences,
    activity: SettingsActivity?,
    onClickAppearance: () -> Unit = {},
    onClickMechanicalKeyboard: () -> Unit = {}
) {
    val soundManager = remember { MechanicalSoundManager.getInstance().apply { init(ctx) } }
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    var isImeCurrent by remember {
        mutableStateOf(UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm))
    }

    LaunchedEffect(Unit) {
        while (true) {
            isImeCurrent = UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)
            delay(1200L)
        }
    }

    // Active theme & settings state
    var currentThemeId by remember {
        mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_MECH_AMBER) ?: KeyboardTheme.THEME_MECH_AMBER)
    }
    var selectedSwitch by remember {
        mutableStateOf(soundManager.getSelectedSwitchType())
    }
    var soundVolume by remember {
        mutableFloatStateOf(soundManager.getVolume())
    }
    var playInSilent by remember {
        mutableStateOf(soundManager.isPlayInSilentMode())
    }
    var soundEnabled by remember {
        mutableStateOf(soundManager.isReadyAndEnabled())
    }
    var keycaps3D by remember {
        mutableStateOf(prefs.getBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, true))
    }
    var bordersEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, true))
    }
    var currentStyle by remember {
        mutableStateOf(prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE) ?: Defaults.PREF_THEME_STYLE)
    }
    var rgbEnabled by remember {
        mutableStateOf(prefs.getBoolean(MechanicalKeycapRenderer.PREF_MECH_RGB_ENABLED, false))
    }
    var dayNightAdaptive by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT))
    }

    val heightPrefKey = remember {
        createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, 0, 2)
    }
    var keyboardHeightScale by remember {
        mutableFloatStateOf(prefs.getFloat(heightPrefKey, 1.0f))
    }
    val gapPrefKey = remember {
        createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, 0, 2)
    }
    var keyGapScale by remember {
        mutableFloatStateOf(prefs.getFloat(gapPrefKey, 1.0f))
    }
    var vibrateOnKeypress by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_VIBRATE_ON, false))
    }

    // Interactive typed line on preview
    var typedPreviewText by remember { mutableStateOf("TAP KEYS TO TYPE") }

    // Studio Sub-Tabs: 0 = Themes & Colors, 1 = Switch Acoustics, 2 = Size & 3D
    var studioSubTab by rememberSaveable { mutableIntStateOf(0) }

    // Custom Palette Colors state
    var customPlateColor by remember {
        mutableIntStateOf(prefs.getInt("pref_mech_custom_plate", 0xFF181B1F.toInt()))
    }
    var customAlphasColor by remember {
        mutableIntStateOf(prefs.getInt("pref_mech_custom_alphas", 0xFF252C33.toInt()))
    }
    var customModsColor by remember {
        mutableIntStateOf(prefs.getInt("pref_mech_custom_mods", 0xFF1E2429.toInt()))
    }
    var customAccentColor by remember {
        mutableIntStateOf(prefs.getInt("pref_mech_custom_accent", 0xFFFF9E00.toInt()))
    }
    var customTextColor by remember {
        mutableIntStateOf(prefs.getInt("pref_mech_custom_text", 0xFFFFA000.toInt()))
    }

    val activePreset = remember(currentThemeId, customPlateColor, customAlphasColor, customModsColor, customAccentColor, customTextColor) {
        if (currentThemeId == KeyboardTheme.THEME_MECH_CUSTOM) {
            MechanicalPreset(
                id = KeyboardTheme.THEME_MECH_CUSTOM,
                name = "Custom Palette",
                description = "Personalized mechanical keycaps",
                recommendedSwitch = selectedSwitch,
                plateColor = Color(customPlateColor),
                alphaKeyColor = Color(customAlphasColor),
                modKeyColor = Color(customModsColor),
                accentKeyColor = Color(customAccentColor),
                textColor = Color(customTextColor)
            )
        } else {
            MECHANICAL_PRESETS.firstOrNull { it.id == currentThemeId } ?: MECHANICAL_PRESETS.first()
        }
    }

    // Light / Dark mode of the keyboard background. Keycaps keep their colorway in both modes.
    var themeMode by remember {
        mutableStateOf(prefs.getString(KeyboardTheme.PREF_MECH_THEME_MODE, KeyboardTheme.MECH_MODE_AUTO) ?: KeyboardTheme.MECH_MODE_AUTO)
    }
    val systemDark = isSystemInDarkTheme()
    val previewNight = when (themeMode) {
        KeyboardTheme.MECH_MODE_DARK -> true
        KeyboardTheme.MECH_MODE_LIGHT -> false
        else -> systemDark
    }
    val previewBg = remember(currentThemeId, previewNight, customPlateColor, activePreset) {
        if (currentThemeId == KeyboardTheme.THEME_MECH_CUSTOM) Color(customPlateColor)
        else KeyboardTheme.mechBackground(currentThemeId, previewNight)?.let { Color(it) } ?: activePreset.plateColor
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // --- 1-Tap Activation Banner if Keyboard was Unselected ---
        AnimatedVisibility(visible = !isImeCurrent) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { imm.showInputMethodPicker() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keyboard Not Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Tap here to activate MechBoard in 1 tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { imm.showInputMethodPicker() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Activate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Header with Current Profile Badge ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MECH KEYBOARD STUDIO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${activePreset.name} • ${selectedSwitch.title}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = if (isImeCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isImeCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (isImeCurrent) "ACTIVE IME" else "READY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isImeCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // =====================================================================
        // PERSISTENT REAL-TIME LIVE MOBILE KEYBOARD PREVIEW (ANDROID 4-ROW LAYOUT)
        // =====================================================================
        val chassisOuterColor = remember(previewBg) {
            Color(ColorUtils.blendARGB(previewBg.toArgb(), android.graphics.Color.BLACK, 0.28f))
        }
        val chassisRimColor = remember(previewBg) {
            Color(ColorUtils.blendARGB(previewBg.toArgb(), android.graphics.Color.WHITE, 0.16f))
        }
        val cavityPlateColor = remember(previewBg) {
            Color(ColorUtils.blendARGB(previewBg.toArgb(), android.graphics.Color.BLACK, 0.52f))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(chassisRimColor, chassisOuterColor, Color(0xFF0C0E12))
                    )
                )
                .border(1.5.dp, if (rgbEnabled) activePreset.accentKeyColor.copy(alpha = 0.6f) else chassisRimColor, RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                // Top Case Bar: Screws + RGB Strip / Status Line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CaseHexScrew()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (rgbEnabled) {
                                        listOf(Color(0xFFFF007F), Color(0xFFFF9E00), Color(0xFF00F0FF), Color(0xFF76FF03), Color(0xFFFF007F))
                                    } else {
                                        listOf(
                                            activePreset.accentKeyColor.copy(alpha = 0.3f),
                                            activePreset.accentKeyColor,
                                            activePreset.accentKeyColor.copy(alpha = 0.3f)
                                        )
                                    }
                                )
                            )
                    )

                    CaseHexScrew()
                }

                // Interactive Live Typing Display Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0D11))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (typedPreviewText.isEmpty()) "TAP KEYS TO TYPE & HEAR SWITCHES" else typedPreviewText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = if (typedPreviewText.isEmpty() || typedPreviewText == "TAP KEYS TO TYPE") Color.Gray else Color(0xFFECEFF4),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (typedPreviewText.isNotEmpty() && typedPreviewText != "TAP KEYS TO TYPE") {
                            Text(
                                text = "CLEAR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFF5252),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { typedPreviewText = "" }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Real Android 4-Row Mobile Mechanical Keyboard
                val previewKeyHeight = (36.dp * keyboardHeightScale).coerceIn(28.dp, 48.dp)
                val keySpacing = (2.2.dp * keyGapScale).coerceIn(1.0.dp, 4.0.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cavityPlateColor)
                        .border(1.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(keySpacing)
                ) {
                    // ROW 1: Q W E R T Y U I O P (10 keys)
                    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(keySpacing)
                    ) {
                        row1.forEach { key ->
                            Box(modifier = Modifier.weight(1f)) {
                                InteractiveMobileMechKey(
                                    label = key,
                                    preset = activePreset,
                                    keyHeight = previewKeyHeight,
                                    isRgb = rgbEnabled,
                                    onKeyPress = {
                                        if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                        typedPreviewText += key
                                        soundManager.playSwitchSound(key[0].code)
                                    }
                                )
                            }
                        }
                    }

                    // ROW 2: A S D F G H J K L (9 keys, with homing bars on F & J)
                    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(keySpacing)
                    ) {
                        row2.forEach { key ->
                            Box(modifier = Modifier.weight(1f)) {
                                InteractiveMobileMechKey(
                                    label = key,
                                    preset = activePreset,
                                    hasHomingBar = (key == "F" || key == "J"),
                                    keyHeight = previewKeyHeight,
                                    isRgb = rgbEnabled,
                                    onKeyPress = {
                                        if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                        typedPreviewText += key
                                        soundManager.playSwitchSound(key[0].code)
                                    }
                                )
                            }
                        }
                    }

                    // ROW 3: SHIFT + Z X C V B N M + BACK (Real mobile 4-row)
                    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(keySpacing)
                    ) {
                        // Shift
                        Box(modifier = Modifier.weight(1.35f)) {
                            InteractiveMobileMechKey(
                                iconRes = R.drawable.sym_keyboard_shift_rounded,
                                preset = activePreset,
                                isModifier = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = { soundManager.playSwitchSound(KeyCode.SHIFT) }
                            )
                        }
                        row3.forEach { key ->
                            Box(modifier = Modifier.weight(1f)) {
                                InteractiveMobileMechKey(
                                    label = key,
                                    preset = activePreset,
                                    keyHeight = previewKeyHeight,
                                    isRgb = rgbEnabled,
                                    onKeyPress = {
                                        if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                        typedPreviewText += key
                                        soundManager.playSwitchSound(key[0].code)
                                    }
                                )
                            }
                        }
                        // Backspace
                        Box(modifier = Modifier.weight(1.35f)) {
                            InteractiveMobileMechKey(
                                iconRes = R.drawable.sym_keyboard_delete_rounded,
                                preset = activePreset,
                                isModifier = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = {
                                    if (typedPreviewText.isNotEmpty() && typedPreviewText != "TAP KEYS TO TYPE") {
                                        typedPreviewText = typedPreviewText.dropLast(1)
                                    }
                                    soundManager.playSwitchSound(KeyCode.DELETE)
                                }
                            )
                        }
                    }

                    // ROW 4: ?123 + EMOJI + SPACE + . + ENTER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(keySpacing)
                    ) {
                        // ?123 (Symbol)
                        Box(modifier = Modifier.weight(1.4f)) {
                            InteractiveMobileMechKey(
                                label = "?123",
                                preset = activePreset,
                                isModifier = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = { soundManager.playSwitchSound(KeyCode.SYMBOL) }
                            )
                        }
                        // Comma
                        Box(modifier = Modifier.weight(1.0f)) {
                            InteractiveMobileMechKey(
                                label = ",",
                                preset = activePreset,
                                isModifier = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = {
                                    if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                    typedPreviewText += ","
                                    soundManager.playSwitchSound(','.code)
                                }
                            )
                        }
                        // Emoji
                        Box(modifier = Modifier.weight(1.1f)) {
                            InteractiveMobileMechKey(
                                iconRes = R.drawable.sym_keyboard_smiley_rounded,
                                preset = activePreset,
                                isModifier = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = { soundManager.playSwitchSound(KeyCode.EMOJI) }
                            )
                        }
                        // Spacebar
                        Box(modifier = Modifier.weight(3.6f)) {
                            InteractiveMobileMechKey(
                                label = "English (US)",
                                preset = activePreset,
                                isSpacebar = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = {
                                    if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                    typedPreviewText += " "
                                    soundManager.playSwitchSound(Constants.CODE_SPACE)
                                }
                            )
                        }
                        // Period .
                        Box(modifier = Modifier.weight(1.0f)) {
                            InteractiveMobileMechKey(
                                label = ".",
                                preset = activePreset,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = {
                                    if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                    typedPreviewText += "."
                                    soundManager.playSwitchSound('.'.code)
                                }
                            )
                        }
                        // Enter (Amber Accent)
                        Box(modifier = Modifier.weight(1.6f)) {
                            InteractiveMobileMechKey(
                                iconRes = R.drawable.sym_keyboard_return_rounded,
                                preset = activePreset,
                                isAccent = true,
                                keyHeight = previewKeyHeight,
                                isRgb = rgbEnabled,
                                onKeyPress = {
                                    if (typedPreviewText == "TAP KEYS TO TYPE") typedPreviewText = ""
                                    typedPreviewText += " "
                                    soundManager.playSwitchSound(Constants.CODE_ENTER)
                                }
                            )
                        }
                    }
                }

                // Bottom Case Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CaseHexScrew()
                    Text(
                        text = "MT3 PROFILE • SPHERICAL DISH • DOUBLESHOT PBT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.45f)
                    )
                    CaseHexScrew()
                }
            }
        }

        // =====================================================================
        // ORGANIZED SUB-TAB PILL SWITCHER (NO CLUTTER, USER-FRIENDLY)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple(0, "Themes", R.drawable.ic_lucide_palette),
                Triple(1, "Sound", R.drawable.ic_lucide_volume_2),
                Triple(2, "Size & 3D", R.drawable.ic_lucide_sliders_horizontal)
            ).forEach { (idx, title, iconRes) ->
                val isSelected = studioSubTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { studioSubTab = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // =====================================================================
        // SUB-TAB 0: THEMES, KEYCAP CUSTOMIZER & RGB
        // =====================================================================
        if (studioSubTab == 0) {
            // Adaptive Mode & RGB Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LIGHT & DARK THEME MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 3-Way Mode: Follow System / Always Dark / Always Light
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(KeyboardTheme.MECH_MODE_AUTO, "Auto", R.drawable.ic_lucide_sparkles),
                            Triple(KeyboardTheme.MECH_MODE_DARK, "Dark", R.drawable.ic_lucide_moon),
                            Triple(KeyboardTheme.MECH_MODE_LIGHT, "Light", R.drawable.ic_lucide_sun)
                        ).forEach { (modeId, modeTitle, iconRes) ->
                            val isModeSelected = themeMode == modeId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isModeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (isModeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        themeMode = modeId
                                        prefs.edit {
                                            putString(KeyboardTheme.PREF_MECH_THEME_MODE, modeId)
                                            putBoolean(Settings.PREF_THEME_DAY_NIGHT, true)
                                        }
                                        KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                        activity?.prefChanged()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isModeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = modeTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isModeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Keycaps keep their colors. Only the keyboard background switches between light and dark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // RGB Underglow Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RGB Underglow & Rim Lighting", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Radiant dynamic spectrum glow beneath keycaps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = rgbEnabled,
                            onCheckedChange = { enabled ->
                                rgbEnabled = enabled
                                prefs.edit { putBoolean(MechanicalKeycapRenderer.PREF_MECH_RGB_ENABLED, enabled) }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                activity?.prefChanged()
                            }
                        )
                    }
                }
            }

            // Mechanical Theme Presets Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "KEYCAP COLORWAYS & PROFILES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val presetRows = MECHANICAL_PRESETS.chunked(2)
                    presetRows.forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { preset ->
                                val isSelected = currentThemeId == preset.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            currentThemeId = preset.id
                                            selectedSwitch = preset.recommendedSwitch
                                            prefs.edit {
                                                putString(Settings.PREF_THEME_COLORS, preset.id)
                                                putString(Settings.PREF_THEME_COLORS_NIGHT, preset.id)
                                                putString(Settings.PREF_THEME_STYLE, KeyboardTheme.STYLE_ROUNDED)
                                                putBoolean(Settings.PREF_THEME_KEY_BORDERS, true)
                                                putBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, true)
                                                putString(MechanicalSoundManager.PREF_MECH_SWITCH_TYPE, preset.recommendedSwitch.id)
                                            }
                                            KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                            activity?.prefChanged()
                                            soundManager.testSwitchSound(preset.recommendedSwitch)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Mini 3-keycap color preview
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(preset.plateColor)
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MiniKeycapSwatch("A", preset.alphaKeyColor, preset.textColor, Modifier.weight(1f))
                                            MiniKeycapSwatch("FN", preset.modKeyColor, Color.White, Modifier.weight(1f))
                                            MiniKeycapSwatch("OK", preset.accentKeyColor, Color.White, Modifier.weight(1f))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = preset.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_lucide_check),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Keycap Color Customizer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CUSTOM KEYCAP COLOR PALETTE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pick colors for individual keycap parts (Plate, Alphas, Modifiers, Accent)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Color Pickers for Part
                    val colorSwatches = listOf(
                        0xFFFFA000.toInt() to "Amber",
                        0xFF00F0FF.toInt() to "Cyan",
                        0xFFE8B4A6.toInt() to "Rose",
                        0xFF4CAF50.toInt() to "Matcha",
                        0xFFE53935.toInt() to "Crimson",
                        0xFF9C27B0.toInt() to "Violet",
                        0xFF252C33.toInt() to "Slate",
                        0xFF181B1F.toInt() to "Obsidian",
                        0xFFFDFCF7.toInt() to "Cream",
                        0xFF00B4D8.toInt() to "Dolch Blue"
                    )

                    // Accent / Artisan Color
                    ColorSelectorRow(
                        title = "Accent Keycaps (Enter, Esc, Arrows)",
                        selectedColor = customAccentColor,
                        swatches = colorSwatches,
                        onColorSelected = { col ->
                            customAccentColor = col
                            currentThemeId = KeyboardTheme.THEME_MECH_CUSTOM
                            prefs.edit {
                                putInt("pref_mech_custom_accent", col)
                                putString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_MECH_CUSTOM)
                                putString(Settings.PREF_THEME_COLORS_NIGHT, KeyboardTheme.THEME_MECH_CUSTOM)
                            }
                            KeyboardSwitcher.getInstance().setThemeNeedsReload()
                            activity?.prefChanged()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Alpha Keycaps Color
                    ColorSelectorRow(
                        title = "Alpha Keycaps (QWERTY letters)",
                        selectedColor = customAlphasColor,
                        swatches = colorSwatches,
                        onColorSelected = { col ->
                            customAlphasColor = col
                            currentThemeId = KeyboardTheme.THEME_MECH_CUSTOM
                            prefs.edit {
                                putInt("pref_mech_custom_alphas", col)
                                putString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_MECH_CUSTOM)
                                putString(Settings.PREF_THEME_COLORS_NIGHT, KeyboardTheme.THEME_MECH_CUSTOM)
                            }
                            KeyboardSwitcher.getInstance().setThemeNeedsReload()
                            activity?.prefChanged()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Modifier Keycaps Color
                    ColorSelectorRow(
                        title = "Modifier Keycaps (Shift, Space, ?123)",
                        selectedColor = customModsColor,
                        swatches = colorSwatches,
                        onColorSelected = { col ->
                            customModsColor = col
                            currentThemeId = KeyboardTheme.THEME_MECH_CUSTOM
                            prefs.edit {
                                putInt("pref_mech_custom_mods", col)
                                putString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_MECH_CUSTOM)
                                putString(Settings.PREF_THEME_COLORS_NIGHT, KeyboardTheme.THEME_MECH_CUSTOM)
                            }
                            KeyboardSwitcher.getInstance().setThemeNeedsReload()
                            activity?.prefChanged()
                        }
                    )
                }
            }
        }

        // =====================================================================
        // SUB-TAB 1: SWITCH ACOUSTICS & VOLUME
        // =====================================================================
        if (studioSubTab == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mechanical Switch Audio", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Authentic mechanical stem & click acoustics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { enabled ->
                                soundEnabled = enabled
                                soundManager.setSoundEnabled(enabled)
                                activity?.prefChanged()
                            }
                        )
                    }

                    AnimatedVisibility(visible = soundEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Switch Grid
                            val switchPairs = SwitchType.entries.chunked(2)
                            switchPairs.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { switchType ->
                                        val isSelected = selectedSwitch == switchType
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedSwitch = switchType
                                                    soundManager.setSwitchType(switchType)
                                                    soundManager.testSwitchSound(switchType)
                                                    activity?.prefChanged()
                                                }
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = switchType.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = switchType.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Volume Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Key Click Volume", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = if (soundVolume <= 0.01f) "MUTE" else "${(soundVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = soundVolume,
                                onValueChange = { vol ->
                                    soundVolume = vol
                                    soundManager.setVolume(vol)
                                    activity?.prefChanged()
                                },
                                valueRange = 0f..1.0f
                            )

                            // Silent Mode Override
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Play Sound in Silent Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Plays switch acoustics on media stream even when phone is silent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = playInSilent,
                                    onCheckedChange = { silent ->
                                        playInSilent = silent
                                        soundManager.setPlayInSilentMode(silent)
                                        activity?.prefChanged()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SUB-TAB 2: SIZE & 3D PHYSICS
        // =====================================================================
        if (studioSubTab == 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "KEYBOARD SIZE & HEIGHT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Height Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Keyboard Height", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "${(keyboardHeightScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = keyboardHeightScale,
                            onValueChange = { scale ->
                                keyboardHeightScale = scale
                                for (i in 0..3) {
                                    prefs.edit { putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, i, 2), scale) }
                                }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                activity?.prefChanged()
                            },
                            valueRange = 0.70f..1.35f
                        )

                        // Height Quick Presets
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Compact" to 0.82f, "Normal" to 1.0f, "Tall" to 1.18f).forEach { (label, presetVal) ->
                                val isCurrent = kotlin.math.abs(keyboardHeightScale - presetVal) < 0.05f
                                FilledTonalButton(
                                    onClick = {
                                        keyboardHeightScale = presetVal
                                        for (i in 0..3) {
                                            prefs.edit { putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, i, 2), presetVal) }
                                        }
                                        KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                        activity?.prefChanged()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Key Gap / Spacing Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Key Spacing (Gaps)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "${(keyGapScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = keyGapScale,
                            onValueChange = { gap ->
                                keyGapScale = gap
                                for (i in 0..3) {
                                    prefs.edit { putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, i, 2), gap) }
                                }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                activity?.prefChanged()
                            },
                            valueRange = 0.65f..1.35f
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // 3D Sculpted Keycaps Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3D Sculpted Keycaps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("MT3 spherical scooped bowl & realistic bevel skirts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = keycaps3D,
                            onCheckedChange = { enabled ->
                                keycaps3D = enabled
                                prefs.edit { putBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, enabled) }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                activity?.prefChanged()
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Key Borders Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Key Borders & Outlines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Defines physical borders around each keycap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = bordersEnabled,
                            onCheckedChange = { enabled ->
                                bordersEnabled = enabled
                                prefs.edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, enabled) }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                activity?.prefChanged()
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Tactile Haptic Bump
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tactile Haptic Bump", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Vibration pulse to simulate physical switch actuation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = vibrateOnKeypress,
                            onCheckedChange = { enabled ->
                                vibrateOnKeypress = enabled
                                prefs.edit { putBoolean(Settings.PREF_VIBRATE_ON, enabled) }
                                activity?.prefChanged()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

/**
 * 1-Tap Swatch Row for the Keycap Color Customizer.
 */
@Composable
private fun ColorSelectorRow(
    title: String,
    selectedColor: Int,
    swatches: List<Pair<Int, String>>,
    onColorSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(selectedColor))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            swatches.forEach { (colorInt, _) ->
                val isSelected = selectedColor == colorInt
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorInt) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Mobile 3D Mechanical Keycap Preview.
 * The keycap shape is drawn by the very same [MechanicalKeycapRenderer] used on the real keyboard,
 * and icons are the same drawables as the keyboard, so the preview looks exactly like the real thing.
 */
@Composable
private fun InteractiveMobileMechKey(
    preset: MechanicalPreset,
    label: String = "",
    iconRes: Int? = null,
    isModifier: Boolean = false,
    isAccent: Boolean = false,
    isSpacebar: Boolean = false,
    hasHomingBar: Boolean = false,
    keyHeight: Dp = 36.dp,
    isRgb: Boolean = false,
    onKeyPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var xFraction by remember { mutableFloatStateOf(0f) }
    val screenWidthPx = LocalContext.current.resources.displayMetrics.widthPixels.coerceAtLeast(1)

    val baseColor = when {
        isAccent -> preset.accentKeyColor
        isModifier -> preset.modKeyColor
        else -> preset.alphaKeyColor
    }
    val contentColor = when {
        isAccent -> if (ColorUtils.calculateLuminance(baseColor.toArgb()) > 0.4) Color(0xFF1B1B1B) else Color.White
        else -> preset.textColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(keyHeight)
            .onGloballyPositioned { xFraction = it.positionInRoot().x / screenWidthPx }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKeyPress()
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { c ->
                MechanicalKeycapRenderer.drawKeycapShape(
                    c.nativeCanvas, size.width, size.height, baseColor.toArgb(), density,
                    isPressed, isSpacebar, false, hasHomingBar,
                    if (isRgb) (xFraction * 360f) % 360f else null
                )
            }
        }
        // legend / icon: always stays inside the keycap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .offset(y = if (isPressed) 1.2.dp else 0.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(minOf(keyHeight * 0.42f, 18.dp)),
                    tint = contentColor
                )
            } else if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = when {
                            label.length > 4 -> 9.sp
                            label.length > 1 -> 10.sp
                            else -> 13.sp
                        }
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun CaseHexScrew() {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(Color(0xFF181A1E))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(3.5.dp)
                .clip(CircleShape)
                .background(Color(0xFF090A0C))
        )
    }
}

@Composable
private fun MiniKeycapSwatch(
    label: String,
    keyColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val skirtColor = remember(keyColor) {
        Color(ColorUtils.blendARGB(keyColor.toArgb(), android.graphics.Color.BLACK, 0.36f))
    }
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(3.5.dp))
            .background(skirtColor)
            .padding(start = 0.8.dp, end = 0.8.dp, top = 0.5.dp, bottom = 2.2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.5.dp))
                .background(keyColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

/**
 * Clean, Organized Settings Hub (Category Cards with Lucide Icons, zero confusing clutter).
 */
@Composable
private fun SettingsOverviewHub(
    onClickAbout: () -> Unit,
    onClickTextCorrection: () -> Unit,
    onClickPreferences: () -> Unit,
    onClickToolbar: () -> Unit,
    onClickGestureTyping: () -> Unit,
    onClickDataGathering: () -> Unit,
    onClickAdvanced: () -> Unit,
    onClickAppearance: () -> Unit,
    onClickLanguage: () -> Unit,
    onClickDictionaries: () -> Unit,
    onClickMechanicalKeyboard: () -> Unit
) {
    val enabledSubtypes = SubtypeSettings.getEnabledSubtypes(true)
    val languageSummary = remember(enabledSubtypes) {
        enabledSubtypes.joinToString(", ") { it.displayName() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "SETTINGS & PREFERENCES",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "All configuration options neatly categorized",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Category 1: Mechanical Audio Studio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickMechanicalKeyboard() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_keyboard),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mechanical Audio & Switches",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Switch profiles, click volume, silent override, and 3D keycaps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_arrow_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Category 2: Appearance & Styling
        SettingsHubCard(title = "Appearance & Styling") {
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_palette,
                title = "Keyboard Themes & Colors",
                subtitle = "Key borders, spacing, height scale, and custom backgrounds",
                onClick = onClickAppearance
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_sliders_horizontal,
                title = "Top Toolbar & Shortcuts",
                subtitle = "Pinned keys, volume shortcut, quick pin toolbar keys",
                onClick = onClickToolbar
            )
        }

        // Category 3: Typing & Correction
        SettingsHubCard(title = "Typing & Correction") {
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_type,
                title = "Text Correction & Autocorrect",
                subtitle = "Suggestion strip, auto-capitalization, and caps lock reset",
                onClick = onClickTextCorrection
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_settings,
                title = "General Preferences",
                subtitle = "Vibration feedback, key popup preview, and input behavior",
                onClick = onClickPreferences
            )
            if (JniUtils.sHaveGestureLib) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingsHubRow(
                    iconRes = R.drawable.ic_lucide_mouse_pointer_click,
                    title = "Gesture & Glide Typing",
                    subtitle = "Continuous glide typing, trails, and gesture float preview",
                    onClick = onClickGestureTyping
                )
            }
        }

        // Category 4: Languages & Dictionaries
        SettingsHubCard(title = "Languages & Dictionaries") {
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_languages,
                title = "Languages & Layouts",
                subtitle = languageSummary.ifEmpty { "English (US)" },
                onClick = onClickLanguage
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_book_open,
                title = "Personal Dictionary",
                subtitle = "Custom words, vocabulary, and typing shortcuts",
                onClick = onClickDictionaries
            )
        }

        // Category 5: System & About
        SettingsHubCard(title = "System & About") {
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_shield_check,
                title = "Advanced & Backup",
                subtitle = "Backup, restore, and customization logs",
                onClick = onClickAdvanced
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            SettingsHubRow(
                iconRes = R.drawable.ic_lucide_info,
                title = "About MechBoard",
                subtitle = "Version details, licenses, and open-source credits",
                onClick = onClickAbout
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun SettingsHubCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsHubRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_lucide_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
