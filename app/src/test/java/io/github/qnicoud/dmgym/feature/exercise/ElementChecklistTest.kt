package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ElementCheck
import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Panneau des 5 éléments de Connect the Story : déplié à la demande, annoncé case par case (D48). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ElementChecklistTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val categories = listOf("place", "object", "trait", "sensory", "line")
    private val toggled = mutableListOf<DrawKey>()

    private val draws =
        categories.mapIndexed { index, category ->
            val check =
                when (category) {
                    "place" -> ElementCheck(auto = true)
                    "object" -> ElementCheck(auto = false, manual = true)
                    else -> null
                }
            Draw(category, 0, "story_elements", "e$index", "élément $category", check)
        }

    private fun show() {
        compose.setContent {
            DmGymTheme {
                DetectionDrawCard(
                    definition = ConnectStory.definition,
                    draws = draws,
                    rerollCount = 0,
                    showReroll = true,
                    onReroll = {},
                    onCheckToggled = { toggled += it },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun stateOf(category: String) =
        compose
            .onNodeWithTag(ExerciseTags.check(DrawKey(category, 0)))
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.StateDescription)

    @Test
    fun `starts unfolded with the five elements and the count`() {
        show()
        categories.forEach { compose.onNodeWithText("élément $it").assertIsDisplayed() }
        compose
            .onNodeWithTag(ExerciseTags.CHECKLIST_COUNT)
            .assertIsDisplayed()
        assertEquals(
            context.getString(R.string.checklist_linked, 2, 5),
            compose
                .onNodeWithTag(ExerciseTags.CHECKLIST_COUNT)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.firstOrNull()
                ?.text,
        )
    }

    @Test
    fun `each check announces automatic, manual or unused`() {
        show()
        assertEquals(context.getString(R.string.checklist_state_auto), stateOf("place"))
        assertEquals(context.getString(R.string.checklist_state_used), stateOf("object"))
        assertEquals(context.getString(R.string.checklist_state_unused), stateOf("trait"))
    }

    @Test
    fun `the chevron folds the panel into chips, which stay checkable and announced`() {
        show()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("élément place").assertDoesNotExist()
        assertEquals(context.getString(R.string.checklist_state_auto), stateOf("place"))
        compose.onNodeWithTag(ExerciseTags.check(DrawKey("trait", 0))).performClick()
        assertEquals(listOf(DrawKey("trait", 0)), toggled)
    }

    @Test
    fun `the panel unfolds again on demand`() {
        show()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("élément place").assertIsDisplayed()
    }
}
