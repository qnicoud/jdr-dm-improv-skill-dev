package io.github.qnicoud.dmgym.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.exercise.shortDuration
import io.github.qnicoud.dmgym.platform.shareText
import io.github.qnicoud.dmgym.ui.components.currentLocale
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.icons.HistoryIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.launch

object DetailTags {
    const val HEADER = "detail_header"
    const val BACK = "detail_back"
    const val SHARE = "detail_share"
    const val MORE = "detail_more"
    const val DELETE_ITEM = "detail_delete_item"
    const val DELETE_PANEL = "detail_delete_panel"
    const val DELETE_CONFIRM = "detail_delete_confirm"
    const val DELETE_CANCEL = "detail_delete_cancel"
    const val DRAW_CARD = "detail_draw_card"
    const val GEM = "detail_gem"
    const val FIRST_PICK = "detail_first_pick"
    const val NO_RATING = "detail_no_rating"
    const val UNFINISHED = "detail_unfinished"
    const val REDO = "detail_redo"
    const val MISSING = "detail_missing"

    fun draw(index: Int) = "detail_draw_$index"

    fun answer(index: Int) = "detail_answer_$index"

    fun level(value: Int) = "detail_level_$value"
}

/** Intentions de l'écran de détail, regroupées pour garder les signatures lisibles. */
class DetailActions(
    val onBack: () -> Unit,
    val onShare: (String) -> Unit,
    val onLevelSelected: (Int) -> Unit,
    val onGemToggled: () -> Unit,
    val onRedo: (ExerciseId) -> Unit,
    val onDeleteRequested: () -> Unit,
    val onDeleteCancelled: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
)

/** Point d'entrée de la route SessionDetail : branche le ViewModel, la feuille de partage et les erreurs d'écriture. */
@Composable
fun SessionDetailRoute(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
    onRedo: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveFailed = stringResource(R.string.detail_save_failed)
    val deleteFailed = stringResource(R.string.detail_delete_failed)
    val shareFailed = stringResource(R.string.detail_share_failed)
    val chooser = stringResource(R.string.detail_share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            snackbar.showSnackbar(
                when (it) {
                    DetailEvent.SaveFailed -> saveFailed
                    DetailEvent.DeleteFailed -> deleteFailed
                },
            )
        }
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    SessionDetailScreen(
        state = state,
        actions =
            DetailActions(
                onBack = onBack,
                onShare = { text ->
                    if (!context.shareText(text, chooser)) scope.launch { snackbar.showSnackbar(shareFailed) }
                },
                onLevelSelected = viewModel::onLevelSelected,
                onGemToggled = viewModel::onGemToggled,
                onRedo = onRedo,
                onDeleteRequested = viewModel::onDeleteRequested,
                onDeleteCancelled = viewModel::onDeleteCancelled,
                onDeleteConfirmed = viewModel::onDeleteConfirmed,
            ),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    )
}

/**
 * Détail d'une session (S09) : tirage, texte en lecture seule, niveau et bijou modifiables, « Refaire avec un
 * nouveau tirage ». La suppression se confirme dans la page, sans dialogue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    actions: DetailActions,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    val detail = state.detail
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title =
                        detail?.exerciseId?.let { stringResource(it.nameRes) }
                            ?: stringResource(R.string.session_detail_title)
                    Text(title, Modifier.semantics { heading() })
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, modifier = Modifier.testTag(DetailTags.BACK)) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = { if (detail != null) DetailActionsMenu(detail, actions) },
            )
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        when {
            detail != null -> DetailBody(detail, state.confirmingDelete, actions, Modifier.padding(padding))
            state.isMissing -> Missing(Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailActionsMenu(
    detail: SessionDetailUi,
    actions: DetailActions,
) {
    var open by remember { mutableStateOf(false) }
    detail.shareText?.let { text ->
        IconButton(onClick = { actions.onShare(text) }, modifier = Modifier.testTag(DetailTags.SHARE)) {
            Icon(HistoryIcons.Share, contentDescription = stringResource(R.string.detail_share))
        }
    }
    IconButton(onClick = { open = true }, modifier = Modifier.testTag(DetailTags.MORE)) {
        Icon(HistoryIcons.More, contentDescription = stringResource(R.string.detail_more))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    HistoryIcons.Trash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                open = false
                actions.onDeleteRequested()
            },
            modifier = Modifier.testTag(DetailTags.DELETE_ITEM),
        )
    }
}

@Composable
private fun DetailBody(
    detail: SessionDetailUi,
    confirmingDelete: Boolean,
    actions: DetailActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (confirmingDelete) DeletePanel(actions.onDeleteCancelled, actions.onDeleteConfirmed)
        Header(detail)
        if (detail.unfinished) {
            Text(
                stringResource(R.string.detail_unfinished_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(DetailTags.UNFINISHED),
            )
        }
        DrawSection(detail.draws)
        AnswersSection(detail.answers)
        EvaluationSection(detail, actions.onLevelSelected, actions.onGemToggled)
        RubricSection(detail.rubric)
        FirstPickSection(detail.firstPick, detail.firstPickTitle ?: R.string.detail_first_pick_title)
        RedoButton({ actions.onRedo(detail.exerciseId) })
    }
}

@Composable
private fun Header(detail: SessionDetailUi) {
    val active = shortDuration(detail.activeSec * MS_PER_SEC)
    val duration =
        if (detail.overtimeSec > 0) {
            stringResource(R.string.detail_duration_overtime, active, shortDuration(detail.overtimeSec * MS_PER_SEC))
        } else {
            active
        }
    val name = stringResource(detail.exerciseId.nameRes)
    val moment = dateTime(detail.startedAtEpochMillis, detail.zoneId, currentLocale())
    Text(
        text = "$name · $moment · $duration",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(DetailTags.HEADER),
    )
}

/** Confirmation dans la page : la suppression est définitive, donc on la demande une fois, sans dialogue modal. */
@Composable
private fun DeletePanel(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(DetailTags.DELETE_PANEL),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.detail_delete_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.detail_delete_body), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(DetailTags.DELETE_CANCEL),
                ) { Text(stringResource(R.string.detail_delete_cancel)) }
                Button(
                    onClick = onConfirm,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(DetailTags.DELETE_CONFIRM),
                ) { Text(stringResource(R.string.detail_delete)) }
            }
        }
    }
}

@Composable
private fun Missing(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.detail_not_found),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(16.dp).testTag(DetailTags.MISSING),
    )
}

private const val MS_PER_SEC = 1_000L

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SessionDetailPreview() {
    DmGymTheme {
        SessionDetailScreen(
            state =
                SessionDetailUiState(
                    isLoading = false,
                    detail =
                        SessionDetailUi(
                            sessionId = 1,
                            exerciseId = ExerciseId.CONSTRAINT_BOX,
                            startedAtEpochMillis = 1_700_000_000_000,
                            zoneId = "UTC",
                            activeSec = 300,
                            overtimeSec = 42,
                            unfinished = false,
                            draws = listOf(DrawLine("tiède", true), DrawLine("moisi", false)),
                            answers = listOf(AnswerLine(null, 1, null, "Le bois du comptoir est tiède.", false, null)),
                            rubric = emptyList(),
                            firstPick = null,
                            selfRating = 2,
                            isGem = true,
                            shareText = "Le bois du comptoir est tiède.",
                        ),
                ),
            actions = DetailActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
