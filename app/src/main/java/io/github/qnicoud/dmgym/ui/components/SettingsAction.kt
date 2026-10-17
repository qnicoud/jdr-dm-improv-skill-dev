package io.github.qnicoud.dmgym.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons

const val SettingsActionTag = "settings_action"

/** Engrenage des Réglages, identique (position, libellé TalkBack) dans la barre du haut des trois onglets. */
@Composable
fun SettingsAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.testTag(SettingsActionTag)) {
        Icon(DmIcons.Settings, contentDescription = stringResource(R.string.action_settings))
    }
}
