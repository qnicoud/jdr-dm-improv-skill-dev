package io.github.qnicoud.dmgym.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen

/** Écran provisoire de l'historique (lot L9). */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) = PlaceholderScreen(stringResource(R.string.history_title), modifier)

/** Écran provisoire du détail d'une session (lot L9). */
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = PlaceholderScreen(stringResource(R.string.session_detail_title), modifier, onBack)
