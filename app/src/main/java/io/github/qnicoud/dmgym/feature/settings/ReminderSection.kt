package io.github.qnicoud.dmgym.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.reminder.ReminderSettings
import io.github.qnicoud.dmgym.ui.icons.ReminderIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import java.time.DayOfWeek

object ReminderTags {
    const val SWITCH = "reminder_switch"
    const val HELP = "reminder_help"
    const val TIME = "reminder_time"
    const val TIME_OK = "reminder_time_ok"
    const val DENIED = "reminder_denied"
    const val BLOCKED = "reminder_blocked"
    const val PAUSED = "reminder_paused"
    const val RESUME = "reminder_resume"
    const val OPEN_ANDROID = "reminder_open_android"
    const val KEEP_ONE_DAY = "reminder_keep_one_day"
    const val SEND_TEST = "reminder_send_test"
    const val CHANNEL = "reminder_channel"

    fun day(day: DayOfWeek) = "reminder_day_${day.name}"

    fun dayLetter(day: DayOfWeek) = "reminder_day_letter_${day.name}"

    fun dayCheck(day: DayOfWeek) = "reminder_day_check_${day.name}"
}

/** Ce que la section Rappels peut demander ; les valeurs par défaut servent aux aperçus et aux tests. */
data class ReminderActions(
    val onSwitch: (Boolean) -> Unit = {},
    val onTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onResume: () -> Unit = {},
    val onSendTest: () -> Unit = {},
    val onOpenAndroidSettings: () -> Unit = {},
    val onOpenChannelSettings: () -> Unit = {},
)

/** Section « Rappels » (UX parcours v1.1 §2) : entre les durées et l'apparence. */
@Composable
fun ReminderSection(
    state: ReminderUiState,
    actions: ReminderActions,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionTitle(stringResource(R.string.settings_section_reminder))
        val time = formatReminderTime(state.minuteOfDay, is24Hour)
        val summary =
            if (state.enabled) {
                stringResource(
                    R.string.reminder_summary_on,
                    joinDays(
                        state.days,
                        DayOfWeek.entries.associateWith {
                            dayShortName(it)
                        },
                    ),
                    time,
                )
            } else {
                stringResource(R.string.reminder_summary_off)
            }
        SwitchRow(
            label = stringResource(R.string.reminder_switch),
            supporting = summary,
            checked = state.enabled,
            onCheckedChange = actions.onSwitch,
            modifier = Modifier.testTag(ReminderTags.SWITCH),
        )
        Text(
            stringResource(R.string.reminder_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag(ReminderTags.HELP),
        )
        if (state.permissionDenied) {
            InfoMessage(R.string.reminder_denied, ReminderTags.DENIED, actions.onOpenAndroidSettings)
        }
        if (state.enabled) EnabledDetails(state, actions, time, is24Hour)
    }
}

@Composable
private fun EnabledDetails(
    state: ReminderUiState,
    actions: ReminderActions,
    time: String,
    is24Hour: Boolean,
) {
    if (state.blocked) {
        InfoMessage(R.string.reminder_blocked, ReminderTags.BLOCKED, actions.onOpenAndroidSettings)
    }
    if (state.paused) {
        InfoMessage(
            R.string.reminder_paused,
            ReminderTags.PAUSED,
            actions.onResume,
            actionLabel = R.string.reminder_resume,
            actionTag = ReminderTags.RESUME,
        )
    }
    TimeRow(state.minuteOfDay, time, is24Hour, actions.onTime)
    DayToggles(state.days, actions.onToggleDay)
    if (state.keepOneDayHint) {
        Text(
            stringResource(R.string.reminder_keep_one_day),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag(ReminderTags.KEEP_ONE_DAY),
        )
    }
    TextAction(R.string.reminder_send_test, ReminderTags.SEND_TEST, actions.onSendTest)
    TextAction(R.string.reminder_open_channel, ReminderTags.CHANNEL, actions.onOpenChannelSettings)
}

/** Message neutre : icône `info` et texte, jamais la couleur seule ; il ne prend pas le focus. */
@Composable
private fun InfoMessage(
    text: Int,
    tag: String,
    onAction: () -> Unit,
    actionLabel: Int = R.string.reminder_open_android_settings,
    actionTag: String = ReminderTags.OPEN_ANDROID,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(ReminderIcons.Info, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(tag))
        }
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp).testTag(actionTag)) {
            Text(stringResource(actionLabel))
        }
    }
}

@Composable
private fun TextAction(
    label: Int,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp).testTag(tag)) {
        Text(stringResource(label))
    }
}

@Composable
private fun TimeRow(
    minuteOfDay: Int,
    timeText: String,
    is24Hour: Boolean,
    onTime: (Int, Int) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button) { picking = true }
                .padding(horizontal = 16.dp)
                .testTag(ReminderTags.TIME),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.reminder_time_row), style = MaterialTheme.typography.bodyLarge)
        Text(timeText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
    if (picking) {
        TimeDialog(minuteOfDay, is24Hour, onDismiss = { picking = false }) { h, m ->
            picking = false
            onTime(h, m)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    minuteOfDay: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val picker =
        rememberTimePickerState(
            initialHour = minuteOfDay / ReminderSettings.MINUTES_PER_HOUR,
            initialMinute = minuteOfDay % ReminderSettings.MINUTES_PER_HOUR,
            is24Hour = is24Hour,
        )
    // La saisie au clavier est le mode accessible ; le cadran reste disponible.
    var keyboard by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time_dialog_title)) },
        text = {
            Column {
                if (keyboard) TimeInput(picker) else TimePicker(picker)
                TextButton(onClick = { keyboard = !keyboard }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(
                            if (keyboard) R.string.reminder_time_to_dial else R.string.reminder_time_to_keyboard,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(picker.hour, picker.minute) },
                modifier = Modifier.testTag(ReminderTags.TIME_OK),
            ) { Text(stringResource(R.string.reminder_time_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminder_time_cancel)) } },
    )
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ReminderSectionPreview() {
    DmGymTheme {
        ReminderSection(ReminderUiState(isLoading = false, enabled = true, paused = true), ReminderActions(), true)
    }
}
