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
import io.github.qnicoud.dmgym.ui.icons.DmIcons

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
