package io.github.qnicoud.dmgym.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Exercise
import io.github.qnicoud.dmgym.app.navigation.History
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.app.navigation.SessionDetail
import io.github.qnicoud.dmgym.app.navigation.Stats
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import io.github.qnicoud.dmgym.feature.stats.StatsTags
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.github.qnicoud.dmgym.core.history.SessionDetail as StoredDetail

/** Câblage de bout en bout (sans stockage) : progrès, historique, détail, nouveau tirage et suppression. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class HistoryNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var stack: NavBackStack<NavKey>
    private val clock = FakeAppContainer().clock
    private val record =
        SessionRecord(
            id = 7,
            exerciseId = ExerciseId.CRAZY_EIGHTS.value,
            status = SessionStatus.COMPLETED,
            startedAtEpochMillis = clock.millis(),
            zoneId = "UTC",
            localEpochDay = clock.localEpochDay(),
            selfRating = 2,
        )
    private val repo =
        FakeHistoryRepository(
            sessionCount = MutableStateFlow(1),
            history = MutableStateFlow(listOf(record)),
            previews = MutableStateFlow(mapOf(7L to "Un PNJ vient demander de l’aide")),
            details =
                MutableStateFlow(
                    mapOf(
                        7L to
                            StoredDetail(
                                record,
                                emptyList(),
                                listOf(AnswerRecord("a", 0, "Un PNJ vient")),
                                emptyList(),
                            ),
                    ),
                ),
        )
    private val container = FakeAppContainer(historyRepository = repo)

    private fun launch(vararg routes: NavKey) {
        stack = NavBackStack<NavKey>(*routes)
        compose.setContent { DmGymTheme { AppNavDisplay(AppNavigator(stack), container) } }
    }

    @Test
    fun `an exercise bar of the progress screen opens the history on that exercise`() {
        launch(Home, Stats)
        compose.onNodeWithTag(StatsTags.bar(ExerciseId.CRAZY_EIGHTS)).performScrollTo().performClick()
        compose.waitForIdle()
        val filter = HistoryFilter.exercise(ExerciseId.CRAZY_EIGHTS)
        assertEquals(History(filter), stack.last())
        compose.onNodeWithTag(HistoryTags.chip(filter)).assertIsSelected()
    }

    @Test
    fun `the gems link of the progress screen opens the history on gems`() {
        val gemRepo =
            FakeHistoryRepository(
                sessionCount = MutableStateFlow(1),
                history = MutableStateFlow(listOf(record.copy(isGem = true))),
            )
        stack = NavBackStack<NavKey>(Home, Stats)
        compose.setContent {
            DmGymTheme { AppNavDisplay(AppNavigator(stack), FakeAppContainer(historyRepository = gemRepo)) }
        }
        compose.onNodeWithTag(StatsTags.GEMS_LINK).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(History(HistoryFilter.Gems), stack.last())
    }

    @Test
    fun `a session of the history opens its detail`() {
        launch(Home, History())
        compose.onNodeWithTag(HistoryTags.card(7)).performClick()
        compose.waitForIdle()
        assertEquals(SessionDetail(7), stack.last())
        compose.onNodeWithTag(DetailTags.HEADER).assertIsDisplayed()
    }

    @Test
    fun `redoing opens the same exercise with a new draw`() {
        launch(Home, History(), SessionDetail(7))
        compose.onNodeWithTag(DetailTags.REDO).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(Exercise(ExerciseId.CRAZY_EIGHTS), stack.last())
    }

    @Test
    fun `changing the level of a session writes it`() {
        launch(Home, History(), SessionDetail(7))
        compose.onNodeWithTag(DetailTags.level(3)).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf(7L to 3), repo.ratings)
    }

    @Test
    fun `deleting a session after confirmation goes back to the history`() {
        launch(Home, History(), SessionDetail(7))
        compose.onNodeWithTag(DetailTags.MORE).performClick()
        compose.onNodeWithTag(DetailTags.DELETE_ITEM).performClick()
        compose.onNodeWithTag(DetailTags.DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertEquals(listOf(7L), repo.deleted)
        assertEquals(History(), stack.last())
    }
}
