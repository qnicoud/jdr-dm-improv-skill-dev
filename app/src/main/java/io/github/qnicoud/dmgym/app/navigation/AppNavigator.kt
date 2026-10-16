package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Opérations de navigation sur la pile de retour (ADR 0003). Home est toujours la racine ;
 * les onglets Historique et Progrès se posent au-dessus et se remplacent entre eux.
 */
class AppNavigator(
    private val backStack: MutableList<NavKey>,
) {
    fun openExercise(
        exerciseId: String,
        resumeSessionId: Long? = null,
        fromDaily: Boolean = false,
    ) {
        backStack.add(Exercise(exerciseId, resumeSessionId, fromDaily))
    }

    fun openDaily() {
        backStack.add(Daily)
    }

    fun openSessionDetail(sessionId: Long) {
        backStack.add(SessionDetail(sessionId))
    }

    fun openSettings() {
        backStack.add(Settings)
    }

    fun selectTab(tab: Route) {
        require(tab in TopLevelRoutes) { "$tab n'est pas un onglet" }
        if (currentTab() == tab) return
        backStack.clear()
        backStack.add(Home)
        if (tab != Home) backStack.add(tab)
    }

    /** Retourne `false` si la pile est déjà à la racine. */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** L'onglet affiché, ou `null` si l'écran du dessus n'est pas un onglet. */
    fun currentTab(): Route? = (backStack.lastOrNull() as? Route)?.takeIf { it in TopLevelRoutes }
}
