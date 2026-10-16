package io.github.qnicoud.dmgym.core.signal

import io.github.qnicoud.dmgym.core.settings.Settings

/** Signal de fin de minuteur (ADR 0006) : vibration et bip, selon les réglages et le mode sonnerie. */
interface EndSignal {
    /** Vrai si l'appareil a un vibreur (l'option est masquée sinon). */
    val canVibrate: Boolean

    /** Charge le bip ; à appeler à l'entrée de l'écran d'exercice. */
    fun prepare()

    /** Joue le signal selon [settings] ; le son est ignoré si [prepare] n'a pas été appelé. */
    fun play(settings: Settings)

    /** Libère les ressources ; à appeler à la sortie de l'écran. */
    fun release()
}
