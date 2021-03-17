buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
    }
}

plugins {
    id("org.jetbrains.dokka") version "1.4.30" apply false
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

val JDK_6 by ext(System.getenv("JDK_6") ?: findProperty("JDK_6") as String? ?: error("Specify path to JDK 6 in JDK_6 environment variable or Gradle property"))

allprojects {
    repositories {
        mavenCentral()
    }
}
