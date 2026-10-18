package io.github.qnicoud.dmgym.build

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ManifestPermissionsTest {
    @TempDir
    lateinit var dir: File

    private fun manifest(vararg permissions: String, tag: String = "uses-permission"): String =
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.github.qnicoud.dmgym">
          ${permissions.joinToString("\n") { "<$tag android:name=\"$it\" />" }}
          <application />
        </manifest>
        """.trimIndent()

    private fun runTask(xml: String, allowed: List<String>) {
        val file = File(dir, "AndroidManifest.xml").apply { writeText(xml) }
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks.register("verifyTestPermissions", VerifyPermissionsTask::class.java).get()
        task.mergedManifest.set(file)
        task.allowed.set(allowed)
        task.verify()
    }

    @Test
    fun `declaredPermissions lists both uses-permission flavours`() {
        assertEquals(listOf("a.A"), ManifestPermissions.declared(manifest("a.A")))
        assertEquals(listOf("b.B"), ManifestPermissions.declared(manifest("b.B", tag = "uses-permission-sdk-23")))
    }

    @Test
    fun `violations ignores whitelisted permissions`() {
        val xml = manifest("android.permission.VIBRATE")
        assertEquals(emptyList<String>(), ManifestPermissions.violations(xml, listOf("android.permission.VIBRATE")))
    }

    @Test
    fun `task passes on a whitelisted manifest`() {
        runTask(manifest("android.permission.VIBRATE"), listOf("android.permission.VIBRATE"))
    }

    @Test
    fun `task fails on a manifest that declares INTERNET`() {
        val error = assertThrows(GradleException::class.java) {
            runTask(
                manifest("android.permission.VIBRATE", "android.permission.INTERNET"),
                listOf("android.permission.VIBRATE"),
            )
        }
        assertTrue(error.message!!.contains("android.permission.INTERNET"))
    }

    @Test
    fun `task fails on a sdk-23 permission outside the whitelist`() {
        assertThrows(GradleException::class.java) {
            runTask(manifest("android.permission.ACCESS_NETWORK_STATE", tag = "uses-permission-sdk-23"), emptyList())
        }
    }

    @Test
    fun `the v1_1 whitelist accepts the reminder permissions`() {
        runTask(
            manifest(
                "android.permission.VIBRATE",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "io.github.qnicoud.dmgym.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            PermissionWhitelist.forApplication("io.github.qnicoud.dmgym"),
        )
    }

    @Test
    fun `the whitelist rejects exact alarms and other forbidden permissions`() {
        listOf(
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        ).forEach { forbidden ->
            val error = assertThrows(GradleException::class.java) {
                runTask(
                    manifest("android.permission.POST_NOTIFICATIONS", forbidden),
                    PermissionWhitelist.forApplication("io.github.qnicoud.dmgym"),
                )
            }
            assertTrue(error.message!!.contains(forbidden))
        }
    }
}
