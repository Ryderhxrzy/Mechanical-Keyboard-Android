// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Constants

/**
 * Fast on-keyboard popup dialog allowing users to adjust volume, switch sounds,
 * silent mode audio, and 3D keycaps directly from the keyboard without opening the full settings app.
 */
fun createMechanicalQuickControlsDialog(latinIme: LatinIME, windowToken: IBinder): AlertDialog {
    val themeContext = getPlatformDialogThemeContext(latinIme)
    val density = themeContext.resources.displayMetrics.density
    fun dp(dp: Float) = (dp * density).toInt()

    val mechSound = MechanicalSoundManager.getInstance()
    val prefs = themeContext.prefs()

    val root = LinearLayout(themeContext).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(16f), dp(20f), dp(8f))
    }

    // Title / Switch acoustic header
    val switchLabel = TextView(themeContext).apply {
        text = "Mechanical Switch Type"
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 0, 0, dp(8f))
    }
    root.addView(switchLabel)

    // Switch Selector RadioGroup
    val radioGroup = RadioGroup(themeContext).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp(12f))
    }
    val currentSwitch = mechSound.getSelectedSwitchType()

    MechanicalSoundManager.SwitchType.entries.forEach { switchType ->
        val radioButton = RadioButton(themeContext).apply {
            text = "${switchType.title}  (${switchType.subtitle})"
            textSize = 13f
            isChecked = switchType == currentSwitch
            setOnClickListener {
                mechSound.setSwitchType(switchType)
                mechSound.playSwitchSound(Constants.CODE_SPACE)
            }
        }
        radioGroup.addView(radioButton)
    }
    root.addView(radioGroup)

    // Volume Section
    val currentVolume = (mechSound.getVolume() * 100).toInt()
    val volumeLabel = TextView(themeContext).apply {
        text = "Key Click Volume: $currentVolume%"
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(4f), 0, dp(4f))
    }
    root.addView(volumeLabel)

    val seekBar = SeekBar(themeContext).apply {
        max = 100
        progress = currentVolume
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceAtLeast(5)
                volumeLabel.text = "Key Click Volume: $clamped%"
                mechSound.setVolume(clamped / 100f)
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {
                mechSound.playSwitchSound(Constants.CODE_SPACE)
            }
        })
    }
    root.addView(seekBar)

    // Checkbox: Sound in Silent Mode
    val silentModeCheck = CheckBox(themeContext).apply {
        text = "Play sound even when phone is on Silent / Vibrate"
        textSize = 13f
        isChecked = mechSound.isPlayInSilentMode()
        setPadding(dp(4f), dp(8f), 0, dp(8f))
        setOnCheckedChangeListener { _, isChecked ->
            mechSound.setPlayInSilentMode(isChecked)
            mechSound.playSwitchSound(Constants.CODE_SPACE)
        }
    }
    root.addView(silentModeCheck)

    // Checkbox: 3D Keycaps
    val is3DEnabled = prefs.getBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, true)
    val keycaps3DCheck = CheckBox(themeContext).apply {
        text = "3D Keycaps (Physical Depth & Travel)"
        textSize = 13f
        isChecked = is3DEnabled
        setPadding(dp(4f), 0, 0, dp(8f))
        setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(MechanicalKeycapRenderer.PREF_MECH_3D_KEYCAPS, isChecked).apply()
            helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().mainKeyboardView?.invalidateAllKeys()
        }
    }
    root.addView(keycaps3DCheck)

    val dialog = AlertDialog.Builder(themeContext)
        .setTitle("Mechanical Keyboard Quick Controls")
        .setView(root)
        .setPositiveButton("Done") { d, _ -> d.dismiss() }
        .setNeutralButton("All Settings...") { d, _ ->
            d.dismiss()
            latinIme.launchSettings()
        }
        .create()

    val window = dialog.window
    val layoutParams = window?.attributes
    layoutParams?.token = windowToken
    layoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
    window?.attributes = layoutParams
    window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    return dialog
}
