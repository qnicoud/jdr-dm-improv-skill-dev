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
    fun `from settings the stack is reset to home then daily`() {
        navigator.openSettings()
        navigator.openAbout()

        navigator.openDailyFromReminder()

        assertEquals(listOf<NavKey>(Home, Daily), stack.toList())
    }

    @Test
    fun `when the daily session is already shown nothing changes`() {
        navigator.openDaily()

        navigator.openDailyFromReminder()

        assertEquals(listOf<NavKey>(Home, Daily), stack.toList())
    }

    @Test
    fun `an exercise in progress is never lost`() {
        navigator.openExercise(ExerciseId.CRAZY_EIGHTS)
        val before = stack.toList()

        navigator.openDailyFromReminder()

        assertEquals(before, stack.toList())
    }

    @Test
    fun `an exercise of the daily session is never lost either`() {
        navigator.openDaily()
        navigator.openExercise(ExerciseId.CRAZY_EIGHTS, fromDaily = true)
        val before = stack.toList()

        navigator.openDailyFromReminder()

        assertEquals(before, stack.toList())
    }
}
