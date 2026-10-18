package io.github.qnicoud.dmgym.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.platform.readBoundedText
import io.github.qnicoud.dmgym.ui.icons.DmIcons

/** Types proposés au sélecteur : certains fournisseurs déclarent mal le type, la validation tranche de toute façon. */
private val PICKER_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain")

/** Ce que l'écran d'import peut demander. */
data class ImportActions(
    val onBack: () -> Unit = {},
    val onChooseFile: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onNoticeShown: () -> Unit = {},
)

/** Écran d'import relié à son ViewModel ; le fichier vient du sélecteur système, sans permission. */
@Composable
fun ImportRoute(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.onFileChosen { resolver.readBoundedText(uri) }
        }
    val actions =
        ImportActions(
            onBack = onBack,
            onChooseFile = { picker.launch(PICKER_TYPES) },
            onConfirm = viewModel::confirm,
            onUndo = viewModel::undo,
            onOpenHistory = onOpenHistory,
            onNoticeShown = viewModel::noticeShown,
        )
    ImportScreen(state, actions, modifier)
}

/** Import des sessions (S13) : un seul bouton principal par état, barre d'action fixée en bas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    actions: ImportActions,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val undoneText = stringResource(R.string.import_undone)
    val notice = (state as? ImportUiState.Choose)?.undone == true
    LaunchedEffect(notice) {
        if (notice) {
            snackbar.showSnackbar(undoneText)
            actions.onNoticeShown()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { ImportActionBar(state, actions) },
    ) { padding ->
        Column(
            Modifier
                .padding(
                    padding,
                ).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ImportBody(state)
        }
    }
}
