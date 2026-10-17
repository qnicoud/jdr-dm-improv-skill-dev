package io.github.qnicoud.dmgym.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private val vibrator: Vibrator? = defaultVibrator(appContext)
    private val audioManager: AudioManager? = appContext.getSystemService(AudioManager::class.java)
    private var soundPool: SoundPool? = null
    private var soundId = 0

    @Volatile private var loaded = false

    override val canVibrate: Boolean get() = vibrator?.hasVibrator() == true

    override fun prepare() {
        if (soundPool != null) return
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
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
        val effect =
            if (target.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(VIBRATION_PATTERN, VIBRATION_AMPLITUDES, NO_REPEAT)
            } else {
                VibrationEffect.createWaveform(VIBRATION_PATTERN, NO_REPEAT)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            target.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(effect, vibrationAudioAttributes())
        }
    }

    private fun vibrationAudioAttributes(): AudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /**
     * Usage MEDIA (flux musique) et non SONIFICATION : sur le Galaxy Z Flip3 de test, ce dernier est routé vers
     * STREAM_SYSTEM, dont le volume est à 0 (sons système coupés) : le bip était joué (flux valide) mais inaudible.
     * Le flux média suit le volume que l'utilisateur règle ; sans focus audio, la musique n'est ni coupée ni baissée.
     */
    private fun beep() {
        val pool = soundPool?.takeIf { loaded } ?: return
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
    }

    private companion object {
        /**
         * Usage ALARM pour la vibration : la fin du minuteur est le seul signal en mode vibreur ou silencieux,
         * et ne doit pas être filtrée par le réglage « retour haptique » (USAGE_ASSISTANCE_SONIFICATION).
         * Elle reste brève (deux impulsions de 250 ms).
         */
        fun defaultVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        /** Pause initiale, impulsion, pause, impulsion (ms). */
        val VIBRATION_PATTERN = longArrayOf(0, 250, 120, 250)
        val VIBRATION_AMPLITUDES = intArrayOf(0, 255, 0, 255)
        const val NO_REPEAT = -1
        const val VOLUME = 1f
    }
}
