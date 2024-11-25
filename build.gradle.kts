import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

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

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.allWarningsAsErrors = true
        kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
        if (this is KotlinJsCompile) {
            kotlinOptions.freeCompilerArgs += "-Xwasm-enable-array-range-checks"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}