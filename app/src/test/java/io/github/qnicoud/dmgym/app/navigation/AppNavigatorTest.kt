package io.github.qnicoud.dmgym.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppNavigatorTest {
    private val stack = mutableListOf<NavKey>(Home)
    private val navigator = AppNavigator(stack)

    @Test
    fun `opening an exercise pushes the exercise route`() {
        navigator.openExercise("crazy_eights")
        assertEquals(listOf(Home, Exercise("crazy_eights")), stack)
    }

    @Test
    fun `opening settings pushes settings and back pops it`() {
        navigator.openSettings()
        assertEquals(Settings, stack.last())
        assertTrue(navigator.back())
        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun `back on the root does nothing and reports false`() {
        assertFalse(navigator.back())
        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun `selecting a tab keeps home at the root and replaces the previous tab`() {
        navigator.selectTab(History)
        assertEquals(listOf(Home, History), stack)
        navigator.selectTab(Stats)
        assertEquals(listOf(Home, Stats), stack)
        navigator.selectTab(Home)
        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun `selecting a tab from a deeper screen drops the screens above`() {
        navigator.openExercise("crazy_eights")
        navigator.selectTab(Stats)
        assertEquals(listOf(Home, Stats), stack)
    }

    @Test
    fun `selecting the current tab changes nothing`() {
        navigator.selectTab(History)
        navigator.selectTab(History)
        assertEquals(listOf(Home, History), stack)
    }

    @Test
    fun `current tab is the top level route on the stack`() {
        assertEquals(Home, navigator.currentTab())
        navigator.selectTab(History)
        assertEquals(History, navigator.currentTab())
        navigator.openSettings()
        assertNull(navigator.currentTab())
    }
}
