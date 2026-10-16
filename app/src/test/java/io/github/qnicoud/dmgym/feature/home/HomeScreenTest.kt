package io.github.qnicoud.dmgym.feature.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Exercise
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.app.navigation.Settings
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
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

    private val stack = mutableStateListOf<NavKey>(Home)

    private fun launch() {
        val navigator = AppNavigator(stack)
        compose.setContent {
            DmGymTheme {
                AppNavDisplay(backStack = stack, navigator = navigator, container = FakeAppContainer())
            }
        }
    }

    @Test
    fun `shows the seven exercise cards`() {
        launch()
        ExerciseId.entries.forEach { id ->
            compose.onNodeWithTag(HomeTags.card(id)).assertIsDisplayed()
        }
    }

    @Test
    fun `shows the welcome message of the empty state`() {
        launch()
        compose.onNodeWithTag(HomeTags.WELCOME).assertIsDisplayed()
    }

    @Test
    fun `clicking a card opens the exercise route`() {
        launch()
        compose.onNodeWithTag(HomeTags.card(ExerciseId.CONSTRAINT_BOX)).performClick()
        compose.waitForIdle()
        assertEquals(Exercise("constraint_box"), stack.last())
    }

    @Test
    fun `clicking the gear opens settings`() {
        launch()
        compose.onNodeWithTag(HomeTags.SETTINGS_BUTTON).performClick()
        compose.waitForIdle()
        assertEquals(Settings, stack.last())
    }
}
