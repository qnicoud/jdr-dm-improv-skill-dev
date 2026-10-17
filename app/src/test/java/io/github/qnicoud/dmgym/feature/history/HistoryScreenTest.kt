package io.github.qnicoud.dmgym.feature.history

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class HistoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<Long>()
    private val selected = mutableListOf<HistoryFilter>()
    private var cleared = 0
    private var chosen = 0

    private fun item(
        id: Long,
        exercise: ExerciseId = ExerciseId.CONSTRAINT_BOX,
        preview: String? = "Le bois du comptoir est tiède",
        gem: Boolean = false,
        rating: Int? = null,
        unfinished: Boolean = false,
    ) = SessionItem(id, exercise, 1_700_000_000_000, "UTC", preview, gem, rating, unfinished)

    private val populated =
        HistoryUiState(
            isLoading = false,
            totalCount = 3,
            groups =
                listOf(
                    DayGroup(DayLabel.Today, listOf(item(1, gem = true, rating = 2))),
                    DayGroup(
                        DayLabel.Yesterday,
                        listOf(item(2, rating = 3), item(3, unfinished = true, preview = null)),
                    ),
                    DayGroup(DayLabel.Date(20_000), listOf(item(4, ExerciseId.CRAZY_EIGHTS))),
                ),
        )

    private fun show(
        state: HistoryUiState,
        fontScale: Float = 1f,
        dark: Boolean = false,
    ) {
        compose.setContent {
            DmGymTheme(darkTheme = dark) {
                WithFontScale(fontScale) {
                    HistoryScreen(
                        state = state,
                        onFilterSelected = { selected += it },
                        onClearFilter = { cleared++ },
                        onOpenSession = { opened += it },
                        onChooseExercise = { chosen++ },
                        onSettingsClick = {},
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
    fun `the empty journal shows one sentence and one action, without filters`() {
        show(HistoryUiState(isLoading = false))
        compose.onNodeWithTag(HistoryTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText("Ton carnet est vide.").assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.CHIPS).assertDoesNotExist()
        compose.onNodeWithTag(HistoryTags.EMPTY_ACTION).performClick()
        assertEquals(1, chosen)
    }

    @Test
    fun `nothing is shown while loading`() {
        show(HistoryUiState())
        compose.onNodeWithTag(HistoryTags.EMPTY).assertDoesNotExist()
        compose.onNodeWithTag(HistoryTags.FILTER_EMPTY).assertDoesNotExist()
    }

    @Test
    fun `the list is grouped by day with its headers`() {
        show(populated)
        compose.onNodeWithText("Aujourd’hui").assertIsDisplayed()
        compose.onNodeWithText("Hier").assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.card(1)).assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.card(2)).assertIsDisplayed()
    }

    @Test
    fun `a card shows the exercise, the time, the preview and the badges as text`() {
        show(populated)
        compose.onNodeWithTag(HistoryTags.card(1)).assertTextContains("Constraint Box", substring = true)
        compose.onNodeWithTag(HistoryTags.card(1)).assertTextContains("22 h 13", substring = true)
        compose.onNodeWithTag(HistoryTags.card(1)).assertTextContains("Le bois du comptoir", substring = true)
        compose.onNodeWithTag(HistoryTags.card(1)).assertTextContains("Solide", substring = true)
        compose.onNodeWithTag(HistoryTags.card(1)).assertTextContains("Bijou", substring = true)
        compose.onNodeWithTag(HistoryTags.card(2)).assertTextContains("Prêt pour la table", substring = true)
    }

    @Test
    fun `an unfinished session is marked with a word`() {
        show(populated)
        compose.onNodeWithTag(HistoryTags.card(3)).assertTextContains("Inachevée", substring = true)
    }

    @Test
    fun `touching a card opens its detail`() {
        show(populated)
        compose.onNodeWithTag(HistoryTags.card(2)).performClick()
        assertEquals(listOf(2L), opened)
    }

    @Test
    fun `the filters offer all, gems and one chip per exercise, with the current one selected`() {
        show(populated.copy(filter = HistoryFilter.Gems))
        compose.onNodeWithTag(HistoryTags.chip(HistoryFilter.All)).assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.chip(HistoryFilter.Gems)).assertIsSelected()
        ExerciseId.ALL.forEach {
            compose.onNodeWithTag(HistoryTags.CHIPS).performScrollToNode(hasText(exerciseName(it)))
        }
    }

    @Test
    fun `touching a chip selects its filter`() {
        show(populated)
        compose.onNodeWithTag(HistoryTags.chip(HistoryFilter.exercise(ExerciseId.CRAZY_EIGHTS))).performClick()
        assertEquals(listOf(HistoryFilter.exercise(ExerciseId.CRAZY_EIGHTS)), selected)
    }

    @Test
    fun `a filter without result says so and offers to clear it`() {
        show(
            HistoryUiState(
                isLoading = false,
                filter = HistoryFilter.exercise(ExerciseId.ONE_PAGE),
                totalCount = 5,
            ),
        )
        compose.onNodeWithText("Aucune session pour ce filtre.").assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.EMPTY).assertDoesNotExist()
        compose.onNodeWithTag(HistoryTags.CLEAR_FILTER).performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun `the list stays readable at 200 percent in the dark theme`() {
        show(populated, fontScale = 2f, dark = true)
        compose.onNodeWithTag(HistoryTags.card(1)).assertIsDisplayed()
    }

    private fun exerciseName(id: ExerciseId): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id.nameRes)
}
