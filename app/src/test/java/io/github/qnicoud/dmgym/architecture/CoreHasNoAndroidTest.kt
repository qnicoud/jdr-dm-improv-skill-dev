package io.github.qnicoud.dmgym.architecture

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Architecture guard: `core` is pure Kotlin and must not depend on Android (architecture §2). */
class CoreHasNoAndroidTest {
    private val coreDir = File("src/main/java/io/github/qnicoud/dmgym/core")
    private val comments = Regex("""/\*.*?\*/|//[^\n]*""", RegexOption.DOT_MATCHES_ALL)
    private val androidReference = Regex("""(?<![\w.])androidx?\.\w""")

    private fun referencesAndroid(source: String): Boolean =
        androidReference.containsMatchIn(source.replace(comments, ""))

    @Test
    fun `core sources exist`() {
        assertTrue(coreDir.walkTopDown().any { it.extension == "kt" }, "core directory not found at $coreDir")
    }

    @Test
    fun `core does not reference android or androidx`() {
        val offenders =
            coreDir
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { referencesAndroid(it.readText()) }
                .map { it.relativeTo(coreDir).path }
                .toList()

        assertTrue(offenders.isEmpty(), "Android references found in core: $offenders")
    }

    @Test
    fun `detector catches imports and fully qualified names`() {
        assertTrue(referencesAndroid("import android.content.Context"))
        assertTrue(referencesAndroid("import androidx.annotation.Keep"))
        assertTrue(referencesAndroid("val c: android.content.Context? = null"))
        assertTrue(referencesAndroid("val x = androidx.core.os.BuildCompat.isAtLeastN()"))
    }

    @Test
    fun `detector ignores comments and unrelated identifiers`() {
        assertFalse(referencesAndroid("// uses android.content.Context"))
        assertFalse(referencesAndroid("/** see android.os.Build */ val a = 1"))
        assertFalse(referencesAndroid("val androidVersion = 3"))
        assertFalse(referencesAndroid("import io.github.qnicoud.dmgym.android.Foo"))
    }
}
