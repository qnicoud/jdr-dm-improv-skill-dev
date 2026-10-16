package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen

/** Écran provisoire de la séance du jour (lot L11). */
@Composable
fun DailyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = PlaceholderScreen(stringResource(R.string.daily_title), modifier, onBack)
