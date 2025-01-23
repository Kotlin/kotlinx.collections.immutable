/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

fun Project.additionalConfiguration() {
    subProject(benchmarksProject(knownBuilds.buildVersion))

    knownBuilds.buildAll.features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = storedToken {
                    tokenId = "tc_token_id:CID_7db3007c46f7e30124f81ef54591b223:-1:f604c3ec-6391-4e29-a6e4-e59397b4622d"
                }
            }
        }
    }

    // Check with Kotlin master only on Linux
    buildWithKotlinMaster(Platform.Linux, knownBuilds.buildVersion).also {
        knownBuilds.buildAll.dependsOnSnapshot(it, onFailure = FailureAction.ADD_PROBLEM)
    }
}

fun Project.buildWithKotlinMaster(platform: Platform, versionBuild: BuildType) = BuildType {
    id("Build_with_Kotlin_Master_Linux")
    this.name = "Build with Kotlin Master (${platform.buildTypeName()})"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform.teamcityAgentName())
    }
    commonConfigure()

    dependsOnSnapshot(versionBuild)
    params {
        param(versionSuffixParameter, versionBuild.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, versionBuild.depParamRefs[teamcitySuffixParameter].ref)
    }

    val kotlinVersionParameter = "dep.Kotlin_KotlinPublic_BuildNumber.deployVersion"

    dependsOn(AbsoluteId("Kotlin_KotlinPublic_Artifacts")) {
        artifacts {
            buildRule = lastSuccessful()
            cleanDestination = true
            artifactRules = "+:maven.zip!**=>artifacts/kotlin"
        }
    }

    steps {
        gradle {
            name = "Build and Test ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            tasks = "clean publishToBuildLocal check"
            // --continue is needed to run tests for all targets even if one target fails
            gradleParams = listOf(
                "-x kotlinStoreYarnLock",
                "--info", "--stacktrace", "--continue",
                "-P$versionSuffixParameter=%$versionSuffixParameter%", "-P$teamcitySuffixParameter=%$teamcitySuffixParameter%",
                "-Pkotlin_repo_url=file://%teamcity.build.checkoutDir%/artifacts/kotlin",
                "-Pkotlin_version=%$kotlinVersionParameter%", "-Pkotlin.native.version=%$kotlinVersionParameter%"
            ).joinToString(separator = " ")
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    // What files to publish as build artifacts
    artifactRules = "+:build/maven=>maven\n+:build/api=>api\n+:artifacts"
}.also { buildType(it) }
