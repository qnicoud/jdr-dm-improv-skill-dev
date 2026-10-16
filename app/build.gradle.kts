import com.android.build.api.artifact.SingleArtifact
import io.github.qnicoud.dmgym.build.VerifyPermissionsTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    id("io.github.qnicoud.dmgym.permissions")
}

android {
    namespace = "io.github.qnicoud.dmgym"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.qnicoud.dmgym"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                // Robolectric 4.17 (SDK 36+) accède à des internes du JDK.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }

    lint {
        warningsAsErrors = false
        error += listOf("HardcodedText", "MissingPermission")
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set(libs.versions.ktlint)
}

// Garde-fou « hors ligne » (ADR 0010) : liste blanche de permissions sur le manifeste fusionné.
val verifyPermissions =
    tasks.register("verifyPermissions") {
        group = "verification"
        description = "Vérifie les permissions du manifeste fusionné de chaque variante."
    }
tasks.named("check") { dependsOn(verifyPermissions) }

androidComponents.onVariants { variant ->
    val variantTask =
        tasks.register<VerifyPermissionsTask>(
            "verify${variant.name.replaceFirstChar(Char::uppercase)}Permissions",
        ) {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            allowed.set(
                listOf(
                    "android.permission.VIBRATE",
                    "${variant.applicationId.get()}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                ),
            )
        }
    verifyPermissions.configure { dependsOn(variantTask) }
}

kover {
    reports {
        filters {
            includes {
                classes("*.core.*", "*.data.*", "*.feature.*ViewModel*", "*.feature.exercise.*.*Definition*")
            }
            excludes {
                classes(
                    "*.ui.*",
                    "*.platform.*",
                    "*_Impl",
                    "*_Impl$*",
                    "*Database_*",
                    "*BuildConfig",
                    "*ComposableSingletons*",
                )
                annotatedBy("androidx.compose.runtime.Composable", "androidx.room3.Dao", "androidx.room3.Database")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.framework)
    ksp(libs.androidx.room3.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
