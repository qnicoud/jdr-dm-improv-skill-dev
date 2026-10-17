package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/** « 4:30 » : minutes et secondes, sans heures (la durée maximale est de 10 h). */
fun formatClock(ms: Long): String {
    val totalSeconds = (ms / MS_PER_SECOND).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}

/** Durée dite à voix haute : « 4 minutes 30 secondes ». */
@Composable
fun spokenDuration(ms: Long): String {
    val totalSeconds = (ms / MS_PER_SECOND).coerceAtLeast(0)
    val minutes = (totalSeconds / SECONDS_PER_MINUTE).toInt()
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt()
    val parts =
        buildList {
            if (minutes > 0) add(pluralStringResource(R.plurals.duration_minutes_spoken, minutes, minutes))
            if (seconds > 0 ||
                minutes == 0
            ) {
                add(pluralStringResource(R.plurals.duration_seconds_spoken, seconds, seconds))
            }
        }
    return parts.joinToString(" ")
}

/** Durée affichée dans le bilan : « 8 min 12 s ». */
@Composable
fun shortDuration(ms: Long): String {
    val totalSeconds = (ms / MS_PER_SECOND).coerceAtLeast(0)
    val minutes = (totalSeconds / SECONDS_PER_MINUTE).toInt()
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt()
    return buildList {
        if (minutes > 0) add(stringResource(R.string.duration_minutes, minutes))
        if (seconds > 0 || minutes == 0) add(stringResource(R.string.duration_seconds, seconds))
    }.joinToString(" ")
}
