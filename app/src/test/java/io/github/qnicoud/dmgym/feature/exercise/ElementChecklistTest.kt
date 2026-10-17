package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ElementCheck
import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Panneau des 5 éléments de Connect the Story : déplié à la demande, annoncé case par case (D48). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
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

    private fun show(readOnly: Boolean = false) {
        compose.setContent {
            DmGymTheme {
                DetectionDrawCard(
                    definition = ConnectStory.definition,
                    draws = draws,
                    rerollCount = 0,
                    showReroll = true,
                    onReroll = {},
                    onCheckToggled = { toggled += it },
                    readOnly = readOnly,
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
    fun `read only shows the elements without checkbox, count or toggle`() {
        show(readOnly = true)
        categories.forEach {
            compose.onNodeWithText("élément $it").assertIsDisplayed()
            assertEquals(null, stateOf(it))
        }
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_COUNT, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(ExerciseTags.check(DrawKey("place", 0))).performClick()
        assertTrue(toggled.isEmpty())
    }

    @Test
    fun `starts unfolded with the five elements and the count`() {
        show()
        categories.forEach { compose.onNodeWithText("élément $it").assertIsDisplayed() }
        compose
            .onNodeWithTag(ExerciseTags.CHECKLIST_COUNT, useUnmergedTree = true)
            .assertIsDisplayed()
        assertEquals(
            context.getString(R.string.checklist_linked, 2, 5),
            compose
                .onNodeWithTag(ExerciseTags.CHECKLIST_COUNT, useUnmergedTree = true)
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
    fun `the chevron folds the panel into a single summary line without any element`() {
        show()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.waitForIdle()
        categories.forEach {
            compose.onNodeWithText("élément $it").assertDoesNotExist()
            compose.onNodeWithTag(ExerciseTags.check(DrawKey(it, 0))).assertDoesNotExist()
        }
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_COUNT, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `the folded line announces the count and the unfold action`() {
        show()
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.waitForIdle()
        val node = compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE, useUnmergedTree = false).fetchSemanticsNode()
        val texts =
            node.config
                .getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .map { it.text }
        assertEquals(listOf(context.getString(R.string.checklist_linked, 2, 5)), texts)
        assertEquals(
            context.getString(R.string.checklist_expand),
            node.config.getOrNull(SemanticsActions.OnClick)?.label,
        )
    }

    @Test
    fun `folded at font scale 2 on a 640dp screen the card stays small and leaves room to the field`() {
        var fieldHeight = 0
        var cardHeight = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                DmGymTheme {
                    Column(Modifier.fillMaxSize()) {
                        DetectionDrawCard(
                            definition = ConnectStory.definition,
                            draws = draws,
                            rerollCount = 0,
                            showReroll = true,
                            onReroll = {},
                            onCheckToggled = {},
                            modifier = Modifier.onSizeChanged { cardHeight = it.height },
                        )
                        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { fieldHeight = it.height })
                    }
                }
            }
        }
        compose.onNodeWithTag(ExerciseTags.CHECKLIST_TOGGLE).performClick()
        compose.waitForIdle()
        val density = compose.density.density
        assertTrue("card ${cardHeight / density}dp", cardHeight / density <= 100f)
        assertTrue("field ${fieldHeight / density}dp", fieldHeight / density >= 500f)
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
