package io.github.qnicoud.dmgym.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.backup.ImportPlan
import io.github.qnicoud.dmgym.core.backup.fullSession
import io.github.qnicoud.dmgym.feature.backup.ImportActions
import io.github.qnicoud.dmgym.feature.backup.ImportScreen
import io.github.qnicoud.dmgym.feature.backup.ImportUiState
import io.github.qnicoud.dmgym.feature.settings.ReminderActions
import io.github.qnicoud.dmgym.feature.settings.ReminderSection
import io.github.qnicoud.dmgym.feature.settings.ReminderUiState
import org.junit.Test

/** Audit v1.1 : section Rappels des Réglages et écran d'import S13, dans chaque état, à 1x et 2x. */
class AccessibilityAuditV11Test : AuditTestBase() {
    private fun reminder(state: ReminderUiState) {
        show {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReminderSection(state, ReminderActions(), is24Hour = true)
            }
        }
    }

    @Test
    fun `reminders off pass the audit`() {
        reminder(ReminderUiState(isLoading = false))
        audit("rappels désactivés", requireHeading = false)
    }

    @Test
    fun `reminders on pass the audit`() {
        reminder(ReminderUiState(isLoading = false, enabled = true))
        audit("rappels activés", requireHeading = false)
    }

    @Test
    fun `reminders paused, blocked and denied pass the audit`() {
        reminder(
            ReminderUiState(
                isLoading = false,
                enabled = true,
                paused = true,
                blocked = true,
                permissionDenied = true,
                keepOneDayHint = true,
            ),
        )
        audit("rappels pause, bloqués, refusés", requireHeading = false)
    }

    private var importState by mutableStateOf<ImportUiState>(ImportUiState.Choose())
    private var importShown = false

    /** Affiche l'état ; les appels suivants changent l'état sans rappeler `setContent`. */
    private fun import(state: ImportUiState) {
        importState = state
        if (!importShown) {
            importShown = true
            show { ImportScreen(importState, ImportActions()) }
        }
        compose.waitForIdle()
    }

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
    fun `import choice passes the audit`() {
        import(ImportUiState.Choose())
        audit("import choix")
    }

    @Test
    fun `import preview passes the audit`() {
        import(ImportUiState.Preview(plan))
        audit("import aperçu")
    }

    @Test
    fun `import progress and nothing to add pass the audit`() {
        import(ImportUiState.Reading)
        audit("import lecture")
        import(ImportUiState.NothingToAdd(found = 3))
        audit("import rien à ajouter")
    }

    @Test
    fun `import result passes the audit`() {
        import(ImportUiState.Done(added = 2, alreadyPresent = 3, canUndo = true))
        audit("import résultat")
    }

    @Test
    fun `every import error passes the audit`() {
        ImportError.entries.forEach {
            import(ImportUiState.Failed(it))
            audit("import erreur $it")
        }
    }
}
