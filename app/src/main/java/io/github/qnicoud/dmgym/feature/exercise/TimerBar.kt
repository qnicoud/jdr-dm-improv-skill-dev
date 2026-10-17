package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons

private val STRIPE_STEP = 8.dp
private val BAR_HEIGHT = 6.dp
private val THICK_BAR_HEIGHT = 8.dp

/**
 * Minuteur de l'exercice : six états distincts sans la couleur (icône, libellé, forme de la barre), voir
 * design-system §5.3 et maquette 05. Un seul nœud d'accessibilité, sans annonce à la seconde.
 */
@Composable
fun TimerBar(
    timer: TimerUi,
    canPause: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = timerColors(timer.kind)
    val description = timerDescription(timer)
    Surface(
        modifier = modifier.fillMaxWidth().testTag(ExerciseTags.TIMER),
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(timerIcon(timer), contentDescription = null, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            timerText(timer),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TABULAR),
                        )
                        timerLabel(timer)?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                PauseButton(timer, canPause, onPause, onResume)
            }
            TimerProgress(timer, colors.bar)
        }
    }
}

@Composable
private fun PauseButton(
    timer: TimerUi,
    canPause: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    if (!canPause) return
    val paused = timer.kind == TimerKind.Paused
    IconButton(
        onClick = if (paused) onResume else onPause,
        modifier = Modifier.testTag(ExerciseTags.PAUSE).size(48.dp),
    ) {
        Icon(
            imageVector = if (paused) ExerciseIcons.Play else ExerciseIcons.Pause,
            contentDescription = stringResource(if (paused) R.string.exercise_resume else R.string.exercise_pause),
        )
    }
}

@Composable
private fun TimerProgress(
    timer: TimerUi,
    color: Color,
) {
    if (timer.kind == TimerKind.Untimed) return
    val thick = timer.kind == TimerKind.LastMinute
    val height = if (thick) THICK_BAR_HEIGHT else BAR_HEIGHT
    val alpha = if (timer.kind == TimerKind.Paused) HALF else 1f
    Canvas(modifier = Modifier.fillMaxWidth().height(height).alpha(alpha)) {
        when (timer.kind) {
            TimerKind.Overtime -> drawStripes(color)
            TimerKind.Ready -> drawOutline(color)
            else -> drawFill(color, timer.fraction.coerceIn(0f, 1f))
        }
    }
}

private fun DrawScope.drawFill(
    color: Color,
    fraction: Float,
) {
    drawRect(color.copy(alpha = TRACK_ALPHA), size = size)
    drawRect(color, size = Size(size.width * fraction, size.height))
}

private fun DrawScope.drawOutline(color: Color) {
    drawRect(color, style = Stroke(width = 2.dp.toPx()), size = size)
}

/** Barre pleine à rayures diagonales : elle ne diminue plus, le temps additionnel n'est pas une alerte. */
private fun DrawScope.drawStripes(color: Color) {
    drawRect(color.copy(alpha = TRACK_ALPHA), size = size)
    val step = STRIPE_STEP.toPx()
    clipRect {
        var x = -size.height
        while (x < size.width) {
            drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = step / 2)
            x += step
        }
    }
}

@Composable
private fun timerText(timer: TimerUi): String =
    when {
        timer.kind == TimerKind.Untimed && timer.elapsedMs == 0L && timer.displayMs == 0L -> {
            stringResource(
                R.string.timer_untimed,
            )
        }

        timer.kind == TimerKind.Overtime || timer.overtime -> {
            "+" + formatClock(timer.displayMs)
        }

        else -> {
            formatClock(timer.displayMs)
        }
    }

@Composable
private fun timerLabel(timer: TimerUi): String? =
    when {
        timer.kind == TimerKind.Ready -> stringResource(R.string.timer_ready)
        timer.kind == TimerKind.LastMinute -> stringResource(R.string.timer_last_minute)
        timer.kind == TimerKind.Overtime -> stringResource(R.string.timer_overtime)
        timer.kind == TimerKind.Paused -> stringResource(R.string.timer_paused)
        timer.kind == TimerKind.Untimed && timer.displayMs > 0 -> stringResource(R.string.timer_untimed)
        else -> null
    }

@Composable
private fun timerIcon(timer: TimerUi): ImageVector =
    when (timer.kind) {
        TimerKind.LastMinute -> ExerciseIcons.Hourglass
        TimerKind.Overtime -> ExerciseIcons.AddCircle
        TimerKind.Paused -> ExerciseIcons.Pause
        TimerKind.Untimed -> DmIcons.TimerOff
        else -> DmIcons.Timer
    }

@Composable
private fun timerDescription(timer: TimerUi): String {
    val spoken = spokenDuration(timer.displayMs)
    return when {
        timer.kind == TimerKind.Ready -> {
            stringResource(R.string.timer_description_ready, spoken)
        }

        timer.kind == TimerKind.Overtime || (timer.kind == TimerKind.Paused && timer.overtime) -> {
            stringResource(R.string.timer_description_overtime, spoken)
        }

        timer.kind == TimerKind.Paused -> {
            stringResource(R.string.timer_description_paused, spoken)
        }

        timer.kind == TimerKind.Untimed && timer.displayMs == 0L -> {
            stringResource(R.string.timer_untimed)
        }

        timer.kind == TimerKind.Untimed -> {
            stringResource(R.string.timer_description_elapsed, spoken)
        }

        else -> {
            stringResource(R.string.timer_description_remaining, spoken)
        }
    }
}

private class TimerColors(
    val container: Color,
    val content: Color,
    val bar: Color,
)

@Composable
private fun timerColors(kind: TimerKind): TimerColors {
    val scheme = MaterialTheme.colorScheme
    return when (kind) {
        TimerKind.LastMinute -> {
            TimerColors(
                scheme.secondaryContainer,
                scheme.onSecondaryContainer,
                scheme.onSecondaryContainer,
            )
        }

        TimerKind.Overtime -> {
            TimerColors(
                scheme.tertiaryContainer,
                scheme.onTertiaryContainer,
                scheme.onTertiaryContainer,
            )
        }

        TimerKind.Untimed -> {
            TimerColors(scheme.surfaceContainer, scheme.onSurface, scheme.primary)
        }

        else -> {
            TimerColors(scheme.surfaceContainerHigh, scheme.onSurface, scheme.primary)
        }
    }
}

private const val TABULAR = "tnum"
private const val HALF = 0.5f
private const val TRACK_ALPHA = 0.18f
