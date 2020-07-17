/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import java.lang.IllegalArgumentException

fun benchmarksProject(buildVersion: BuildType) = Project {
    this.id("Benchmarks")
    this.name = "Benchmarks"

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val benchmarkAll = benchmarkAll(buildVersion)
    val benchmarks = listOf(
            benchmark("js", Platform.Linux, buildVersion),
            benchmark("jvm", Platform.Linux, buildVersion),
            *platforms.map { benchmark("native", it, buildVersion) }.toTypedArray()
    )

    benchmarks.forEach { benchmark ->
        benchmarkAll.dependsOnSnapshot(benchmark, onFailure = FailureAction.ADD_PROBLEM)
        benchmarkAll.dependsOn(benchmark) {
            artifacts {
                artifactRules = "+:reports=>reports"
            }
        }
    }

    buildTypesOrder = listOf(benchmarkAll, *benchmarks.toTypedArray())
}

fun Project.benchmarkAll(buildVersion: BuildType) = BuildType {
    id("Benchmark_All")
    this.name = "Benchmark (All)"
    type = BuildTypeSettings.Type.COMPOSITE

    dependsOnSnapshot(buildVersion)
    buildNumberPattern = buildVersion.depParamRefs.buildNumber.ref

    commonConfigure()
}.also { buildType(it) }

fun Project.benchmark(target: String, platform: Platform, buildVersion: BuildType) = buildType("${target}Benchmark", platform) {

    dependsOnSnapshot(buildVersion)

    params {
        param(versionSuffixParameter, buildVersion.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, buildVersion.depParamRefs[teamcitySuffixParameter].ref)
    }

    steps {
        gradle {
            name = "Benchmark"
            tasks = benchmarkTask(target, platform)
            jdkHome = "%env.$jdk%"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$teamcitySuffixParameter=%$teamcitySuffixParameter%"
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

fun benchmarkTask(target: String, platform: Platform): String = when(target) {
    "js", "jvm" -> "${target}FastBenchmark"
    "native" -> "${platform.nativeTaskPrefix()}FastBenchmark"
    else -> throw IllegalArgumentException("Unknown target: $target")
}

fun Requirements.benchmarkAgentInstanceTypeRequirement(platform: Platform) {
    if (platform == Platform.Linux || platform == Platform.Windows) {
        matches("system.ec2.instance-type", "m5d?.xlarge")
    }
}
