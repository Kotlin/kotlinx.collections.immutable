import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.properties["kotlin_version"]}")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-80"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3"
}

infra {
    publishing {
        include(":kotlinx-collections-immutable")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx.collections.immutable"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

apiValidation {
    ignoredProjects += listOf(
        "benchmarks",
        "runner",
    )

    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

allprojects {
    repositories {
        mavenCentral()

        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
        }
    }

    tasks.withType(KotlinCompilationTask::class).configureEach {
        compilerOptions {
            allWarningsAsErrors = true
            freeCompilerArgs.add("-Xexpect-actual-classes")
            languageVersion.set(KotlinVersion.fromVersion(rootProject.properties["kotlin_language_version"].toString()))
            apiVersion.set(KotlinVersion.fromVersion(rootProject.properties["kotlin_api_version"].toString()))
        }
        if (this is KotlinJsCompile) {
            compilerOptions {
                freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
            }
        } else if (this is KotlinJvmCompile) {
            compilerOptions {
                freeCompilerArgs.add("-jvm-default=disable")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
