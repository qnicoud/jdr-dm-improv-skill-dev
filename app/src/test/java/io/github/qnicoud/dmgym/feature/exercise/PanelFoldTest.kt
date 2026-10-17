package io.github.qnicoud.dmgym.feature.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PanelFoldTest {
    @Test
    fun `starts unfolded`() {
        assertTrue(PanelFold().expanded)
    }

    @Test
    fun `the first keyboard opening folds the panel`() {
        assertFalse(PanelFold().onKeyboardShown().expanded)
    }

    @Test
    fun `a panel unfolded by hand stays unfolded when the keyboard opens again`() {
        val unfolded = PanelFold().onKeyboardShown().toggled()
        assertTrue(unfolded.expanded)
        assertEquals(unfolded, unfolded.onKeyboardShown())
    }

    @Test
    fun `folding by hand before the keyboard does not use up the automatic fold`() {
        val fold = PanelFold().toggled().toggled()
        assertFalse(fold.onKeyboardShown().expanded)
    }

    @Test
    fun `the state round-trips through the saver list`() {
        val fold = PanelFold(expanded = false, autoFolded = true)
        assertEquals(fold, PanelFold.fromList(fold.toList()))
    }
}
