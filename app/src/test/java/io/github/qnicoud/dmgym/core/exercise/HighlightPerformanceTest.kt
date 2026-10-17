package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

/** Coût du surlignage à chaque frappe : vraie détection, 5 tirages de Connect the Story, 5 000 caractères. */
class HighlightPerformanceTest {
    private val text =
        "Le voyageur traverse la forêt sombre près d’une auberge, puis il croise un vieux pont. ".repeat(
            58,
        )

    @Test
    fun `highlighting 5000 characters against five real draws stays under a millisecond or two`() =
        runTest {
            val detector = KeywordAnswerDetector(RealTables.repository)
            val draws = RealTables.drawAll(ConnectStory.definition, seed = 11)
            detector.prepare(draws)
            repeat(WARM_UP) { detector.highlights(draws, text) }

            val highlightNanos = measureNanoTime { repeat(RUNS) { detector.highlights(draws, text) } } / RUNS
            val detectNanos = measureNanoTime { repeat(RUNS) { detector.detect(draws, text) } } / RUNS

            println(
                "highlight ${text.length} chars: ${highlightNanos / NANOS_PER_MS} ms, detect: ${detectNanos / NANOS_PER_MS} ms",
            )
            assertTrue(highlightNanos < LIMIT_NANOS, "highlight took ${highlightNanos / NANOS_PER_MS} ms")
            assertTrue(detectNanos < LIMIT_NANOS, "detect took ${detectNanos / NANOS_PER_MS} ms")
        }

    private companion object {
        const val WARM_UP = 300
        const val RUNS = 200
        const val NANOS_PER_MS = 1_000_000.0
        const val LIMIT_NANOS = 2_000_000L
    }
}
