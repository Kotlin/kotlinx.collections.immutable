buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
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

    // TODO: enable after https://youtrack.jetbrains.com/issue/KT-46257 gets fixed
//    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
//        kotlinOptions.allWarningsAsErrors = true
//    }
}