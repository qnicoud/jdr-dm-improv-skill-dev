package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.history.HistoryFilter

/**
 * Opérations de navigation sur la pile de retour (ADR 0003). Home est toujours la racine ;
 * les onglets Historique et Progrès se posent au-dessus et se remplacent entre eux.
 */
class AppNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    fun openExercise(
        exerciseId: ExerciseId,
        resumeSessionId: Long? = null,
        fromDaily: Boolean = false,
    ) {
        backStack.add(Exercise(exerciseId, resumeSessionId, fromDaily))
    }

    /**
     * Enchaîne sur l'exercice suivant de la séance du jour en remplaçant l'exercice terminé : « retour » mène alors
     * à l'aperçu de la séance (S03), pas à un exercice fini.
     */
    fun openNextDailyExercise(exerciseId: ExerciseId) {
        replaceTop(Exercise(exerciseId, fromDaily = true))
    }

    /** Remplace le dernier exercice de la séance par sa fin (S07). */
    fun finishDailySession() {
        replaceTop(DailyDone)
    }

    private fun replaceTop(route: Route) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }

    fun openDaily() {
        backStack.add(Daily)
    }

    fun openSessionDetail(sessionId: Long) {
        backStack.add(SessionDetail(sessionId))
    }

    fun openAbout() {
        backStack.add(About)
    }

    fun openImport() {
        backStack.add(ImportSessions)
    }

    fun openSettings() {
        backStack.add(Settings)
    }

    /** Ouvre l'onglet Historique avec un filtre initial ; sans effet si cet écran est déjà affiché. */
    fun openHistory(filter: HistoryFilter) {
        val target = History(filter)
        if (backStack.lastOrNull() == target) return
        backStack.clear()
        backStack.add(Home)
        backStack.add(target)
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
    fun currentTab(): Route? = (backStack.lastOrNull() as? Route)?.tab()
}
