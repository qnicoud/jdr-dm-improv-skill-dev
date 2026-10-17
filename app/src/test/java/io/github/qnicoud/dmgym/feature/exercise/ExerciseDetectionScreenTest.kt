package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.KeywordAnswerDetector
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Surlignage des mots imposés et cases « utilisé » annoncées par TalkBack (L8). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ExerciseDetectionScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val slot = "words"
    private val wordKey = DrawKey(slot, 0)

    /** « mine » est la seule entrée de la table de fixture à porter des mots-clés. */
    private val definition =
        DemoExercise.definition.copy(
            slots = listOf(TableSlot(slot, TableId.Places, category = "c", label = R.string.constraint_box_words)),
            detection = DetectionSpec(listOf(slot), DemoExercise.FIELD_A),
        )

    private fun launch(): ExerciseViewModel {
        val tables = DemoExercise.tables()
        val vm =
            ExerciseViewModel(
                definition = definition,
                deps =
                    ExerciseDependencies(
                        tables,
                        RecordingHistoryRepository(),
                        FakeSettingsRepository(Settings()),
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                        KeywordAnswerDetector(tables),
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 3),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        return vm
    }

    private fun type(text: String) {
        compose.onNodeWithTag(ExerciseTags.field(DemoExercise.keyA)).performTextInput(text)
        compose.waitForIdle()
    }

    private fun stateOfCheck() =
        compose
            .onNodeWithTag(ExerciseTags.check(wordKey))
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.StateDescription)

    /** Texte tel qu'il est mis en page (après la transformation de sortie), avec ses styles. */
    private fun shownText(): AnnotatedString {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithTag(ExerciseTags.field(DemoExercise.keyA))
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.firstOrNull()?.layoutInput?.text ?: AnnotatedString("")
    }

    @Test
    fun `the drawn word is highlighted in the text with more than a color`() {
        launch()
        type("Une mine abandonnée")
        val spans = shownText().spanStyles
        assertEquals(1, spans.size)
        val span = spans.single()
        assertEquals(4, span.start)
        assertEquals(8, span.end)
        assertEquals(FontWeight.Bold, span.item.fontWeight)
        assertEquals(TextDecoration.Underline, span.item.textDecoration)
    }

    @Test
    fun `there is no highlight when the word is absent`() {
        launch()
        type("Une cave abandonnée")
        assertTrue(shownText().spanStyles.isEmpty())
    }

    @Test
    fun `the check announces its state to TalkBack, automatic then manual`() {
        val vm = launch()
        compose.onNodeWithTag(ExerciseTags.check(wordKey)).assertIsDisplayed()
        assertEquals(context.getString(R.string.checklist_state_unused), stateOfCheck())
        type("Une mine")
        assertEquals(context.getString(R.string.checklist_state_auto), stateOfCheck())
        compose.onNodeWithTag(ExerciseTags.check(wordKey)).performClick()
        compose.waitForIdle()
        assertEquals(context.getString(R.string.checklist_state_unused), stateOfCheck())
        compose.onNodeWithTag(ExerciseTags.check(wordKey)).performClick()
        compose.waitForIdle()
        assertEquals(context.getString(R.string.checklist_state_used), stateOfCheck())
        assertEquals(
            true,
            vm.uiState.value.session
                .drawsOf(slot)
                .single()
                .check
                ?.manual,
        )
    }
}
