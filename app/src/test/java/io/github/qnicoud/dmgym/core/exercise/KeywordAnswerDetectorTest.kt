package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeywordAnswerDetectorTest {
    private val detector = KeywordAnswerDetector(RealTables.repository)

    private fun word(
        position: Int,
        id: String,
    ) = Draw("words", position, "sensory_words", id, id)

    private val dusty = word(0, "poussiereux")
    private val keyDusty = DrawKey("words", 0)

    @Test
    fun `detects a keyword once the draws are prepared`() =
        runTest {
            detector.prepare(listOf(dusty))
            assertEquals(mapOf(keyDusty to true), detector.detect(listOf(dusty), "Une cave poussiéreuse."))
            assertEquals(mapOf(keyDusty to false), detector.detect(listOf(dusty), "Une cave propre."))
        }

    @Test
    fun `an unprepared draw is left out so it never overwrites a check`() =
        runTest {
            assertEquals(emptyMap<DrawKey, Boolean>(), detector.detect(listOf(dusty), "poussiéreux"))
        }

    @Test
    fun `highlights are the ranges of the matches in the original text`() =
        runTest {
            detector.prepare(listOf(dusty))
            val text = "Une cave poussiéreuse et sombre"
            assertEquals(listOf(9..20), detector.highlights(listOf(dusty), text))
            assertEquals("poussiéreuse", text.substring(9, 21))
        }

    @Test
    fun `highlights are empty for an unprepared draw or an empty text`() =
        runTest {
            assertTrue(detector.highlights(listOf(dusty), "poussiéreux").isEmpty())
            detector.prepare(listOf(dusty))
            assertTrue(detector.highlights(listOf(dusty), "").isEmpty())
        }

    @Test
    fun `a table that cannot be read leaves detection silent`() =
        runTest {
            val failing =
                KeywordAnswerDetector(
                    TableRepository(
                        object : TableSource {
                            override suspend fun load(id: TableId) =
                                throw TableLoadException(id, IllegalStateException())
                        },
                    ),
                )
            failing.prepare(listOf(dusty))
            assertEquals(emptyMap<DrawKey, Boolean>(), failing.detect(listOf(dusty), "poussiéreux"))
        }

    @Test
    fun `draws without a table are ignored`() =
        runTest {
            val card = Draw("n", 0, null, "7", "7")
            detector.prepare(listOf(card))
            assertEquals(emptyMap<DrawKey, Boolean>(), detector.detect(listOf(card), "7"))
        }

    @Test
    fun `a table that cannot be read is reported to the failure callback`() =
        runTest {
            val failures = mutableListOf<TableLoadException>()
            val failing =
                KeywordAnswerDetector(
                    TableRepository(
                        object : TableSource {
                            override suspend fun load(id: TableId) =
                                throw TableLoadException(id, IllegalStateException())
                        },
                    ),
                    onFailure = { failures += it },
                )
            failing.prepare(listOf(dusty))
            assertEquals(listOf(TableId.SensoryWords), failures.map { it.id })
        }
}
