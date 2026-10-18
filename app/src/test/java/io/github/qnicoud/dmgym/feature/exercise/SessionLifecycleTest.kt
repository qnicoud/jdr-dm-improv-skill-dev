package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleTest {
    // `FieldStates` enregistre un `snapshotFlow` dans le snapshot global : des collecteurs d'autres tests peuvent
    // encore y réagir et exigent un `Dispatchers.Main` de test, sinon l'enregistrement échoue au hasard.
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.rig(override: ((RecordingHistoryRepository) -> HistoryRepository)? = null) =
        ControllerRig(backgroundScope, StandardTestDispatcher(testScheduler), override)

    private fun ControllerRig.events() = generateSequence { channel.tryReceive().getOrNull() }.toList()

    @Test
    fun `start creates the session, starts the clock and announces it`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            assertEquals(1, rig.history.sessions.size)
            assertEquals(Phase.Active, rig.model.value.session.phase)
            assertEquals(1L, rig.handle.get<Long>(HandleKeys.SESSION))
            assertTrue(ExerciseEvent.Announce(Milestone.Started) in rig.events())
        }

    @Test
    fun `a second start while already active does nothing`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.lifecycle.start()
            runCurrent()
            assertEquals(1, rig.history.sessions.size)
        }

    @Test
    fun `start is ignored while loading`() =
        runTest {
            val rig = rig()
            rig.model.value = rig.model.value.copy(loading = true)
            rig.lifecycle.start()
            runCurrent()
            assertTrue(rig.history.sessions.isEmpty())
        }

    @Test
    fun `a creation error shows a message and allows a new try`() =
        runTest {
            val rig = rig()
            rig.history.failOnCreate = true
            rig.lifecycle.start()
            runCurrent()
            assertTrue(ExerciseEvent.Snackbar(R.string.start_failed) in rig.events())
            assertEquals(Phase.Ready, rig.model.value.session.phase)
            rig.history.failOnCreate = false
            rig.lifecycle.start()
            runCurrent()
            assertEquals(Phase.Active, rig.model.value.session.phase)
        }

    @Test
    fun `a cancellation during creation is rethrown, not turned into a message`() =
        runTest {
            val rig =
                rig { base ->
                    object : HistoryRepository by base {
                        override suspend fun createSession(session: SessionRecord): Long =
                            throw CancellationException("stop")
                    }
                }
            rig.lifecycle.start()
            runCurrent()
            assertTrue(rig.events().none { it is ExerciseEvent.Snackbar })
            assertEquals(Phase.Ready, rig.model.value.session.phase)
        }

    @Test
    fun `a cancellation during the save is rethrown`() =
        runTest {
            val rig =
                rig { base ->
                    object : HistoryRepository by base {
                        override suspend fun saveSnapshot(
                            session: SessionRecord,
                            draws: List<io.github.qnicoud.dmgym.core.history.DrawRecord>,
                            answers: List<io.github.qnicoud.dmgym.core.history.AnswerRecord>,
                            rubric: List<io.github.qnicoud.dmgym.core.history.RubricRecord>,
                        ): Unit = throw CancellationException("stop")
                    }
                }
            rig.lifecycle.start()
            runCurrent()
            try {
                rig.lifecycle.persist(rig.model.value.session)
                fail("CancellationException expected")
            } catch (_: CancellationException) {
                // attendu
            }
        }

    @Test
    fun `a save error marks the save as failed`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.history.failOnSave = true
            rig.lifecycle.persist(rig.model.value.session)
            assertEquals(SaveStatus.Failed, rig.model.value.save)
            rig.history.failOnSave = false
            rig.lifecycle.persist(rig.model.value.session)
            assertEquals(SaveStatus.Saved, rig.model.value.save)
        }

    @Test
    fun `flush pushes the typed text before saving`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.typed[DemoExercise.keyA] = "bonjour"
            rig.fields.get(DemoExercise.keyA)
            rig.lifecycle.flush()
            val saved = rig.history.snapshots.last()
            assertEquals("bonjour", saved.answers.single { it.fieldKey == DemoExercise.FIELD_A }.text)
        }

    @Test
    fun `flush before the start writes nothing`() =
        runTest {
            val rig = rig()
            rig.lifecycle.flush()
            assertTrue(rig.history.snapshots.isEmpty())
        }

    @Test
    fun `finish freezes the clock and ends the session`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.lifecycle.finish()
            assertEquals(Phase.Ended, rig.model.value.session.phase)
        }

    @Test
    fun `finish does nothing before the start`() =
        runTest {
            val rig = rig()
            rig.lifecycle.finish()
            assertEquals(Phase.Ready, rig.model.value.session.phase)
        }

    @Test
    fun `stop writes the draft at once`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.dispatch(ExerciseAction.AnswerChanged(DemoExercise.keyA, "brouillon"))
            rig.lifecycle.stop()
            advanceUntilIdle()
            runCurrent()
            assertEquals(
                "brouillon",
                rig.history.snapshots
                    .last()
                    .answers
                    .single()
                    .text,
            )
        }

    @Test
    fun `leaving without content deletes the session`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.lifecycle.leave()
            runCurrent()
            assertEquals(listOf(1L), rig.history.deleted)
        }

    @Test
    fun `leaving with content keeps and saves the session`() =
        runTest {
            val rig = rig()
            rig.lifecycle.start()
            runCurrent()
            rig.dispatch(ExerciseAction.AnswerChanged(DemoExercise.keyA, "du texte"))
            rig.lifecycle.leave()
            runCurrent()
            assertTrue(rig.history.deleted.isEmpty())
            assertFalse(rig.history.snapshots.isEmpty())
            assertNull(
                rig.history.sessions[1L]
                    ?.session
                    ?.endedAtEpochMillis,
            )
        }
}
