package io.github.qnicoud.dmgym.feature.home

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
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
    fun `lists the seven exercises in order with default durations`() =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                assertEquals(ExerciseId.ALL, state.exercises.map { it.id })
                assertEquals(listOf(480, 300, 600, 180, 30, 600, null), state.exercises.map { it.durationSeconds })
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
