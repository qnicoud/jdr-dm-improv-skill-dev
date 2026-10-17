package io.github.qnicoud.dmgym.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class SessionDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val shared = mutableListOf<String>()
    private val levels = mutableListOf<Int>()
    private val redone = mutableListOf<ExerciseId>()
    private var gems = 0
    private var backs = 0
    private var requested = 0
    private var cancelled = 0
    private var confirmed = 0

    private val detail =
        SessionDetailUi(
            sessionId = 7,
            exerciseId = ExerciseId.CONSTRAINT_BOX,
            startedAtEpochMillis = 1_700_000_000_000,
            zoneId = "UTC",
            activeSec = 300,
            overtimeSec = 42,
            unfinished = false,
            draws = listOf(DrawLine("tiède", true), DrawLine("moisi", false), DrawLine("Auberge de relais", null)),
            answers =
                listOf(
                    AnswerLine(null, 1, null, "Le bois du comptoir est tiède sous la paume.", false, null),
                    AnswerLine(null, 2, null, "Personne ne lève la tête.", true, R.string.gem_kept),
                ),
            rubric = listOf(RubricLine(R.string.rubric_yes, true)),
            selfRating = 3,
            isGem = false,
            shareText = "texte",
        )

    private fun show(
        state: SessionDetailUiState = SessionDetailUiState(isLoading = false, detail = detail),
        fontScale: Float = 1f,
        dark: Boolean = false,
    ) {
        compose.setContent {
            DmGymTheme(darkTheme = dark) {
                WithFontScale(fontScale) {
                    SessionDetailScreen(
                        state = state,
                        actions =
                            DetailActions(
                                onBack = { backs++ },
                                onShare = { shared += it },
                                onLevelSelected = { levels += it },
                                onGemToggled = { gems++ },
                                onRedo = { redone += it },
                                onDeleteRequested = { requested++ },
                                onDeleteCancelled = { cancelled++ },
                                onDeleteConfirmed = { confirmed++ },
                            ),
                    )
                }
            }
        }
    }

    @Composable
    private fun WithFontScale(
        scale: Float,
        content: @Composable () -> Unit,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale), content = content)
    }

    @Test
    fun `the header names the exercise, the moment and the durations`() {
        show()
        compose.onNodeWithTag(DetailTags.HEADER).assertTextContains("Constraint Box", substring = true)
        compose.onNodeWithTag(DetailTags.HEADER).assertTextContains("14 novembre, 22 h 13", substring = true)
        compose.onNodeWithTag(DetailTags.HEADER).assertTextContains("5 min + 42 s", substring = true)
    }

    @Test
    fun `used words say so in words, not by color alone`() {
        show()
        compose.onNodeWithTag(DetailTags.draw(0)).assertTextContains("utilisé", substring = true)
        compose.onNodeWithTag(DetailTags.draw(1)).assertTextContains("non utilisé", substring = true)
        compose.onNodeWithTag(DetailTags.draw(2)).assertTextEquals("Auberge de relais")
    }

    @Test
    fun `the typed text is shown and cannot be edited`() {
        show()
        compose.onNodeWithTag(DetailTags.answer(0)).assertTextContains("Le bois du comptoir", substring = true)
        compose.onNodeWithTag(DetailTags.answer(0)).performClick()
        compose.onNodeWithText("Le bois du comptoir est tiède sous la paume.").assertIsDisplayed()
    }

    @Test
    fun `the current level is selected and touching another one reports it`() {
        show()
        compose.onNodeWithTag(DetailTags.level(3)).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(DetailTags.level(1)).performScrollTo().performClick()
        assertEquals(listOf(1), levels)
    }

    @Test
    fun `touching the gem toggle reports it`() {
        show()
        compose.onNodeWithTag(DetailTags.GEM).performScrollTo().performClick()
        assertEquals(1, gems)
    }

    @Test
    fun `no rating is said discreetly`() {
        show(SessionDetailUiState(isLoading = false, detail = detail.copy(selfRating = null)))
        compose.onNodeWithTag(DetailTags.NO_RATING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `an unfinished session is flagged and its evaluation is locked`() {
        show(SessionDetailUiState(isLoading = false, detail = detail.copy(unfinished = true)))
        compose.onNodeWithTag(DetailTags.UNFINISHED).assertIsDisplayed()
        compose.onNodeWithTag(DetailTags.level(1)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag(DetailTags.GEM).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `redoing opens a new draw of the same exercise`() {
        show()
        compose.onNodeWithTag(DetailTags.REDO).performScrollTo().performClick()
        assertEquals(listOf(ExerciseId.CONSTRAINT_BOX), redone)
    }

    @Test
    fun `sharing sends the text`() {
        show()
        compose.onNodeWithTag(DetailTags.SHARE).performClick()
        assertEquals(listOf("texte"), shared)
    }

    @Test
    fun `there is no share action without text`() {
        show(SessionDetailUiState(isLoading = false, detail = detail.copy(shareText = null)))
        compose.onNodeWithTag(DetailTags.SHARE).assertDoesNotExist()
    }

    @Test
    fun `delete lives in the overflow menu and asks for confirmation in the page`() {
        show()
        compose.onNodeWithTag(DetailTags.DELETE_PANEL).assertDoesNotExist()
        compose.onNodeWithTag(DetailTags.MORE).performClick()
        compose.onNodeWithTag(DetailTags.DELETE_ITEM).performClick()
        assertEquals(1, requested)
        assertEquals(0, confirmed)
    }

    @Test
    fun `the confirmation panel confirms or cancels`() {
        show(SessionDetailUiState(isLoading = false, detail = detail, confirmingDelete = true))
        compose.onNodeWithTag(DetailTags.DELETE_PANEL).assertIsDisplayed()
        compose.onNodeWithTag(DetailTags.DELETE_CANCEL).performClick()
        compose.onNodeWithTag(DetailTags.DELETE_CONFIRM).performClick()
        assertEquals(1, cancelled)
        assertEquals(1, confirmed)
    }

    @Test
    fun `a missing session says so and goes back`() {
        show(SessionDetailUiState(isLoading = false))
        compose.onNodeWithTag(DetailTags.MISSING).assertIsDisplayed()
        compose.onNodeWithTag(DetailTags.BACK).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun `nothing is shown while loading`() {
        show(SessionDetailUiState())
        compose.onNodeWithTag(DetailTags.MISSING).assertDoesNotExist()
        compose.onNodeWithTag(DetailTags.HEADER).assertDoesNotExist()
        assertFalse(shared.isNotEmpty())
    }

    @Test
    fun `the detail stays reachable at 200 percent in the dark theme`() {
        show(fontScale = 2f, dark = true)
        compose.onNodeWithTag(DetailTags.REDO).performScrollTo().assertIsDisplayed()
    }
}
