package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/** Écrit la sauvegarde réimportable (ADR 0015 §1) : JSON compact, neutre en langue, toujours dans la dernière version. */
object BackupEncoder {
    private val json = Json { encodeDefaults = true }

    /** Les sessions exportées sont celles du texte lisible : terminées, ou avec au moins une réponse non vide. */
    fun exportable(details: List<SessionDetail>): List<SessionDetail> =
        details
            .filter { it.session.status == SessionStatus.COMPLETED || it.answers.any { a -> a.text.isNotBlank() } }
            .sortedBy { it.session.startedAtEpochMillis }

    fun encode(
        details: List<SessionDetail>,
        exportedAt: Instant,
        appVersion: String,
    ): String =
        json.encodeToString(
            BackupV1(
                format = BACKUP_FORMAT,
                formatVersion = BACKUP_VERSION,
                exportedAt = exportedAt.toString(),
                appVersion = appVersion,
                sessions = exportable(details).map { it.toV1() },
            ),
        )

    /** Nom ASCII daté proposé au sélecteur de fichiers. */
    fun fileName(day: LocalDate): String = "dm-gym-sauvegarde-$day.json"
}
