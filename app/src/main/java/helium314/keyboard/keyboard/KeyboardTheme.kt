/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */
package helium314.keyboard.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.AllColors
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.DefaultColors
import helium314.keyboard.latin.common.DynamicColors
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.brightenOrDarken
import helium314.keyboard.latin.utils.isBrightColor
import helium314.keyboard.latin.utils.isGoodContrast
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.EnumMap
import androidx.core.graphics.toColorInt

class KeyboardTheme // Note: The themeId should be aligned with "themeId" attribute of Keyboard style in values/themes-<style>.xml.
private constructor(val themeId: Int, @JvmField val mStyleId: Int) {
    override fun equals(other: Any?) = if (other === this) true
        else (other as? KeyboardTheme)?.themeId == themeId

    override fun hashCode(): Int {
        return themeId
    }

    companion object {
        // old themes, now called styles
        const val STYLE_MATERIAL = "Material"
        const val STYLE_HOLO = "Holo"
        const val STYLE_ROUNDED = "Rounded"

        // mechanical keyboard themes
        const val THEME_MECH_AMBER = "mech_amber"
        const val THEME_MECH_RETRO = "mech_retro"
        const val THEME_MECH_CYBERPUNK = "mech_cyberpunk"
        const val THEME_MECH_MATCHA = "mech_matcha"
        const val THEME_MECH_STEALTH = "mech_stealth"
        const val THEME_MECH_OLIVIA = "mech_olivia"
        const val THEME_MECH_DOLCH = "mech_dolch"
        const val THEME_MECH_CUSTOM = "mech_custom"

        const val PREF_MECH_THEME_MODE = "pref_mech_theme_mode" // "auto", "dark" or "light"
        const val MECH_MODE_AUTO = "auto"
        const val MECH_MODE_DARK = "dark"
        const val MECH_MODE_LIGHT = "light"

        class MechPalette(
            val bgLight: String, val bgDark: String, val accent: String,
            val key: String, val func: String, val space: String, val text: String, val hint: String
        )

        val MECH_PALETTES = mapOf(
            THEME_MECH_AMBER to MechPalette("#E9E4DC", "#14171B", "#FF9E00", "#252C33", "#1E2429", "#2C353D", "#FFA000", "#CC7A00"),
            THEME_MECH_RETRO to MechPalette("#D4CFBF", "#26251F", "#B22222", "#FDFCF7", "#B5B0A2", "#FDFCF7", "#212121", "#757575"),
            THEME_MECH_CYBERPUNK to MechPalette("#E6DFF3", "#110D20", "#FF007F", "#1A162F", "#2D124D", "#2D124D", "#00F0FF", "#FF007F"),
            THEME_MECH_MATCHA to MechPalette("#D9E3D2", "#1E2A1E", "#607D5A", "#F2F5EC", "#6B7F64", "#F2F5EC", "#243324", "#5A7056"),
            THEME_MECH_STEALTH to MechPalette("#D9D9DC", "#121214", "#FF6F00", "#26262B", "#1A1A1E", "#26262B", "#F5F5F7", "#FF8F00"),
            THEME_MECH_OLIVIA to MechPalette("#E8DED6", "#1C1A19", "#E8B4A6", "#FAF4EE", "#36312E", "#FAF4EE", "#2B2624", "#8C7A72"),
            THEME_MECH_DOLCH to MechPalette("#D3D8DD", "#181B20", "#00B4D8", "#4A5058", "#2B2F36", "#4A5058", "#F0F4F8", "#00B4D8"),
        )

        /** keyboard background (not keycap) color of a mechanical theme, as ARGB int; null for other themes */
        @JvmStatic
        fun mechBackground(themeId: String, isNight: Boolean): Int? =
            MECH_PALETTES[themeId]?.let { (if (isNight) it.bgDark else it.bgLight).toColorInt() }

        @JvmStatic
        fun isNightForMode(context: Context, prefs: SharedPreferences): Boolean =
            when (prefs.getString(PREF_MECH_THEME_MODE, MECH_MODE_AUTO)) {
                MECH_MODE_DARK -> true
                MECH_MODE_LIGHT -> false
                else -> ResourceUtils.isNight(context.resources) && prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT)
            }

        // new themes that are just colors
        const val THEME_LIGHT = "light"
        const val THEME_HOLO_WHITE = "holo_white"
        const val THEME_DARK = "dark"
        const val THEME_DARKER = "darker"
        const val THEME_BLACK = "black"
        const val THEME_DYNAMIC = "dynamic"
        const val THEME_BLUE_GRAY = "blue_gray"
        const val THEME_BROWN = "brown"
        const val THEME_CHOCOLATE = "chocolate"
        const val THEME_CLOUDY = "cloudy"
        const val THEME_FOREST = "forest"
        const val THEME_INDIGO = "indigo"
        const val THEME_OCEAN = "ocean"
        const val THEME_PINK = "pink"
        const val THEME_SAND = "sand"
        const val THEME_VIOLETTE = "violette"
        fun getAvailableDefaultColors(prefs: SharedPreferences, isNight: Boolean) = listOfNotNull(
            THEME_MECH_AMBER,
            THEME_MECH_RETRO,
            THEME_MECH_CYBERPUNK,
            THEME_MECH_MATCHA,
            THEME_MECH_STEALTH,
            THEME_MECH_OLIVIA,
            THEME_MECH_DOLCH,
            THEME_MECH_CUSTOM,
            if (!isNight) THEME_LIGHT else null, THEME_DARK,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) THEME_DYNAMIC else null,
            if (prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE) == STYLE_HOLO) THEME_HOLO_WHITE else null,
            THEME_DARKER,
            THEME_BLACK,
            if (!isNight) THEME_BLUE_GRAY else null,
            if (!isNight) THEME_BROWN else null,
            THEME_CHOCOLATE,
            THEME_CLOUDY,
            THEME_FOREST,
            if (!isNight) THEME_INDIGO else null,
            if (!isNight) THEME_PINK else null,
            THEME_OCEAN,
            if (!isNight) THEME_SAND else null,
            THEME_VIOLETTE
        )
        val STYLES = arrayOf(STYLE_MATERIAL, STYLE_HOLO, STYLE_ROUNDED)

        // These should be aligned with Keyboard.themeId and Keyboard.Case.keyboardTheme
        // attributes' values in attrs.xml.
        private const val THEME_ID_HOLO_BASE = 0
        private const val THEME_ID_LXX_BASE = 1
        private const val THEME_ID_LXX_BASE_BORDER = 2
        private const val THEME_ID_ROUNDED_BASE = 3
        private const val THEME_ID_ROUNDED_BASE_BORDER = 4
        private const val DEFAULT_THEME_ID = THEME_ID_LXX_BASE

        private val KEYBOARD_THEMES = arrayOf(
            KeyboardTheme(THEME_ID_HOLO_BASE, R.style.KeyboardTheme_HoloBase),
            KeyboardTheme(THEME_ID_LXX_BASE, R.style.KeyboardTheme_LXX_Base),
            KeyboardTheme(THEME_ID_LXX_BASE_BORDER, R.style.KeyboardTheme_LXX_Base_Border),
            KeyboardTheme(THEME_ID_ROUNDED_BASE, R.style.KeyboardTheme_Rounded_Base),
            KeyboardTheme(THEME_ID_ROUNDED_BASE_BORDER, R.style.KeyboardTheme_Rounded_Base_Border)
        )

        // named colors, with names from old settings
        const val COLOR_ACCENT = "accent"
        const val COLOR_GESTURE = "gesture"
        const val COLOR_SUGGESTION_TEXT = "suggestion_text"
        const val COLOR_TEXT = "text"
        const val COLOR_HINT_TEXT = "hint_text"
        const val COLOR_KEYS = "keys"
        const val COLOR_FUNCTIONAL_KEYS = "functional_keys"
        const val COLOR_SPACEBAR = "spacebar"
        const val COLOR_SPACEBAR_TEXT = "spacebar_text"
        const val COLOR_BACKGROUND = "background"

        @JvmStatic
        fun getKeyboardTheme(context: Context): KeyboardTheme {
            val prefs = context.prefs()
            val style = prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE)
            val borders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
            val matchingId = when (style) {
                STYLE_HOLO -> THEME_ID_HOLO_BASE
                STYLE_ROUNDED -> if (borders) THEME_ID_ROUNDED_BASE_BORDER else THEME_ID_ROUNDED_BASE
                else -> if (borders) THEME_ID_LXX_BASE_BORDER else THEME_ID_LXX_BASE
            }
            return KEYBOARD_THEMES.firstOrNull { it.themeId == matchingId } ?: KEYBOARD_THEMES[DEFAULT_THEME_ID]
        }

        fun getThemeActionAndEmojiKeyLabelFlags(themeId: Int): Int {
            return if (themeId == THEME_ID_LXX_BASE || themeId == THEME_ID_ROUNDED_BASE) Key.LABEL_FLAGS_KEEP_BACKGROUND_ASPECT_RATIO else 0
        }

        @JvmStatic
        fun getColorsForCurrentTheme(context: Context): Colors {
            val prefs = context.prefs()
            val isNight = SettingsActivity.forceNight ?: isNightForMode(context, prefs)
            val themeName = SettingsActivity.forceTheme ?: if (isNight)
                prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT)
            else
                prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
            val themeStyle = prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE)

            return getThemeColors(themeName!!, themeStyle!!, context, prefs, isNight)
        }

        private fun getThemeColors(themeName: String, themeStyle: String, context: Context, prefs: SharedPreferences, isNight: Boolean): Colors {
            val hasBorders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
            val backgroundImage = Settings.readUserBackgroundImage(context, isNight)
            return when (themeName) {
                THEME_DYNAMIC -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) DynamicColors(context, themeStyle, hasBorders, backgroundImage)
                    else getThemeColors(THEME_LIGHT, themeStyle, context, prefs, isNight)
                }
                THEME_LIGHT -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_light),
                    ContextCompat.getColor(context, R.color.keyboard_background_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_functional_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_light),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_light),
                    keyboardBackground = backgroundImage
                )
                THEME_DARK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    "#263238".toColorInt(),
                    "#364248".toColorInt(),
                    "#2d393f".toColorInt(),
                    "#364248".toColorInt(),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_HOLO_WHITE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.WHITE,
                    "#282828".toColorInt(),
                    Color.WHITE, // drawable is transparent
                    "#444444".toColorInt(), // should be 222222, but the key drawable is already grey
                    Color.WHITE,
                    Color.WHITE,
                    "#282828".toColorInt(),
                    Color.WHITE,
                    "#80FFFFFF".toColorInt(),
                    keyboardBackground = backgroundImage
                )
                THEME_DARKER -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.keyboard_background_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_functional_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_BLACK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_black),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_BLUE_GRAY -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(120, 144, 156),
                    Color.rgb(236, 239, 241),
                    Color.WHITE,
                    Color.rgb(207, 216, 220),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_BROWN -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(141, 110, 99),
                    Color.rgb(239, 235, 233),
                    Color.WHITE,
                    Color.rgb(215, 204, 200),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_CHOCOLATE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(80, 128, 255),
                    Color.rgb(140, 112, 94),
                    Color.rgb(193, 163, 146),
                    Color.rgb(168, 127, 103),
                    Color.rgb(193, 163, 146),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_CLOUDY -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 113, 129),
                    Color.rgb(81, 97, 113),
                    Color.rgb(117, 128, 142),
                    Color.rgb(99, 109, 121),
                    Color.rgb(117, 128, 142),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_FOREST -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(75, 110, 75),
                    Color.rgb(181, 125, 88),
                    Color.rgb(228, 212, 191),
                    Color.rgb(212, 186, 153),
                    Color.rgb(228, 212, 191),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 80, 0),
                    keyboardBackground = backgroundImage
                )
                THEME_INDIGO -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(92, 107, 192),
                    Color.rgb(232, 234, 246),
                    Color.WHITE,
                    Color.rgb(197, 202, 233),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_OCEAN -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 124, 0),
                    Color.rgb(89, 109, 155),
                    Color.rgb(132, 157, 212),
                    Color.rgb(81, 116, 194),
                    Color.rgb(132, 157, 212),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_PINK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(236, 64, 122),
                    Color.rgb(252, 228, 236),
                    Color.WHITE,
                    Color.rgb(248, 187, 208),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_SAND -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(110, 155, 255),
                    Color.rgb(242, 232, 218),
                    Color.WHITE,
                    Color.rgb(234, 211, 185),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_VIOLETTE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 96, 255),
                    Color.rgb(112, 112, 174),
                    Color.rgb(150, 150, 216),
                    Color.rgb(123, 123, 206),
                    Color.rgb(150, 150, 216),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                // Keycaps keep their colorway in light AND dark mode. Only the keyboard background changes.
                THEME_MECH_AMBER, THEME_MECH_RETRO, THEME_MECH_CYBERPUNK, THEME_MECH_MATCHA,
                THEME_MECH_STEALTH, THEME_MECH_OLIVIA, THEME_MECH_DOLCH -> {
                    val p = MECH_PALETTES.getValue(themeName)
                    DefaultColors(
                        themeStyle,
                        hasBorders,
                        p.accent.toColorInt(),
                        (if (isNight) p.bgDark else p.bgLight).toColorInt(),
                        p.key.toColorInt(),
                        p.func.toColorInt(),
                        p.space.toColorInt(),
                        p.text.toColorInt(),
                        p.hint.toColorInt(),
                        keyboardBackground = backgroundImage
                    )
                }
                THEME_MECH_CUSTOM -> {
                    val plate = prefs.getInt("pref_mech_custom_plate", "#181B1F".toColorInt())
                    val alphas = prefs.getInt("pref_mech_custom_alphas", "#252C33".toColorInt())
                    val mods = prefs.getInt("pref_mech_custom_mods", "#1E2429".toColorInt())
                    val accent = prefs.getInt("pref_mech_custom_accent", "#FF9E00".toColorInt())
                    val text = prefs.getInt("pref_mech_custom_text", "#FFA000".toColorInt())
                    val hint = prefs.getInt("pref_mech_custom_hint", "#CC7A00".toColorInt())
                    DefaultColors(
                        themeStyle,
                        hasBorders,
                        accent,
                        plate,
                        alphas,
                        mods,
                        alphas,
                        text,
                        hint,
                        keyboardBackground = backgroundImage
                    )
                }
                else -> { // user-defined theme
                    val colorSettings = readUserColors(prefs, themeName)
                    val colors = readUserColorTheme(themeStyle, hasBorders, colorSettings, context, isNight, backgroundImage)
                    if (readUserMoreColors(prefs, themeName) == 2)
                        AllColors(readUserAllColors(prefs, themeName, colors), themeStyle, hasBorders, backgroundImage)
                    else {
                        colors
                    }
                }
            }
        }

        fun readUserColorTheme(themeStyle: String, hasBorders: Boolean, colorSettings: List<ColorSetting>, context: Context, isNight: Boolean, backgroundImage: Drawable?): Colors {
            return DefaultColors(
                themeStyle,
                hasBorders,
                determineUserColor(colorSettings, context, COLOR_ACCENT, isNight),
                determineUserColor(colorSettings, context, COLOR_BACKGROUND, isNight),
                determineUserColor(colorSettings, context, COLOR_KEYS, isNight),
                determineUserColor(colorSettings, context, COLOR_FUNCTIONAL_KEYS, isNight),
                determineUserColor(colorSettings, context, COLOR_SPACEBAR, isNight),
                determineUserColor(colorSettings, context, COLOR_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_HINT_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_SUGGESTION_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_SPACEBAR_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_GESTURE, isNight),
                backgroundImage,
            )
        }

        fun writeUserColors(prefs: SharedPreferences, themeName: String, colors: List<ColorSetting>) {
            val key = Settings.PREF_USER_COLORS_PREFIX + themeName
            val value = Json.encodeToString(colors.filter { it.color != null || it.auto == false })
            prefs.edit { putString(key, value) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserColors(prefs: SharedPreferences, themeName: String): List<ColorSetting> {
            val key = Settings.PREF_USER_COLORS_PREFIX + themeName
            return Json.decodeFromString(prefs.getString(key, Defaults.PREF_USER_COLORS)!!)
        }

        fun writeUserMoreColors(prefs: SharedPreferences, themeName: String, value: Int) {
            val key = Settings.PREF_USER_MORE_COLORS_PREFIX + themeName
            prefs.edit { putInt(key, value) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserMoreColors(prefs: SharedPreferences, themeName: String): Int {
            val key = Settings.PREF_USER_MORE_COLORS_PREFIX + themeName
            return prefs.getInt(key, Defaults.PREF_USER_MORE_COLORS)
        }

        fun writeUserAllColors(prefs: SharedPreferences, themeName: String, colorMap: EnumMap<ColorType, Int>) {
            val key = Settings.PREF_USER_ALL_COLORS_PREFIX + themeName
            prefs.edit { putString(key, colorMap.map { "${it.key},${it.value}" }.joinToString(";")) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserAllColors(prefs: SharedPreferences, themeName: String, fallback: Colors?): EnumMap<ColorType, Int> {
            val key = Settings.PREF_USER_ALL_COLORS_PREFIX + themeName
            val colorsString = prefs.getString(key, Defaults.PREF_USER_ALL_COLORS)!!
            val colorMap = EnumMap<ColorType, Int>(ColorType::class.java)
            colorsString.split(";").forEach {
                val ct = try {
                    ColorType.valueOf(it.substringBefore(",").uppercase())
                } catch (_: IllegalArgumentException) {
                    return@forEach
                }
                val i = it.substringAfter(",").toIntOrNull() ?: return@forEach
                colorMap[ct] = i
            }
            if (fallback != null && colorMap.size < ColorType.entries.size) {
                ColorType.entries.forEach {
                    if (it in colorMap) return@forEach
                    colorMap[it] = fallback.get(it)
                }
            }
            return colorMap
        }

        fun getUnusedThemeName(initialName: String, prefs: SharedPreferences): String {
            val existingNames = getExistingThemeNames(prefs)
            if (initialName !in existingNames) return initialName
            var i = 1
            while ("$initialName$i" in existingNames)
                i++
            return "$initialName$i"
        }

        private fun getExistingThemeNames(prefs: SharedPreferences) =
            prefs.all.keys.mapNotNull {
                when {
                    it.startsWith(Settings.PREF_USER_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_COLORS_PREFIX)
                    it.startsWith(Settings.PREF_USER_ALL_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_ALL_COLORS_PREFIX)
                    it.startsWith(Settings.PREF_USER_MORE_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_MORE_COLORS_PREFIX)
                    else -> null
                }
            }.toSortedSet()

        // returns false if not renamed due to invalid name or collision
        fun renameUserColors(from: String, to: String, prefs: SharedPreferences): Boolean {
            if (to.isBlank()) return false // don't want that
            if (to == from) return true // nothing to do
            val existingNames = getExistingThemeNames(prefs)
            if (to in existingNames) return false
            // all good, now rename
            prefs.edit {
                if (prefs.contains(Settings.PREF_USER_COLORS_PREFIX + from)) {
                    putString(Settings.PREF_USER_COLORS_PREFIX + to, prefs.getString(Settings.PREF_USER_COLORS_PREFIX + from, ""))
                    remove(Settings.PREF_USER_COLORS_PREFIX + from)
                }
                if (prefs.contains(Settings.PREF_USER_ALL_COLORS_PREFIX + from)) {
                    putString(Settings.PREF_USER_ALL_COLORS_PREFIX + to, prefs.getString(Settings.PREF_USER_ALL_COLORS_PREFIX + from, ""))
                    remove(Settings.PREF_USER_ALL_COLORS_PREFIX + from)
                }
                if (prefs.contains(Settings.PREF_USER_MORE_COLORS_PREFIX + from)) {
                    putInt(Settings.PREF_USER_MORE_COLORS_PREFIX + to, prefs.getInt(Settings.PREF_USER_MORE_COLORS_PREFIX + from, 0))
                    remove(Settings.PREF_USER_MORE_COLORS_PREFIX + from)
                }
                if (prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) == from)
                    putString(Settings.PREF_THEME_COLORS, to)
                if (prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT) == from)
                    putString(Settings.PREF_THEME_COLORS_NIGHT, to)
            }
            return true
        }

        fun determineUserColor(colors: List<ColorSetting>, context: Context, colorName: String, isNight: Boolean): Int {
            val c = colors.firstOrNull { it.name == colorName }
            val color = c?.color
            val auto = c?.auto ?: true
            return if (auto || color == null)
                determineAutoColor(colors, colorName, isNight, context)
            else color
        }

        private fun determineAutoColor(colors: List<ColorSetting>, colorName: String, isNight: Boolean, context: Context): Int {
            when (colorName) {
                COLOR_ACCENT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        // try determining accent color on Android 10 & 11, accent is not available in resources
                        val wrapper: Context = ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
                        val value = TypedValue()
                        if (wrapper.theme.resolveAttribute(android.R.attr.colorAccent, value, true)) return value.data
                    }
                    return ContextCompat.getColor(Settings.getDayNightContext(context, isNight), R.color.accent)
                }
                COLOR_GESTURE -> return determineUserColor(colors, context, COLOR_ACCENT, isNight)
                COLOR_SUGGESTION_TEXT ->
                    return determineUserColor(colors, context, COLOR_TEXT, isNight)
                COLOR_TEXT -> {
                    // base it on background color, and not key, because it's also used for suggestions
                    val background = determineUserColor(colors, context, COLOR_BACKGROUND, isNight)
                    return if (isBrightColor(background)) {
                        // but if key borders are enabled, we still want reasonable contrast
                        if (!context.prefs().getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
                            || isGoodContrast(Color.BLACK, determineUserColor(colors, context, COLOR_KEYS, isNight))
                        ) Color.BLACK
                        else Color.GRAY
                    } else Color.WHITE
                }
                COLOR_HINT_TEXT -> {
                    return if (isBrightColor(determineUserColor(colors, context, COLOR_KEYS, isNight))) Color.DKGRAY
                    else determineUserColor(colors, context, COLOR_TEXT, isNight)
                }
                COLOR_KEYS ->
                    return brightenOrDarken(determineUserColor(colors, context, COLOR_BACKGROUND, isNight), isNight)
                COLOR_FUNCTIONAL_KEYS ->
                    return brightenOrDarken(determineUserColor(colors, context, COLOR_KEYS, isNight), true)
                COLOR_SPACEBAR -> return determineUserColor(colors, context, COLOR_KEYS, isNight)
                COLOR_SPACEBAR_TEXT -> {
                    val spacebar = determineUserColor(colors, context, COLOR_SPACEBAR, isNight)
                    val hintText = determineUserColor(colors, context, COLOR_HINT_TEXT, isNight)
                    if (isGoodContrast(hintText, spacebar)) return hintText and -0x7f000001 // add some transparency
                    val text = determineUserColor(colors, context, COLOR_TEXT, isNight)
                    if (isGoodContrast(text, spacebar)) return text and -0x7f000001
                    return if (isBrightColor(spacebar)) Color.BLACK and -0x7f000001
                    else Color.WHITE and -0x7f000001
                }
                COLOR_BACKGROUND -> return ContextCompat.getColor(
                    Settings.getDayNightContext(context, isNight),
                    R.color.keyboard_background
                )
                else -> return ContextCompat.getColor(Settings.getDayNightContext(context, isNight), R.color.keyboard_background)
            }
        }
    }
}

@Serializable
data class ColorSetting(val name: String, val auto: Boolean?, val color: Int?) {
    var displayName = name
}
