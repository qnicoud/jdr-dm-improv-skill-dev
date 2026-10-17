package io.github.qnicoud.dmgym.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** L'étiquette « Préparation » a la forme, la taille et la typographie de l'étiquette de durée. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class ExerciseCardTagTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                DmGymTheme { ExerciseCard(ExerciseSummary(ExerciseId.ONE_PAGE, durationSeconds = null), onClick = {}) }
            }
        }
    }

    @Test
    fun `the preparation tag is as tall as the duration tag`() {
        show()
        val duration = compose.onNodeWithTag(HomeTags.DURATION_TAG, useUnmergedTree = true).assertIsDisplayed()
        val tag = compose.onNodeWithTag(HomeTags.PREPARATION_TAG, useUnmergedTree = true).assertIsDisplayed()
        tag.assertHeightIsEqualTo(duration.getUnclippedBoundsInRoot().height)
    }

    @Test
    fun `the tag stays aligned with the duration tag at 200 percent font`() {
        show(fontScale = 2f)
        val duration = compose.onNodeWithTag(HomeTags.DURATION_TAG, useUnmergedTree = true)
        val tag = compose.onNodeWithTag(HomeTags.PREPARATION_TAG, useUnmergedTree = true)
        assertEquals(duration.getUnclippedBoundsInRoot().height, tag.getUnclippedBoundsInRoot().height)
    }
}
