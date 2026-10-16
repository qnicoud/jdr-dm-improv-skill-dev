package io.github.qnicoud.dmgym.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Échoue si le manifeste fusionné déclare une permission hors liste blanche (ADR 0010). */
abstract class VerifyPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowed: ListProperty<String>

    @TaskAction
    fun verify() {
        val xml = mergedManifest.get().asFile.readText()
        val violations = ManifestPermissions.violations(xml, allowed.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Permissions interdites dans le manifeste fusionné : ${violations.joinToString()}. " +
                    "Liste blanche : ${allowed.get().joinToString()} (voir ADR 0010).",
            )
        }
    }
}
