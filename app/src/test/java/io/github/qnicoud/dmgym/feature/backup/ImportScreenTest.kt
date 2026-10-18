package io.github.qnicoud.dmgym.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.backup.ImportPlan
import io.github.qnicoud.dmgym.core.backup.fullSession
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class ImportScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val log = mutableListOf<String>()
    private val actions =
        ImportActions(
            onBack = { log += "back" },
            onChooseFile = { log += "choose" },
            onConfirm = { log += "confirm" },
            onUndo = { log += "undo" },
            onOpenHistory = { log += "history" },
        )

    private fun show(state: ImportUiState) = compose.setContent { Themed { ImportScreen(state, actions) } }

    @Composable
    private fun Themed(content: @Composable () -> Unit) = DmGymTheme(content = content)

    private val plan =
        ImportPlan(
            toInsert = listOf(fullSession(), fullSession(1_792_220_460_000)),
            alreadyPresent = 3,
            unknownExercise = 2,
            invalid = 1,
            firstEpochDay = 20_743,
            lastEpochDay = 20_750,
        )

    @Test
    fun `choice offers the file picker and a foldable switch procedure`() {
        show(ImportUiState.Choose())

        compose.onNodeWithTag(ImportTags.CHOOSE).assertIsDisplayed().performClick()
        compose.onNodeWithTag(ImportTags.SWITCH).assertIsDisplayed()
        assertEquals(listOf("choose"), log)
    }

    @Test
    fun `preview shows the recap and confirms with the number to add`() {
        show(ImportUiState.Preview(plan))

        compose.onNodeWithTag(ImportTags.PREVIEW).assertIsDisplayed()
        compose.onNodeWithTag(ImportTags.CONFIRM).assertIsDisplayed().performClick()
        compose.onNodeWithTag(ImportTags.CHOOSE_OTHER).performClick()
        assertEquals(listOf("confirm", "choose"), log)
    }

    @Test
    fun `nothing to add has no primary action`() {
        show(ImportUiState.NothingToAdd(found = 3))

        compose.onAllNodesWithTag(ImportTags.CONFIRM).assertCountEquals(0)
        compose.onNodeWithTag(ImportTags.CLOSE).performClick()
        assertEquals(listOf("back"), log)
    }

    @Test
    fun `result offers history, undo and done`() {
        show(ImportUiState.Done(added = 2, alreadyPresent = 3, canUndo = true))

        compose.onNodeWithTag(ImportTags.RESULT).assertIsDisplayed()
        compose.onNodeWithTag(ImportTags.UNDO).performClick()
        compose.onNodeWithTag(ImportTags.HISTORY).performClick()
        compose.onNodeWithTag(ImportTags.FINISHED).performClick()
        assertEquals(listOf("undo", "history", "back"), log)
    }

    @Test
    fun `undo is hidden when nothing was added`() {
        show(ImportUiState.Done(added = 0, alreadyPresent = 3, canUndo = false))

        compose.onAllNodesWithTag(ImportTags.UNDO).assertCountEquals(0)
    }

    @Test
    fun `an error shows its title and offers another file`() {
        show(ImportUiState.Failed(ImportError.Incomplete))

        compose.onNodeWithTag(ImportTags.ERROR_TITLE).assertIsDisplayed()
        compose.onNodeWithTag(ImportTags.CHOOSE_OTHER).performClick()
        assertEquals(listOf("choose"), log)
    }

    @Test
    fun `reading shows progress`() {
        show(ImportUiState.Reading)

        compose.onNodeWithTag(ImportTags.READING).assertIsDisplayed()
    }
}
