package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.YesAnd
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** « Oui, et… » joué sur le ViewModel générique, avec les vraies tables. */
class YesAndViewModelTest : ExerciseViewModelTestBase() {
    override fun tables() = RealTables.repository

    private fun ExerciseViewModel.session() = uiState.value.session

    private fun key(
        field: String,
        offer: Int,
    ) = AnswerKey(field, offer)

    @Test
    fun `five offers are drawn before the start and cannot be rerolled`() =
        runTest {
            val vm = vm(YesAnd.definition)
            collect(vm)
            assertEquals(Phase.Ready, vm.session().phase)
            assertEquals(YesAnd.OFFER_COUNT, vm.session().drawsOf(YesAnd.OFFERS_SLOT).size)
            assertEquals(0, vm.session().rerollCount)
        }

    @Test
    fun `the minute beat never moves to the next offer`() =
        runTest {
            val vm = started(YesAnd.definition)
            repeat(3) { pass(60_000) }
            assertEquals(0, vm.session().currentQuestion)
            assertEquals(Phase.Active, vm.session().phase)
        }

    @Test
    fun `the next offer is manual and bounded to the five offers`() =
        runTest {
            val vm = started(YesAnd.definition)
            repeat(10) { vm.onNextQuestion() }
            assertEquals(YesAnd.OFFER_COUNT - 1, vm.session().currentQuestion)
            vm.onPreviousQuestion()
            assertEquals(YesAnd.OFFER_COUNT - 2, vm.session().currentQuestion)
        }

    @Test
    fun `reflex and answer are kept per offer when blocking first`() =
        runTest {
            val vm = vm(YesAnd.definition)
            collect(vm)
            vm.onOptionChanged(YesAnd.BLOCK_FIRST, true)
            vm.onStart()
            runCurrent()
            vm.fieldState(key(YesAnd.BLOCK_FIELD, 0)).setTextAndPlaceCursorAtEnd("je dis non")
            vm.fieldState(key(YesAnd.YES_AND_FIELD, 0)).setTextAndPlaceCursorAtEnd("il ouvre la porte")
            vm.onNextQuestion()
            vm.fieldState(key(YesAnd.YES_AND_FIELD, 1)).setTextAndPlaceCursorAtEnd("on la suit")
            runCurrent()
            vm.onStop()
            runCurrent()
            val saved = history.sessions.values.single()
            assertEquals(
                mapOf(
                    ("block" to 0) to "je dis non",
                    ("yes_and" to 0) to "il ouvre la porte",
                    ("yes_and" to 1) to "on la suit",
                ),
                saved.answers.filter { it.text.isNotBlank() }.associate { (it.fieldKey to it.groupIndex) to it.text },
            )
            assertTrue(saved.session.optionsJson.contains("block_first"))
        }

    @Test
    fun `the session ends with all five offers and a two question grid by default`() =
        runTest {
            val vm = started(YesAnd.definition)
            vm.onAnswerChanged(key(YesAnd.YES_AND_FIELD, 0), "et ça brille")
            runCurrent()
            vm.onFinish()
            vm.onStop()
            runCurrent()
            assertEquals(Phase.Ended, vm.session().phase)
            assertEquals(2, YesAnd.definition.rubricFor(vm.session().options).size)
            assertTrue(
                history.sessions.values
                    .single()
                    .answers
                    .any { it.text == "et ça brille" },
            )
        }

    @Test
    fun `resuming from the history lands on the last answered offer`() =
        runTest {
            val first = started(YesAnd.definition)
            first.onAnswerChanged(key(YesAnd.YES_AND_FIELD, 0), "un")
            first.onAnswerChanged(key(YesAnd.YES_AND_FIELD, 2), "trois")
            runCurrent()
            first.onStop()
            runCurrent()
            val id = history.sessions.keys.single()
            handle = androidx.lifecycle.SavedStateHandle()
            val resumed = vm(YesAnd.definition, ExerciseLaunch(resumeSessionId = id, seed = 7))
            collect(resumed)
            assertEquals(2, resumed.session().currentQuestion)
        }
}
