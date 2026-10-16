package io.github.qnicoud.dmgym.feature.home

import app.cash.turbine.test
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `lists the seven exercises in order with default durations`() =
        runTest {
            val viewModel = HomeViewModel(DefaultExerciseCatalog())
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(ExerciseId.entries, state.exercises.map { it.id })
                assertEquals(
                    listOf(480, 300, 600, 180, 30, 600, null),
                    state.exercises.map { it.durationSeconds },
                )
            }
        }

    @Test
    fun `one page has no timer`() =
        runTest {
            val viewModel = HomeViewModel(DefaultExerciseCatalog())
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertNull(state.exercises.single { it.id == ExerciseId.ONE_PAGE }.durationSeconds)
            }
        }

    @Test
    fun `starts with zero sessions which is the empty state`() =
        runTest {
            val viewModel = HomeViewModel(DefaultExerciseCatalog())
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(0, state.sessionCount)
                assertTrue(state.isEmpty)
            }
        }

    @Test
    fun `follows the catalog when a duration changes`() =
        runTest {
            val source = MutableStateFlow(listOf(ExerciseSummary(ExerciseId.CRAZY_EIGHTS, 480)))
            val viewModel = HomeViewModel(FakeCatalog(source))
            viewModel.uiState.test {
                assertEquals(480, expectMostRecentItem().exercises.single().durationSeconds)
                source.value = listOf(ExerciseSummary(ExerciseId.CRAZY_EIGHTS, 600))
                assertEquals(600, awaitItem().exercises.single().durationSeconds)
            }
        }

    private class FakeCatalog(
        override val exercises: Flow<List<ExerciseSummary>>,
    ) : ExerciseCatalog
}
