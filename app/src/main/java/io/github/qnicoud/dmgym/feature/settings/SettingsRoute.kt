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
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.platform.writeText

/**
 * Écran Réglages relié à son ViewModel. L'export passe par le sélecteur de fichiers du système
 * (`ACTION_CREATE_DOCUMENT`) : l'utilisateur choisit où le fichier est écrit, et l'app n'a besoin d'aucune permission.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    reminder: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val resources = LocalResources.current
    val labels = remember(resources) { ExportLabels.from(ExerciseRegistry.Default, resources::getString) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) viewModel.export(labels) { resolver.writeText(uri, it) }
        }
    val fileName = stringResource(R.string.settings_export_file_name)
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
