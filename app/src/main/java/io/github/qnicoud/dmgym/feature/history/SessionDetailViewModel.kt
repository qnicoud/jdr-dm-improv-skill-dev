package io.github.qnicoud.dmgym.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionNotFoundException
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État du détail. [isLoading] vaut vrai jusqu'à la première lecture ; [deleted] est vrai dès que la suppression
 * est confirmée, pour que l'écran se ferme sans afficher « session introuvable ».
 */
data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val detail: SessionDetailUi? = null,
    val confirmingDelete: Boolean = false,
    val deleted: Boolean = false,
) {
    val isMissing: Boolean get() = !isLoading && detail == null && !deleted
}

/** Échec d'une écriture : l'écran l'annonce dans une barre d'information. */
enum class DetailEvent { SaveFailed, }

/** ViewModel du détail d'une session (S09). Le texte est en lecture seule (D40) ; seuls le niveau et le bijou changent. */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val history: HistoryRepository,
    registry: ExerciseRegistry,
) : ViewModel() {
    private data class Flags(
        val confirmingDelete: Boolean = false,
        val deleted: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags())
    private val channel = Channel<DetailEvent>(Channel.BUFFERED)
    private var latest: SessionDetail? = null

    val events: Flow<DetailEvent> = channel.receiveAsFlow()

    val uiState: StateFlow<SessionDetailUiState> =
        combine(history.observeDetail(sessionId).onEach { latest = it }, flags) { detail, current ->
            SessionDetailUiState(
                isLoading = false,
                detail = detail?.toUi(registry[ExerciseId(detail.session.exerciseId)]),
                confirmingDelete = current.confirmingDelete,
                deleted = current.deleted,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionDetailUiState())

    /** Touche sur un niveau : le choisit, ou le retire s'il était déjà choisi (parcours §4.5). */
    fun onLevelSelected(level: Int) = edit { it.copy(selfRating = level.takeIf { l -> l != it.selfRating }) }

    fun onGemToggled() = edit { it.copy(isGem = !it.isGem) }

    fun onDeleteRequested() = flags.update { it.copy(confirmingDelete = true) }

    fun onDeleteCancelled() = flags.update { it.copy(confirmingDelete = false) }

    fun onDeleteConfirmed() {
        if (!flags.value.confirmingDelete) return
        flags.value = Flags(confirmingDelete = false, deleted = true)
        viewModelScope.launch {
            try {
                history.deleteSession(sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.value = Flags()
                channel.trySend(DetailEvent.SaveFailed)
            }
        }
    }

    private fun edit(change: (SessionRecord) -> SessionRecord) {
        val session = latest?.session?.takeIf { it.status == SessionStatus.COMPLETED } ?: return
        viewModelScope.launch {
            try {
                history.updateSession(change(session))
            } catch (e: CancellationException) {
                throw e
            } catch (_: SessionNotFoundException) {
                // La session a été supprimée entre-temps : l'écran bascule déjà sur « introuvable ».
            } catch (_: Exception) {
                channel.trySend(DetailEvent.SaveFailed)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
