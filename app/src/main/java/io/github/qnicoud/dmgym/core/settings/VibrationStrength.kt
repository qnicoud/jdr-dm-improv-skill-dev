package io.github.qnicoud.dmgym.core.settings

/**
 * Force des vibrations (fin du minuteur et rythme de Crazy Eights). [STRONG] est le signal d'origine
 * (2 x 250 ms à pleine amplitude). [amplitude] n'a d'effet que si le vibreur la contrôle ; sinon seule la durée
 * change. Plancher de 150 ms : 120 ms ne se sentait pas sur le Galaxy Z Flip3.
 */
enum class VibrationStrength(
    val pulseMillis: Long,
    val amplitude: Int,
) {
    SOFT(pulseMillis = 150, amplitude = 100),
    NORMAL(pulseMillis = 200, amplitude = 170),
    STRONG(pulseMillis = 250, amplitude = 255),
    ;

    /** Pause initiale, impulsion, pause, impulsion (ms). */
    fun endPattern(): LongArray = longArrayOf(0, pulseMillis, GAP_MILLIS, pulseMillis)

    fun endAmplitudes(): IntArray = intArrayOf(0, amplitude, 0, amplitude)

    /** Une seule impulsion : le rappel périodique. */
    fun tickPattern(): LongArray = longArrayOf(0, pulseMillis)

    fun tickAmplitudes(): IntArray = intArrayOf(0, amplitude)

    companion object {
        const val GAP_MILLIS = 120L

        fun parse(name: String?): VibrationStrength = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
