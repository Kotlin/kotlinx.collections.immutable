/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests

const val githubTokenId = "tc_token_id:CID_7db3007c46f7e30124f81ef54591b223:-1:f604c3ec-6391-4e29-a6e4-e59397b4622d"

fun Project.additionalConfiguration() {
    subProject(benchmarksProject(knownBuilds.buildVersion))

    knownBuilds.buildAll.features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = storedToken {
                    tokenId = githubTokenId
                }
            }
        }
        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = github {
                authType = storedToken {
                    tokenId = githubTokenId
                }
                filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
            }
        }
    }

    val deploymentProject = knownBuilds.deploymentSubproject
    val startTask = deploymentProject.knownBuilds.deployStart
    startTask.params.param("reverse.dep.*.DeploymentName", "kotlinx.collections.immutable %releaseVersion%")
}
