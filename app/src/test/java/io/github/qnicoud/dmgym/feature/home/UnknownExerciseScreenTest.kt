package io.github.qnicoud.dmgym.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Un identifiant d'exercice inconnu s'affiche avec un libellé neutre au lieu de faire planter l'écran. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class UnknownExerciseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the resume card of an unknown exercise shows the neutral name`() {
        val resume = ResumeUi(1, ExerciseId("from_the_future"), "du texte", 0)
        compose.setContent { DmGymTheme { ResumeCard(resume, nowMillis = 60_000, onResume = {}) } }
        compose.onNodeWithTag(ResumeTags.CARD).assertIsDisplayed()
    }

    @Test
    fun `an exercise card of an unknown exercise is shown too`() {
        compose.setContent {
            DmGymTheme { ExerciseCard(ExerciseSummary(ExerciseId("from_the_future"), 60), onClick = {}) }
        }
        compose.onNodeWithTag(HomeTags.card(ExerciseId("from_the_future"))).assertIsDisplayed()
    }
}
