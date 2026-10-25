// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.BackButton
import helium314.keyboard.latin.utils.MechanicalSoundManager
import helium314.keyboard.latin.utils.MechanicalSoundManager.SwitchType
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity

data class MechanicalPreset(
    val id: String,
    val name: String,
    val description: String,
    val recommendedSwitch: SwitchType,
    val plateColor: Color,
    val alphaKeyColor: Color,
    val modKeyColor: Color,
    val accentKeyColor: Color,
    val textColor: Color
)

val MECHANICAL_PRESETS = listOf(
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_AMBER,
        name = "Cyber Amber (MT3)",
        description = "Drop MT3 profile with slate-charcoal dishes & warm amber-gold legends",
        recommendedSwitch = SwitchType.BOX_JADE,
        plateColor = Color(0xFF181B1F),
        alphaKeyColor = Color(0xFF252C33),
        modKeyColor = Color(0xFF1E2429),
        accentKeyColor = Color(0xFFFF9E00),
        textColor = Color(0xFFFFA000)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_RETRO,
        name = "Retro Classic",
        description = "IBM Model M vintage aesthetic with cream keycaps & crimson accent",
        recommendedSwitch = SwitchType.BLUE,
        plateColor = Color(0xFFD4CFBF),
        alphaKeyColor = Color(0xFFFDFCF7),
        modKeyColor = Color(0xFFB5B0A2),
        accentKeyColor = Color(0xFFB22222),
        textColor = Color(0xFF212121)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_CYBERPUNK,
        name = "Cyberpunk Neon",
        description = "Dark obsidian base with luminous magenta & cyan keycaps",
        recommendedSwitch = SwitchType.BOX_JADE,
        plateColor = Color(0xFF110D20),
        alphaKeyColor = Color(0xFF1A162F),
        modKeyColor = Color(0xFF2D124D),
        accentKeyColor = Color(0xFF00F0FF),
        textColor = Color(0xFF00F0FF)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_MATCHA,
        name = "Matcha Green",
        description = "Aesthetic forest green plate, matcha milk alphas & cream keycaps",
        recommendedSwitch = SwitchType.BROWN,
        plateColor = Color(0xFF344234),
        alphaKeyColor = Color(0xFFF2F5EC),
        modKeyColor = Color(0xFF6B7F64),
        accentKeyColor = Color(0xFF8FA885),
        textColor = Color(0xFF243324)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_STEALTH,
        name = "Carbon Stealth",
        description = "Anodized charcoal plate with matte PBT keycaps & amber orange",
        recommendedSwitch = SwitchType.RED,
        plateColor = Color(0xFF121214),
        alphaKeyColor = Color(0xFF26262B),
        modKeyColor = Color(0xFF1A1A1E),
        accentKeyColor = Color(0xFFFF6F00),
        textColor = Color(0xFFF5F5F7)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_OLIVIA,
        name = "GMK Olivia",
        description = "Warm cream alphas, dark charcoal modifiers & rose gold accents",
        recommendedSwitch = SwitchType.BROWN,
        plateColor = Color(0xFF1C1B1D),
        alphaKeyColor = Color(0xFFF5ECE4),
        modKeyColor = Color(0xFF363438),
        accentKeyColor = Color(0xFFE8B4B8),
        textColor = Color(0xFF363438)
    ),
    MechanicalPreset(
        id = KeyboardTheme.THEME_MECH_DOLCH,
        name = "GMK Dolch",
        description = "Iconic two-tone slate grey keycaps with vibrant cyan enter key",
        recommendedSwitch = SwitchType.RED,
        plateColor = Color(0xFF1E2024),
        alphaKeyColor = Color(0xFF474B52),
        modKeyColor = Color(0xFF2D3036),
        accentKeyColor = Color(0xFF00BFA5),
        textColor = Color(0xFFECEFF4)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalKeyboardScreen(
    onClickBack: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val activity = ctx.getActivity() as? SettingsActivity
    val prefTrigger = activity?.prefChanged?.collectAsState()
    if ((prefTrigger?.value ?: 0) < 0) { /* trigger recompose */ }

    var soundEnabled by remember {
        mutableStateOf(prefs.getBoolean(MechanicalSoundManager.PREF_MECH_SOUND_ENABLED, true))
    }
    var selectedSwitch by remember {
        mutableStateOf(
            SwitchType.entries.firstOrNull {
                it.id == prefs.getString(MechanicalSoundManager.PREF_MECH_SWITCH_TYPE, MechanicalSoundManager.DEFAULT_SWITCH_TYPE)
            } ?: SwitchType.BLUE
        )
    }
    var soundVolume by remember {
        mutableFloatStateOf(prefs.getFloat(MechanicalSoundManager.PREF_MECH_SOUND_VOLUME, MechanicalSoundManager.DEFAULT_VOLUME))
    }
    var currentTheme by remember {
        mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) ?: Defaults.PREF_THEME_COLORS)
    }
    var bordersEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, true))
    }
    var testText by remember { mutableStateOf("") }

    val soundManager = remember { MechanicalSoundManager.getInstance().apply { init(ctx) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mechanical Keyboard", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onClickBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Section 1: Switch Audio & Zero-Delay Feedback
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_volume_2),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mechanical Switch Sounds",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Zero-latency acoustic feedback on every keypress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { enabled ->
                                soundEnabled = enabled
                                prefs.edit { putBoolean(MechanicalSoundManager.PREF_MECH_SOUND_ENABLED, enabled) }
                                activity?.prefChanged()
                            }
                        )
                    }

                    AnimatedVisibility(visible = soundEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Switch Profile",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            SwitchType.entries.forEach { switchType ->
                                val isSelected = selectedSwitch == switchType
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedSwitch = switchType
                                            prefs.edit { putString(MechanicalSoundManager.PREF_MECH_SWITCH_TYPE, switchType.id) }
                                            activity?.prefChanged()
                                            soundManager.testSwitchSound(switchType)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = switchType.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                        Text(
                                            text = switchType.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { soundManager.testSwitchSound(switchType) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lucide_play),
                                            contentDescription = "Test sound",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Sound Volume: ${(soundVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = soundVolume,
                                onValueChange = { vol ->
                                    soundVolume = vol
                                    prefs.edit { putFloat(MechanicalSoundManager.PREF_MECH_SOUND_VOLUME, vol) }
                                    activity?.prefChanged()
                                },
                                valueRange = 0.1f..1.0f
                            )
                        }
                    }
                }
            }

            // Section 2: Mechanical Themes Preset Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_palette),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mechanical Keycap Themes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Hand-crafted colorways inspired by iconic custom keycap sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MECHANICAL_PRESETS.forEach { preset ->
                        val isSelected = currentTheme == preset.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    currentTheme = preset.id
                                    bordersEnabled = true
                                    selectedSwitch = preset.recommendedSwitch
                                    prefs.edit {
                                        putString(Settings.PREF_THEME_COLORS, preset.id)
                                        putString(Settings.PREF_THEME_COLORS_NIGHT, preset.id)
                                        putString(Settings.PREF_THEME_STYLE, KeyboardTheme.STYLE_ROUNDED)
                                        putBoolean(Settings.PREF_THEME_KEY_BORDERS, true)
                                        putString(MechanicalSoundManager.PREF_MECH_SWITCH_TYPE, preset.recommendedSwitch.id)
                                    }
                                    KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                    activity?.prefChanged()
                                    soundManager.testSwitchSound(preset.recommendedSwitch)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini keycap preview visual
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(preset.plateColor)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(preset.alphaKeyColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("A", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = preset.textColor)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(preset.accentKeyColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lucide_check),
                                            contentDescription = "Active",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Spacing, Borders & Keycap Styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_sliders_horizontal),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Keycap Styling & Spacing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Fine-tune physical keycap borders and gaps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Key Borders Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Key Borders & 3D Bevels", fontWeight = FontWeight.SemiBold)
                            Text("Adds physical borders and depth around keycaps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = bordersEnabled,
                            onCheckedChange = { enabled ->
                                bordersEnabled = enabled
                                prefs.edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, enabled) }
                                activity?.prefChanged()
                            }
                        )
                    }

                    // Key Style Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keycap Profile", fontWeight = FontWeight.SemiBold)
                            Text("Choose between rounded keycaps or classic profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val currentStyle = prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE)
                        FilledTonalButton(
                            onClick = {
                                val newStyle = if (currentStyle == KeyboardTheme.STYLE_ROUNDED) KeyboardTheme.STYLE_MATERIAL else KeyboardTheme.STYLE_ROUNDED
                                prefs.edit { putString(Settings.PREF_THEME_STYLE, newStyle) }
                                activity?.prefChanged()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (currentStyle == KeyboardTheme.STYLE_ROUNDED) "Rounded" else "Material")
                        }
                    }
                }
            }

            // Section 4: Interactive Typing Sandbox
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Interactive Test Sandbox",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap below to test your mechanical switch sounds and keycap themes in real-time!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("Start typing to test your mechanical keyboard...") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_keyboard),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}

