package io.github.qnicoud.dmgym.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.core.timer.DurationBounds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Durée réglable d'un exercice. [untimed] : « Sans minuteur » ; [seconds] reste la durée à retrouver en le décochant. */
data class DurationRow(
    val id: ExerciseId,
    val seconds: Int,
    val minSeconds: Int,
    val maxSeconds: Int,
    val untimed: Boolean,
) {
    val canDecrease: Boolean get() = seconds > minSeconds
    val canIncrease: Boolean get() = seconds < maxSeconds
}

/** État de l'écran Réglages (S11). [palette] est la palette réellement appliquée (repli sur Parchemin avant l'API 31). */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val durations: List<DurationRow> = emptyList(),
    val vibration: Boolean = true,
    val sound: Boolean = true,
    val crazyMinuteHaptic: Boolean = false,
    val dailyIncludeOnePage: Boolean = false,
    val palette: ThemePalette = ThemePalette.PARCHMENT,
    val palettes: List<ThemePalette> = listOf(ThemePalette.PARCHMENT),
)

/** Réglages (ADR 0013) : chaque changement s'écrit aussitôt dans le dépôt, sans bouton « Enregistrer ». */
class SettingsViewModel(
    private val settings: SettingsRepository,
    sdkInt: Int,
) : ViewModel() {
    private val palettes = ThemePalette.available(sdkInt)

    val uiState: StateFlow<SettingsUiState> =
        settings.settings
            .map { toUiState(it, sdkInt) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setVibration(enabled: Boolean) = change { it.copy(vibrationEnabled = enabled) }

    fun setSound(enabled: Boolean) = change { it.copy(soundEnabled = enabled) }

    fun setCrazyMinuteHaptic(enabled: Boolean) = change { it.copy(crazyEightsMinuteHaptic = enabled) }

    fun setDailyIncludeOnePage(enabled: Boolean) = change { it.copy(dailyIncludeOnePage = enabled) }

    fun setPalette(palette: ThemePalette) {
        if (palette in palettes) change { it.copy(themePalette = palette) }
    }

    fun setUntimed(
        id: ExerciseId,
        untimed: Boolean,
    ) {
        if (id !in ExerciseId.DEFAULT_DURATIONS) return
        change { it.copy(untimed = if (untimed) it.untimed + id else it.untimed - id) }
    }

    fun increase(id: ExerciseId) = step(id) { bounds, current -> bounds.increase(current) }

    fun decrease(id: ExerciseId) = step(id) { bounds, current -> bounds.decrease(current) }

    private fun step(
        id: ExerciseId,
        move: (DurationBounds, Duration) -> Duration,
    ) {
        val default = ExerciseId.DEFAULT_DURATIONS[id] ?: return
        change {
            val bounds = DurationBounds.of(default)
            it.copy(durations = it.durations + (id to move(bounds, it.durations[id] ?: default)))
        }
    }

    private fun change(transform: (Settings) -> Settings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun toUiState(
        current: Settings,
        sdkInt: Int,
    ) = SettingsUiState(
        isLoading = false,
        durations = ExerciseId.ALL.map { durationRow(it, current) },
        vibration = current.vibrationEnabled,
        sound = current.soundEnabled,
        crazyMinuteHaptic = current.crazyEightsMinuteHaptic,
        dailyIncludeOnePage = current.dailyIncludeOnePage,
        palette = current.themePalette.effective(sdkInt),
        palettes = palettes,
    )

    private fun durationRow(
        id: ExerciseId,
        current: Settings,
    ): DurationRow {
        val default = ExerciseId.DEFAULT_DURATIONS.getValue(id)
        val bounds = DurationBounds.of(default)
        return DurationRow(
            id = id,
            seconds = (current.durations[id] ?: default).inWholeSeconds.toInt(),
            minSeconds = bounds.min.inWholeSeconds.toInt(),
            maxSeconds = bounds.max.inWholeSeconds.toInt(),
            untimed = id in current.untimed,
        )
    }
}
