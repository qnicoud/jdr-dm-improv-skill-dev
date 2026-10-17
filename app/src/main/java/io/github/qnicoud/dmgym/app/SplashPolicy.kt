package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.ThemePalette

/** Aspect de l'écran de démarrage : un thème par palette et par mode, clair ou sombre (recette 9.1). */
enum class SplashVariant {
    PARCHMENT_LIGHT,
    PARCHMENT_DARK,
    FOREST_LIGHT,
    FOREST_DARK,
    NIGHT_LIGHT,
    NIGHT_DARK,
    MIST_LIGHT,
    MIST_DARK,
    ;

    companion object {
        /** [dark] est le mode déjà résolu (réglage de l'app, « Système » suivant le téléphone). « Couleurs du téléphone » prend Parchemin. */
        fun of(
            palette: ThemePalette,
            dark: Boolean,
        ): SplashVariant =
            when (palette) {
                ThemePalette.PARCHMENT, ThemePalette.DYNAMIC -> if (dark) PARCHMENT_DARK else PARCHMENT_LIGHT
                ThemePalette.FOREST -> if (dark) FOREST_DARK else FOREST_LIGHT
                ThemePalette.NIGHT -> if (dark) NIGHT_DARK else NIGHT_LIGHT
                ThemePalette.MIST -> if (dark) MIST_DARK else MIST_LIGHT
            }
    }
}

/**
 * Quand l'écran de démarrage part. Au démarrage à froid il reste au moins [MIN_COLD_MILLIS] depuis le début de
 * `onCreate`, sans jamais aller au-delà de la lecture des réglages ; au démarrage à chaud, il n'attend que celle-ci.
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

/** Vrai une seule fois par processus ; [reset] remet l'état initial (tests). */
object ColdStartTracker {
    private var pending = true

    fun consume(): Boolean = pending.also { pending = false }

    fun reset() {
        pending = true
    }
}

/** Ne redemande au système le thème de démarrage que lorsque la variante diffère de la dernière appliquée. */
class SplashThemeSync(
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val apply: (SplashVariant) -> Unit,
) {
    fun onVariant(variant: SplashVariant) {
        if (load() == variant.name) return
        apply(variant)
        save(variant.name)
    }
}
