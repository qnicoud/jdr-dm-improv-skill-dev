package io.github.qnicoud.dmgym.build

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Plugin vide : il rend `VerifyPermissionsTask` disponible dans le classpath des scripts de build. */
class PermissionsPlugin : Plugin<Project> {
    override fun apply(target: Project) = Unit
}
