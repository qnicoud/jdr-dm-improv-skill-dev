package io.github.qnicoud.dmgym.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.signal.EndSignal

/**
 * Signal de fin (ADR 0006) : deux impulsions de vibration si activé et possible, bip SoundPool si activé
 * et seulement en mode sonnerie normal. Aucun focus audio n'est demandé. Vérifié à la main (docs/qa/l3-signal-fin.md).
 */
class AndroidEndSignal(
    context: Context,
) : EndSignal {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = appContext.getSystemService(Vibrator::class.java)
    private val audioManager: AudioManager? = appContext.getSystemService(AudioManager::class.java)
    private var soundPool: SoundPool? = null
    private var soundId = 0
    private var loaded = false

    override val canVibrate: Boolean get() = vibrator?.hasVibrator() == true

    override fun prepare() {
        if (soundPool != null) return
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        val pool =
            SoundPool
                .Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build()
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        soundId = pool.load(appContext, R.raw.end_beep, 1)
        soundPool = pool
    }

    override fun play(settings: Settings) {
        if (settings.vibrationEnabled) vibrate()
        if (settings.soundEnabled) beep()
    }

    override fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }

    private fun vibrate() {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        target.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, NO_REPEAT))
    }

    private fun beep() {
        val pool = soundPool?.takeIf { loaded } ?: return
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
    }

    private companion object {
        /** Pause initiale, impulsion, pause, impulsion (ms). */
        val VIBRATION_PATTERN = longArrayOf(0, 120, 100, 120)
        const val NO_REPEAT = -1
        const val VOLUME = 1f
    }
}
