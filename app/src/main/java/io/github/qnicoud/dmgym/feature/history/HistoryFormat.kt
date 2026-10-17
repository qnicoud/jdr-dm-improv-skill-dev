package io.github.qnicoud.dmgym.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)
private val HourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("H 'h' mm", Locale.FRENCH)

/** « Aujourd’hui », « Hier » ou « 12 octobre ». */
@Composable
fun DayLabel.text(): String =
    when (this) {
        DayLabel.Today -> stringResource(R.string.history_day_today)
        DayLabel.Yesterday -> stringResource(R.string.history_day_yesterday)
        is DayLabel.Date -> LocalDate.ofEpochDay(epochDay).format(DayMonth)
    }

/** Heure locale de la session, dans le fuseau où elle a été faite : « 14 h 05 ». */
fun clockTime(
    epochMillis: Long,
    zoneId: String,
): String = Instant.ofEpochMilli(epochMillis).atZone(zone(zoneId)).format(HourMinute)

/** Date et heure du détail : « 12 octobre, 21 h 03 ». */
fun dateTime(
    epochMillis: Long,
    zoneId: String,
): String {
    val moment = Instant.ofEpochMilli(epochMillis).atZone(zone(zoneId))
    return "${moment.format(DayMonth)}, ${moment.format(HourMinute)}"
}

private fun zone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())
