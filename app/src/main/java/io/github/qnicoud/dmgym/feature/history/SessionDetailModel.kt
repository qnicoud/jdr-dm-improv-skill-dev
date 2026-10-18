package io.github.qnicoud.dmgym.feature.history

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FirstPick
import io.github.qnicoud.dmgym.core.exercise.StringResId
import io.github.qnicoud.dmgym.core.exercise.reachedQuestions
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus

/** Un élément tiré. [used] : « utilisé » / « non utilisé », `null` si l'exercice ne suit pas cet élément. */
data class DrawLine(
    val text: String,
    val used: Boolean?,
    /** Catégorie de l'entrée (sens, type d'élément d'histoire) : donne l'icône de la ligne. */
    val category: String? = null,
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
    /** Libellé « Offre %1$d sur %2$d » du rang [group] quand l'exercice montre ses questions une à une. */
    val groupLabel: StringResId? = null,
    val groupCount: Int? = null,
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
    /** Texte de la réponse choisie dans le bilan « jouée en premier » (Decision Point), ou `null`. */
    val firstPick: String? = null,
    /** Titre de ce choix (`endChoiceLabel` de la définition), `null` : le titre générique. */
    val firstPickTitle: StringResId? = null,
)

internal fun SessionDetail.toUi(definition: ExerciseDefinition?): SessionDetailUi {
    val answers = answers.toLines(definition, questionCount(definition))
    return SessionDetailUi(
        sessionId = session.id,
        exerciseId = ExerciseId(session.exerciseId),
        startedAtEpochMillis = session.startedAtEpochMillis,
        zoneId = session.zoneId,
        activeSec = session.activeDurationSec,
        overtimeSec = session.overtimeSec,
        unfinished = session.status == SessionStatus.IN_PROGRESS,
        draws = reachedDraws(definition).map { it.toLine() },
        answers = answers,
        rubric = rubricLines(definition),
        firstPick = firstPickText(),
        firstPickTitle = definition?.endChoiceLabel,
        selfRating = session.selfRating,
        isGem = session.isGem,
        shareText = answers.joinToString("\n\n") { it.text }.takeIf { it.isNotBlank() },
    )
}

private fun SessionDetail.questionCount(definition: ExerciseDefinition?): Int {
    val layout = definition?.layout as? FieldLayout.Sequential ?: return 0
    return draws.count { it.slotKey == layout.questionsSlot }
}

/** Tirages à montrer : les questions révélées une à une s'arrêtent à la plus haute atteinte (jamais une offre cachée). */
private fun SessionDetail.reachedDraws(definition: ExerciseDefinition?): List<DrawRecord> {
    val layout = definition?.layout as? FieldLayout.Sequential
    if (layout == null || !layout.revealProgressively) return draws
    val answered = answers.filter { it.text.isNotBlank() }.map { it.groupIndex }
    val reached = reachedQuestions(answered, questionCount(definition))
    return draws.filter { it.slotKey != layout.questionsSlot || it.position < reached }
}

private fun DrawRecord.toLine() = DrawLine(textSnapshot, manualChecked ?: autoDetected, category)

private fun List<AnswerRecord>.toLines(
    definition: ExerciseDefinition?,
    questionCount: Int,
): List<AnswerLine> {
    val keys = definition?.fieldKeys.orEmpty()
    val specs = definition?.fieldSpecs().orEmpty()
    val repeatable = definition?.layout is FieldLayout.RepeatableGroup
    val itemLabel = (definition?.layout as? FieldLayout.Sequential)?.itemLabel
    val fieldRank = { a: AnswerRecord -> keys.indexOf(a.fieldKey).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
    val order =
        if (itemLabel != null) {
            compareBy<AnswerRecord>({ it.groupIndex }, fieldRank)
        } else {
            compareBy(fieldRank, { it.groupIndex })
        }
    return filter { it.text.isNotBlank() }
        .sortedWith(order)
        .mapIndexed { position, answer ->
            val spec = specs.firstOrNull { it.key == answer.fieldKey }
            AnswerLine(
                fieldLabel = spec?.label,
                fieldIndex = keys.indexOf(answer.fieldKey).takeIf { it >= 0 }?.plus(1) ?: (position + 1),
                group = answer.groupIndex.takeIf { repeatable || itemLabel != null }?.plus(1),
                text = answer.text,
                flagged = answer.flagged,
                flagLabel = spec?.flagLabel,
                groupLabel = itemLabel,
                groupCount = questionCount.takeIf { itemLabel != null },
            )
        }
}

private fun SessionDetail.rubricLines(definition: ExerciseDefinition?): List<RubricLine> =
    definition
        ?.rubric
        ?.mapNotNull { question ->
            rubric.firstOrNull { it.questionKey == question.key }?.let { RubricLine(question.text, it.value) }
        }.orEmpty()

private fun SessionDetail.firstPickText(): String? {
    val field = FirstPick.chosen(rubric.associate { it.questionKey to it.value }) ?: return null
    return answers.firstOrNull { it.fieldKey == field }?.text?.takeIf { it.isNotBlank() }
}

private fun ExerciseDefinition.fieldSpecs() =
    when (val l = layout) {
        is FieldLayout.Fixed -> l.fields
        is FieldLayout.Single -> listOf(l.field)
        is FieldLayout.Sequential -> l.fields
        is FieldLayout.RepeatableGroup -> l.before + l.fields + l.after
        is FieldLayout.None -> emptyList()
    } + listOfNotNull(endNote)
