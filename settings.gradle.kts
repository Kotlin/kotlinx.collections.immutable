pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        gradlePluginPortal()
    }
}

rootProject.name = "Kotlin-Immutable-Collections" // TODO: Make readable name when it's not used in js module names

include(":core")
project(":core").name="kotlinx-collections-immutable"

include(":serialization")
project(":serialization").name="kotlinx-collections-immutable-serialization"

include(
    ":benchmarks",
    ":benchmarks:runner"
)
