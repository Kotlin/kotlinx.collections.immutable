pluginManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/kotlinx-team-infra/maven")
        gradlePluginPortal()

        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
        }
    }
    val dokkaVersion: String by settings
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
    }
}

rootProject.name = "Kotlin-Immutable-Collections" // TODO: Make readable name when it's not used in js module names

include(":core")
project(":core").name="kotlinx-collections-immutable"

include(
    ":benchmarks",
    ":benchmarks:runner"
)
