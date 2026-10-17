package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.ThemePalette

/** Aspect de l'écran de démarrage : un thème par palette (recette 9.1). « Couleurs du téléphone » prend Parchemin. */
enum class SplashVariant {
    PARCHMENT,
    FOREST,
    NIGHT,
    MIST,
    ;

    companion object {
        fun of(palette: ThemePalette): SplashVariant =
            when (palette) {
                ThemePalette.PARCHMENT, ThemePalette.DYNAMIC -> PARCHMENT
                ThemePalette.FOREST -> FOREST
                ThemePalette.NIGHT -> NIGHT
                ThemePalette.MIST -> MIST
            }
    }
}

/**
 * Quand l'écran de démarrage part. Au démarrage à froid il reste au moins [MIN_COLD_MILLIS] depuis le début de
 * `onCreate`, sans jamais aller au-delà du temps de lecture des réglages ; au démarrage à chaud, il n’attend que ceux-ci.
 */
class SplashPolicy(
    private val isColdStart: Boolean,
) {
    fun keepOnScreen(
        settingsReady: Boolean,
        elapsedMillis: Long,
    ): Boolean = !settingsReady || (isColdStart && elapsedMillis < MIN_COLD_MILLIS)

    companion object {
        const val MIN_COLD_MILLIS = 1_000L
    }
}
