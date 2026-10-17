package io.github.qnicoud.dmgym.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** L'audit doit détecter les défauts qu'il prétend détecter : sinon un résultat vert ne prouve rien. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class AccessibilityAuditSelfTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a 24 dp unlabelled clickable has an extended touch area but a small visual size and no label`() {
        compose.setContent { Box(Modifier.size(24.dp).clickable {}) }
        val audit = AccessibilityAudit(compose)
        assertTrue("la zone tactile est étendue à 48 dp", audit.smallTargets().isEmpty())
        assertEquals(1, audit.smallVisualTargets().size)
        assertEquals(1, audit.unlabelledClickables().size)
    }

    @Test
    fun `a 48 dp labelled clickable is accepted`() {
        compose.setContent { Box(Modifier.size(48.dp).clickable {}) { Text("Valider") } }
        val audit = AccessibilityAudit(compose)
        assertTrue(audit.smallTargets().isEmpty())
        assertTrue(audit.unlabelledClickables().isEmpty())
    }

    @Test
    fun `an ellipsized text is reported as clipped`() {
        compose.setContent {
            Text(
                "Un texte beaucoup trop long pour tenir sur une seule ligne étroite",
                Modifier.width(40.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        assertEquals(1, AccessibilityAudit(compose).clippedTexts().size)
    }

    @Test
    fun `a screen without heading is counted as having none`() {
        compose.setContent { Text("Titre") }
        assertEquals(0, AccessibilityAudit(compose).headingCount())
    }

    @Test
    fun `a text field is reported only when it has no label`() {
        compose.setContent {
            OutlinedTextField(value = "abc", onValueChange = {}, label = { Text("Idée 1 sur 8") })
            OutlinedTextField(value = "def", onValueChange = {})
        }
        val problems = AccessibilityAudit(compose).unlabelledTextFields()
        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("def") || problems.single().contains("champ"))
    }

    @Test
    fun `a toggle without role is reported`() {
        compose.setContent { Box(Modifier.size(48.dp).semantics { toggleableState = ToggleableState.On }) }
        assertEquals(1, AccessibilityAudit(compose).statefulWithoutRole().size)
    }
}
