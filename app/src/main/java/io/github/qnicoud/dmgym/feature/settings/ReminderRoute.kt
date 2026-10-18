package io.github.qnicoud.dmgym.feature.settings

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Section Rappels reliée à son ViewModel. La permission `POST_NOTIFICATIONS` n'est demandée qu'ici, quand
 * l'interrupteur passe à « activé » (ADR 0014 §3) ; un refus laisse l'interrupteur désactivé.
 */
@Composable
fun ReminderRoute(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onPermissionResult)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    if (state.isLoading) return
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    ReminderSection(
        state = state,
        actions =
            ReminderActions(
                onSwitch = viewModel::onSwitch,
                onTime = viewModel::setTime,
                onToggleDay = viewModel::toggleDay,
                onResume = viewModel::resume,
                onSendTest = viewModel::sendTest,
                onOpenAndroidSettings = viewModel::openAndroidSettings,
                onOpenChannelSettings = viewModel::openChannelSettings,
            ),
        is24Hour = is24Hour,
        modifier = modifier,
    )
}
