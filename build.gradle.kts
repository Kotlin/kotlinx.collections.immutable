import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-80"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
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
}

allprojects {
    repositories {
        mavenCentral()
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