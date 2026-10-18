package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Fichiers témoins `backup/v1/` (ADR 0015 §2) : ils ne sont **jamais modifiés** une fois commités. Toute version
 * future de l'app doit continuer à les lire de la même façon, comme les schémas Room exportés.
 */
class BackupWitnessFilesTest {
    private fun witness(name: String): String =
        checkNotNull(javaClass.getResource("/backup/v1/$name")) { "missing witness $name" }.readText()

    private fun decode(name: String) = BackupDecoder.decode(witness(name), KNOWN_EXERCISES, BACKUP_NOW)

    @Test
    fun `the complete v1 witness stays importable and decodes to exactly this`() {
        val result = decode("valid-complete.json") as DecodeResult.Success

        val first =
            SessionDetail(
                session =
                    SessionRecord(
                        exerciseId = "crazy_eights",
                        status = SessionStatus.COMPLETED,
                        startedAtEpochMillis = 1_792_220_400_000L,
                        lastActivityAtEpochMillis = 1_792_220_880_000L,
                        endedAtEpochMillis = 1_792_220_892_000L,
                        zoneId = "Europe/Paris",
                        localEpochDay = 20_743,
                        plannedDurationSec = 480,
                        activeDurationSec = 470,
                        overtimeSec = 12,
                        difficulty = 2,
                        optionsJson = """{"situation_mode":true,"event:minute":true}""",
                        selfRating = 3,
                        isGem = true,
                        fromDaily = true,
                        drawSeed = 123_456_789,
                    ),
                draws =
                    listOf(
                        DrawRecord("item", 0, "sensory_words", "poussiereux", "poussiéreux", true, false, "sight"),
                    ),
                answers =
                    listOf(
                        AnswerRecord("idea", 0, "Une idée\nsur deux lignes", flagged = true, isGem = true),
                    ),
                rubric = listOf(RubricRecord("beyond_first_ideas", true), RubricRecord("first_pick:idea", true)),
            )
        val second =
            SessionDetail(
                session =
                    SessionRecord(
                        exerciseId = "constraint_box",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_792_224_000_000L,
                        lastActivityAtEpochMillis = 1_792_224_060_000L,
                        endedAtEpochMillis = null,
                        zoneId = "UTC",
                        localEpochDay = 20_743,
                        plannedDurationSec = null,
                        activeDurationSec = 60,
                        overtimeSec = 0,
                        difficulty = 1,
                        optionsJson = "{}",
                        selfRating = null,
                        isGem = false,
                        fromDaily = false,
                        drawSeed = 0,
                    ),
                draws = emptyList(),
                answers = listOf(AnswerRecord("scene", 0, "Début", flagged = false, isGem = false)),
                rubric = emptyList(),
            )
        assertEquals(listOf(first, second), result.sessions)
        assertEquals(0, result.unknownExercise)
        assertEquals(0, result.invalid)
    }

    @Test
    fun `the v1 witness without any language is imported as french`() {
        val stripped = Regex(",\\s*\"contentLocale\"\\s*:\\s*\"[a-z]+\"").replace(witness("valid-complete.json"), "")
        assertEquals(false, stripped.contains("contentLocale"))

        val result = BackupDecoder.decode(stripped, KNOWN_EXERCISES, BACKUP_NOW) as DecodeResult.Success

        assertEquals(2, result.sessions.size)
        result.sessions.forEach { assertEquals("fr", it.session.contentLocale) }
        assertEquals(decode("valid-complete.json"), result)
    }

    @Test
    fun `a witness from a newer version is refused`() {
        assertEquals(DecodeResult.Failure(ImportError.NewerVersion), decode("invalid-newer-version.json"))
    }

    @Test
    fun `the truncated witness is incomplete`() {
        assertEquals(DecodeResult.Failure(ImportError.Incomplete), decode("invalid-truncated.json"))
    }

    @Test
    fun `the readable export witness is recognised`() {
        assertEquals(DecodeResult.Failure(ImportError.ReadableExport), decode("invalid-readable-export.txt"))
    }
}
