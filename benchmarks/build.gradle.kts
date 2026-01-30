import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.15"
}


evaluationDependsOn(":kotlinx-collections-immutable")

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    js {
        nodejs {

        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    //TODO: Add wasmWasi benchmarks as soon as kx-benchmark supports the target

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.13")
                implementation(project(":kotlinx-collections-immutable"))
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
            jmhVersion = "1.37"
        }
        register("js")
        register("wasmJs")
        register("macosX64")
        register("macosArm64")
        register("linuxX64")
        register("mingwX64")
    }
}

val benchmarksJar: Configuration by configurations.creating

afterEvaluate {
    val jvmBenchmarkJar by tasks.getting(Jar::class)
    artifacts.add("benchmarksJar", jvmBenchmarkJar)
}
