package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.history.HistoryFilter
import kotlinx.serialization.Serializable

/** Destinations de l'app (ADR 0003). Sérialisables pour survivre à la rotation et à la mort du processus. */
@Serializable
sealed interface Route : NavKey

@Serializable
data object Home : Route

@Serializable
data class Exercise(
    val exerciseId: ExerciseId,
    val resumeSessionId: Long? = null,
    val fromDaily: Boolean = false,
) : Route

@Serializable
data object Daily : Route

/** Fin de la séance du jour (S07), après le dernier exercice. */
@Serializable
data object DailyDone : Route

/** Historique (S08) ; [filter] est le filtre initial (liens de l'écran Progrès). */
@Serializable
data class History(
    val filter: HistoryFilter = HistoryFilter.All,
) : Route

@Serializable
data class SessionDetail(
    val sessionId: Long,
) : Route

@Serializable
data object Stats : Route

@Serializable
data object Settings : Route

/** Import des sessions (S13), plein écran, sans barre d'onglets. */
@Serializable
data object ImportSessions : Route

@Serializable
data object About : Route

/** Les trois onglets de la barre de navigation (D37). */
val TopLevelRoutes: List<Route> = listOf(Home, History(), Stats)

/** L'onglet auquel appartient cette route (l'historique filtré reste l'onglet Historique), sinon `null`. */
fun Route.tab(): Route? =
    when (this) {
        is History -> History()
        else -> takeIf { it in TopLevelRoutes }
    }
