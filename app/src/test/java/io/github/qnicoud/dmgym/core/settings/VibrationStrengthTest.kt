package io.github.qnicoud.dmgym.core.settings

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VibrationStrengthTest {
    @Test
    fun `strong is the historical end signal, two 250 ms pulses at full amplitude`() {
        assertArrayEquals(longArrayOf(0, 250, 120, 250), VibrationStrength.STRONG.endPattern())
        assertArrayEquals(intArrayOf(0, 255, 0, 255), VibrationStrength.STRONG.endAmplitudes())
    }

    @Test
    fun `strength grows from soft to strong in duration and amplitude`() {
        val ordered = listOf(VibrationStrength.SOFT, VibrationStrength.NORMAL, VibrationStrength.STRONG)
        assertEquals(ordered, VibrationStrength.entries)
        ordered.zipWithNext().forEach { (weaker, stronger) ->
            assertTrue(weaker.pulseMillis < stronger.pulseMillis)
            assertTrue(weaker.amplitude < stronger.amplitude)
        }
    }

    @Test
    fun `even the soft pulse stays perceptible`() {
        // 120 ms ne se sentait pas sur le Galaxy Z Flip3 (test utilisateur) : plancher à 150 ms.
        VibrationStrength.entries.forEach {
            assertTrue(it.pulseMillis >= 150)
            assertTrue(it.amplitude in 1..255)
        }
    }

    @Test
    fun `the tick is one pulse of the same strength`() {
        val normal = VibrationStrength.NORMAL
        assertArrayEquals(longArrayOf(0, normal.pulseMillis), normal.tickPattern())
        assertArrayEquals(intArrayOf(0, normal.amplitude), normal.tickAmplitudes())
    }

    @Test
    fun `parse falls back on normal`() {
        assertEquals(VibrationStrength.SOFT, VibrationStrength.parse("SOFT"))
        assertEquals(VibrationStrength.NORMAL, VibrationStrength.parse("LOUD"))
        assertEquals(VibrationStrength.NORMAL, VibrationStrength.parse(null))
    }
}
