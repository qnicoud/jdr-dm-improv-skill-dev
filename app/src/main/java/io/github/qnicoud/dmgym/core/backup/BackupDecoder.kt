package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Lit une sauvegarde (ADR 0015 §3) : reconnaissance de l'export texte, version, décodage strict puis validation.
 * Ne touche à rien et ne lève jamais : tout échec est un [DecodeResult.Failure] typé. Pure, donc testable en JVM ;
 * l'appelant la fait tourner hors du fil principal.
 */
object BackupDecoder {
    private const val STALE_MARGIN_MILLIS = 25 * 3_600_000L
    private const val PREFIX_WINDOW = 200
    private val BOM = Char(0xFEFF)

    private val envelopeJson = Json { ignoreUnknownKeys = true }
    private val strictJson = Json { ignoreUnknownKeys = false }
    private val formatMarker = Regex("^\\s*\\{\\s*\"format\"\\s*:\\s*\"$BACKUP_FORMAT\"")

    /** [knownExercises] : identifiants d'exercices de cette version ; [now] borne les dates dans le futur. */
    fun decode(
        text: String,
        knownExercises: Set<String>,
        now: Instant,
    ): DecodeResult {
        if (text.trimStart(BOM, ' ', '\n', '\r', '\t').startsWith(READABLE_EXPORT_HEADER)) {
            return DecodeResult.Failure(ImportError.ReadableExport)
        }
        val envelope =
            try {
                envelopeJson.decodeFromString<BackupEnvelope>(text)
            } catch (_: SerializationException) {
                return DecodeResult.Failure(failureOfUnreadableHeader(text))
            } catch (_: IllegalArgumentException) {
                return DecodeResult.Failure(failureOfUnreadableHeader(text))
            }
        return when {
            envelope.format != BACKUP_FORMAT || envelope.formatVersion < 1 -> {
                DecodeResult.Failure(
                    ImportError.NotABackup,
                )
            }

            envelope.formatVersion > BACKUP_VERSION -> {
                DecodeResult.Failure(ImportError.NewerVersion)
            }

            else -> {
                decodeV1(text, knownExercises, now)
            }
        }
    }

    /** Un début de fichier qui annonce notre format mais ne se lit pas : tronqué ou abîmé, pas un autre fichier. */
    private fun failureOfUnreadableHeader(text: String): ImportError =
        if (formatMarker.containsMatchIn(text.take(PREFIX_WINDOW))) ImportError.Incomplete else ImportError.NotABackup

    private fun decodeV1(
        text: String,
        knownExercises: Set<String>,
        now: Instant,
    ): DecodeResult {
        val backup =
            try {
                strictJson.decodeFromString<BackupV1>(text)
            } catch (_: SerializationException) {
                return DecodeResult.Failure(ImportError.Incomplete)
            } catch (_: IllegalArgumentException) {
                return DecodeResult.Failure(ImportError.Incomplete)
            }
        if (backup.sessions.size > BackupValidator.MAX_SESSIONS) return DecodeResult.Failure(ImportError.Incomplete)
        val validator = BackupValidator(now)
        val (known, unknown) = backup.sessions.partition { it.exerciseId in knownExercises }
        val (valid, invalid) = known.partition { validator.isValid(it) }
        val unknownInvalid = unknown.count { !validator.isValidExerciseId(it.exerciseId) }
        return DecodeResult.Success(
            sessions = valid.map { it.toDomain().safeAt(now) },
            unknownExercise = unknown.size - unknownInvalid,
            invalid = invalid.size + unknownInvalid,
        )
    }

    /**
     * Une session importée ne devient jamais la session active ni celle à reprendre (elle est « inachevée »), et
     * aucune de ses dates n'est dans le futur.
     */
    private fun SessionDetail.safeAt(now: Instant): SessionDetail {
        val nowMillis = now.toEpochMilli()
        val ceiling = maxOf(nowMillis, session.startedAtEpochMillis)
        val stale = nowMillis - STALE_MARGIN_MILLIS
        var lastActivity = minOf(session.lastActivityAtEpochMillis, ceiling)
        if (session.status == SessionStatus.IN_PROGRESS) lastActivity = minOf(lastActivity, stale)
        return copy(
            session =
                session.copy(
                    lastActivityAtEpochMillis = lastActivity,
                    endedAtEpochMillis = session.endedAtEpochMillis?.let { minOf(it, ceiling) },
                ),
        )
    }
}
