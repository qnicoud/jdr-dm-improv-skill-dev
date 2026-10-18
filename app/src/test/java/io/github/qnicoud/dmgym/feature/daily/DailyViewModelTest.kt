package io.github.qnicoud.dmgym.feature.daily

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.daily.replacementsFor
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import io.github.qnicoud.dmgym.feature.home.SettingsExerciseCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {
    private val settings = FakeSettingsRepository()
    private val clock = MutableClock(Instant.parse("2026-10-16T10:00:00Z"))
    private val sessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    private val today get() = clock.localEpochDay()

    private fun viewModel() =
        DailyViewModel(FakeHistoryRepository(history = sessions), settings, SettingsExerciseCatalog(settings), clock)

    private fun session(
        id: ExerciseId,
        status: SessionStatus,
        day: Long = today,
    ) = SessionRecord(
        exerciseId = id.value,
        status = status,
        startedAtEpochMillis = 0,
        zoneId = "UTC",
        localEpochDay = day,
    )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts loading and then shows three distinct exercises to do`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(today, state.epochDay)
                assertEquals(
                    3,
                    state.items
                        .map { it.id }
                        .toSet()
                        .size,
                )
                assertEquals(listOf(0, 1, 2), state.items.map { it.position })
                assertTrue(state.items.all { it.progress == ExerciseProgress.TODO })
                assertFalse(state.started)
                assertFalse(state.allDone)
                assertEquals(state.items.first().id, state.nextExercise)
            }
        }

    @Test
    fun `the initial state is loading`() {
        assertTrue(DailyUiState().isLoading)
    }

    @Test
    fun `durations come from the settings and the total is rounded to minutes`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                val expected = state.items.sumOf { it.durationSeconds ?: 0 }
                assertEquals((expected + 30) / 60, state.totalMinutes)
            }
        }

    @Test
    fun `one page is not suggested unless the option is on`() =
        runTest {
            viewModel().uiState.test {
                assertTrue(ExerciseId.ONE_PAGE !in expectMostRecentItem().items.map { it.id })
            }
        }

    @Test
    fun `the progress follows the history of the day`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val ids = expectMostRecentItem().items.map { it.id }
                sessions.value =
                    listOf(
                        session(ids[0], SessionStatus.COMPLETED),
                        session(ids[1], SessionStatus.IN_PROGRESS),
                        session(ids[2], SessionStatus.COMPLETED, day = today - 1),
                    )
                val state = expectMostRecentItem()
                assertEquals(
                    listOf(
                        ExerciseProgress.DONE,
                        ExerciseProgress.IN_PROGRESS,
                        ExerciseProgress.TODO,
                    ),
                    state.items.map {
                        it.progress
                    },
                )
                assertEquals(ids[1], state.nextExercise)
                assertTrue(state.started)
            }
        }

    @Test
    fun `the plan does not change when sessions are added`() =
        runTest {
            viewModel().uiState.test {
                val before = expectMostRecentItem().items.map { it.id }
                sessions.value = before.map { session(it, SessionStatus.COMPLETED) }
                val after = expectMostRecentItem()
                assertEquals(before, after.items.map { it.id })
                assertTrue(after.allDone)
                assertNull(after.nextExercise)
            }
        }

    @Test
    fun `replacing a to do exercise updates the plan and is stored for the day`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val before = expectMostRecentItem()
                val replacement = before.alternatives.first().id
                vm.replace(1, replacement)
                val after = expectMostRecentItem()
                assertEquals(replacement, after.items[1].id)
                assertEquals(before.items[0].id, after.items[0].id)
                assertTrue(before.items[1].id in after.alternatives.map { it.id })
                assertTrue(replacement !in after.alternatives.map { it.id })
            }
            assertEquals(
                mapOf(
                    1 to
                        vm.uiState.value.items[1]
                            .id,
                ),
                settings.settings.first().replacementsFor(today),
            )
        }

    @Test
    fun `a replacement is kept when the screen is opened again`() =
        runTest {
            val first = viewModel()
            first.uiState.test {
                val replacement = expectMostRecentItem().alternatives.first().id
                first.replace(2, replacement)
                expectMostRecentItem()
            }
            viewModel().uiState.test {
                assertEquals(
                    first.uiState.value.items[2]
                        .id,
                    expectMostRecentItem().items[2].id,
                )
            }
        }

    @Test
    fun `only exercises still to do can be replaced`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val ids = expectMostRecentItem().items.map { it.id }
                sessions.value =
                    listOf(session(ids[0], SessionStatus.COMPLETED), session(ids[1], SessionStatus.IN_PROGRESS))
                val state = expectMostRecentItem()
                assertFalse(state.items[0].canReplace)
                assertFalse(state.items[1].canReplace)
                assertTrue(state.items[2].canReplace)
                vm.replace(0, state.alternatives.first().id)
                vm.replace(1, state.alternatives.first().id)
                expectNoEvents()
            }
            assertTrue(
                settings.settings
                    .first()
                    .replacementsFor(today)
                    .isEmpty(),
            )
        }

    @Test
    fun `an exercise already in the plan or an unknown position is refused`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val state = expectMostRecentItem()
                vm.replace(0, state.items[1].id)
                vm.replace(7, state.alternatives.first().id)
                vm.replace(-1, state.alternatives.first().id)
                expectNoEvents()
            }
        }

    @Test
    fun `a replacement before the first plan is ignored`() {
        val vm = viewModel()
        vm.replace(0, ExerciseId.CRAZY_EIGHTS)
        assertTrue(
            settings.state.value
                .replacementsFor(today)
                .isEmpty(),
        )
    }

    @Test
    fun `one page appears in the proposals when the option is on`() =
        runTest {
            settings.update { it.copy(dailyIncludeOnePage = true) }
            val seen = mutableSetOf<ExerciseId>()
            repeat(120) {
                clock.now = clock.now.plusSeconds(86_400)
                val vm = viewModel()
                vm.uiState.test { seen += expectMostRecentItem().items.map { it.id } }
            }
            assertTrue(ExerciseId.ONE_PAGE in seen)
        }

    @Test
    fun `alternatives list the exercises outside the plan including one page`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(ExerciseId.ALL.size - state.items.size, state.alternatives.size)
                assertTrue(ExerciseId.ONE_PAGE in state.alternatives.map { it.id })
                assertTrue(
                    state.alternatives
                        .map { it.id }
                        .intersect(state.items.map { it.id }.toSet())
                        .isEmpty(),
                )
            }
        }

    @Test
    fun `refreshing after midnight moves to a new day and drops the old replacements`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val day1 = expectMostRecentItem()
                vm.replace(0, day1.alternatives.first().id)
                expectMostRecentItem()
                clock.now = Instant.parse("2026-10-17T00:00:30Z")
                vm.refreshToday()
                val day2 = expectMostRecentItem()
                assertEquals(day1.epochDay + 1, day2.epochDay)
                assertTrue(
                    settings.state.value
                        .replacementsFor(day2.epochDay)
                        .isEmpty(),
                )
                assertTrue(day2.items.all { it.progress == ExerciseProgress.TODO })
            }
        }

    @Test
    fun `refreshing on the same day changes nothing`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val before = expectMostRecentItem()
                vm.refreshToday()
                expectNoEvents()
                assertEquals(before, vm.uiState.value)
            }
        }
}
