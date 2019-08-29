buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.1.0-dev-49"
}

infra {
    teamcity {
        bintrayUser = "bintrayUser"
        bintrayToken = "bintrayToken"
    }
    publishing {
        include(":kotlinx-collections-immutable")

        bintray {

            organization = "kotlin"
            repository = "kotlinx"
            library = "kotlinx-collections-immutable"
            username = findProperty("bintrayUser") as String?
            password = findProperty("bintrayApiKey") as String?
        }
    }
}

val JDK_6 by ext(System.getenv("JDK_6") ?: findProperty("JDK_6") as String? ?: error("Specify path to JDK 6 in JDK_6 environment variable or Gradle property"))

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}