package io.github.qnicoud.dmgym.feature.history

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
class HistoryViewModelTest {
    private val clock = MutableClock(Instant.parse("2026-10-14T10:00:00Z"))
    private val today get() = clock.localEpochDay()
    private val all = MutableStateFlow<List<SessionRecord>>(emptyList())
    private val previews = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val count = MutableStateFlow(0)
    private val repo =
        FakeHistoryRepository(
            sessionCount = count,
            previews = previews,
            historyBy = { exercise, gems ->
                all.map { list ->
                    list.filter { (exercise == null || it.exerciseId == exercise) && (!gems || it.isGem) }
                }
            },
        )

    private fun viewModel(filter: HistoryFilter = HistoryFilter.All) = HistoryViewModel(repo, clock, filter)

    private fun session(
        id: Long,
        exercise: ExerciseId = ExerciseId.CRAZY_EIGHTS,
        dayOffset: Int = 0,
        gem: Boolean = false,
        status: SessionStatus = SessionStatus.COMPLETED,
        rating: Int? = null,
    ) = SessionRecord(
        id = id,
        exerciseId = exercise.value,
        status = status,
        startedAtEpochMillis = 1_000L * id,
        zoneId = "UTC",
        localEpochDay = today + dayOffset,
        isGem = gem,
        selfRating = rating,
    )

    private fun fill(vararg sessions: SessionRecord) {
        all.value = sessions.toList()
        count.value = sessions.size
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the initial state is loading and is neither empty nor filtered empty`() {
        val state = HistoryUiState()
        assertTrue(state.isLoading)
        assertFalse(state.isJournalEmpty)
        assertFalse(state.isFilterEmpty)
    }

    @Test
    fun `no session at all is the empty journal`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertTrue(state.isJournalEmpty)
                assertFalse(state.isFilterEmpty)
            }
        }

    @Test
    fun `sessions are grouped by day, newest first, with today and yesterday labelled`() =
        runTest {
            fill(session(5), session(4, dayOffset = -1), session(3, dayOffset = -1), session(2, dayOffset = -5))
            viewModel().uiState.test {
                val groups = expectMostRecentItem().groups
                assertEquals(
                    listOf(DayLabel.Today, DayLabel.Yesterday, DayLabel.Date(today - 5)),
                    groups.map { it.label },
                )
                assertEquals(
                    listOf(listOf(5L), listOf(4L, 3L), listOf(2L)),
                    groups.map { g ->
                        g.sessions.map { it.id }
                    },
                )
            }
        }

    @Test
    fun `an item carries the preview, the gem, the level and the unfinished mark`() =
        runTest {
            previews.value = mapOf(1L to "Le bois du comptoir")
            fill(session(1, gem = true, rating = 2), session(2, status = SessionStatus.IN_PROGRESS))
            viewModel().uiState.test {
                val items =
                    expectMostRecentItem()
                        .groups
                        .single()
                        .sessions
                        .associateBy { it.id }
                with(items.getValue(1)) {
                    assertEquals("Le bois du comptoir", preview)
                    assertTrue(isGem)
                    assertEquals(2, selfRating)
                    assertFalse(unfinished)
                }
                assertNull(items.getValue(2).preview)
                assertTrue(items.getValue(2).unfinished)
            }
        }

    @Test
    fun `selecting an exercise filters the list`() =
        runTest {
            fill(session(1, ExerciseId.CRAZY_EIGHTS), session(2, ExerciseId.ONE_PAGE))
            val vm = viewModel()
            vm.uiState.test {
                assertEquals(
                    2,
                    expectMostRecentItem()
                        .groups
                        .single()
                        .sessions.size,
                )
                vm.onFilterSelected(HistoryFilter.exercise(ExerciseId.ONE_PAGE))
                val state = expectMostRecentItem()
                assertEquals(HistoryFilter.exercise(ExerciseId.ONE_PAGE), state.filter)
                assertEquals(
                    listOf(2L),
                    state.groups
                        .single()
                        .sessions
                        .map { it.id },
                )
            }
            assertTrue((ExerciseId.ONE_PAGE.value to false) in repo.historyCalls.map { it.first to it.second })
        }

    @Test
    fun `the gems filter only keeps gems`() =
        runTest {
            fill(session(1, gem = true), session(2))
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onFilterSelected(HistoryFilter.Gems)
                assertEquals(
                    listOf(1L),
                    expectMostRecentItem()
                        .groups
                        .single()
                        .sessions
                        .map { it.id },
                )
            }
            assertEquals(null to true, repo.historyCalls.last())
        }

    @Test
    fun `the initial filter comes from the route`() =
        runTest {
            fill(session(1, ExerciseId.CRAZY_EIGHTS), session(2, ExerciseId.ONE_PAGE))
            viewModel(HistoryFilter.exercise(ExerciseId.CRAZY_EIGHTS)).uiState.test {
                val state = expectMostRecentItem()
                assertEquals(HistoryFilter.exercise(ExerciseId.CRAZY_EIGHTS), state.filter)
                assertEquals(
                    listOf(1L),
                    state.groups
                        .single()
                        .sessions
                        .map { it.id },
                )
            }
        }

    @Test
    fun `a filter without result is not the empty journal and can be cleared`() =
        runTest {
            fill(session(1, ExerciseId.CRAZY_EIGHTS))
            val vm = viewModel(HistoryFilter.exercise(ExerciseId.ONE_PAGE))
            vm.uiState.test {
                val empty = expectMostRecentItem()
                assertTrue(empty.isFilterEmpty)
                assertFalse(empty.isJournalEmpty)
                vm.onClearFilter()
                val cleared = expectMostRecentItem()
                assertEquals(HistoryFilter.All, cleared.filter)
                assertEquals(
                    1,
                    cleared.groups
                        .single()
                        .sessions.size,
                )
            }
        }

    @Test
    fun `is loading until the history has emitted`() =
        runTest {
            val never = MutableSharedFlow<List<SessionRecord>>()
            val vm = HistoryViewModel(FakeHistoryRepository(history = never), clock, HistoryFilter.All)
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                assertFalse(state.isJournalEmpty)
                never.emit(emptyList())
                assertTrue(awaitItem().isJournalEmpty)
            }
        }
}
