pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        gradlePluginPortal()

        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
        }
    }
}

rootProject.name = "Kotlin-Immutable-Collections" // TODO: Make readable name when it's not used in js module names

include(":core")
project(":core").name="kotlinx-collections-immutable"

include(
    ":benchmarks",
    ":benchmarks:runner"
)
