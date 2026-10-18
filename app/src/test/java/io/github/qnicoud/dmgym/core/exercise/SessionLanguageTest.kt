package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SessionLanguageTest {
    private fun detail(locale: String) =
        SessionDetail(
            SessionRecord(
                id = 1,
                exerciseId = "demo",
                status = SessionStatus.IN_PROGRESS,
                startedAtEpochMillis = 0,
                zoneId = "UTC",
                localEpochDay = 0,
                contentLocale = locale,
            ),
            emptyList(),
            emptyList(),
            emptyList(),
        )

    @Test
    fun `a stored session restores its language`() {
        assertEquals(ContentLanguage.FR, detail("fr").toExerciseSession().language)
    }

    @Test
    fun `an unknown stored language falls back to french`() {
        assertEquals(ContentLanguage.FR, detail("xx").toExerciseSession().language)
    }

    @Test
    fun `a new session starts in the default language`() {
        assertEquals(ContentLanguage.DEFAULT, ExerciseSession(DemoExercise.id).language)
    }

    @Test
    fun `the drawer asks the tables of the language it is given`() =
        runTest {
            val asked = mutableListOf<ContentLanguage>()
            val source =
                object : TableSource {
                    private val inner = DemoExercise.FixtureSource()

                    override suspend fun load(
                        id: TableId,
                        language: ContentLanguage,
                    ) = inner.load(id, language).also { asked += language }
                }

            SlotDrawer(TableRepository(source), Random(1), language = { ContentLanguage.FR })
                .drawAll(DemoExercise.definition.slots)

            assertEquals(setOf(ContentLanguage.FR), asked.toSet())
        }

    @Test
    fun `the keyword detector hands out one detector per language and reuses it`() {
        val detector = KeywordAnswerDetector(TableRepository(DemoExercise.FixtureSource()))

        assertSame(detector, detector.forLanguage(ContentLanguage.FR))
        assertSame(detector.forLanguage(ContentLanguage.FR), detector.forLanguage(ContentLanguage.FR))
    }
}
