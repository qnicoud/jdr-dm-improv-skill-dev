package io.github.qnicoud.dmgym.feature.settings

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
 * il est en français et ne passe pas par `strings.xml`. [exerciseNames] : nom affiché par identifiant d'exercice.
 * Les sessions en cours sans réponse écrite sont omises (rien à exporter).
 */
class SessionExportFormatter(
    private val exerciseNames: Map<String, String>,
) {
    fun format(
        details: List<SessionDetail>,
        exportedAt: Instant,
    ): String {
        val kept = details.filter { it.hasContent() }.sortedBy { it.session.startedAtEpochMillis }
        return buildString {
            appendLine("DM Gym - export de mes sessions")
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
        appendLine("=== ${exerciseNames[session.exerciseId] ?: session.exerciseId} ===")
        appendLine("Début : ${localStart(session.startedAtEpochMillis, session.zoneId)}")
        val status = if (session.status == SessionStatus.COMPLETED) "terminée" else "inachevée"
        appendLine("Statut : $status")
        appendLine("Durée active : ${duration(session.activeDurationSec)} (dépassement ${session.overtimeSec} s)")
        session.selfRating?.let { appendLine("Auto-évaluation : $it") }
        if (session.isGem) appendLine("Bijou : oui")
        appendSection("Tirages", detail.draws.map { "${it.slotKey} : ${it.textSnapshot}" })
        appendSection("Réponses", detail.answers.filter { it.text.isNotBlank() }.map { "${it.fieldKey} : ${it.text}" })
        appendSection("Grille", detail.rubric.map { "${it.questionKey} : ${if (it.value) "oui" else "non"}" })
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
