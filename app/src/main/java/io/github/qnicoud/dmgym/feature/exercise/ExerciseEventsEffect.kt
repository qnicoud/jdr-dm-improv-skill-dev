package io.github.qnicoud.dmgym.feature.exercise

import android.view.HapticFeedbackConstants
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Milestone

/** Événements ponctuels du ViewModel : annonces TalkBack, messages brefs et bascule (annonce et vibration). */
@Composable
internal fun EventsEffect(
    viewModel: ExerciseViewModel,
    snackbarHost: SnackbarHostState,
) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val texts =
        mapOf(
            Milestone.Started to stringResource(R.string.announce_started),
            Milestone.Halfway to stringResource(R.string.announce_halfway),
            Milestone.LastMinute to stringResource(R.string.announce_last_minute),
            Milestone.TimeUp to stringResource(R.string.announce_time_up),
        )
    val resources = LocalResources.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ExerciseEvent.Announce -> {
                        @Suppress("DEPRECATION")
                        view.announceForAccessibility(texts.getValue(event.milestone))
                    }

                    is ExerciseEvent.Snackbar -> {
                        snackbarHost.showSnackbar(resources.getString(event.textRes))
                    }

                    is ExerciseEvent.Reminder -> {
                        Unit
                    }

                    is ExerciseEvent.Shift -> {
                        @Suppress("DEPRECATION")
                        view.announceForAccessibility(resources.getString(R.string.shift_banner, event.text))
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                }
            }
        }
    }
}
