package io.github.qnicoud.dmgym.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.i18n.LocaleFormats
import io.github.qnicoud.dmgym.ui.components.currentLocale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** « Aujourd’hui », « Hier » ou « 12 octobre ». */
@Composable
fun DayLabel.text(): String =
    when (this) {
        DayLabel.Today -> stringResource(R.string.history_day_today)
        DayLabel.Yesterday -> stringResource(R.string.history_day_yesterday)
        is DayLabel.Date -> LocaleFormats.dayMonth(LocalDate.ofEpochDay(epochDay), currentLocale())
    }

/** Heure locale de la session, dans le fuseau où elle a été faite : « 14 h 05 ». */
fun clockTime(
    epochMillis: Long,
    zoneId: String,
    locale: Locale,
): String = LocaleFormats.clock(Instant.ofEpochMilli(epochMillis).atZone(zone(zoneId)).toLocalTime(), locale)

/** Date et heure du détail : « 12 octobre, 21 h 03 ». */
fun dateTime(
    epochMillis: Long,
    zoneId: String,
    locale: Locale,
): String {
    val moment = Instant.ofEpochMilli(epochMillis).atZone(zone(zoneId))
    return "${LocaleFormats.dayMonth(
        moment.toLocalDate(),
        locale,
    )}, ${LocaleFormats.clock(moment.toLocalTime(), locale)}"
}

private fun zone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())
