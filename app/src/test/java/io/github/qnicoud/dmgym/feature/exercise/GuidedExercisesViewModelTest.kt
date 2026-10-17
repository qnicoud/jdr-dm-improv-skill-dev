package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.contrastBand
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Les quatre exercices de L10 joués sur le ViewModel générique, avec les vraies tables. */
class GuidedExercisesViewModelTest : ExerciseViewModelTestBase() {
    override fun tables() = RealTables.repository

    private val roles = RealTables.file(TableId.NpcRoles).entries.associateBy { it.id }

    private fun ExerciseViewModel.session() = uiState.value.session

    @Test
    fun `options start from their defaults`() =
        runTest {
            val vm = vm(StatusNumbers.definition)
            collect(vm)
            assertEquals(
                mapOf(StatusNumbers.OPTION_COUNTER to false, StatusNumbers.OPTION_SHIFT to false),
                vm.session().options,
            )
        }

    @Test
    fun `switching the counter stereotype redraws only the card, against the same role`() =
        runTest {
            val vm = vm(StatusNumbers.definition)
            collect(vm)
            val role = vm.session().drawsOf(StatusNumbers.ROLE_SLOT).single()
            repeat(10) {
                vm.onOptionChanged(StatusNumbers.OPTION_COUNTER, false)
                vm.onOptionChanged(StatusNumbers.OPTION_COUNTER, true)
                runCurrent()
                assertEquals(role, vm.session().drawsOf(StatusNumbers.ROLE_SLOT).single())
                val card =
                    vm
                        .session()
                        .drawsOf(StatusNumbers.CARD_SLOT)
                        .single()
                        .text
                        .toInt()
                val band = checkNotNull(contrastBand(roles.getValue(role.entryId).tags))
                assertTrue(card in band, "card $card outside $band")
            }
            assertEquals(0, vm.session().rerollCount)
        }

    @Test
    fun `rerolling keeps following the counter stereotype`() =
        runTest {
            val vm = vm(StatusNumbers.definition)
            collect(vm)
            vm.onOptionChanged(StatusNumbers.OPTION_COUNTER, true)
            repeat(15) {
                vm.onReroll(null)
                runCurrent()
                val role = vm.session().drawsOf(StatusNumbers.ROLE_SLOT).single()
                val card =
                    vm
                        .session()
                        .drawsOf(StatusNumbers.CARD_SLOT)
                        .single()
                        .text
                        .toInt()
                assertTrue(card in checkNotNull(contrastBand(roles.getValue(role.entryId).tags)))
            }
        }

    @Test
    fun `the shift fires once in the 40 to 60 percent window when the option is on`() =
        runTest {
            val vm = vm(StatusNumbers.definition)
            collect(vm)
            vm.onOptionChanged(StatusNumbers.OPTION_SHIFT, true)
            vm.onStart()
            runCurrent()
            val total = 180_000L
            var firedAt: Long? = null
            var elapsed = 0L
            while (elapsed < total && firedAt == null) {
                pass(1_000)
                elapsed += 1_000
                if (StatusNumbers.SHIFT_SLOT in vm.session().draws.map { it.slotKey }) firedAt = elapsed
            }
            val at = checkNotNull(firedAt) { "the shift never fired" }
            assertTrue(at in 72_000L..109_000L, "fired at $at")
            assertEquals(setOf(StatusNumbers.SHIFT_EVENT), vm.session().firedEvents)
            assertEquals(1, vm.session().drawsOf(StatusNumbers.SHIFT_SLOT).size)
        }

    @Test
    fun `no shift without the option`() =
        runTest {
            val vm = started(StatusNumbers.definition)
            repeat(170) { pass(1_000) }
            assertTrue(vm.session().firedEvents.isEmpty())
            assertTrue(vm.session().drawsOf(StatusNumbers.SHIFT_SLOT).isEmpty())
        }

