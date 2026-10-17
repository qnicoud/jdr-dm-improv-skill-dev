package io.github.qnicoud.dmgym.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object SettingsTags {
    const val VIBRATION = "settings_vibration"
    const val SOUND = "settings_sound"
    const val CRAZY_HAPTIC = "settings_crazy_haptic"
    const val DAILY_ONE_PAGE = "settings_daily_one_page"
    const val ABOUT = "settings_about"
    const val DICTATION_HINT = "settings_dictation_hint"
    const val TRY_VIBRATION = "settings_try_vibration"
    const val REDUCE_SYSTEM = "settings_reduce_system"
    const val REDUCE_ALWAYS = "settings_reduce_always"
    const val REDUCE_NEVER = "settings_reduce_never"
    const val DATA_HINT = "settings_data_hint"
    const val EXPORT = "settings_export"
    const val ERASE = "settings_erase"
    const val ERASE_CONFIRM = "settings_erase_confirm"
    const val ERASE_CANCEL = "settings_erase_cancel"
    const val ERASE_PROMPT = "settings_erase_prompt"
    const val ERASE_INPUT = "settings_erase_input"
    const val DATA_MESSAGE = "settings_data_message"

    fun strength(strength: VibrationStrength) = "settings_strength_${strength.name}"

    fun themeMode(mode: ThemeMode) = "settings_theme_mode_${mode.name}"

    fun decrease(id: ExerciseId) = "settings_decrease_${id.value}"

    fun increase(id: ExerciseId) = "settings_increase_${id.value}"

    fun value(id: ExerciseId) = "settings_value_${id.value}"

    fun untimed(id: ExerciseId) = "settings_untimed_${id.value}"

    fun palette(palette: ThemePalette) = "settings_palette_${palette.name}"
}

/** Ce que l'écran Réglages peut demander ; les valeurs par défaut servent aux aperçus et aux tests. */
data class SettingsActions(
    val onBack: () -> Unit = {},
    val onOpenAbout: () -> Unit = {},
    val onVibration: (Boolean) -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
    val onCrazyHaptic: (Boolean) -> Unit = {},
    val onDailyOnePage: (Boolean) -> Unit = {},
    val onPalette: (ThemePalette) -> Unit = {},
    val onVibrationStrength: (VibrationStrength) -> Unit = {},
    val onTryVibration: () -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onReduceAnimations: (Boolean?) -> Unit = {},
    val onExport: () -> Unit = {},
    val onRequestErase: () -> Unit = {},
    val onCancelErase: () -> Unit = {},
    val onConfirmErase: () -> Unit = {},
    val onEraseInput: (String) -> Unit = {},
    val onDecrease: (ExerciseId) -> Unit = {},
    val onIncrease: (ExerciseId) -> Unit = {},
    val onUntimed: (ExerciseId, Boolean) -> Unit = { _, _ -> },
)

/** Réglages (S11) : chaque changement s'applique immédiatement, sans bouton « Enregistrer ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isLoading) {
            Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                EndSignalSection(state, actions)
                DurationsSection(state, actions)
                DailySection(state, actions)
                AppearanceSection(state, actions)
                HelpSection()
                DataSection(state.data, actions)
                AboutEntry(actions.onOpenAbout)
            }
        }
    }
}

@Composable
private fun EndSignalSection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SectionTitle(stringResource(R.string.settings_section_end_signal))
    SwitchRow(
        label = stringResource(R.string.settings_vibration),
        checked = state.vibration,
        onCheckedChange = actions.onVibration,
        modifier = Modifier.testTag(SettingsTags.VIBRATION),
    )
    VibrationStrengthRows(state, actions)
    SwitchRow(
        label = stringResource(R.string.settings_sound),
        supporting = stringResource(R.string.settings_sound_hint),
        checked = state.sound,
        onCheckedChange = actions.onSound,
        modifier = Modifier.testTag(SettingsTags.SOUND),
    )
    SwitchRow(
        label = stringResource(R.string.settings_crazy_haptic),
        supporting = stringResource(R.string.settings_crazy_haptic_hint),
        checked = state.crazyMinuteHaptic,
        onCheckedChange = actions.onCrazyHaptic,
        modifier = Modifier.testTag(SettingsTags.CRAZY_HAPTIC),
    )
}

@Composable
private fun DurationsSection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SectionTitle(stringResource(R.string.settings_section_durations))
    state.durations.forEach { DurationItem(it, actions) }
}

@Composable
private fun DailySection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SectionTitle(stringResource(R.string.settings_section_daily))
    SwitchRow(
        label = stringResource(R.string.settings_daily_one_page),
        supporting = stringResource(R.string.settings_daily_one_page_hint),
        checked = state.dailyIncludeOnePage,
        onCheckedChange = actions.onDailyOnePage,
        modifier = Modifier.testTag(SettingsTags.DAILY_ONE_PAGE),
    )
}

@Composable
private fun HelpSection() {
    SectionTitle(stringResource(R.string.settings_section_help))
    Text(
        text = stringResource(R.string.settings_dictation_hint),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag(SettingsTags.DICTATION_HINT),
    )
}

@Composable
private fun AboutEntry(onOpenAbout: () -> Unit) {
    Row(
        modifier =
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onOpenAbout)
                .padding(horizontal = 16.dp)
                .testTag(SettingsTags.ABOUT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(DmIcons.Chevron, contentDescription = null)
    }
}

@Preview(name = "Clair", showBackground = true, heightDp = 1600)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 1600)
@Composable
private fun SettingsScreenPreview() {
    DmGymTheme {
        SettingsScreen(PreviewState, SettingsActions())
    }
}

private val PreviewState =
    SettingsUiState(
        isLoading = false,
        durations =
            ExerciseId.ALL.map {
                DurationRow(it, seconds = 300, minSeconds = 150, maxSeconds = 3000, untimed = it == ExerciseId.ONE_PAGE)
            },
        palette = ThemePalette.FOREST,
        palettes = ThemePalette.entries,
    )
