package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.feature.home.durationLabel
import io.github.qnicoud.dmgym.feature.settings.SwitchRow
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private const val MS_PER_SECOND = 1_000L

/** Identifiants de test de la feuille de réglage de la durée (écran Prêt). */
object DurationSheetTags {
    const val SHEET = "exercise_duration_sheet"
    const val VALUE = "exercise_duration_value"
    const val DECREASE = "exercise_duration_decrease"
    const val INCREASE = "exercise_duration_increase"
    const val UNTIMED = "exercise_duration_untimed"
}

/**
 * Feuille du bas de l'écran Prêt : durée de l'exercice et « sans minuteur ». Elle n'existe que sur cet écran,
 * jamais pendant l'exercice en cours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSheet(
    exerciseName: String,
    setting: TimerSetting,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onUntimedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(DurationSheetTags.SHEET),
    ) {
        DurationSheetContent(exerciseName, setting, onDecrease, onIncrease, onUntimedChange)
    }
}

/** Contenu sans état de [DurationSheet] : pas − / +, durée, option « sans minuteur ». */
@Composable
fun DurationSheetContent(
    exerciseName: String,
    setting: TimerSetting,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onUntimedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(bottom = 16.dp),
    ) {
        Text(
            stringResource(R.string.duration_sheet_title, exerciseName),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
        )
        DurationStepper(exerciseName, setting, onDecrease, onIncrease)
        SwitchRow(
            label = stringResource(R.string.duration_sheet_untimed),
            checked = setting.untimed,
            onCheckedChange = onUntimedChange,
            modifier = Modifier.testTag(DurationSheetTags.UNTIMED),
        )
    }
}

@Composable
private fun DurationStepper(
    name: String,
    setting: TimerSetting,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val spoken =
        if (setting.untimed) {
            stringResource(R.string.timer_untimed)
        } else {
            spokenDuration(setting.seconds * MS_PER_SECOND)
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = setting.seconds > setting.minSeconds && !setting.untimed,
            modifier = Modifier.testTag(DurationSheetTags.DECREASE),
        ) {
            Icon(DmIcons.Minus, contentDescription = stringResource(R.string.settings_duration_decrease, name))
        }
        Text(
            text = durationLabel(if (setting.untimed) null else setting.seconds),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .widthIn(min = 120.dp)
                    .testTag(DurationSheetTags.VALUE)
                    .semantics {
                        stateDescription = spoken
                        liveRegion = LiveRegionMode.Polite
                    },
        )
        IconButton(
            onClick = onIncrease,
            enabled = setting.seconds < setting.maxSeconds && !setting.untimed,
            modifier = Modifier.testTag(DurationSheetTags.INCREASE),
        ) {
            Icon(DmIcons.Plus, contentDescription = stringResource(R.string.settings_duration_increase, name))
        }
    }
}
