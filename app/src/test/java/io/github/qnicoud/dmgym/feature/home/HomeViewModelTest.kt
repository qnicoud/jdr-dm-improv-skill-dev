package io.github.qnicoud.dmgym.feature.home

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val settings = FakeSettingsRepository()
    private val sessionCount = MutableStateFlow(0)

    private fun viewModel() = HomeViewModel(SettingsExerciseCatalog(settings), FakeHistoryRepository(sessionCount))

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `lists the exercises in order with default durations`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(ExerciseId.ALL, state.exercises.map { it.id })
                assertEquals(listOf(480, 300, 600, 180, 30, 600, null, 240), state.exercises.map { it.durationSeconds })
            }
        }

    @Test
    fun `one page has no timer by default`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.exercises.single { it.id == ExerciseId.ONE_PAGE }.durationSeconds)
            }
        }

    @Test
    fun `is loading until history and settings have emitted`() =
        runTest {
            val never = MutableSharedFlow<Int>()
            val vm = HomeViewModel(SettingsExerciseCatalog(settings), FakeHistoryRepository(never))
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                assertFalse(state.isEmpty)
                never.emit(0)
                assertTrue(awaitItem().isEmpty)
            }
        }

    @Test
    fun `empty state only when loaded with zero sessions`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(0, state.sessionCount)
                assertTrue(state.isEmpty)
            }
        }

    @Test
    fun `exposes the real session count`() =
        runTest {
            sessionCount.value = 47
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(47, state.sessionCount)
                assertFalse(state.isEmpty)
                sessionCount.value = 48
                assertEquals(48, awaitItem().sessionCount)
            }
        }

    @Test
    fun `no resumable session means no resume card`() =
        runTest {
            viewModel().uiState.test { assertNull(expectMostRecentItem().resume) }
        }

    @Test
    fun `the resumable session becomes the resume card with its first non blank answer`() =
        runTest {
            val resumable = MutableStateFlow<SessionDetail?>(null)
            val vm = HomeViewModel(SettingsExerciseCatalog(settings), FakeHistoryRepository(resumable = resumable))
            vm.uiState.test {
                assertNull(expectMostRecentItem().resume)
                resumable.value =
                    SessionDetail(
                        SessionRecord(
                            id = 4,
                            exerciseId = ExerciseId.CONSTRAINT_BOX.value,
                            status = SessionStatus.IN_PROGRESS,
                            startedAtEpochMillis = 1_000,
                            lastActivityAtEpochMillis = 5_000,
                            zoneId = "UTC",
                            localEpochDay = 1,
                            activeDurationSec = 125,
                        ),
                        draws = emptyList(),
                        answers = listOf(AnswerRecord("a", 0, "  "), AnswerRecord("b", 0, "  Le bois du comptoir  ")),
                        rubric = emptyList(),
                    )
                val resume = expectMostRecentItem().resume!!
                assertEquals(4L, resume.sessionId)
                assertEquals(ExerciseId.CONSTRAINT_BOX, resume.exerciseId)
                assertEquals("Le bois du comptoir", resume.preview)
                assertEquals(5_000L, resume.lastActivityAtEpochMillis)
                resumable.value = null
                assertNull(expectMostRecentItem().resume)
            }
        }

    @Test
    fun `follows the settings when a duration changes or the timer is removed`() =
        runTest {
            viewModel().uiState.test {
                expectMostRecentItem()
                settings.update { it.copy(durations = mapOf(ExerciseId.CRAZY_EIGHTS to 12.minutes)) }
                assertEquals(720, awaitItem().exercises.first().durationSeconds)
                settings.update { it.copy(untimed = setOf(ExerciseId.CRAZY_EIGHTS)) }
                assertNull(awaitItem().exercises.first().durationSeconds)
            }
        }
}
