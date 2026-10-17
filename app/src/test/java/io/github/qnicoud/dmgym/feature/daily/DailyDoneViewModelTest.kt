package io.github.qnicoud.dmgym.feature.daily

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.daily.withReplacement
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DailyDoneViewModelTest {
    private val settings = FakeSettingsRepository()
    private val clock = MutableClock(Instant.parse("2026-10-16T10:00:00Z"))
    private val today get() = clock.localEpochDay()
    private val sessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    private val count = MutableStateFlow(0)
    private val days = MutableStateFlow<List<Long>>(emptyList())

    private fun viewModel() =
        DailyDoneViewModel(
            FakeHistoryRepository(sessionCount = count, history = sessions, practicedDays = days),
            settings,
            clock,
        )

    private fun session(
        id: ExerciseId,
        rating: Int? = null,
        gem: Boolean = false,
    ) = SessionRecord(
        exerciseId = id.value,
        status = SessionStatus.COMPLETED,
        startedAtEpochMillis = 0,
        zoneId = "UTC",
        localEpochDay = today,
        selfRating = rating,
        isGem = gem,
        activeDurationSec = 60,
    )

    private suspend fun planIds(): List<ExerciseId> {
        var ids = emptyList<ExerciseId>()
        viewModel().uiState.test { ids = expectMostRecentItem().lines.map { it.id } }
        return ids
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the initial state is loading`() {
        assertTrue(DailyDoneUiState().isLoading)
    }

    @Test
    fun `lists the three exercises of the session with their level and gem`() =
        runTest {
            val plan = planIds()
            count.value = 48
            days.value = listOf(today - 1, today)
            sessions.value =
                listOf(
                    session(plan[2], rating = 3),
                    session(plan[1], gem = true),
                    session(plan[0], rating = 2),
                )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(plan, state.lines.map { it.id })
                assertEquals(listOf(2, null, 3), state.lines.map { it.rating })
                assertEquals(listOf(false, true, false), state.lines.map { it.isGem })
                assertTrue(state.allDone)
                assertEquals(3, state.doneCount)
            }
        }

    @Test
    fun `the counters come from the history`() =
        runTest {
            count.value = 48
            days.value = listOf(today - 5, today - 1, today)
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(48, state.sessionCount)
                assertEquals(2, state.streakDays)
            }
        }

    @Test
    fun `a partial session is not complete and counts only what was done`() =
        runTest {
            val ids = planIds()
            sessions.value = listOf(session(ids[1], rating = 1))
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.allDone)
                assertEquals(1, state.doneCount)
                assertEquals(listOf(false, true, false), state.lines.map { it.done })
            }
        }

    @Test
    fun `follows a replacement chosen earlier in the day`() =
        runTest {
            val before = planIds()
            val replacement = ExerciseId.ALL.first { it !in before && it != ExerciseId.ONE_PAGE }
            settings.update { it.withReplacement(today, 0, replacement) }
            viewModel().uiState.test { assertEquals(replacement, expectMostRecentItem().lines.first().id) }
        }
}
