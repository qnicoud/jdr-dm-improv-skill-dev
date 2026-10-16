package io.github.qnicoud.dmgym.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

/** Racine de l'UI : thème + navigation. La pile est sauvegardée sur rotation et mort du processus. */
@Composable
fun DmGymApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Home)
    val navigator = remember(backStack) { AppNavigator(backStack as MutableList<NavKey>) }
    DmGymTheme {
        AppNavDisplay(backStack = backStack, navigator = navigator, container = container, modifier = modifier)
    }
}
