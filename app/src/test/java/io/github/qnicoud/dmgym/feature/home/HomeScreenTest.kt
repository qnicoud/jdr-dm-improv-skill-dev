package io.github.qnicoud.dmgym.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Exercise
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.app.navigation.Settings
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val stack = NavBackStack<NavKey>(Home)
    private val sessionCount = MutableStateFlow(0)

    private fun launch(
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        val navigator = AppNavigator(stack)
        val container = FakeAppContainer(historyRepository = FakeHistoryRepository(sessionCount))
        compose.setContent {
            DmGymTheme(darkTheme = dark) {
                WithFontScale(fontScale) { AppNavDisplay(navigator = navigator, container = container) }
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
    fun `shows the seven exercise cards`() {
        launch()
        ExerciseId.ALL.forEach { compose.onNodeWithTag(HomeTags.card(it)).assertIsDisplayed() }
    }

    @Test
    fun `shows the welcome message of the empty state`() {
        launch()
        compose.onNodeWithTag(HomeTags.WELCOME).assertIsDisplayed()
    }

    @Test
    fun `hides the welcome message when sessions exist`() {
        sessionCount.value = 5
        launch()
        compose.onNodeWithTag(HomeTags.WELCOME).assertDoesNotExist()
    }

    @Test
    fun `clicking a card opens the exercise route`() {
        launch()
        compose.onNodeWithTag(HomeTags.card(ExerciseId.CONSTRAINT_BOX)).performClick()
        compose.waitForIdle()
        assertEquals(Exercise(ExerciseId.CONSTRAINT_BOX), stack.last())
    }

    @Test
    fun `clicking the gear opens settings`() {
        launch()
        compose.onNodeWithTag(HomeTags.SETTINGS_BUTTON).performClick()
        compose.waitForIdle()
        assertEquals(Settings, stack.last())
    }

    @Test
    fun `renders in dark theme`() {
        launch(dark = true)
        ExerciseId.ALL.forEach { compose.onNodeWithTag(HomeTags.card(it)).assertIsDisplayed() }
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp")
    fun `stays usable at 200 percent font on a 640dp screen`() {
        launch(fontScale = 2f)
        compose.onNodeWithTag(HomeTags.card(ExerciseId.CRAZY_EIGHTS)).assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(HomeTags.card(ExerciseId.ONE_PAGE)))
        compose.onNodeWithTag(HomeTags.card(ExerciseId.ONE_PAGE)).assertIsDisplayed()
        compose.onNodeWithTag(HomeTags.card(ExerciseId.ONE_PAGE)).performClick()
        compose.waitForIdle()
        assertEquals(Exercise(ExerciseId.ONE_PAGE), stack.last())
    }
}