    @Test
    fun `a status scene without notes is kept, and notes written after the end are saved`() =
        runTest {
            val vm = started(StatusNumbers.definition)
            vm.onFinish()
            runCurrent()
            assertEquals(Phase.Ended, vm.session().phase)
            vm.fieldState(AnswerKey(StatusNumbers.NOTES_FIELD)).setTextAndPlaceCursorAtEnd("trop de regard fuyant")
            runCurrent()
            vm.onLeave()
            runCurrent()
            assertTrue(history.deleted.isEmpty())
            val saved = history.sessions.values.single()
            assertEquals(SessionStatus.COMPLETED, saved.session.status)
            assertEquals(listOf("trop de regard fuyant"), saved.answers.map { it.text })
        }

    @Test
    fun `a status scene left without notes still counts as played`() =
        runTest {
            val vm = started(StatusNumbers.definition)
            vm.onFinish()
            runCurrent()
            vm.onLeave()
            runCurrent()
            assertTrue(history.deleted.isEmpty())
            assertEquals(1, history.sessions.size)
        }

    @Test
    fun `decision point shows no draw in video mode and a situation in table mode`() =
        runTest {
            val vm = vm(DecisionPoint.definition)
            collect(vm)
            assertTrue(vm.session().draws.isEmpty())
            vm.onOptionChanged(DecisionPoint.OPTION_SITUATION, true)
            runCurrent()
            assertEquals(listOf(DecisionPoint.SITUATION_SLOT), vm.session().draws.map { it.slotKey })
            vm.onOptionChanged(DecisionPoint.OPTION_SITUATION, false)
            runCurrent()
            assertTrue(vm.session().draws.isEmpty())
        }

    @Test
    fun `decision point never flag is stored with its answer`() =
        runTest {
            val vm = started(DecisionPoint.definition)
            val key = AnswerKey(DecisionPoint.FIELD_KEYS[1])
            vm.fieldState(key).setTextAndPlaceCursorAtEnd("brûler la bibliothèque")
            vm.onFlagToggled(key)
            runCurrent()
            assertTrue(
                vm
                    .session()
                    .answers
                    .getValue(key)
                    .flagged,
            )
            vm.onStop()
            runCurrent()
            assertTrue(
                history.sessions.values
                    .single()
                    .answers
                    .single { it.flagged }
                    .text
                    .isNotBlank(),
            )
        }

    @Test
    fun `core image walks the six questions back and forth`() =
        runTest {
            val vm = vm(CoreImage.definition)
            collect(vm)
            assertEquals(CoreImage.QUESTION_COUNT, vm.session().drawsOf(CoreImage.QUESTIONS_SLOT).size)
            repeat(10) { vm.onNextQuestion() }
            assertEquals(CoreImage.QUESTION_COUNT - 1, vm.session().currentQuestion)
            vm.onPreviousQuestion()
            assertEquals(CoreImage.QUESTION_COUNT - 2, vm.session().currentQuestion)
            repeat(10) { vm.onPreviousQuestion() }
            assertEquals(0, vm.session().currentQuestion)
            assertEquals(0, handle.get<Int>("question"))
        }

    @Test
    fun `core image keeps one answer per question`() =
        runTest {
            val vm = started(CoreImage.definition)
            vm.fieldState(AnswerKey(CoreImage.ANSWER_FIELD, 0)).setTextAndPlaceCursorAtEnd("elle ouvre la crypte")
            vm.onNextQuestion()
            vm.fieldState(AnswerKey(CoreImage.ANSWER_FIELD, 1)).setTextAndPlaceCursorAtEnd("la pluie")
            runCurrent()
            vm.onStop()
            runCurrent()
            val saved =
                history.sessions.values
                    .single()
                    .answers
                    .associate { it.groupIndex to it.text }
            assertEquals(mapOf(0 to "elle ouvre la crypte", 1 to "la pluie"), saved)
        }

    @Test
    fun `one page adds up to five npcs`() =
        runTest {
            val vm = started(OnePage.definition)
            repeat(8) { vm.onAddGroup() }
            assertEquals(OnePage.MAX_NPCS, vm.uiState.value.groupCount)
        }

    @Test
    fun `one page draws a situation only in the no campaign mode`() =
        runTest {
            val vm = vm(OnePage.definition)
            collect(vm)
            assertTrue(vm.session().draws.isEmpty())
            vm.onOptionChanged(OnePage.OPTION_DRAWN, true)
            runCurrent()
            assertEquals(1, vm.session().drawsOf(OnePage.SITUATION_SLOT).size)
        }
}
