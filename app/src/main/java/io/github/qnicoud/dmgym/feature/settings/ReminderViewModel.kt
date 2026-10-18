package io.github.qnicoud.dmgym.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.reminder.ReminderDelivery
import io.github.qnicoud.dmgym.core.reminder.ReminderPolicy
import io.github.qnicoud.dmgym.core.reminder.ReminderSettings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.DayOfWeek

/** Accès à l'état des notifications d'Android (ADR 0014 §3), implémenté côté plateforme. */
interface NotificationPermissionGate {
    /** API 33 ou plus : `POST_NOTIFICATIONS` est une permission d'exécution. */
    val needsRuntimePermission: Boolean

    /** Notifications autorisées pour l'app **et** canal des rappels non bloqué. */
    fun notificationsAllowed(): Boolean

    fun openAppNotificationSettings()

    fun openChannelSettings()
}

/** Effets à usage unique, consommés par l'écran. */
sealed interface ReminderEffect {
    /** Lancer le dialogue système de `POST_NOTIFICATIONS`. */
    data object RequestPermission : ReminderEffect
}

data class ReminderUiState(
    val isLoading: Boolean = true,
    /** L'intention de l'utilisateur ; elle ne dit pas si le rappel peut partir. */
    val enabled: Boolean = false,
    val minuteOfDay: Int = ReminderSettings.DEFAULT_MINUTE,
    val days: Set<DayOfWeek> = ReminderSettings.DEFAULT_DAYS,
    /** Aucune session depuis 14 jours : le rappel est en pause jusqu'à « Reprendre ». */
    val paused: Boolean = false,
    /** Activé, mais les notifications sont coupées dans Android. */
    val blocked: Boolean = false,
    /** Message neutre « notifications refusées » avec lien vers les réglages Android. */
    val permissionDenied: Boolean = false,
    /** Aide sous les jours : la désélection du dernier jour est ignorée. */
    val keepOneDayHint: Boolean = false,
)

/** Section « Rappels » des Réglages (UX parcours v1.1 §2), à part de [SettingsViewModel] (architecture §16). */
class ReminderViewModel(
    private val settings: SettingsRepository,
    history: HistoryRepository,
    private val delivery: ReminderDelivery,
    private val gate: NotificationPermissionGate,
    private val clock: Clock,
) : ViewModel() {
    private val allowed = MutableStateFlow(gate.notificationsAllowed())
    private val local = MutableStateFlow(LocalState(today = clock.localEpochDay()))
    private val effectChannel = Channel<ReminderEffect>(Channel.BUFFERED)

    val effects: Flow<ReminderEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<ReminderUiState> =
        combine(settings.settings, history.observePracticedDays(), allowed, local) { all, days, ok, extra ->
            val reminder = all.reminder
            ReminderUiState(
                isLoading = false,
                enabled = reminder.enabled,
                minuteOfDay = reminder.minuteOfDay,
                days = reminder.days,
                paused =
                    reminder.enabled &&
                        ReminderPolicy.isPaused(extra.today, days.toSet(), reminder.resumedEpochDay),
                blocked = reminder.enabled && !ok,
                permissionDenied = extra.permissionDenied,
                keepOneDayHint = extra.keepOneDayHint,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ReminderUiState())

    /** Interrupteur : la permission n'est demandée qu'ici, au passage à « activé » (ADR 0014 §3). */
    fun onSwitch(on: Boolean) {
        when {
            !on -> change { it.copy(enabled = false) }
            gate.notificationsAllowed() -> enable()
            gate.needsRuntimePermission -> effectChannel.trySend(ReminderEffect.RequestPermission)
            else -> local.update { it.copy(permissionDenied = true) }
        }
    }

    /** Résultat du dialogue système. Un refus laisse l'interrupteur désactivé. */
    fun onPermissionResult(granted: Boolean) {
        refresh()
        if (granted && gate.notificationsAllowed()) enable() else local.update { it.copy(permissionDenied = true) }
    }

    /** À chaque retour au premier plan : relit l'état des notifications d'Android. */
    fun refresh() {
        val ok = gate.notificationsAllowed()
        allowed.value = ok
        local.update { it.copy(today = clock.localEpochDay()) }
        if (ok) local.update { it.copy(permissionDenied = false) }
    }

    fun setTime(
        hour: Int,
        minute: Int,
    ) {
        if (hour !in 0 until HOURS_PER_DAY || minute !in 0 until ReminderSettings.MINUTES_PER_HOUR) return
        change { it.copy(minuteOfDay = hour * ReminderSettings.MINUTES_PER_HOUR + minute) }
    }

    fun toggleDay(day: DayOfWeek) {
        val current = uiState.value.days
        val next = if (day in current) current - day else current + day
        if (next.isEmpty()) {
            local.update { it.copy(keepOneDayHint = true) }
        } else {
            local.update { it.copy(keepOneDayHint = false) }
            change { it.copy(days = next) }
        }
    }

    /** « Reprendre » après la pause de 14 jours. */
    fun resume() = change { it.copy(resumedEpochDay = clock.localEpochDay()) }

    fun sendTest() {
        refresh()
        if (gate.notificationsAllowed()) delivery.sendTest() else local.update { it.copy(permissionDenied = true) }
    }

    fun openAndroidSettings() = gate.openAppNotificationSettings()

    fun openChannelSettings() = gate.openChannelSettings()

    private fun enable() {
        local.update { it.copy(permissionDenied = false) }
        change { it.copy(enabled = true, resumedEpochDay = clock.localEpochDay()) }
    }

    private fun change(transform: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch {
            settings.update { it.copy(reminder = transform(it.reminder)) }
            // Quitter l'écran ne doit pas laisser l'alarme désynchronisée des réglages déjà écrits.
            withContext(NonCancellable) { delivery.resync() }
        }
    }

    private data class LocalState(
        val today: Long,
        val permissionDenied: Boolean = false,
        val keepOneDayHint: Boolean = false,
    )

    private companion object {
        const val HOURS_PER_DAY = 24
    }
}
