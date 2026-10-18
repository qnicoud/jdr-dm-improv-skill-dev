package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupDecoderTest {
    private fun encode(vararg sessions: SessionDetail) = BackupEncoder.encode(sessions.toList(), BACKUP_NOW, "1.1.0")

    private fun decode(text: String) = BackupDecoder.decode(text, KNOWN_EXERCISES, BACKUP_NOW)

    private fun success(text: String) = decode(text) as DecodeResult.Success

    private fun errorOf(text: String) = (decode(text) as DecodeResult.Failure).error

    /** Un fichier valide dont on remplace [from] par [to]. */
    private fun mutated(
        from: String,
        to: String,
        session: SessionDetail = fullSession(),
    ): String {
        val text = encode(session)
        check(text.contains(from)) { "fixture does not contain $from" }
        return text.replace(from, to)
    }

    @Test
    fun `decode gives back exactly what was encoded`() {
        val input = listOf(fullSession(), fullSession(START_2026_10_17 + 60_000, "constraint_box"))

        val result = success(BackupEncoder.encode(input, BACKUP_NOW, "1.1.0"))

        assertEquals(input, result.sessions)
        assertEquals(0, result.unknownExercise)
    }

    @Test
    fun `a missing contentLocale is accepted as french`() {
        success(mutated(""","contentLocale":"fr"""", ""))
    }

    @Test
    fun `an unknown exercise is counted and ignored, the rest is kept`() {
        val text = encode(fullSession(), fullSession(START_2026_10_17 + 1, "juggling"))

        val result = success(text)

        assertEquals(1, result.sessions.size)
        assertEquals(1, result.unknownExercise)
    }

    @Test
    fun `a newer format version is refused with its own error`() {
        assertEquals(
            ImportError.NewerVersion,
            errorOf(mutated(""""formatVersion":1""", """"formatVersion":2,"future":1""")),
        )
    }

    @Test
    fun `a readable export is recognised`() {
        assertEquals(ImportError.ReadableExport, errorOf("DM Gym - export de mes sessions\nExporté le ..."))
    }

    @Test
    fun `something that is not json is not a backup`() {
        assertEquals(ImportError.NotABackup, errorOf("hello"))
        assertEquals(ImportError.NotABackup, errorOf(""))
        assertEquals(ImportError.NotABackup, errorOf("""{"format":"other","formatVersion":1}"""))
        assertEquals(ImportError.NotABackup, errorOf("[1,2]"))
    }

    @Test
    fun `a truncated backup is reported as incomplete`() {
        val text = encode(fullSession())

        assertEquals(ImportError.Incomplete, errorOf(text.take(text.length / 2)))
    }

    @Test
    fun `an unknown key in a known version is refused`() {
        assertEquals(
            ImportError.Incomplete,
            errorOf(mutated(""""isGem":true,"fromDaily""", """"isGem":true,"extra":1,"fromDaily""")),
        )
    }

    @Test
    fun `a wrong type is refused`() {
        assertEquals(ImportError.Incomplete, errorOf(mutated(""""difficulty":2""", """"difficulty":true""")))
    }

    @Test
    fun `every validation rule refuses the whole file`() {
        val cases =
            mapOf(
                "bad exercise key" to mutated(""""exerciseId":"crazy_eights"""", """"exerciseId":"Crazy Eights""""),
                "bad status" to mutated(""""status":"COMPLETED"""", """"status":"DONE""""),
                "rating 4" to mutated(""""selfRating":3""", """"selfRating":4"""),
                "difficulty 0" to mutated(""""difficulty":2""", """"difficulty":0"""),
                "negative duration" to mutated(""""activeDurationSec":470""", """"activeDurationSec":-1"""),
                "duration over 24h" to mutated(""""overtimeSec":12""", """"overtimeSec":90000"""),
                "before 2026" to mutated("$START_2026_10_17", "1700000000000"),
                "in the far future" to mutated("$START_2026_10_17", "2000000000000"),
                "ended before start" to mutated("${START_2026_10_17 + 492_000}", "${START_2026_10_17 - 1}"),
                "activity before start" to mutated("${START_2026_10_17 + 480_000}", "${START_2026_10_17 - 1}"),
                "bad zone" to mutated(""""zoneId":"Europe/Paris"""", """"zoneId":"Mars/Olympus""""),
                "wrong day" to mutated(""""localEpochDay":20743""", """"localEpochDay":20750"""),
                "bad locale" to mutated(""""contentLocale":"fr"""", """"contentLocale":"xx""""),
                "bad table" to mutated(""""tableId":"sensory_words"""", """"tableId":"nope""""),
                "bad entry id" to mutated(""""entryId":"poussiereux"""", """"entryId":"a b""""),
                "bad slot key" to mutated(""""slotKey":"item"""", """"slotKey":"I T""""),
                "bad option key" to mutated(""""situation_mode"""", """"Situation Mode""""),
                "control character" to mutated("Texte avec", "Texte\\u0007avec"),
                "duplicate draw" to mutated(""""position":1""", """"position":0"""),
                "duplicate answer" to mutated(""""groupIndex":1""", """"groupIndex":0"""),
                "duplicate rubric" to
                    mutated(""""questionKey":"first_pick:idea"""", """"questionKey":"beyond_first_ideas""""),
            )

        cases.forEach { (name, text) ->
            val result = decode(text)
            assertTrue(result is DecodeResult.Failure, "$name should be refused but gave $result")
        }
    }

    @Test
    fun `size limits are enforced`() {
        val longText = "x".repeat(20_001)
        val tooLong = fullSession().copy(answers = listOf(fullSession().answers.first().copy(text = longText)))
        val manyDraws = fullSession().copy(draws = List(65) { fullSession().draws.first().copy(position = it) })
        val longSnapshot =
            fullSession().copy(
                draws = listOf(fullSession().draws.first().copy(textSnapshot = "y".repeat(1_001))),
            )

        listOf(tooLong, manyDraws, longSnapshot).forEach {
            assertTrue(decode(encode(it)) is DecodeResult.Failure)
        }
    }

    @Test
    fun `newline and tab are allowed in texts`() {
        val session = fullSession().let { it.copy(answers = listOf(it.answers.first().copy(text = "a\nb\tc"))) }

        assertEquals(listOf(session), success(encode(session)).sessions)
    }

    @Test
    fun `an unknown exercise is ignored before its fields are checked`() {
        val text = mutated(""""exerciseId":"crazy_eights"""", """"exerciseId":"juggling"""")

        assertEquals(1, success(text).unknownExercise)
    }

    @Test
    fun `5000 sessions are decoded and validated quickly`() {
        val sessions = List(5_000) { fullSession(START_2026_10_17 + it * 1_000L) }
        val text = BackupEncoder.encode(sessions, BACKUP_NOW, "1.1.0")

        val started = System.nanoTime()
        val result = success(text)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(5_000, result.sessions.size)
        assertTrue(elapsedMs < 5_000, "took $elapsedMs ms")
    }
}
