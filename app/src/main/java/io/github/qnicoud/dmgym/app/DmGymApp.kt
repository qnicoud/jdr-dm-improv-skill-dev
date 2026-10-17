package io.github.qnicoud.dmgym.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Racine de l'UI : thème + navigation. La pile est sauvegardée sur rotation et mort du processus.
 * La palette suit les réglages en direct (D53) ; « Parchemin » sert tant qu'ils ne sont pas lus.
 */
@Composable
fun DmGymApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Home)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val paletteFlow =
        remember(container) {
            container.settingsRepository.settings
                .map { it.themePalette }
                .distinctUntilChanged()
        }
    val palette by paletteFlow.collectAsStateWithLifecycle(ThemePalette.PARCHMENT)
    DmGymTheme(palette = palette) {
        AppNavDisplay(navigator = navigator, container = container, modifier = modifier)
    }
}
