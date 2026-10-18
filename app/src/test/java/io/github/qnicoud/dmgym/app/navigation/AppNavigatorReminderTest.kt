package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Appui sur la notification du rappel : la séance du jour s'ouvre, et « retour » mène à l'accueil. */
class AppNavigatorReminderTest {
    private val stack = NavBackStack<NavKey>(Home)
    private val navigator = AppNavigator(stack)

    @Test
    fun `from the home it opens the daily session above home`() {
        navigator.openDailyFromReminder()

        assertEquals(listOf<NavKey>(Home, Daily), stack.toList())
    }

    @Test
    fun `from deeper in the app the stack is reset to home then daily`() {
        navigator.openSettings()
        navigator.openExercise(ExerciseId.CRAZY_EIGHTS)

        navigator.openDailyFromReminder()

        assertEquals(listOf<NavKey>(Home, Daily), stack.toList())
    }

    @Test
    fun `when the daily session is already shown nothing changes`() {
        navigator.openDaily()

        navigator.openDailyFromReminder()

        assertEquals(listOf<NavKey>(Home, Daily), stack.toList())
    }
}
