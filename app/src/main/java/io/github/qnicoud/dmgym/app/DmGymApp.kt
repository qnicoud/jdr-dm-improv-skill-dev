package io.github.qnicoud.dmgym.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

/**
 * Racine de l'UI : thème + navigation. La pile est sauvegardée sur rotation et mort du processus.
 * [palette], [darkTheme] et [reduceMotion] viennent des réglages, lus en direct par [ThemeBootstrap] (D53).
 */
@Composable
fun DmGymApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
    palette: ThemePalette = ThemePalette.PARCHMENT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
) {
    val backStack = rememberNavBackStack(Home)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    DmGymTheme(darkTheme = darkTheme, palette = palette, reduceMotion = reduceMotion) {
        AppNavDisplay(navigator = navigator, container = container, modifier = modifier)
    }
}
