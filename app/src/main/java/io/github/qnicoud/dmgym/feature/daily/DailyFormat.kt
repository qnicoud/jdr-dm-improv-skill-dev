package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)

/** « Séance du 16 octobre » à partir du `localEpochDay` du plan. */
@Composable
fun dailyDateTitle(epochDay: Long): String =
    stringResource(R.string.daily_date_title, LocalDate.ofEpochDay(epochDay).format(DayMonth))

@Composable
fun ExerciseProgress.label(): String =
    stringResource(
        when (this) {
            ExerciseProgress.DONE -> R.string.daily_state_done
            ExerciseProgress.IN_PROGRESS -> R.string.daily_state_in_progress
            ExerciseProgress.TODO -> R.string.daily_state_todo
        },
    )
