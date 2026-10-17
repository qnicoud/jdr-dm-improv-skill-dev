package io.github.qnicoud.dmgym.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.VibrationStrength

private val MinButtonHeight = 48.dp

/** Force de la vibration : trois choix et le bouton « Essayer ». Grisés si la vibration est coupée. */
@Composable
internal fun VibrationStrengthRows(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    if (!state.canVibrate) return
    GroupLabel(R.string.settings_vibration_strength)
    Column(Modifier.selectableGroup()) {
        VibrationStrength.entries.forEach { strength ->
            ChoiceRow(
                label = stringResource(strength.labelRes),
                selected = strength == state.vibrationStrength,
                onSelect = { actions.onVibrationStrength(strength) },
                enabled = state.vibration,
                modifier = Modifier.testTag(SettingsTags.strength(strength)),
            )
        }
    }
    val tryDescription = stringResource(R.string.settings_vibration_try_description)
    OutlinedButton(
        onClick = actions.onTryVibration,
        enabled = state.vibration,
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .heightIn(min = MinButtonHeight)
                .semantics { contentDescription = tryDescription }
                .testTag(SettingsTags.TRY_VIBRATION),
    ) {
        Text(stringResource(R.string.settings_vibration_try))
    }
}

/** Apparence : thème Clair / Sombre / Système, palette, animations. */
@Composable
internal fun AppearanceSection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SectionTitle(stringResource(R.string.settings_section_appearance))
    GroupLabel(R.string.settings_theme_mode)
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            ChoiceRow(
                label = stringResource(mode.labelRes),
                selected = mode == state.themeMode,
                onSelect = { actions.onThemeMode(mode) },
                modifier = Modifier.testTag(SettingsTags.themeMode(mode)),
            )
        }
    }
    GroupLabel(R.string.settings_palette_title)
    Column(Modifier.selectableGroup()) {
        state.palettes.forEach { palette ->
            PaletteRow(
                palette = palette,
                selected = palette == state.palette,
                onSelect = { actions.onPalette(palette) },
                modifier = Modifier.testTag(SettingsTags.palette(palette)),
            )
        }
    }
    GroupLabel(R.string.settings_reduce_animations)
    Column(Modifier.selectableGroup()) {
        ReduceOption(R.string.settings_reduce_system, state.reduceAnimations == null, SettingsTags.REDUCE_SYSTEM) {
            actions.onReduceAnimations(null)
        }
        ReduceOption(R.string.settings_reduce_always, state.reduceAnimations == true, SettingsTags.REDUCE_ALWAYS) {
            actions.onReduceAnimations(true)
        }
        ReduceOption(R.string.settings_reduce_never, state.reduceAnimations == false, SettingsTags.REDUCE_NEVER) {
            actions.onReduceAnimations(false)
        }
    }
}

@Composable
private fun GroupLabel(
    @StringRes text: Int,
) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
}

@Composable
private fun ReduceOption(
    @StringRes label: Int,
    selected: Boolean,
    tag: String,
    onSelect: () -> Unit,
) {
    ChoiceRow(stringResource(label), selected, onSelect, Modifier.testTag(tag))
}

/** Données : export lisible et effacement total, avec confirmation dans la page. */
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
    )
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
    state.message?.let { message ->
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
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
private val VibrationStrength.labelRes: Int
    get() =
        when (this) {
            VibrationStrength.SOFT -> R.string.settings_strength_soft
            VibrationStrength.NORMAL -> R.string.settings_strength_normal
            VibrationStrength.STRONG -> R.string.settings_strength_strong
        }

@get:StringRes
private val ThemeMode.labelRes: Int
    get() =
        when (this) {
            ThemeMode.SYSTEM -> R.string.settings_theme_system
            ThemeMode.LIGHT -> R.string.settings_theme_light
            ThemeMode.DARK -> R.string.settings_theme_dark
        }

@get:StringRes
private val DataMessage.textRes: Int
    get() =
        when (this) {
            DataMessage.EXPORTED -> R.string.settings_data_exported
            DataMessage.EXPORT_FAILED -> R.string.settings_data_export_failed
            DataMessage.ERASED -> R.string.settings_data_erased
            DataMessage.ERASE_FAILED -> R.string.settings_data_erase_failed
        }
