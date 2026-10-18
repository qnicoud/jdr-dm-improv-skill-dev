package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import java.time.DayOfWeek

internal val DayButtonSize = 48.dp

@Composable
internal fun dayName(day: DayOfWeek): String =
    stringResource(
        when (day) {
            DayOfWeek.MONDAY -> R.string.reminder_day_monday
            DayOfWeek.TUESDAY -> R.string.reminder_day_tuesday
            DayOfWeek.WEDNESDAY -> R.string.reminder_day_wednesday
            DayOfWeek.THURSDAY -> R.string.reminder_day_thursday
            DayOfWeek.FRIDAY -> R.string.reminder_day_friday
            DayOfWeek.SATURDAY -> R.string.reminder_day_saturday
            DayOfWeek.SUNDAY -> R.string.reminder_day_sunday
        },
    )

@Composable
internal fun dayShortName(day: DayOfWeek): String =
    stringResource(
        when (day) {
            DayOfWeek.MONDAY -> R.string.reminder_day_short_monday
            DayOfWeek.TUESDAY -> R.string.reminder_day_short_tuesday
            DayOfWeek.WEDNESDAY -> R.string.reminder_day_short_wednesday
            DayOfWeek.THURSDAY -> R.string.reminder_day_short_thursday
            DayOfWeek.FRIDAY -> R.string.reminder_day_short_friday
            DayOfWeek.SATURDAY -> R.string.reminder_day_short_saturday
            DayOfWeek.SUNDAY -> R.string.reminder_day_short_sunday
        },
    )

@Composable
private fun dayLetter(day: DayOfWeek): String =
    stringResource(
        when (day) {
            DayOfWeek.MONDAY -> R.string.reminder_day_letter_monday
            DayOfWeek.TUESDAY -> R.string.reminder_day_letter_tuesday
            DayOfWeek.WEDNESDAY -> R.string.reminder_day_letter_wednesday
            DayOfWeek.THURSDAY -> R.string.reminder_day_letter_thursday
            DayOfWeek.FRIDAY -> R.string.reminder_day_letter_friday
            DayOfWeek.SATURDAY -> R.string.reminder_day_letter_saturday
            DayOfWeek.SUNDAY -> R.string.reminder_day_letter_sunday
        },
    )

/** Sept boutons à bascule de 48 dp, sur deux rangées si la place manque (police à 200 %). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayToggles(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = stringResource(R.string.reminder_days_group)
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp).semantics { contentDescription = group },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.entries.forEach { day -> DayToggle(day, day in selected, onToggle) }
    }
}

@Composable
private fun DayToggle(
    day: DayOfWeek,
    selected: Boolean,
    onToggle: (DayOfWeek) -> Unit,
) {
    val name = dayName(day)
    val state = stringResource(if (selected) R.string.reminder_day_selected else R.string.reminder_day_unselected)
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = { onToggle(day) },
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline),
        modifier =
            Modifier
                .size(DayButtonSize)
                .testTag(ReminderTags.day(day))
                .clearAndSetSemantics {
                    contentDescription = name
                    stateDescription = state
                    role = Role.Checkbox
                    onClick(label = null) {
                        onToggle(day)
                        true
                    }
                },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(dayLetter(day), style = MaterialTheme.typography.labelLarge)
            if (selected) {
                // La coche s'ajoute à la couleur et au contour : l'état ne repose jamais sur la couleur seule.
                Icon(
                    DmIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(12.dp),
                )
            }
        }
    }
}
