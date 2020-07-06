/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.gradle
import java.lang.IllegalArgumentException

val platforms = listOf("Windows", "Linux", "Mac OS X")
val jdk = "JDK_18_x64"

fun benchmarksProject() = Project {
    this.id("Benchmarks")
    this.name = "Benchmarks"

    val benchmarks = listOf(
            benchmark("js", "Linux"),
            benchmark("jvm", "Linux"),
            *platforms.map { benchmark("native", it) }.toTypedArray()
    )

    buildTypesOrder = benchmarks
}

fun Project.benchmark(target: String, platform: String) = platform(platform, "${target}Benchmark") {
    steps {
        gradle {
            name = "Benchmark"
            tasks = benchmarkTask(target, platform)
            jdkHome = "%env.$jdk%"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    artifactRules = "benchmarks/build/reports/**=> reports"

    requirements {
        benchmarkAgentInstanceTypeRequirement(platform)
    }

    failureConditions {
        executionTimeoutMin = 1440
    }
}

fun benchmarkTask(target: String, platform: String): String = when(target) {
    "js", "jvm" -> "${target}FastBenchmark"
    "native" -> when(platform) {
        "Mac OS X" -> "macosX64FastBenchmark"
        "Linux" -> "linuxX64FastBenchmark"
        "Windows" -> "mingwX64FastBenchmark"
        else -> throw IllegalArgumentException("Unknown platform: $platform")
    }
    else -> throw IllegalArgumentException("Unknown target: $target")
}

fun Requirements.benchmarkAgentInstanceTypeRequirement(platform: String) {
    if (platform == "Linux") equals("system.ec2.instance-type", "m5d.xlarge")
    else if (platform == "Windows") equals("system.ec2.instance-type", "m5.xlarge")
}

fun Project.platform(platform: String, name: String, configure: BuildType.() -> Unit) = BuildType {
    // ID is prepended with Project ID, so don't repeat it here
    // ID should conform to identifier rules, so just letters, numbers and underscore
    id("${name}_${platform.substringBefore(" ")}")
    // Display name of the build configuration
    this.name = "$name ($platform)"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform)
    }

    params {
        // This parameter is needed for macOS agent to be compatible
        if (platform.startsWith("Mac")) param("env.JDK_17", "")
    }

    commonConfigure()
    configure()
}.also { buildType(it) }


fun BuildType.commonConfigure() {
    requirements {
        noLessThan("teamcity.agent.hardware.memorySizeMb", "6144")
    }

    // Allow to fetch build status through API for badges
    allowExternalStatus = true

    // Configure VCS, by default use the same and only VCS root from which this configuration is fetched
    vcs {
        root(DslContext.settingsRoot)
        showDependenciesChanges = true
        checkoutMode = CheckoutMode.ON_AGENT
    }

    failureConditions {
        errorMessage = true
        nonZeroExitCode = true
        executionTimeoutMin = 120
    }

    features {
        feature {
            id = "perfmon"
            type = "perfmon"
        }
    }
}
