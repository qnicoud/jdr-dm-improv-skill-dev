package io.github.qnicoud.dmgym.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.core.exercise.ExerciseId

/**
 * Icônes de trait (24 dp, trait 2) reprises des maquettes de docs/mockups/, embarquées pour rester hors ligne
 * sans dépendre de material-icons-extended. Teintées par `Icon`.
 */
object DmIcons {
    val Settings =
        icon(
            "settings",
            circle(12f, 12f, 3f),
            circle(12f, 12f, 6.5f),
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1",
        )
    val Back = icon("back", "M19 12H5M11 6l-6 6 6 6")
    val Chevron = icon("chevron", "M9 6l6 6-6 6")
    val Timer = icon("timer", circle(12f, 13f, 8f), "M12 9v4l3 2M9 2h6")
    val TimerOff = icon("timer_off", circle(12f, 13f, 8f), "M12 9v4l3 2M9 2h6M3 3l18 18")
    val Fire = icon("fire", "M12 3c1 4 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-5 1-9z")
    val Train = icon("train", "M2 9v6M5 6v12M19 6v12M22 9v6M5 12h14")
    val Book = icon("book", "M12 6c-2-1.5-5-2-8-1v13c3-1 6-.5 8 1 2-1.5 5-2 8-1V5c-3-1-6-.5-8 1zM12 6v13")
    val Chart = icon("chart", "M5 20V11M12 20V4M19 20v-6")
    val CrazyEights =
        icon(
            "c8",
            "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z",
            circle(12f, 9f, 2.5f),
            circle(12f, 15f, 3f),
        )
    val Cube = icon("cube", "M12 3l8 4.5v9L12 21l-8-4.5v-9zM12 12l8-4.5M12 12L4 7.5M12 12v9")
    val Hub =
        icon(
            "hub",
            circle(12f, 12f, 2.5f),
            circle(5f, 5f, 2f),
            circle(19f, 5f, 2f),
            circle(5f, 19f, 2f),
            circle(19f, 19f, 2f),
            "M7 7l3 3M17 7l-3 3M7 17l3-3M17 17l-3-3",
        )
    val Cards = icon("cards", "M5 5h8a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M18 7l3 11")
    val Route = icon("route", "M12 21v-8M12 13L6 8M12 13l6-5M6 8V4M3.5 6.5L6 4l2.5 2.5M18 8V4M15.5 6.5L18 4l2.5 2.5")
    val Link =
        icon(
            "link",
            "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1",
        )
    val Doc = icon("doc", "M6 3h9l4 4v14H6zM15 3v4h4M9 12h7M9 16h7")

    fun forExercise(id: ExerciseId): ImageVector =
        when (id) {
            ExerciseId.CRAZY_EIGHTS -> CrazyEights
            ExerciseId.CONSTRAINT_BOX -> Cube
            ExerciseId.CORE_IMAGE -> Hub
            ExerciseId.STATUS_NUMBERS -> Cards
            ExerciseId.DECISION_POINT -> Route
            ExerciseId.CONNECT_STORY -> Link
            ExerciseId.ONE_PAGE -> Doc
            else -> error("Unknown exercise: ${id.value}")
        }
}

internal fun circle(
    cx: Float,
    cy: Float,
    r: Float,
): String = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

internal fun icon(
    name: String,
    vararg paths: String,
): ImageVector {
    val builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
    paths.forEach { data ->
        builder.addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}
