package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Destinations de l'app (ADR 0003). Sérialisables pour survivre à la rotation et à la mort du processus. */
@Serializable
sealed interface Route : NavKey

@Serializable
data object Home : Route

@Serializable
data class Exercise(
    val exerciseId: String,
    val resumeSessionId: Long? = null,
    val fromDaily: Boolean = false,
) : Route

@Serializable
data object Daily : Route

@Serializable
data object History : Route

@Serializable
data class SessionDetail(
    val sessionId: Long,
) : Route

@Serializable
data object Stats : Route

@Serializable
data object Settings : Route

/** Les trois onglets de la barre de navigation (D37). */
val TopLevelRoutes: List<Route> = listOf(Home, History, Stats)
