package io.github.qnicoud.dmgym.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.streak.DayMark
import io.github.qnicoud.dmgym.core.streak.DayStatus
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DaySize = 36.dp
private val RingPadding = 3.dp
private const val DASH_ON = 6f
private const val DASH_OFF = 4f

/**
 * Rangée glissante des 7 derniers jours, aujourd'hui à droite. Trois formes, sans couleur d'échec :
 * disque plein avec coche (pratiqué), cercle pointillé avec « g » (jour de grâce), cercle vide (rien).
 */
@Composable
fun RecentDaysRow(
    days: List<DayStatus>,
    today: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().testTag(StatsTags.RECENT_DAYS),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { day -> DayCell(day, isToday = day.epochDay == today) }
    }
}

@Composable
private fun DayCell(
    day: DayStatus,
    isToday: Boolean,
) {
    val date = LocalDate.ofEpochDay(day.epochDay)
    val markText = stringResource(day.mark.labelRes())
    val name = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
    val description =
        if (isToday) {
            stringResource(
                R.string.stats_day_today,
                name,
                markText,
            )
        } else {
            stringResource(R.string.stats_day_description, name, markText)
        }
    Column(
        modifier =
            Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                }.testTag(StatsTags.day(day.epochDay)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(RingPadding).let { if (isToday) it.todayRing() else it },
            contentAlignment = Alignment.Center,
        ) {
            DayShape(day.mark)
        }
        Text(
            text = date.dayOfWeek.initial(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun Modifier.todayRing(): Modifier =
    border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(RingPadding)

@Composable
private fun DayShape(mark: DayMark) {
    val neutral = MaterialTheme.colorScheme.outline
    when (mark) {
        DayMark.PRACTICED -> {
            Box(
                modifier = Modifier.size(DaySize).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DmIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
            }
        }

        DayMark.GRACE -> {
            Box(modifier = Modifier.size(DaySize), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(DaySize)) {
                    drawCircle(
                        color = neutral,
                        radius = size.minDimension / 2,
                        style =
                            Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
                            ),
                    )
                }
            }
        }

        DayMark.NONE -> {
            Canvas(Modifier.size(DaySize)) {
                drawCircle(color = neutral, radius = size.minDimension / 2, style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

private fun DayMark.labelRes(): Int =
    when (this) {
        DayMark.PRACTICED -> R.string.stats_day_practiced
        DayMark.GRACE -> R.string.stats_day_grace
        DayMark.NONE -> R.string.stats_day_none
    }

private fun DayOfWeek.initial(): String = getDisplayName(TextStyle.NARROW, Locale.FRENCH).uppercase(Locale.FRENCH)
