package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen

/** Écran provisoire des réglages ; les vrais réglages arrivent avec le lot L3 (DataStore). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = PlaceholderScreen(stringResource(R.string.settings_title), modifier, onBack)
