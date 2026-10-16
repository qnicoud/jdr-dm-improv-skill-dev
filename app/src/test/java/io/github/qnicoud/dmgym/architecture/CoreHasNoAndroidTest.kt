package io.github.qnicoud.dmgym.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Architecture guard: `core` is pure Kotlin and must not depend on Android (architecture §2). */
class CoreHasNoAndroidTest {
    private val coreDir = File("src/main/java/io/github/qnicoud/dmgym/core")
    private val forbidden = Regex("""^\s*import\s+(android|androidx)\.""", RegexOption.MULTILINE)

    @Test
    fun `core sources exist`() {
        assertTrue(coreDir.walkTopDown().any { it.extension == "kt" }, "core directory not found at $coreDir")
    }

    @Test
    fun `core does not import android or androidx`() {
        val offenders =
            coreDir
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { forbidden.containsMatchIn(it.readText()) }
                .map { it.relativeTo(coreDir).path }
                .toList()

        assertTrue(offenders.isEmpty(), "Android imports found in core: $offenders")
    }
}
