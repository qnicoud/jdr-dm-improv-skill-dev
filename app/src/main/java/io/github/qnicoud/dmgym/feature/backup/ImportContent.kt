package io.github.qnicoud.dmgym.feature.backup

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.backup.ImportPlan
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object ImportTags {
    const val CHOOSE = "import_choose"
    const val CHOOSE_OTHER = "import_choose_other"
    const val CONFIRM = "import_confirm"
    const val UNDO = "import_undo"
    const val HISTORY = "import_history"
    const val FINISHED = "import_finished"
    const val CLOSE = "import_close"
    const val ERROR_TITLE = "import_error_title"
    const val PREVIEW = "import_preview"
    const val READING = "import_reading"
    const val RESULT = "import_result"
    const val SWITCH = "import_switch"
}

private val MinButtonHeight = 48.dp

/** Contenu de chaque état (A à E, ordre de focus : titre, texte, bouton). */
@Composable
internal fun ImportBody(state: ImportUiState) {
    when (state) {
        is ImportUiState.Choose -> {
            ChooseBody()
        }

        ImportUiState.Reading -> {
            ProgressBody(R.string.import_reading, ImportTags.READING)
        }

        ImportUiState.Importing -> {
            ProgressBody(R.string.import_importing, ImportTags.READING)
        }

        is ImportUiState.Preview -> {
            PreviewBody(state.plan)
        }

        is ImportUiState.NothingToAdd -> {
            Text(
                text =
                    stringResource(
                        if (state.found ==
                            0
                        ) {
                            R.string.import_empty_file
                        } else {
                            R.string.import_nothing_to_add
                        },
                    ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(ImportTags.PREVIEW),
            )
        }

        is ImportUiState.Done -> {
            DoneBody(state)
        }

        is ImportUiState.Failed -> {
            ErrorBody(state.error)
        }
    }
}

@Composable
private fun ChooseBody() {
    var expanded by remember { mutableStateOf(false) }
    Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyLarge)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp)
                .testTag(ImportTags.SWITCH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.import_switch_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.import_switch_warning), style = MaterialTheme.typography.titleSmall)
            listOf(
                R.string.import_switch_step1,
                R.string.import_switch_step2,
                R.string.import_switch_step3,
                R.string.import_switch_step4,
            ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ProgressBody(
    @StringRes text: Int,
    tag: String,
) {
    Column(
        Modifier.testTag(tag).semantics {
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PreviewBody(plan: ImportPlan) {
    val spoken =
        pluralStringResource(
            R.plurals.import_preview_spoken,
            plan.found,
            plan.found,
            plan.toInsert.size,
            plan.alreadyPresent,
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier =
                Modifier.fillMaxWidth().testTag(ImportTags.PREVIEW).semantics(mergeDescendants = true) {
                    contentDescription =
                        spoken
                },
        ) {
            Column(Modifier.padding(16.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    pluralStringResource(R.plurals.import_found, plan.found, plan.found),
                    style = MaterialTheme.typography.titleLarge,
                )
                rangeText(plan)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                CountLine(
                    DmIcons.Plus,
                    pluralStringResource(R.plurals.import_will_add, plan.toInsert.size, plan.toInsert.size),
                )
                CountLine(
                    DmIcons.Check,
                    pluralStringResource(R.plurals.import_already, plan.alreadyPresent, plan.alreadyPresent),
                )
                if (plan.unknownExercise > 0) {
                    Text(
                        pluralStringResource(R.plurals.import_unknown, plan.unknownExercise, plan.unknownExercise),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Text(stringResource(R.string.import_preview_reassure), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CountLine(
    icon: ImageVector,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun rangeText(plan: ImportPlan): String? {
    val first = plan.firstEpochDay ?: return null
    val last = plan.lastEpochDay ?: return null
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val from = LocalDate.ofEpochDay(first).format(format)
    return if (first == last) {
        stringResource(R.string.import_range_single, from)
    } else {
        stringResource(R.string.import_range, from, LocalDate.ofEpochDay(last).format(format))
    }
}

@Composable
private fun DoneBody(state: ImportUiState.Done) {
    Column(
        Modifier.testTag(ImportTags.RESULT).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            DmIcons.Check,
            contentDescription = stringResource(R.string.import_done_check),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            pluralStringResource(R.plurals.import_done_added, state.added, state.added),
            style = MaterialTheme.typography.titleLarge,
        )
        if (state.alreadyPresent > 0) {
            Text(
                pluralStringResource(R.plurals.import_done_already, state.alreadyPresent, state.alreadyPresent),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(stringResource(R.string.import_done_uptodate), style = MaterialTheme.typography.bodyLarge)
        if (state.undoFailed) Text(stringResource(R.string.import_undo_failed), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ErrorBody(error: ImportError) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(error) { focus.requestFocus() }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(error.titleRes),
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier
                        .focusRequester(focus)
                        .focusable()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag(ImportTags.ERROR_TITLE),
            )
            Text(stringResource(error.textRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Barre d'action fixée en bas : un bouton principal au plus, les autres en texte. */
@Composable
internal fun ImportActionBar(
    state: ImportUiState,
    actions: ImportActions,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (state) {
            is ImportUiState.Choose -> {
                Primary(R.string.import_choose_file, ImportTags.CHOOSE, actions.onChooseFile)
            }

            is ImportUiState.Preview -> {
                val count = state.plan.toInsert.size
                Button(
                    onClick = actions.onConfirm,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinButtonHeight).testTag(ImportTags.CONFIRM),
                ) { Text(pluralStringResource(R.plurals.import_confirm, count, count)) }
                Secondary(R.string.import_choose_other, ImportTags.CHOOSE_OTHER, actions.onChooseFile)
            }

            is ImportUiState.NothingToAdd -> {
                Secondary(R.string.import_choose_other, ImportTags.CHOOSE_OTHER, actions.onChooseFile)
                Secondary(R.string.import_close, ImportTags.CLOSE, actions.onBack)
            }

            is ImportUiState.Done -> {
                Primary(R.string.import_see_history, ImportTags.HISTORY, actions.onOpenHistory)
                if (state.canUndo) Secondary(R.string.import_undo, ImportTags.UNDO, actions.onUndo)
                Secondary(R.string.import_finished, ImportTags.FINISHED, actions.onBack)
            }

            is ImportUiState.Failed -> {
                Primary(R.string.import_choose_other, ImportTags.CHOOSE_OTHER, actions.onChooseFile)
            }

            ImportUiState.Reading, ImportUiState.Importing -> {
                Unit
            }
        }
    }
}

@Composable
private fun Primary(
    @StringRes label: Int,
    tag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick,
        Modifier.fillMaxWidth().heightIn(min = MinButtonHeight).testTag(tag),
    ) { Text(stringResource(label)) }
}

@Composable
private fun Secondary(
    @StringRes label: Int,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = MinButtonHeight).testTag(tag)) {
        Text(stringResource(label))
    }
}

@get:StringRes
private val ImportError.titleRes: Int
    get() =
        when (this) {
            ImportError.NotABackup -> R.string.import_error_not_backup_title
            ImportError.ReadableExport -> R.string.import_error_readable_title
            ImportError.Incomplete -> R.string.import_error_incomplete_title
            ImportError.NewerVersion -> R.string.import_error_newer_title
            ImportError.Unreadable -> R.string.import_error_unreadable_title
            ImportError.TooLarge -> R.string.import_error_too_large_title
            ImportError.WriteFailed -> R.string.import_error_write_title
        }

@get:StringRes
private val ImportError.textRes: Int
    get() =
        when (this) {
            ImportError.NotABackup -> R.string.import_error_not_backup_text
            ImportError.ReadableExport -> R.string.import_error_readable_text
            ImportError.Incomplete -> R.string.import_error_incomplete_text
            ImportError.NewerVersion -> R.string.import_error_newer_text
            ImportError.Unreadable -> R.string.import_error_unreadable_text
            ImportError.TooLarge -> R.string.import_error_too_large_text
            ImportError.WriteFailed -> R.string.import_error_write_text
        }
