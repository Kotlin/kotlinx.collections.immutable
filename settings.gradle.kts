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
    val kotlinxBenchmarkVersion: String by settings
    val koverVersion: String by settings
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        id("org.jetbrains.kotlinx.benchmark") version kotlinxBenchmarkVersion
        id("org.jetbrains.kotlinx.kover") version koverVersion
        id("org.jetbrains.kotlin.plugin.power-assert") version kotlin_version
    }
}

plugins {
    id("org.jetbrains.kotlinx.artifacts-validator-plugin") version "0.0.2"
}

rootProject.name = "Kotlin-Immutable-Collections" // TODO: Make readable name when it's not used in js module names

include(":core")
project(":core").name="kotlinx-collections-immutable"

include(
    ":benchmarks",
    ":benchmarks:runner"
)
