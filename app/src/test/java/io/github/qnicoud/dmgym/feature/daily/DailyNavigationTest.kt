package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Daily
import io.github.qnicoud.dmgym.app.navigation.Exercise
import io.github.qnicoud.dmgym.app.navigation.History
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.app.navigation.Stats
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import io.github.qnicoud.dmgym.feature.history.HistoryFilter
import io.github.qnicoud.dmgym.feature.stats.StatsTags
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Câblage de bout en bout (sans stockage) : accueil, séance du jour, progrès. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class DailyNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private val sessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    private val count = MutableStateFlow(0)
    private lateinit var stack: NavBackStack<NavKey>
    private val container =
        FakeAppContainer(
            settingsRepository = FakeSettingsRepository(),
            historyRepository =
                FakeHistoryRepository(
                    sessionCount = count,
                    history = sessions,
                    practicedDays = MutableStateFlow(emptyList()),
                ),
        )

    private fun launch(vararg routes: NavKey) {
        stack = NavBackStack<NavKey>(*routes)
        compose.setContent { DmGymTheme { AppNavDisplay(AppNavigator(stack), container) } }
    }

    @Test
    fun `the home screen shows the daily card and opens the daily screen`() {
        launch(Home)
        compose.onNodeWithTag(DailyCardTags.CARD).assertIsDisplayed()
        compose.onNodeWithTag(DailyCardTags.BUTTON).assertTextEquals("Commencer").performClick()
        compose.waitForIdle()
        assertEquals(Daily, stack.last())
        compose.onNodeWithTag(DailyTags.TITLE).assertIsDisplayed()
    }

    @Test
    fun `starting from the daily screen opens the first exercise flagged from daily`() {
        launch(Home, Daily)
        compose.onNodeWithTag(DailyTags.START).performClick()
        compose.waitForIdle()
        val route = stack.last() as Exercise
        assertEquals(true, route.fromDaily)
    }

    @Test
    fun `the daily screen and the home card show the session as finished once everything is done`() {
        launch(Home, Daily)
        val today = container.clock.localEpochDay()
        sessions.value =
            ExerciseId.ALL.map {
                SessionRecord(
                    exerciseId = it.value,
                    status = SessionStatus.COMPLETED,
                    startedAtEpochMillis = 0,
                    zoneId = "UTC",
                    localEpochDay = today,
                )
            }
        compose.waitForIdle()
        compose.onNodeWithTag(DailyTags.FINISHED).assertTextEquals("Séance terminée")
        compose.onNodeWithTag(DailyTags.START).assertDoesNotExist()
    }

    @Test
    fun `the progress tab shows the sessions and its empty action goes back to the exercises`() {
        launch(Home, Stats)
        compose.onNodeWithTag(StatsTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(StatsTags.EMPTY_ACTION).performClick()
        compose.waitForIdle()
        assertEquals(Home, stack.last())
    }

    @Test
    fun `the progress tab shows the counters from the history and the gems link opens the history`() {
        count.value = 3
        sessions.value =
            listOf(
                SessionRecord(
                    exerciseId = ExerciseId.CRAZY_EIGHTS.value,
                    status = SessionStatus.COMPLETED,
                    startedAtEpochMillis = 0,
                    zoneId = "UTC",
                    localEpochDay = container.clock.localEpochDay(),
                    isGem = true,
                    activeDurationSec = 600,
                ),
            )
        launch(Home, Stats)
        compose.onNodeWithTag(StatsTags.SESSIONS).assertTextEquals("3 sessions")
        compose.onNodeWithTag(StatsTags.GEMS_LINK).performClick()
        compose.waitForIdle()
        assertEquals(History(HistoryFilter.Gems), stack.last())
    }
}
