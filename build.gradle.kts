plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ktlint) apply false
}

// Les tests du build lui-même (tâche verifyPermissions) font partie de `check`.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
