pluginManagement {
    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven") }
        gradlePluginPortal()
    }
}

rootProject.name = "Kotlin-Immutable-Collections" // TODO: Make readable name when it's not used in js module names

include(":core")
project(":core").name = "kotlinx-collections-immutable"

include(":benchmarks")
include(":benchmarks:runner")
