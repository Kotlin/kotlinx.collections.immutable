import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.properties["kotlin_version"]}")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-85"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
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

    val setAllWarningsAsError = providers.gradleProperty("kotlin_Werror_override").map {
        when (it) {
            "enable" -> true
            "disable" -> false
            else -> error("Unexpected value for 'kotlin_Werror_override' property: $it")
        }
    }

    tasks.withType(KotlinCompilationTask::class).configureEach {
        compilerOptions {
            languageVersion.set(KotlinVersion.fromVersion(rootProject.properties["kotlin_language_version"].toString()))
            apiVersion.set(KotlinVersion.fromVersion(rootProject.properties["kotlin_api_version"].toString()))

            if (setAllWarningsAsError.orNull != false) {
                allWarningsAsErrors = true
            } else {
                freeCompilerArgs.addAll(
                    "-Wextra",
                    "-Xuse-fir-experimental-checkers"
                )
            }
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes",
                "-Xreport-all-warnings",
                "-Xrender-internal-diagnostic-names"
            )
        }
        if (this is KotlinJsCompile) {
            compilerOptions {
                freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
            }
        } else if (this is KotlinJvmCompile) {
            compilerOptions {
                freeCompilerArgs.add("-Xjvm-default=disable")
            }
        }

        val extraOpts = providers.gradleProperty("kotlin_additional_cli_options").orNull
        extraOpts?.split(' ')?.map(String::trim)?.filter(String::isNotBlank)?.let { opts ->
            if (opts.isNotEmpty()) {
                compilerOptions.freeCompilerArgs.addAll(opts)
            }
        }

        doFirst {
            logger.info("Added Kotlin compiler flags: ${compilerOptions.freeCompilerArgs.get().joinToString(", ")}")
            logger.info("allWarningsAsErrors=${compilerOptions.allWarningsAsErrors.get()}")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
