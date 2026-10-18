package io.github.qnicoud.dmgym.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private val MinButtonHeight = 48.dp

/** Données : export lisible, sauvegarde réimportable, import et effacement total (confirmation dans la page). */
@Composable
internal fun DataSection(
    state: DataUiState,
    actions: SettingsActions,
) {
    SectionTitle(stringResource(R.string.settings_section_data))
    Text(
        text = stringResource(R.string.settings_data_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag(SettingsTags.DATA_HINT),
    )
    ActionRow(
        label = stringResource(R.string.settings_export),
        hint = stringResource(R.string.settings_export_hint),
        onClick = actions.onExport,
        modifier = Modifier.testTag(SettingsTags.EXPORT),
        trailing = if (state.message == DataMessage.EXPORTED) ({ ExportDoneCheck() }) else null,
    )
    ActionRow(
        label = stringResource(R.string.settings_backup),
        hint = stringResource(R.string.settings_backup_hint),
        onClick = actions.onBackup,
        modifier = Modifier.testTag(SettingsTags.BACKUP),
        trailing =
            if (state.message == DataMessage.BACKED_UP) {
                { ExportDoneCheck(SettingsTags.BACKUP_DONE, R.string.settings_backup_done) }
            } else {
                null
            },
    )
    ActionRow(
        label = stringResource(R.string.settings_import),
        hint = stringResource(R.string.settings_import_hint),
        onClick = actions.onImport,
        modifier = Modifier.testTag(SettingsTags.IMPORT),
    )
    state.backup?.takeIf { state.message == DataMessage.BACKED_UP }?.let { info ->
        Text(
            text =
                pluralStringResource(
                    R.plurals.settings_backup_proof,
                    info.sessionCount,
                    info.fileName,
                    info.sessionCount,
                ),
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(SettingsTags.BACKUP_PROOF),
        )
    }
    if (state.confirmingErase) {
        EraseConfirmation(state, actions)
    } else {
        ActionRow(
            label = stringResource(R.string.settings_erase),
            hint = stringResource(R.string.settings_erase_hint),
            onClick = actions.onRequestErase,
            modifier = Modifier.testTag(SettingsTags.ERASE),
        )
    }
    state.message?.takeIf { it != DataMessage.EXPORTED && it != DataMessage.BACKED_UP }?.let { message ->
        Text(
            text = stringResource(message.textRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(SettingsTags.DATA_MESSAGE),
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** Coche de validation à côté du bouton d'export, hors de la ligne cliquable pour que sa zone vive soit annoncée (recette 8.5). */
@Composable
private fun ExportDoneCheck(
    tag: String = SettingsTags.EXPORT_DONE,
    @StringRes description: Int = R.string.settings_data_exported,
) {
    Icon(
        DmIcons.Check,
        contentDescription = stringResource(description),
        tint = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(end = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(tag),
    )
}

/**
 * Confirmation d'effacement dans la page (parcours §4.8) : on tape le mot pour activer le bouton destructif.
 * Le focus va sur la consigne à l'apparition, donc TalkBack la lit en premier ; « Annuler » précède l'action.
 */
@Composable
private fun EraseConfirmation(
    state: DataUiState,
    actions: SettingsActions,
) {
    val promptFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { promptFocus.requestFocus() }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_erase_confirm_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_erase_confirm_text, DataUiState.ERASE_WORD),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.focusRequester(promptFocus).focusable().testTag(SettingsTags.ERASE_PROMPT),
            )
            OutlinedTextField(
                value = state.eraseInput,
                onValueChange = actions.onEraseInput,
                label = { Text(stringResource(R.string.settings_erase_input_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SettingsTags.ERASE_INPUT),
            )
            TextButton(
                onClick = actions.onCancelErase,
                modifier = Modifier.heightIn(min = MinButtonHeight).testTag(SettingsTags.ERASE_CANCEL),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) {
                Text(stringResource(R.string.settings_erase_cancel))
            }
            Button(
                onClick = actions.onConfirmErase,
                enabled = state.canErase,
                modifier = Modifier.heightIn(min = MinButtonHeight).testTag(SettingsTags.ERASE_CONFIRM),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.settings_erase_confirm))
            }
        }
    }
}

@get:StringRes
private val DataMessage.textRes: Int
    get() =
        when (this) {
            DataMessage.EXPORTED -> R.string.settings_data_exported
            DataMessage.EXPORT_FAILED -> R.string.settings_data_export_failed
            DataMessage.BACKED_UP -> R.string.settings_backup_done
            DataMessage.BACKUP_FAILED -> R.string.settings_backup_failed
            DataMessage.ERASED -> R.string.settings_data_erased
            DataMessage.ERASE_FAILED -> R.string.settings_data_erase_failed
        }
