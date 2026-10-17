package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Ce que les réglages décident de l'aspect de l'app ; [reduceAnimations] `null` suit le système. */
data class Appearance(
    val palette: ThemePalette = ThemePalette.PARCHMENT,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val reduceAnimations: Boolean? = null,
)

/**
 * Aspect courant dès le démarrage (D53). [appearance] vaut `null` tant que les réglages ne sont pas lus : l'écran
 * de démarrage reste affiché jusque-là ([isReady]), pour ne pas montrer Parchemin avant la vraie palette.
 * Une lecture en échec retombe sur l'aspect par défaut plutôt que de bloquer le démarrage.
 */
class ThemeBootstrap(
    settings: SettingsRepository,
    scope: CoroutineScope,
) {
    val appearance: StateFlow<Appearance?> =
        settings.settings
            .map<_, Appearance?> { Appearance(it.themePalette, it.themeMode, it.reduceAnimations) }
            .catch { emit(Appearance()) }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    val isReady: Boolean get() = appearance.value != null
}
