package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.core.backup.READABLE_EXPORT_HEADER
import io.github.qnicoud.dmgym.core.exercise.FirstPick
import io.github.qnicoud.dmgym.core.exercise.reachedQuestions
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SECONDS_PER_MINUTE = 60

/**
 * Texte lisible de l'export des sessions (S11, Données). Le contenu du fichier est un document, pas de l'interface :
 * il est en français ; ses mots fixes viennent de [ExportLabels] (`strings.xml`). [labels] : libellés lisibles, repli sur les clés brutes.
 * Les sessions en cours sans réponse écrite sont omises (rien à exporter).
 */
class SessionExportFormatter(
    private val labels: ExportLabels,
) {
    fun format(
        details: List<SessionDetail>,
        exportedAt: Instant,
    ): String {
        val kept = details.filter { it.hasContent() }.sortedBy { it.session.startedAtEpochMillis }
        return buildString {
            appendLine(READABLE_EXPORT_HEADER)
            appendLine("Exporté le ${DATE_TIME.format(exportedAt.atZone(ZoneOffset.UTC))} UTC")
            appendLine("${kept.size} ${if (kept.size > 1) "sessions" else "session"}")
            kept.forEach { appendSession(it) }
        }
    }

    private fun SessionDetail.hasContent(): Boolean =
        session.status == SessionStatus.COMPLETED || answers.any { it.text.isNotBlank() }

    private fun StringBuilder.appendSession(detail: SessionDetail) {
        val session = detail.session
        appendLine()
        appendLine("=== ${labels.exercise(session.exerciseId)} ===")
        appendLine("Début : ${localStart(session.startedAtEpochMillis, session.zoneId)}")
        val status = if (session.status == SessionStatus.COMPLETED) "terminée" else "inachevée"
        appendLine("Statut : $status")
        appendLine("Durée active : ${duration(session.activeDurationSec)} (dépassement ${session.overtimeSec} s)")
        session.selfRating?.let { appendLine("Auto-évaluation : $it") }
        if (session.isGem) appendLine("Bijou : oui")
        val exercise = session.exerciseId
        val answers = detail.answers.filter { it.text.isNotBlank() }
        val total = detail.draws.count { it.slotKey == labels.questionSlot(exercise) }
        appendSection(
            "Tirages",
            reachedDraws(detail, answers).map {
                "${labels.slot(exercise, it.slotKey)} : ${it.textSnapshot}"
            },
        )
        appendSection(
            "Réponses",
            orderedAnswers(exercise, answers).map { "${answerLabel(exercise, it, total)} : ${it.text}" },
        )
        appendSection(
            labels.rubricTitle,
            detail.rubric
                .filterNot { FirstPick.isKey(it.questionKey) }
                .map { "${labels.question(exercise, it.questionKey)} : ${if (it.value) labels.yes else labels.no}" },
        )
        val chosen = FirstPick.chosen(detail.rubric.associate { it.questionKey to it.value })
        detail.answers.firstOrNull { it.fieldKey == chosen && it.text.isNotBlank() }?.let {
            appendLine("${labels.endChoice(exercise)} : ${it.text}")
        }
    }

    /** Questions révélées une à une : seules celles atteintes (jusqu'à la plus haute répondue) sont listées. */
    private fun reachedDraws(
        detail: SessionDetail,
        answers: List<AnswerRecord>,
    ): List<DrawRecord> {
        val slot = labels.questionSlot(detail.session.exerciseId) ?: return detail.draws
        val reached = reachedQuestions(answers.map { it.groupIndex }, detail.draws.count { it.slotKey == slot })
        return detail.draws.filter { it.slotKey != slot || it.position < reached }
    }

    /** Questions numérotées : par question d'abord, puis dans l'ordre des champs de la définition. */
    private fun orderedAnswers(
        exercise: String,
        answers: List<AnswerRecord>,
    ): List<AnswerRecord> =
        if (labels.itemLabel(exercise, 1, 1) == null) {
            answers
        } else {
            answers.sortedWith(compareBy({ it.groupIndex }, { labels.fieldRank(exercise, it.fieldKey) }))
        }

    /** Le premier groupe (PNJ n°1) garde le libellé seul ; les suivants sont numérotés (« Offre 2 sur 5 » si défini). */
    private fun answerLabel(
        exercise: String,
        answer: AnswerRecord,
        total: Int,
    ): String {
        val label = labels.field(exercise, answer.fieldKey)
        labels.itemLabel(exercise, answer.groupIndex + 1, total)?.let { return "$it · $label" }
        return if (answer.groupIndex > 0) "$label (${answer.groupIndex + 1})" else label
    }

    private fun StringBuilder.appendSection(
        title: String,
        lines: List<String>,
    ) {
        if (lines.isEmpty()) return
        appendLine("$title :")
        lines.forEach { appendLine("- ${it.replace("\n", "\n  ")}") }
    }

    private fun localStart(
        epochMillis: Long,
        zoneId: String,
    ): String {
        val zone = zoneOrUtc(zoneId)
        return "${DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(zone))} ($zoneId)"
    }

    private fun zoneOrUtc(zoneId: String): ZoneId =
        try {
            ZoneId.of(zoneId)
        } catch (_: DateTimeException) {
            ZoneOffset.UTC
        }

    private fun duration(seconds: Int): String =
        String.format(Locale.ROOT, "%d min %02d s", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

    private companion object {
        val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
