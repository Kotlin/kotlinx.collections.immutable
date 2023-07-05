buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.3.0-dev-64"
}

infra {
    teamcity {
        libraryStagingRepoDescription = project.name
    }
    publishing {
        include(":kotlinx-collections-immutable")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx.collections.immutable"
        sonatype {}
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.allWarningsAsErrors = true
    }
}