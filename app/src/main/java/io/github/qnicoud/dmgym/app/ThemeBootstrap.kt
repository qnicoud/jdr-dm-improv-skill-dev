package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Palette courante dès le démarrage (D53). [palette] vaut `null` tant que les réglages ne sont pas lus : l'écran
 * de démarrage reste affiché jusque-là ([isReady]), pour ne pas montrer Parchemin avant la vraie palette.
 * Une lecture en échec retombe sur Parchemin plutôt que de bloquer le démarrage.
 */
class ThemeBootstrap(
    settings: SettingsRepository,
    scope: CoroutineScope,
) {
    val palette: StateFlow<ThemePalette?> =
        settings.settings
            .map<_, ThemePalette?> { it.themePalette }
            .catch { emit(ThemePalette.PARCHMENT) }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    val isReady: Boolean get() = palette.value != null
}
