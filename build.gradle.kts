buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.2.0-dev-55"
}

infra {
    teamcity {
        bintrayUser = "%env.BINTRAY_USER%"
        bintrayToken = "%env.BINTRAY_API_KEY%"
    }
    publishing {
        include(":kotlinx-collections-immutable")

        bintray {
            organization = "kotlin"
            repository = "kotlinx"
            library = "kotlinx.collections.immutable"
            username = findProperty("bintrayUser") as String?
            password = findProperty("bintrayApiKey") as String?
        }

        bintrayDev {
            organization = "kotlin"
            repository = "kotlin-dev"
            library = "kotlinx.collections.immutable"
            username = findProperty("bintrayUser") as String?
            password = findProperty("bintrayApiKey") as String?
        }
    }
}

val JDK_6 by ext(System.getenv("JDK_6") ?: findProperty("JDK_6") as String? ?: error("Specify path to JDK 6 in JDK_6 environment variable or Gradle property"))

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}