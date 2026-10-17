package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Seuil de la zone épinglée compacte, avec une hauteur simulée (le clavier réduit la hauteur disponible). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h900dp")
class ExerciseCompactThresholdTest {
    @get:Rule
    val compose = createComposeRule()

    private var screenHeight by mutableStateOf(800.dp)
    private val noOp = {}
    private val reminderText = R.string.draw_error_body
    private val drawText = "Un pont s’effondre"

    private fun state() =
        ExerciseUiState(
            definition = DemoExercise.definition.copy(reminder = reminderText),
            session =
                ExerciseSession(
                    definitionId = DemoExercise.id,
                    phase = Phase.Active,
                    draws = listOf(Draw(DemoExercise.SLOT, 0, "problems", "x", drawText)),
                ),
            load = LoadStatus.Ready,
        )

    private fun actions() =
        ExerciseActions(
            onLeave = noOp,
            onStart = noOp,
            onFinish = noOp,
            onPause = noOp,
            onResume = noOp,
            onReroll = {},
            onRetry = noOp,
            onCheckToggled = {},
            fields = FieldHandlers({ TextFieldState() }, {}, noOp, noOp),
            assess = AssessActions({ _, _, _ -> }, noOp),
        )

    private fun show() {
        compose.setContent {
            DmGymTheme {
                Box(Modifier.height(screenHeight)) {
                    ExerciseContent(state(), { TimerUi(TimerKind.Running, 200_000, 0.6f) }, actions())
                }
            }
        }
        compose.waitForIdle()
    }

    private fun summaryShown() =
        compose.onAllNodesWithTag(ExerciseTags.PINNED_SUMMARY).fetchSemanticsNodes().isNotEmpty()

    private fun reminderBodyShown(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return compose.onAllNodesWithText(context.getString(reminderText)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun resize(body: Dp) {
        screenHeight = body + APP_BAR
        compose.waitForIdle()
    }

    @Test
    fun `just above the threshold the full pinned area stays`() {
        show()
        resize(COMPACT_HEIGHT + 1.dp)
        assertFalse(summaryShown())
        compose.onAllNodesWithTag(ExerciseTags.DRAW_CARD).assertCountEquals(1)
    }

    @Test
    fun `just below the threshold the pinned area is compact`() {
        show()
        resize(COMPACT_HEIGHT - 1.dp)
        assertTrue(summaryShown())
        compose.onAllNodesWithTag(ExerciseTags.DRAW_CARD).assertCountEquals(0)
    }

    @Test
    fun `the keyboard opening then closing switches compact on then off`() {
        show()
        assertFalse(summaryShown())
        resize(300.dp)
        assertTrue(summaryShown())
        resize(800.dp)
        assertFalse(summaryShown())
    }

    @Test
    fun `the reminder folds when the area becomes compact afterwards`() {
        show()
        assertTrue(reminderBodyShown())
        resize(300.dp)
        assertFalse(reminderBodyShown())
    }

    @Test
    fun `the folded line is described in full for TalkBack`() {
        show()
        resize(300.dp)
        val description =
            compose
                .onNodeWithTag(ExerciseTags.PINNED_SUMMARY)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
                ?.joinToString(" ")
        assertTrue(description.orEmpty(), description.orEmpty().contains(drawText))
    }

    private companion object {
        val APP_BAR = 64.dp
    }
}
