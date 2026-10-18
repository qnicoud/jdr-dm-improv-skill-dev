package io.github.qnicoud.dmgym.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.backup.BackupEncoder
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.platform.appVersionName
import io.github.qnicoud.dmgym.platform.writeBackup
import io.github.qnicoud.dmgym.platform.writeText
import java.time.LocalDate

private const val BACKUP_MIME = "application/json"

/**
 * Écran Réglages relié à son ViewModel. L'export passe par le sélecteur de fichiers du système
 * (`ACTION_CREATE_DOCUMENT`) : l'utilisateur choisit où le fichier est écrit, et l'app n'a besoin d'aucune permission.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenImport: () -> Unit,
    reminder: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val resources = LocalResources.current
    val labels =
        remember(resources) {
            ExportLabels.from(
                ExerciseRegistry.Default,
                resolveItem = { id, a, b -> resources.getString(id, a, b) },
                resolve = resources::getString,
            )
        }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) viewModel.export(labels) { resolver.writeText(uri, it) }
        }
    val fileName = stringResource(R.string.settings_export_file_name)
    val context = LocalContext.current
    val appVersion = remember(context) { context.appVersionName() }
    val backupPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
            if (uri != null) viewModel.backup(appVersion) { resolver.writeBackup(uri, it) }
        }
    val actions =
        SettingsActions(
            onBack = onBack,
            onOpenAbout = onOpenAbout,
            onVibration = viewModel::setVibration,
            onSound = viewModel::setSound,
            onCrazyHaptic = viewModel::setCrazyMinuteHaptic,
            onDailyOnePage = viewModel::setDailyIncludeOnePage,
            onPalette = viewModel::setPalette,
            onVibrationStrength = viewModel::setVibrationStrength,
            onTryVibration = viewModel::tryVibration,
            onThemeMode = viewModel::setThemeMode,
            onReduceAnimations = viewModel::setReduceAnimations,
            onExport = { picker.launch(fileName) },
            onBackup = { backupPicker.launch(BackupEncoder.fileName(LocalDate.now())) },
            onImport = onOpenImport,
            onRequestErase = viewModel::requestErase,
            onCancelErase = viewModel::cancelErase,
            onConfirmErase = viewModel::confirmErase,
            onEraseInput = viewModel::setEraseInput,
            onDecrease = viewModel::decrease,
            onIncrease = viewModel::increase,
            onUntimed = viewModel::setUntimed,
        )
    SettingsScreen(state, actions, reminder, modifier)
}
