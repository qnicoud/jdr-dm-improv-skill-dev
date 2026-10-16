plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

gradlePlugin {
    plugins {
        register("dmgymPermissions") {
            id = "io.github.qnicoud.dmgym.permissions"
            implementationClass = "io.github.qnicoud.dmgym.build.PermissionsPlugin"
        }
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
