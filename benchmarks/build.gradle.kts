import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.jvm.tasks.Jar

plugins {
    id("kotlin-multiplatform")
    id("kotlinx.benchmark") version "0.2.0-dev-8"
}


evaluationDependsOn(":kotlinx-collections-immutable")

val JDK_6: String by project

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
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
                api("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:0.2.0-dev-8")
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
            param("immutablePercentage", /*"95", "30", */"0")
            param("hashCodeType", "random", "collision")
        }

        register("fast") {
            warmups = 7
            iterations = 7
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "1000")
            param("immutablePercentage", "0")
            param("hashCodeType", "random")

            include("immutableList.Add.addLast$")
            include("immutableList.Get.getByIndex$")
            include("immutableList.Iterate.firstToLast$")
            include("immutableList.Remove.removeLast$")
            include("immutableList.Set.setByIndex$")

            include("immutableMap.Get.get$")
            include("immutableMap.Iterate.iterateKeys$")
            include("immutableMap.Put.put$")
            include("immutableMap.Remove.remove$")

            include("immutableSet.Add.add$")
            include("immutableSet.Contains.contains$")
            include("immutableSet.Iterate.iterate$")
            include("immutableSet.Remove.remove$")
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