// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MechanicalSoundManager private constructor() {

    enum class SwitchType(val id: String, val title: String, val subtitle: String) {
        BLUE("blue", "Cherry MX Blue", "Crisp, clicky & tactile snap"),
        BROWN("brown", "Cherry MX Brown", "Smooth tactile bump with muted clack"),
        RED("red", "Gateron Red (Thock)", "Deep, creamy linear bottom-out thock"),
        BOX_JADE("box_jade", "Kailh Box Jade", "Authoritative, heavy tactile clickbar")
    }

    companion object {
        const val PREF_MECH_SOUND_ENABLED = "pref_mech_sound_enabled"
        const val PREF_MECH_SWITCH_TYPE = "pref_mech_switch_type"
        const val PREF_MECH_SOUND_VOLUME = "pref_mech_sound_volume"
        const val PREF_MECH_SOUND_IN_SILENT = "pref_mech_sound_in_silent"

        val DEFAULT_SWITCH_TYPE = SwitchType.BLUE.id
        const val DEFAULT_VOLUME = 0.85f

        private val instance = MechanicalSoundManager()

        @JvmStatic
        fun getInstance(): MechanicalSoundManager = instance
    }

    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<SwitchType, Int>()
    private var isInitialized = false
    private var prefs: SharedPreferences? = null
    private val random = Random()
    private val executor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.prefs()

        // USAGE_MEDIA allows mechanical switch sounds to play on the media stream,
        // which remains active even when the phone is on Silent, Vibrate, or DND mode.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(14)
            .setAudioAttributes(audioAttributes)
            .build()

        executor.execute {
            try {
                loadSounds(context)
                isInitialized = true
            } catch (e: Exception) {
                Log.e("MechSound", "Error preloading mechanical sounds", e)
            }
        }
    }

    private fun loadSounds(context: Context) {
        val cacheDir = File(context.cacheDir, "mech_audio").apply { if (!exists()) mkdirs() }
        val pool = soundPool ?: return

        for (switch in SwitchType.entries) {
            val wavFile = File(cacheDir, "switch_${switch.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 100) {
                val pcm = generateSwitchPcm(switch)
                writeWavFile(wavFile, pcm, 44100)
            }
            val soundId = pool.load(wavFile.absolutePath, 1)
            soundIds[switch] = soundId
        }
    }

    fun isReadyAndEnabled(): Boolean {
        val p = prefs ?: return false
        return p.getBoolean(PREF_MECH_SOUND_ENABLED, true)
    }

    fun isPlayInSilentMode(): Boolean {
        val p = prefs ?: return true
        return p.getBoolean(PREF_MECH_SOUND_IN_SILENT, true)
    }

    fun getSelectedSwitchType(): SwitchType {
        val p = prefs ?: return SwitchType.BLUE
        val id = p.getString(PREF_MECH_SWITCH_TYPE, DEFAULT_SWITCH_TYPE) ?: DEFAULT_SWITCH_TYPE
        return SwitchType.entries.firstOrNull { it.id == id } ?: SwitchType.BLUE
    }

    fun getVolume(): Float {
        val p = prefs ?: return DEFAULT_VOLUME
        return p.getFloat(PREF_MECH_SOUND_VOLUME, DEFAULT_VOLUME)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_MECH_SOUND_ENABLED, enabled)?.apply()
    }

    fun setPlayInSilentMode(enabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_MECH_SOUND_IN_SILENT, enabled)?.apply()
    }

    fun setVolume(volume: Float) {
        prefs?.edit()?.putFloat(PREF_MECH_SOUND_VOLUME, volume.coerceIn(0f, 1.0f))?.apply()
    }

    fun setSwitchType(switchType: SwitchType) {
        prefs?.edit()?.putString(PREF_MECH_SWITCH_TYPE, switchType.id)?.apply()
    }

    fun playSwitchSound(code: Int) {
        val pool = soundPool ?: return
        if (!isReadyAndEnabled()) return

        val switchType = getSelectedSwitchType()
        val soundId = soundIds[switchType] ?: return
        val baseVolume = getVolume().coerceIn(0f, 1.0f)
        if (baseVolume <= 0.01f) return

        // Subtle acoustic variance per key type
        val (pitch, volumeMult) = when (code) {
            Constants.CODE_SPACE -> Pair(0.80f, 1.05f) // Deep stabilizer thock
            Constants.CODE_ENTER -> Pair(0.92f, 1.0f)  // Solid return
            KeyCode.DELETE -> Pair(1.10f, 0.95f)       // Snappy high return
            else -> {
                // Natural ±2% pitch jitter simulates real physical switches
                val jitter = 1.0f + (random.nextFloat() * 0.04f - 0.02f)
                Pair(jitter, 1.0f)
            }
        }

        val finalVol = (baseVolume * volumeMult).coerceIn(0.05f, 1.0f)
        pool.play(soundId, finalVol, finalVol, 1, 0, pitch)
    }

    fun testSwitchSound(type: SwitchType) {
        val pool = soundPool ?: return
        val soundId = soundIds[type] ?: return
        val vol = getVolume().coerceIn(0.05f, 1.0f)
        pool.play(soundId, vol, vol, 1, 0, 1.0f)
    }

    private fun generateSwitchPcm(type: SwitchType): ShortArray {
        val sampleRate = 44100
        val durationMs = when (type) {
            SwitchType.BLUE -> 45
            SwitchType.BROWN -> 38
            SwitchType.RED -> 50
            SwitchType.BOX_JADE -> 52
        }
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(totalSamples)
        val rnd = Random(42)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            when (type) {
                SwitchType.BLUE -> {
                    val clickDecay = kotlin.math.exp(-1400.0 * t)
                    val bodyDecay = kotlin.math.exp(-220.0 * t)
                    val noise = if (t < 0.003) (rnd.nextDouble() * 2 - 1) * 0.45 else 0.0
                    val click = kotlin.math.sin(2.0 * Math.PI * 2300.0 * t) * clickDecay
                    val body = kotlin.math.sin(2.0 * Math.PI * 450.0 * t) * bodyDecay
                    sample = (click * 0.55 + body * 0.35 + noise)
                }
                SwitchType.BROWN -> {
                    val clickDecay = kotlin.math.exp(-1100.0 * t)
                    val bodyDecay = kotlin.math.exp(-180.0 * t)
                    val noise = if (t < 0.002) (rnd.nextDouble() * 2 - 1) * 0.25 else 0.0
                    val click = kotlin.math.sin(2.0 * Math.PI * 1350.0 * t) * clickDecay
                    val body = kotlin.math.sin(2.0 * Math.PI * 340.0 * t) * bodyDecay
                    sample = (click * 0.45 + body * 0.45 + noise)
                }
                SwitchType.RED -> {
                    val bodyDecay = kotlin.math.exp(-130.0 * t)
                    val subDecay = kotlin.math.exp(-95.0 * t)
                    val noise = if (t < 0.0015) (rnd.nextDouble() * 2 - 1) * 0.15 else 0.0
                    val thock = kotlin.math.sin(2.0 * Math.PI * 220.0 * t) * subDecay
                    val body = kotlin.math.sin(2.0 * Math.PI * 440.0 * t) * bodyDecay
                    sample = (thock * 0.65 + body * 0.25 + noise)
                }
                SwitchType.BOX_JADE -> {
                    val click1 = kotlin.math.sin(2.0 * Math.PI * 1900.0 * t) * kotlin.math.exp(-1500.0 * t)
                    val t2 = kotlin.math.max(0.0, t - 0.0035)
                    val click2 = if (t > 0.0035) kotlin.math.sin(2.0 * Math.PI * 2600.0 * t2) * kotlin.math.exp(-1600.0 * t2) else 0.0
                    val body = kotlin.math.sin(2.0 * Math.PI * 310.0 * t) * kotlin.math.exp(-160.0 * t)
                    val noise = if (t < 0.004) (rnd.nextDouble() * 2 - 1) * 0.35 else 0.0
                    sample = (click1 * 0.40 + click2 * 0.35 + body * 0.40 + noise)
                }
            }

            val clamped = (sample * 24000.0).coerceIn(-32767.0, 32767.0)
            pcm[i] = clamped.toInt().toShort()
        }
        return pcm
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalDataLen = pcmData.size * 2 + 36
        val byteRate = sampleRate * 2
        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            // RIFF/WAVE header
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            // Subchunk 1: fmt
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // 16 for PCM
            header[20] = 1; header[21] = 0 // format = 1 (PCM)
            header[22] = 1; header[23] = 0 // 1 channel
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0 // block align
            header[34] = 16; header[35] = 0 // bits per sample
            // Subchunk 2: data
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            val dataLen = pcmData.size * 2
            header[40] = (dataLen and 0xff).toByte()
            header[41] = ((dataLen shr 8) and 0xff).toByte()
            header[42] = ((dataLen shr 16) and 0xff).toByte()
            header[43] = ((dataLen shr 24) and 0xff).toByte()
            out.write(header)

            val byteBuf = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuf.putShort(sample)
            }
            out.write(byteBuf.array())
        }
    }
}

