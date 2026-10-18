package io.github.qnicoud.dmgym.feature.stats

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.streak.DayMark
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    // 2026-10-14 est un mercredi.
    private val clock = MutableClock(Instant.parse("2026-10-14T10:00:00Z"))
    private val today get() = clock.localEpochDay()
    private val count = MutableStateFlow(0)
    private val days = MutableStateFlow<List<Long>>(emptyList())
    private val history = MutableStateFlow<List<SessionRecord>>(emptyList())

    private fun viewModel() =
        StatsViewModel(FakeHistoryRepository(sessionCount = count, history = history, practicedDays = days), clock)

    private fun session(
        id: ExerciseId,
        dayOffset: Int = 0,
        seconds: Int = 300,
        gem: Boolean = false,
    ) = SessionRecord(
        exerciseId = id.value,
        status = SessionStatus.COMPLETED,
        startedAtEpochMillis = 0,
        zoneId = "UTC",
        localEpochDay = today + dayOffset,
        activeDurationSec = seconds,
        isGem = gem,
    )

    private fun fill(vararg sessions: SessionRecord) {
        history.value = sessions.toList()
        count.value = sessions.size
        days.value = sessions.map { it.localEpochDay }.distinct()
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the initial state is loading and shows neither zero nor empty message`() {
        val state = StatsUiState()
        assertTrue(state.isLoading)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `an empty history is the empty state`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertTrue(state.isEmpty)
                assertEquals(0, state.sessions)
            }
        }

    @Test
    fun `sessions come first and this week counts from monday`() =
        runTest {
            fill(
                session(ExerciseId.CRAZY_EIGHTS, dayOffset = 0),
                session(ExerciseId.CRAZY_EIGHTS, dayOffset = -2), // lundi
                session(ExerciseId.CORE_IMAGE, dayOffset = -3), // dimanche précédent
            )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isEmpty)
                assertEquals(3, state.sessions)
                assertEquals(2, state.sessionsThisWeek)
            }
        }

    @Test
    fun `the streak summary and the recent row follow the practiced days`() =
        runTest {
            fill(
                session(ExerciseId.CRAZY_EIGHTS, -3),
                session(ExerciseId.CRAZY_EIGHTS, -2),
                session(ExerciseId.CRAZY_EIGHTS, 0),
            )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(3, state.streak.current)
                assertEquals(3, state.streak.best)
                assertTrue(state.streak.graceUsedThisWeek(today))
                assertEquals(7, state.recent.size)
                assertEquals(today, state.recent.last().epochDay)
                assertEquals(DayMark.GRACE, state.recent[5].mark)
                assertFalse(state.isStreakRestart)
            }
        }

    @Test
    fun `a broken streak is a new streak with the best kept`() =
        runTest {
            fill(
                session(ExerciseId.CRAZY_EIGHTS, -9),
                session(ExerciseId.CRAZY_EIGHTS, -8),
                session(ExerciseId.CRAZY_EIGHTS, -7),
                session(ExerciseId.CRAZY_EIGHTS, 0),
            )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(1, state.streak.current)
                assertEquals(3, state.streak.best)
                assertTrue(state.isStreakRestart)
            }
        }

    @Test
    fun `a session of an unknown exercise is counted but not listed per exercise`() =
        runTest {
            fill(session(ExerciseId("from_the_future")), session(ExerciseId.CRAZY_EIGHTS))
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(2, state.sessions)
                assertEquals(listOf(ExerciseId.CRAZY_EIGHTS), state.perExercise.map { it.id })
            }
        }

    @Test
    fun `exercises are ranked by count without listing the unpracticed ones`() =
        runTest {
            fill(
                session(ExerciseId.CORE_IMAGE),
                session(ExerciseId.CRAZY_EIGHTS),
                session(ExerciseId.CRAZY_EIGHTS, -1),
                session(ExerciseId.ONE_PAGE),
                session(ExerciseId.CONNECT_STORY),
            )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(
                    listOf(
                        ExerciseId.CRAZY_EIGHTS to 2,
                        ExerciseId.CORE_IMAGE to 1,
                        ExerciseId.CONNECT_STORY to 1,
                        ExerciseId.ONE_PAGE to 1,
                    ),
                    state.perExercise.map { it.id to it.count },
                )
            }
        }

    @Test
    fun `gems and cumulated time are summed`() =
        runTest {
            fill(
                session(ExerciseId.CRAZY_EIGHTS, seconds = 3_600, gem = true),
                session(ExerciseId.CORE_IMAGE, seconds = 600, gem = true),
                session(ExerciseId.CORE_IMAGE, seconds = 90),
            )
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(2, state.gems)
                assertEquals(4_290L, state.totalSeconds)
            }
        }

    @Test
    fun `refreshing after midnight recomputes today`() =
        runTest {
            fill(session(ExerciseId.CRAZY_EIGHTS, 0))
            val vm = viewModel()
            vm.uiState.test {
                assertTrue(expectMostRecentItem().streak.todayDone)
                clock.now = Instant.parse("2026-10-15T00:05:00Z")
                vm.refreshToday()
                val next = expectMostRecentItem()
                assertFalse(next.streak.todayDone)
                assertEquals(1, next.streak.current)
                assertEquals(1, next.sessionsThisWeek)
            }
        }

    @Test
    fun `refreshing on the same day changes nothing`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.refreshToday()
                expectNoEvents()
            }
        }

    @Test
    fun `a time zone change moves today`() =
        runTest {
            fill(session(ExerciseId.CRAZY_EIGHTS, 0))
            val vm = viewModel()
            vm.uiState.test {
                assertEquals(today, expectMostRecentItem().today)
                clock.now = Instant.parse("2026-10-14T16:00:00Z")
                clock.setZone(ZoneId.of("Asia/Tokyo"))
                vm.refreshToday()
                assertEquals(today, expectMostRecentItem().today)
            }
        }
}
