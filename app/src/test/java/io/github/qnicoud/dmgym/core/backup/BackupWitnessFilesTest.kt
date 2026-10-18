package io.github.qnicoud.dmgym.core.backup

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
    fun `the complete v1 witness stays importable and keeps its content`() {
        val result = decode("valid-complete.json") as DecodeResult.Success

        assertEquals(listOf("crazy_eights", "constraint_box"), result.sessions.map { it.session.exerciseId })
        val first = result.sessions.first()
        assertEquals(1_792_220_400_000L, first.session.startedAtEpochMillis)
        assertEquals("""{"situation_mode":true,"event:minute":true}""", first.session.optionsJson)
        assertEquals("sight", first.draws.single().category)
        assertEquals(true, first.rubric.any { it.questionKey == "first_pick:idea" && it.value })
        assertEquals(
            null,
            result.sessions
                .last()
                .session.endedAtEpochMillis,
        )
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
