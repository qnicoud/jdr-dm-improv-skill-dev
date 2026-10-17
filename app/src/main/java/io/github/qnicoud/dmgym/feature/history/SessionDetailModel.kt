package io.github.qnicoud.dmgym.feature.history

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.StringResId
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus

/** Un élément tiré. [used] : « utilisé » / « non utilisé », `null` si l'exercice ne suit pas cet élément. */
data class DrawLine(
    val text: String,
    val used: Boolean?,
)

/** Une réponse saisie. Les libellés viennent de la définition de l'exercice quand elle existe. */
data class AnswerLine(
    /** Libellé du champ, ou `null` : l'écran affiche alors « Réponse n ». */
    val fieldLabel: StringResId?,
    val fieldIndex: Int,
    /** Rang du groupe (à partir de 1) pour les exercices à groupes répétables, sinon `null`. */
    val group: Int?,
    val text: String,
    val flagged: Boolean,
    val flagLabel: StringResId?,
)

data class RubricLine(
    val question: StringResId,
    val value: Boolean,
)

/** Contenu du détail d'une session (S09). */
data class SessionDetailUi(
    val sessionId: Long,
    val exerciseId: ExerciseId,
    val startedAtEpochMillis: Long,
    val zoneId: String,
    val activeSec: Int,
    val overtimeSec: Int,
    val unfinished: Boolean,
    val draws: List<DrawLine>,
    val answers: List<AnswerLine>,
    val rubric: List<RubricLine>,
    val selfRating: Int?,
    val isGem: Boolean,
    /** Texte à envoyer à la feuille de partage ; `null` s'il n'y a rien à partager. */
    val shareText: String?,
)

internal fun SessionDetail.toUi(definition: ExerciseDefinition?): SessionDetailUi {
    val answers = answers.toLines(definition)
    return SessionDetailUi(
        sessionId = session.id,
        exerciseId = ExerciseId(session.exerciseId),
        startedAtEpochMillis = session.startedAtEpochMillis,
        zoneId = session.zoneId,
        activeSec = session.activeDurationSec,
        overtimeSec = session.overtimeSec,
        unfinished = session.status == SessionStatus.IN_PROGRESS,
        draws = draws.map { it.toLine() },
        answers = answers,
        rubric = rubricLines(definition),
        selfRating = session.selfRating,
        isGem = session.isGem,
        shareText = answers.joinToString("\n\n") { it.text }.takeIf { it.isNotBlank() },
    )
}

private fun DrawRecord.toLine() = DrawLine(textSnapshot, manualChecked ?: autoDetected)

private fun List<AnswerRecord>.toLines(definition: ExerciseDefinition?): List<AnswerLine> {
    val keys = definition?.fieldKeys.orEmpty()
    val specs = definition?.fieldSpecs().orEmpty()
    val repeatable = definition?.layout is FieldLayout.RepeatableGroup
    return filter { it.text.isNotBlank() }
        .sortedWith(compareBy({ keys.indexOf(it.fieldKey).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }, { it.groupIndex }))
        .mapIndexed { position, answer ->
            val spec = specs.firstOrNull { it.key == answer.fieldKey }
            AnswerLine(
                fieldLabel = spec?.label,
                fieldIndex = keys.indexOf(answer.fieldKey).takeIf { it >= 0 }?.plus(1) ?: (position + 1),
                group = answer.groupIndex.takeIf { repeatable }?.plus(1),
                text = answer.text,
                flagged = answer.flagged,
                flagLabel = spec?.flagLabel,
            )
        }
}

private fun SessionDetail.rubricLines(definition: ExerciseDefinition?): List<RubricLine> =
    definition
        ?.rubric
        ?.mapNotNull { question ->
            rubric.firstOrNull { it.questionKey == question.key }?.let { RubricLine(question.text, it.value) }
        }.orEmpty()

private fun ExerciseDefinition.fieldSpecs() =
    when (val l = layout) {
        is FieldLayout.Fixed -> l.fields
        is FieldLayout.Single -> listOf(l.field)
        is FieldLayout.Sequential -> listOf(l.field)
        is FieldLayout.RepeatableGroup -> l.fields
    }
