import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.jvm.tasks.Jar

plugins {
    id("kotlin-multiplatform")
    id("kotlinx.benchmark") version "0.2.0-dev-6"
}


evaluationDependsOn(":kotlinx-collections-immutable")

val JDK_6: String by project

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}


kotlin {
    infra {
        target("macosX64")
        target("linuxX64")
        target("mingwX64")
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.6"
                jdkHome = JDK_6
            }
        }
    }

    js {
        nodejs {

        }
    }

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
                api("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:0.2.0-dev-6")
                api(project(":kotlinx-collections-immutable"))
            }
        }
    }
}


// Configure benchmark
benchmark {
    configurations {
        named("main") {
            warmups = 7
            iterations = 7
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "1", "10", "100", "1000", "10000")
            param("immutablePercentage", "95", "30", "0")
            param("hashCodeType", "random", "collision")
        }
    }

    targets {
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
        register("js")
        register("native")
        register("macosX64")
        register("linuxX64")
        register("mingwX64")
    }
}

val benchmarksJar: Configuration by configurations.creating

afterEvaluate {
    val jvmBenchmarkJar by tasks.getting(Jar::class)
    artifacts.add("benchmarksJar", jvmBenchmarkJar)
}