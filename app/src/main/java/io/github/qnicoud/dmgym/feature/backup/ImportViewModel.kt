package io.github.qnicoud.dmgym.feature.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.backup.BackupDecoder
import io.github.qnicoud.dmgym.core.backup.DecodeResult
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.backup.ImportPlan
import io.github.qnicoud.dmgym.core.backup.ImportPlanner
import io.github.qnicoud.dmgym.core.backup.TooLargeException
import io.github.qnicoud.dmgym.core.history.SessionArchive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock

/** Les états A à E de l'écran d'import S13 (UX §3.2). */
sealed interface ImportUiState {
    /** A. [undone] : l'import vient d'être annulé, un message bref le dit une fois. */
    data class Choose(
        val undone: Boolean = false,
    ) : ImportUiState

    /** B. Affiché seulement si la lecture dépasse 300 ms. */
    data object Reading : ImportUiState

    /** C. Aperçu avant de confirmer. */
    data class Preview(
        val plan: ImportPlan,
    ) : ImportUiState

    /** C′. Rien à ajouter : [found] sessions dans le fichier, toutes déjà présentes (ou aucune). */
    data class NothingToAdd(
        val found: Int,
    ) : ImportUiState

    /** Écriture en cours : un second appui sur « Ajouter » est ignoré. */
    data object Importing : ImportUiState

    /** Annulation en cours : un second appui sur « Annuler cet import » est ignoré. */
    data object Undoing : ImportUiState

    /** D. Résultat ; [canUndo] tant que des sessions ajoutées peuvent être retirées. */
    data class Done(
        val added: Int,
        val alreadyPresent: Int,
        val canUndo: Boolean,
        val undoFailed: Boolean = false,
    ) : ImportUiState

    /** E. Erreur : rien n'a été ajouté. */
    data class Failed(
        val error: ImportError,
    ) : ImportUiState
}

/**
 * Importe une sauvegarde (ADR 0015). Le décodage et la validation tournent sur [computation], jamais sur le fil
 * principal. Les identifiants ajoutés sont gardés dans [savedState] : l'annulation survit à la mort du processus,
 * puisque `AUTOINCREMENT` ne réutilise jamais un identifiant (elle ne peut donc pas supprimer autre chose).
 */
class ImportViewModel(
    private val archive: SessionArchive,
    private val knownExercises: Set<String>,
    private val clock: Clock,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val computation: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val state = MutableStateFlow(restoredState())
    val uiState: StateFlow<ImportUiState> = state.asStateFlow()

    private val busyStates = setOf(ImportUiState.Importing, ImportUiState.Reading, ImportUiState.Undoing)
    private var job: Job? = null
    private var importedIds: List<Long>
        get() = savedState.get<LongArray>(KEY_IDS)?.toList().orEmpty()
        set(value) {
            savedState[KEY_IDS] = value.toLongArray()
        }

    private fun restoredState(): ImportUiState {
        val ids = savedState.get<LongArray>(KEY_IDS)
        return if (ids == null || ids.isEmpty()) {
            ImportUiState.Choose()
        } else {
            ImportUiState.Done(ids.size, savedState[KEY_ALREADY] ?: 0, canUndo = true)
        }
    }

    /** [read] lit le fichier choisi et renvoie son texte ; ses exceptions d'entrée-sortie deviennent des erreurs typées. */
    fun onFileChosen(read: suspend () -> String) {
        if (job?.isActive == true) return
        job = viewModelScope.launch { preview(read) }
    }

    private suspend fun preview(read: suspend () -> String) {
        val reading =
            viewModelScope.launch {
                delay(READING_DELAY_MILLIS)
                state.value = ImportUiState.Reading
            }
        state.value =
            try {
                previewOf(read())
            } catch (e: CancellationException) {
                throw e
            } catch (_: TooLargeException) {
                ImportUiState.Failed(ImportError.TooLarge)
            } catch (_: OutOfMemoryError) {
                ImportUiState.Failed(ImportError.TooLarge)
            } catch (_: Exception) {
                ImportUiState.Failed(ImportError.Unreadable)
            } finally {
                reading.cancel()
            }
    }

    private suspend fun previewOf(text: String): ImportUiState =
        when (val decoded = withContext(computation) { BackupDecoder.decode(text, knownExercises, clock.instant()) }) {
            is DecodeResult.Failure -> {
                ImportUiState.Failed(decoded.error)
            }

            is DecodeResult.Success -> {
                val plan =
                    ImportPlanner.plan(decoded.sessions, decoded.unknownExercise, archive.identities(), decoded.invalid)
                if (plan.hasSomethingToAdd) ImportUiState.Preview(plan) else ImportUiState.NothingToAdd(plan.found)
            }
        }

    /** Ajoute les sessions de l'aperçu. Sans effet hors de l'état C. */
    fun confirm() {
        val preview = state.value as? ImportUiState.Preview ?: return
        state.value = ImportUiState.Importing
        job =
            viewModelScope.launch {
                state.value =
                    try {
                        val result = archive.importSessions(preview.plan.toInsert)
                        val already = preview.plan.alreadyPresent + result.alreadyPresent
                        importedIds = result.insertedIds
                        savedState[KEY_ALREADY] = already
                        ImportUiState.Done(
                            added = result.insertedIds.size,
                            alreadyPresent = already,
                            canUndo = result.insertedIds.isNotEmpty(),
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        ImportUiState.Failed(ImportError.WriteFailed)
                    }
            }
    }

    /** Supprime exactement les sessions ajoutées (D67), sans dialogue. */
    fun undo() {
        val done = state.value as? ImportUiState.Done ?: return
        if (!done.canUndo) return
        state.value = ImportUiState.Undoing
        viewModelScope.launch {
            try {
                archive.deleteSessions(importedIds)
                importedIds = emptyList()
                savedState.remove<Int>(KEY_ALREADY)
                state.value = ImportUiState.Choose(undone = true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                state.value = done.copy(undoFailed = true)
            } catch (_: RuntimeException) {
                state.value = done.copy(undoFailed = true)
            }
        }
    }

    /** « Choisir un autre fichier » : retour à l'état A. */
    fun chooseAnother() {
        if (state.value in busyStates) return
        state.value = ImportUiState.Choose()
    }

    /** Le message « Import annulé » a été montré. */
    fun noticeShown() = state.update { if (it is ImportUiState.Choose) ImportUiState.Choose() else it }

    private companion object {
        const val READING_DELAY_MILLIS = 300L
        const val KEY_IDS = "imported_ids"
        const val KEY_ALREADY = "imported_already_present"
    }
}
