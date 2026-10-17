package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

/** Lit [timer] ici, dans sa propre portée, pour qu'un tic ne recompose que la barre (pas tout l'écran). */
@Composable
fun LiveTimerBar(
    timer: () -> TimerUi,
    canPause: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) = TimerBar(timer(), canPause, onPause, onResume, modifier, compact, onClick)

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
    /** Hauteur disponible faible (paysage, clavier ouvert) : marges et texte réduits, libellé sous l'heure omis. */
    compact: Boolean = false,
    /** Écran Prêt : toute la barre ouvre le réglage de la durée. */
    onClick: (() -> Unit)? = null,
) {
    val colors = timerColors(timer.kind)
    val description = timerDescription(timer)
    val editLabel = stringResource(R.string.timer_ready_edit)
    val tap =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.heightIn(min = 48.dp).clickable(onClickLabel = editLabel, role = Role.Button, onClick = onClick)
        }
    Surface(
        modifier = modifier.fillMaxWidth().testTag(ExerciseTags.TIMER).then(tap),
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 2.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp),
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
                            style = timerTextStyle(compact).copy(fontFeatureSettings = TABULAR),
                        )
                        timerLabel(timer)?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                if (onClick != null) Icon(DmIcons.Chevron, contentDescription = null)
                PauseButton(timer, canPause, onPause, onResume)
            }
            TimerProgress(timer, colors.bar)
        }
    }
}

@Composable
private fun timerTextStyle(compact: Boolean) =
    if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall

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
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(color.copy(alpha = TRACK_ALPHA), size = size, cornerRadius = radius)
    val width = progressFillWidth(size.width, size.height, fraction)
    if (width > 0f) drawRoundRect(color, size = Size(width, size.height), cornerRadius = radius)
}

/** Largeur du remplissage : jamais sous la hauteur de la barre, pour que les deux demi-cercles restent visibles. */
internal fun progressFillWidth(
    trackWidth: Float,
    height: Float,
    fraction: Float,
): Float = if (fraction <= 0f) 0f else (trackWidth * fraction).coerceAtLeast(height).coerceAtMost(trackWidth)

private fun DrawScope.drawOutline(color: Color) {
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius((size.height - stroke) / 2),
        style = Stroke(width = stroke),
    )
}

/** Barre pleine à rayures diagonales : elle ne diminue plus, le temps additionnel n'est pas une alerte. */
private fun DrawScope.drawStripes(color: Color) {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(color.copy(alpha = TRACK_ALPHA), size = size, cornerRadius = radius)
    val step = STRIPE_STEP.toPx()
    val shape = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), radius)) }
    clipPath(shape) {
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
