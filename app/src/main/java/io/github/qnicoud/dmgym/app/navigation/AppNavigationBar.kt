package io.github.qnicoud.dmgym.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private data class TabSpec(
    val route: Route,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val Tabs =
    listOf(
        TabSpec(Home, R.string.nav_train, DmIcons.Train),
        TabSpec(History(), R.string.nav_history, DmIcons.Book),
        TabSpec(Stats, R.string.nav_progress, DmIcons.Chart),
    )

/** Barre de navigation à 3 onglets (D37) ; les Réglages sont dans la barre du haut de l'accueil. */
@Composable
fun AppNavigationBar(
    selected: Route,
    onSelect: (Route) -> Unit,
) {
    NavigationBar {
        Tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
