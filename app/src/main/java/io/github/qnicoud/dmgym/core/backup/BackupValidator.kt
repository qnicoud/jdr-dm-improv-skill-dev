package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.tables.TableId
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId

/**
 * Règles de validation d'une session lue dans un fichier non fiable (ADR 0015 §3). Pure : une seule violation fait
 * refuser le fichier entier. Les langues de contenu connues sont celles de l'ADR 0016 (français seul pour l'instant).
 */
internal class BackupValidator(
    now: Instant,
    private val contentLocales: Set<String> = setOf(DEFAULT_CONTENT_LOCALE),
) {
    private val earliest = Instant.parse(EARLIEST_START).toEpochMilli()
    private val latest = now.plusSeconds(SECONDS_PER_DAY).toEpochMilli()
    private val tables = TableId.entries.map { it.fileName }.toSet()

    /** L'identifiant d'exercice est vérifié même pour les exercices inconnus de cette version. */
    fun isValidExerciseId(id: String): Boolean = KEY.matches(id)

    fun isValid(session: SessionV1): Boolean =
        isValidExerciseId(session.exerciseId) &&
            hasValidSizes(session) &&
            hasValidScalars(session) &&
            hasValidTimes(session) &&
            hasValidChildren(session)

    private fun hasValidSizes(s: SessionV1) =
        s.draws.size <= MAX_DRAWS && s.answers.size <= MAX_ANSWERS && s.rubric.size <= MAX_RUBRIC

    private fun hasValidScalars(s: SessionV1): Boolean =
        SessionStatus.entries.any { it.name == s.status } &&
            (s.selfRating == null || s.selfRating in RATING_RANGE) &&
            s.difficulty in DIFFICULTY_RANGE &&
            listOfNotNull(s.plannedDurationSec, s.activeDurationSec, s.overtimeSec).all { it in 0..MAX_SECONDS } &&
            s.options.size <= MAX_OPTIONS &&
            s.options.keys.all { KEY.matches(it) } &&
            (s.contentLocale == null || s.contentLocale in contentLocales)

    private fun hasValidTimes(s: SessionV1): Boolean {
        val zone = zoneOrNull(s.zoneId) ?: return false
        val day =
            Instant
                .ofEpochMilli(s.startedAtEpochMillis)
                .atZone(zone)
                .toLocalDate()
                .toEpochDay()
        return s.startedAtEpochMillis in earliest..latest &&
            s.lastActivityAtEpochMillis >= s.startedAtEpochMillis &&
            (s.endedAtEpochMillis == null || s.endedAtEpochMillis >= s.startedAtEpochMillis) &&
            s.localEpochDay in (day - 1)..(day + 1)
    }

    private fun hasValidChildren(s: SessionV1): Boolean =
        s.draws.all(::isValid) &&
            s.answers.all(::isValid) &&
            s.rubric.all { KEY.matches(it.questionKey) } &&
            s.draws.distinctBy { it.slotKey to it.position }.size == s.draws.size &&
            s.answers.distinctBy { it.fieldKey to it.groupIndex }.size == s.answers.size &&
            s.rubric.distinctBy { it.questionKey }.size == s.rubric.size

    private fun isValid(d: DrawV1) =
        KEY.matches(d.slotKey) &&
            d.position >= 0 &&
            (d.tableId == null || d.tableId in tables) &&
            ENTRY.matches(d.entryId) &&
            isPlainText(d.textSnapshot, MAX_SNAPSHOT) &&
            (d.category == null || KEY.matches(d.category))

    private fun isValid(a: AnswerV1) =
        KEY.matches(a.fieldKey) && a.groupIndex >= 0 && isPlainText(a.text, MAX_ANSWER_TEXT)

    private fun isPlainText(
        text: String,
        maxLength: Int,
    ) = text.length <= maxLength && text.none { (it.isISOControl() && it != '\n' && it != '\t') || it in BIDI_CONTROLS }

    private fun zoneOrNull(id: String): ZoneId? =
        try {
            ZoneId.of(id)
        } catch (_: DateTimeException) {
            null
        }

    companion object {
        const val MAX_SESSIONS = 20_000
        private const val MAX_DRAWS = 64
        private const val MAX_ANSWERS = 256
        private const val MAX_RUBRIC = 16
        private const val MAX_OPTIONS = 32
        private const val MAX_ANSWER_TEXT = 20_000
        private const val MAX_SNAPSHOT = 1_000
        private const val MAX_SECONDS = 24 * 3_600
        private const val SECONDS_PER_DAY = 86_400L
        private const val EARLIEST_START = "2026-01-01T00:00:00Z"
        private val RATING_RANGE = 1..3
        private val DIFFICULTY_RANGE = 1..3
        private val BIDI_CONTROLS = ('\u202A'..'\u202E') + ('\u2066'..'\u2069')
        private val KEY = Regex("[a-z0-9_:.-]{1,64}")
        private val ENTRY = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
