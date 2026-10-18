package io.github.qnicoud.dmgym.accessibility

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.core.exercise.definitions.ConstraintBox
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.CrazyEights
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.FailingForward
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers
import io.github.qnicoud.dmgym.core.exercise.definitions.ThreeClues
import io.github.qnicoud.dmgym.core.exercise.definitions.YesAnd
import io.github.qnicoud.dmgym.feature.backup.ImportActions
import io.github.qnicoud.dmgym.feature.backup.ImportScreen
import io.github.qnicoud.dmgym.feature.backup.ImportUiState
import io.github.qnicoud.dmgym.feature.settings.ReminderActions
import io.github.qnicoud.dmgym.feature.settings.ReminderSection
import io.github.qnicoud.dmgym.feature.settings.ReminderUiState
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Pseudo-locale `en-XA` (textes allongés d'environ 30 % et accentués) à 200 % de police : révèle les textes coupés
 * que le français seul ne montre pas. Le premier test garantit que la pseudo-locale est bien chargée.
 */
@Config(sdk = [34], qualifiers = "en-rXA-w360dp-h10000dp")
class PseudoLocaleAuditTest : AuditTestBase() {
    @Test
    fun `pseudo locale is really loaded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pseudo = context.getString(R.string.import_title)
        println("PSEUDO import_title = $pseudo")
        assertNotEquals("Importer des sessions", pseudo)
    }

    @Test
    fun `import error in pseudo locale passes the audit`() {
        show { ImportScreen(ImportUiState.Failed(ImportError.Incomplete), ImportActions()) }
        audit("import erreur en-XA")
    }

    @Test
    fun `yes and in pseudo locale passes the audit`() = exerciseJourney("Yes And en-XA", YesAnd.definition)

    @Test
    fun `failing forward in pseudo locale passes the audit`() =
        exerciseJourney("Failing Forward en-XA", FailingForward.definition)

    @Test
    fun `crazy eights in pseudo locale passes the audit`() =
        exerciseJourney("Crazy Eights en-XA", CrazyEights.definition)

    @Test
    fun `constraint box in pseudo locale passes the audit`() =
        exerciseJourney("Constraint Box en-XA", ConstraintBox.definition)

    @Test
    fun `connect the story in pseudo locale passes the audit`() =
        exerciseJourney("Connect the Story en-XA", ConnectStory.definition)

    @Test
    fun `core image in pseudo locale passes the audit`() = exerciseJourney("Core Image en-XA", CoreImage.definition)

    @Test
    fun `status numbers in pseudo locale passes the audit`() =
        exerciseJourney("Status Numbers en-XA", StatusNumbers.definition)

    @Test
    fun `decision point in pseudo locale passes the audit`() =
        exerciseJourney("Decision Point en-XA", DecisionPoint.definition)

    @Test
    fun `one-page in pseudo locale passes the audit`() = exerciseJourney("One-Page en-XA", OnePage.definition)

    @Test
    fun `three clues in pseudo locale passes the audit`() = exerciseJourney("Three Clues en-XA", ThreeClues.definition)

    @Test
    fun `reminders in pseudo locale pass the audit`() {
        show {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReminderSection(
                    ReminderUiState(isLoading = false, enabled = true, paused = true, blocked = true),
                    ReminderActions(),
                    is24Hour = true,
                )
            }
        }
        audit("rappels en-XA", requireHeading = false)
    }
}
