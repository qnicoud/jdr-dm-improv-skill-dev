package io.github.qnicoud.dmgym.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.feature.home.durationLabel
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.theme.staticColorScheme

private val MinRowHeight = 56.dp

@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp).semantics { heading() },
    )
}

/** Ligne à commutateur : toute la ligne est cible (≥ 56 dp) et la coche du curseur évite de dépendre de la couleur. */
@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val stateText = stringResource(if (checked) R.string.settings_state_on else R.string.settings_state_off)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinRowHeight)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .semantics { stateDescription = stateText }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            thumbContent =
                if (checked) {
                    {
                        Icon(
                            DmIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
        )
    }
}

/** Durée d'un exercice : pas − / + et option « Sans minuteur » (les boutons s'éteignent quand elle est cochée). */
@Composable
internal fun DurationItem(
    row: DurationRow,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(row.id.nameRes)
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { actions.onDecrease(row.id) },
                enabled = row.canDecrease && !row.untimed,
                modifier = Modifier.testTag(SettingsTags.decrease(row.id)),
            ) {
                Icon(DmIcons.Minus, contentDescription = stringResource(R.string.settings_duration_decrease, name))
            }
            Text(
                text = durationLabel(if (row.untimed) null else row.seconds),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 96.dp).testTag(SettingsTags.value(row.id)),
            )
            IconButton(
                onClick = { actions.onIncrease(row.id) },
                enabled = row.canIncrease && !row.untimed,
                modifier = Modifier.testTag(SettingsTags.increase(row.id)),
            ) {
                Icon(DmIcons.Plus, contentDescription = stringResource(R.string.settings_duration_increase, name))
            }
        }
        SwitchRow(
            label = stringResource(R.string.settings_untimed_for, name),
            checked = row.untimed,
            onCheckedChange = { actions.onUntimed(row.id, it) },
            modifier = Modifier.testTag(SettingsTags.untimed(row.id)),
        )
    }
}

/** Ligne de thème : bouton radio, pastille de la couleur principale et nom. */
@Composable
internal fun PaletteRow(
    palette: ThemePalette,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinRowHeight)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Swatch(palette)
        Text(stringResource(palette.labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Pastille décorative (le nom suffit à TalkBack) ; la palette dynamique, sans couleur fixe, montre un dégradé. */
@Composable
private fun Swatch(palette: ThemePalette) {
    val dark = isSystemInDarkTheme()
    val brush =
        if (palette == ThemePalette.DYNAMIC) {
            Brush.sweepGradient(
                ThemePalette.entries
                    .filter { it != ThemePalette.DYNAMIC }
                    .map { staticColorScheme(it, dark).primary },
            )
        } else {
            SolidColor(staticColorScheme(palette, dark).primary)
        }
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(brush)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape),
    )
}

@get:StringRes
internal val ThemePalette.labelRes: Int
    get() =
        when (this) {
            ThemePalette.PARCHMENT -> R.string.settings_palette_parchment
            ThemePalette.FOREST -> R.string.settings_palette_forest
            ThemePalette.NIGHT -> R.string.settings_palette_night
            ThemePalette.MIST -> R.string.settings_palette_mist
            ThemePalette.DYNAMIC -> R.string.settings_palette_dynamic
        }
