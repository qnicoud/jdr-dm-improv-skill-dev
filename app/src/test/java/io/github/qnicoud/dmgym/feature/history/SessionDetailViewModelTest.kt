package io.github.qnicoud.dmgym.feature.history

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {
    private val repo = FakeHistoryRepository()
    private val registry = ExerciseRegistry(listOf(DemoExercise.definition))

    private fun record(
        status: SessionStatus = SessionStatus.COMPLETED,
        rating: Int? = null,
        gem: Boolean = false,
        exercise: ExerciseId = DemoExercise.id,
    ) = SessionRecord(
        id = 7,
        exerciseId = exercise.value,
        status = status,
        startedAtEpochMillis = 1_700_000_000_000,
        zoneId = "UTC",
        localEpochDay = 19_675,
        activeDurationSec = 300,
        overtimeSec = 42,
        selfRating = rating,
        isGem = gem,
    )

    private fun store(
        record: SessionRecord = record(),
        draws: List<DrawRecord> = emptyList(),
        answers: List<AnswerRecord> = emptyList(),
        rubric: List<RubricRecord> = emptyList(),
    ) {
        repo.details.value = mapOf(record.id to SessionDetail(record, draws, answers, rubric))
    }

    private fun viewModel(id: Long = 7) = SessionDetailViewModel(id, repo, registry)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the initial state is loading`() {
        val state = SessionDetailUiState()
        assertTrue(state.isLoading)
        assertFalse(state.isMissing)
    }

    @Test
    fun `an unknown session is reported as missing`() =
        runTest {
            viewModel(99).uiState.test {
                val state = expectMostRecentItem()
                assertTrue(state.isMissing)
                assertNull(state.detail)
            }
        }

    @Test
    fun `the detail carries the header data`() =
        runTest {
            store(record(rating = 2, gem = true))
            viewModel().uiState.test {
                val detail = expectMostRecentItem().detail!!
                assertEquals(DemoExercise.id, detail.exerciseId)
                assertEquals(300, detail.activeSec)
                assertEquals(42, detail.overtimeSec)
                assertEquals(2, detail.selfRating)
                assertTrue(detail.isGem)
                assertFalse(detail.unfinished)
            }
        }

    @Test
    fun `words are used, unused or not tracked, with the manual correction winning`() =
        runTest {
            store(
                draws =
                    listOf(
                        DrawRecord("prompt", 0, "t", "a", "tiède", autoDetected = true, manualChecked = null),
                        DrawRecord("prompt", 1, "t", "b", "moisi", autoDetected = true, manualChecked = false),
                        DrawRecord("prompt", 2, "t", "c", "grésillement", autoDetected = false, manualChecked = true),
                        DrawRecord("prompt", 3, "t", "d", "Auberge"),
                    ),
            )
            viewModel().uiState.test {
                val draws = expectMostRecentItem().detail!!.draws
                assertEquals(listOf(true, false, true, null), draws.map { it.used })
                assertEquals(listOf("tiède", "moisi", "grésillement", "Auberge"), draws.map { it.text })
            }
        }

    @Test
    fun `answers follow the definition order, skip blank ones and keep the flag`() =
        runTest {
            store(
                answers =
                    listOf(
                        AnswerRecord("b", 0, "seconde", flagged = true),
                        AnswerRecord("a", 0, "première"),
                        AnswerRecord("a", 1, "   "),
                    ),
            )
            viewModel().uiState.test {
                val answers = expectMostRecentItem().detail!!.answers
                assertEquals(listOf("première", "seconde"), answers.map { it.text })
                assertEquals(listOf(false, true), answers.map { it.flagged })
            }
        }

    @Test
    fun `without a definition the answers keep the stored order`() =
        runTest {
            store(
                record = record(exercise = ExerciseId("later")),
                answers = listOf(AnswerRecord("x", 0, "un"), AnswerRecord("y", 0, "deux")),
            )
            viewModel().uiState.test {
                assertEquals(listOf("un", "deux"), expectMostRecentItem().detail!!.answers.map { it.text })
            }
        }

    @Test
    fun `the rubric answers are listed with their question`() =
        runTest {
            store(rubric = listOf(RubricRecord("q1", true), RubricRecord("q2", false), RubricRecord("gone", true)))
            viewModel().uiState.test {
                val rubric = expectMostRecentItem().detail!!.rubric
                assertEquals(listOf(true, false), rubric.map { it.value })
            }
        }

    @Test
    fun `the share text joins the non blank answers`() =
        runTest {
            store(answers = listOf(AnswerRecord("a", 0, "un"), AnswerRecord("b", 0, "deux"), AnswerRecord("c", 0, " ")))
            viewModel().uiState.test {
                assertEquals("un\n\ndeux", expectMostRecentItem().detail!!.shareText)
            }
        }

    @Test
    fun `there is nothing to share without text`() =
        runTest {
            store()
            viewModel().uiState.test { assertNull(expectMostRecentItem().detail!!.shareText) }
        }

    @Test
    fun `choosing a level saves it and touching it again clears it`() =
        runTest {
            store(record(rating = 1))
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onLevelSelected(3)
                assertEquals(3, expectMostRecentItem().detail!!.selfRating)
                vm.onLevelSelected(3)
                assertNull(expectMostRecentItem().detail!!.selfRating)
            }
            assertEquals(listOf(7L to 3, 7L to null), repo.ratings)
        }

    @Test
    fun `the gem toggles and nothing else changes in the record`() =
        runTest {
            store(record(rating = 2))
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onGemToggled()
                assertTrue(expectMostRecentItem().detail!!.isGem)
            }
            assertEquals(listOf(7L to true), repo.gems)
            assertTrue(repo.ratings.isEmpty())
        }

    @Test
    fun `an unfinished session can be evaluated too`() =
        runTest {
            store(record(status = SessionStatus.IN_PROGRESS))
            val vm = viewModel()
            vm.uiState.test {
                assertTrue(expectMostRecentItem().detail!!.unfinished)
                vm.onLevelSelected(2)
                assertEquals(2, expectMostRecentItem().detail!!.selfRating)
                vm.onGemToggled()
                assertTrue(expectMostRecentItem().detail!!.isGem)
            }
            assertEquals(listOf(7L to 2), repo.ratings)
        }

    @Test
    fun `a failed write is announced as a save failure`() =
        runTest {
            store()
            repo.failOnUpdate = true
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onLevelSelected(2)
            }
            vm.events.test { assertEquals(DetailEvent.SaveFailed, awaitItem()) }
        }

    @Test
    fun `a failed deletion is announced as a delete failure and the page stays`() =
        runTest {
            store()
            repo.failOnDelete = true
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onDeleteRequested()
                expectMostRecentItem()
                vm.onDeleteConfirmed()
                val state = expectMostRecentItem()
                assertFalse(state.deleted)
                assertFalse(state.isMissing)
            }
            vm.events.test { assertEquals(DetailEvent.DeleteFailed, awaitItem()) }
        }

    @Test
    fun `deleting asks for confirmation first, and cancelling keeps the session`() =
        runTest {
            store()
            val vm = viewModel()
            vm.uiState.test {
                assertFalse(expectMostRecentItem().confirmingDelete)
                vm.onDeleteRequested()
                assertTrue(expectMostRecentItem().confirmingDelete)
                vm.onDeleteCancelled()
                assertFalse(expectMostRecentItem().confirmingDelete)
            }
            assertTrue(repo.deleted.isEmpty())
        }

    @Test
    fun `confirming the deletion removes the session and signals it`() =
        runTest {
            store()
            val vm = viewModel()
            vm.uiState.test {
                expectMostRecentItem()
                vm.onDeleteRequested()
                expectMostRecentItem()
                vm.onDeleteConfirmed()
                val state = expectMostRecentItem()
                assertTrue(state.deleted)
                assertFalse(state.isMissing)
            }
            assertEquals(listOf(7L), repo.deleted)
        }

    @Test
    fun `confirming without asking does nothing`() =
        runTest {
            store()
            val vm = viewModel()
            vm.onDeleteConfirmed()
            assertTrue(repo.deleted.isEmpty())
        }
}
