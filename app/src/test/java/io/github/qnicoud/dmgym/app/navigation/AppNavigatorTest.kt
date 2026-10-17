package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.history.HistoryFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppNavigatorTest {
    private val stack = NavBackStack<NavKey>(Home)
    private val navigator = AppNavigator(stack)
    private val crazy = ExerciseId.CRAZY_EIGHTS

    @Test
    fun `opening an exercise pushes the exercise route`() {
        navigator.openExercise(crazy)
        assertEquals(listOf<NavKey>(Home, Exercise(crazy)), stack.toList())
    }

    @Test
    fun `about opens above settings and back returns to settings`() {
        navigator.openSettings()
        navigator.openAbout()
        assertEquals(listOf<NavKey>(Home, Settings, About), stack.toList())
        assertTrue(navigator.back())
        assertEquals(Settings, stack.last())
    }

    @Test
    fun `opening settings pushes settings and back pops it`() {
        navigator.openSettings()
        assertEquals(Settings, stack.last())
        assertTrue(navigator.back())
        assertEquals(listOf<NavKey>(Home), stack.toList())
    }

    @Test
    fun `back on the root does nothing and reports false`() {
        assertFalse(navigator.back())
        assertEquals(listOf<NavKey>(Home), stack.toList())
    }

    @Test
    fun `selecting a tab keeps home at the root and replaces the previous tab`() {
        navigator.selectTab(History())
        assertEquals(listOf<NavKey>(Home, History()), stack.toList())
        navigator.selectTab(Stats)
        assertEquals(listOf<NavKey>(Home, Stats), stack.toList())
        navigator.selectTab(Home)
        assertEquals(listOf<NavKey>(Home), stack.toList())
    }

    @Test
    fun `selecting a tab from a deeper screen drops the screens above`() {
        navigator.openExercise(crazy)
        navigator.selectTab(Stats)
        assertEquals(listOf<NavKey>(Home, Stats), stack.toList())
    }

    @Test
    fun `selecting the current tab changes nothing`() {
        navigator.selectTab(History())
        navigator.selectTab(History())
        assertEquals(listOf<NavKey>(Home, History()), stack.toList())
    }

    @Test
    fun `current tab is the top level route on the stack`() {
        assertEquals(Home, navigator.currentTab())
        navigator.selectTab(History())
        assertEquals(History(), navigator.currentTab())
        navigator.openSettings()
        assertNull(navigator.currentTab())
    }

    @Test
    fun `opening the history with a filter replaces the stack above home`() {
        val gems = HistoryFilter(gemsOnly = true)
        navigator.selectTab(Stats)
        navigator.openHistory(gems)
        assertEquals(listOf<NavKey>(Home, History(gems)), stack.toList())
    }

    @Test
    fun `opening the same filtered history twice changes nothing`() {
        val filter = HistoryFilter(exerciseId = crazy)
        navigator.openHistory(filter)
        navigator.openHistory(filter)
        assertEquals(listOf<NavKey>(Home, History(filter)), stack.toList())
    }

    @Test
    fun `a filtered history still counts as the history tab`() {
        navigator.openHistory(HistoryFilter(gemsOnly = true))
        assertEquals(History(), navigator.currentTab())
    }

    @Test
    fun `selecting the history tab from a filtered history keeps the screen`() {
        val filter = HistoryFilter(gemsOnly = true)
        navigator.openHistory(filter)
        navigator.selectTab(History())
        assertEquals(listOf<NavKey>(Home, History(filter)), stack.toList())
    }

    @Test
    fun `the next daily exercise replaces the current one, so back returns to the daily screen`() {
        navigator.openDaily()
        navigator.openExercise(crazy, fromDaily = true)
        navigator.openNextDailyExercise(ExerciseId.CONSTRAINT_BOX)
        assertEquals(listOf<NavKey>(Home, Daily, Exercise(ExerciseId.CONSTRAINT_BOX, fromDaily = true)), stack.toList())
        assertTrue(navigator.back())
        assertEquals(Daily, stack.last())
    }

    @Test
    fun `finishing the daily session replaces the last exercise with the summary`() {
        navigator.openDaily()
        navigator.openExercise(crazy, fromDaily = true)
        navigator.finishDailySession()
        assertEquals(listOf<NavKey>(Home, Daily, DailyDone), stack.toList())
        assertNull(navigator.currentTab())
    }

    @Test
    fun `going home from the summary drops the daily screens`() {
        navigator.openDaily()
        navigator.openExercise(crazy, fromDaily = true)
        navigator.finishDailySession()
        navigator.selectTab(Home)
        assertEquals(listOf<NavKey>(Home), stack.toList())
    }

    @Test
    fun `opening a session detail pushes it above the history`() {
        navigator.selectTab(History())
        navigator.openSessionDetail(7)
        assertEquals(listOf<NavKey>(Home, History(), SessionDetail(7)), stack.toList())
        assertNull(navigator.currentTab())
    }
}
