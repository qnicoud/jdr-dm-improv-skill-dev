package io.github.qnicoud.dmgym.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.core.signal.EndSignal
import io.github.qnicoud.dmgym.core.timer.DurationBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
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

/** Résultat de la dernière action de la section Données, lu à voix haute par TalkBack. */
enum class DataMessage { EXPORTED, EXPORT_FAILED, ERASED, ERASE_FAILED }

/** État de la section Données : la confirmation d'effacement est dans la page (pas de dialogue). */
data class DataUiState(
    val confirmingErase: Boolean = false,
    /** Ce que l'utilisateur a tapé pour confirmer ; le mot attendu est [ERASE_WORD]. */
    val eraseInput: String = "",
    val message: DataMessage? = null,
) {
    /** Le mot est accepté sans tenir compte de la casse ni des espaces autour. */
    val canErase: Boolean get() = eraseInput.trim().equals(ERASE_WORD, ignoreCase = true)

    companion object {
        const val ERASE_WORD = "EFFACER"
    }
}

/** État de l'écran Réglages (S11). [palette] est la palette réellement appliquée (repli sur Parchemin avant l'API 31). */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val durations: List<DurationRow> = emptyList(),
    val vibration: Boolean = true,
    val canVibrate: Boolean = true,
    val vibrationStrength: VibrationStrength = VibrationStrength.NORMAL,
    val sound: Boolean = true,
    val crazyMinuteHaptic: Boolean = false,
    val dailyIncludeOnePage: Boolean = false,
    val palette: ThemePalette = ThemePalette.PARCHMENT,
    val palettes: List<ThemePalette> = listOf(ThemePalette.PARCHMENT),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** `null` : suit le système. */
    val reduceAnimations: Boolean? = null,
    val data: DataUiState = DataUiState(),
)

/** Réglages (ADR 0013) : chaque changement s'écrit aussitôt dans le dépôt, sans bouton « Enregistrer ». */
class SettingsViewModel(
    private val settings: SettingsRepository,
    sdkInt: Int,
    private val endSignal: EndSignal,
    private val archive: SessionArchive,
    private val clock: Clock = Clock.systemUTC(),
    private val computation: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private var lastTryMillis: Long? = null
    private var messageJob: Job? = null
    private val palettes = ThemePalette.available(sdkInt)
    private val data = MutableStateFlow(DataUiState())

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.settings, data) { current, dataState -> toUiState(current, sdkInt, dataState) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setVibration(enabled: Boolean) = change { it.copy(vibrationEnabled = enabled) }

    fun setVibrationStrength(strength: VibrationStrength) = change { it.copy(vibrationStrength = strength) }

    /** Bouton « Essayer » : joue la vibration à la force choisie, même si l'interrupteur est coupé. */
    fun tryVibration() {
        val now = clock.millis()
        if (lastTryMillis?.let { now - it < TRY_DEBOUNCE_MILLIS } == true) return
        lastTryMillis = now
        endSignal.previewVibration(uiState.value.vibrationStrength)
    }

    fun setThemeMode(mode: ThemeMode) = change { it.copy(themeMode = mode) }

    fun setReduceAnimations(reduce: Boolean?) = change { it.copy(reduceAnimations = reduce) }

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

    fun requestErase() = showData(DataUiState(confirmingErase = true))

    fun cancelErase() = showData(DataUiState())

    fun setEraseInput(text: String) = data.update { it.copy(eraseInput = text) }

    /** Sans effet tant que le mot de confirmation n'est pas tapé. */
    fun confirmErase() {
        if (!data.value.canErase) return
        viewModelScope.launch {
            val outcome = if (attempt { archive.deleteAll() }) DataMessage.ERASED else DataMessage.ERASE_FAILED
            showData(DataUiState(message = outcome))
            scheduleMessageClear(outcome)
        }
    }

    /** Écrit l'export via [write] (fichier choisi par l'utilisateur), avec les libellés [labels]. */
    fun export(
        labels: ExportLabels,
        write: suspend (String) -> Unit,
    ) {
        showData(DataUiState())
        viewModelScope.launch {
            val done =
                attempt {
                    val details = archive.readAll()
                    val text =
                        withContext(computation) { SessionExportFormatter(labels).format(details, clock.instant()) }
                    write(text)
                }
            val outcome = if (done) DataMessage.EXPORTED else DataMessage.EXPORT_FAILED
            showData(DataUiState(message = outcome))
            scheduleMessageClear(outcome)
        }
    }

    private fun showData(state: DataUiState) {
        messageJob?.cancel()
        data.value = state
    }

    /** Le message s'efface après un moment, sauf si une autre action l'a déjà remplacé. */
    private fun scheduleMessageClear(shown: DataMessage) {
        messageJob =
            viewModelScope.launch {
                delay(MESSAGE_MILLIS)
                data.update { if (it.message == shown) it.copy(message = null) else it }
            }
    }

    private suspend fun attempt(block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

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
        dataState: DataUiState,
    ) = SettingsUiState(
        isLoading = false,
        durations = ExerciseId.ALL.map { durationRow(it, current) },
        vibration = current.vibrationEnabled,
        canVibrate = endSignal.canVibrate,
        vibrationStrength = current.vibrationStrength,
        sound = current.soundEnabled,
        crazyMinuteHaptic = current.crazyEightsMinuteHaptic,
        dailyIncludeOnePage = current.dailyIncludeOnePage,
        palette = current.themePalette.effective(sdkInt),
        palettes = palettes,
        themeMode = current.themeMode,
        reduceAnimations = current.reduceAnimations,
        data = dataState,
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

    companion object {
        /** Durée d'affichage du résultat d'une action de la section Données. */
        const val MESSAGE_MILLIS = 6_000L

        /** Deux appuis sur « Essayer » plus proches que cela ne rejouent pas la vibration. */
        const val TRY_DEBOUNCE_MILLIS = 500L
    }
}
