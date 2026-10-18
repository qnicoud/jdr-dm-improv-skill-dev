package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.i18n.LocaleFormats
import io.github.qnicoud.dmgym.ui.components.currentLocale
import java.time.LocalDate

/** « Séance du 16 octobre » à partir du `localEpochDay` du plan. */
@Composable
fun dailyDateTitle(epochDay: Long): String =
    stringResource(R.string.daily_date_title, LocaleFormats.dayMonth(LocalDate.ofEpochDay(epochDay), currentLocale()))

@Composable
fun ExerciseProgress.label(): String =
    stringResource(
        when (this) {
            ExerciseProgress.DONE -> R.string.daily_state_done
            ExerciseProgress.IN_PROGRESS -> R.string.daily_state_in_progress
            ExerciseProgress.TODO -> R.string.daily_state_todo
        },
    )
