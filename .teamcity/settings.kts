import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.*
import jetbrains.buildServer.configs.kotlin.triggers.*

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.05"

project {
    // Disable editing of project and build settings from the UI to avoid issues with TeamCity
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildVersion = buildVersion()
    val buildAll = buildAll(buildVersion)
    val builds = platforms.map { build(it, buildVersion) }
    builds.forEach { build ->
        buildAll.dependsOnSnapshot(build, onFailure = FailureAction.ADD_PROBLEM)
        buildAll.dependsOn(build) {
            artifacts {
                artifactRules = "+:maven=>maven\n+:api=>api"
            }
        }
    }

    buildTypesOrder = listOf(buildAll, buildVersion, *builds.toTypedArray())

    val deploymentProject = Project {
        id("Deployment")
        this.name = "Deployment"

        params {
            param("teamcity.ui.settings.readOnly", "true")
        }

        val deployVersion = deployVersion()
        val deploys = platforms.map { buildArtifacts(it) }
        val deployUpload = deployUpload().apply {
            dependencies {
                deploys.forEach { dep ->
                    dependency(dep) {
                        snapshot {
                            onDependencyFailure = FailureAction.FAIL_TO_START
                            onDependencyCancel = FailureAction.CANCEL
                        }
                        artifacts {
                            artifactRules = "buildRepo.zip!** => buildRepo"
                        }
                    }
                }
            }
        }
        val deployPublish = deployPublish().apply {
            dependsOnSnapshot(deployUpload)
        }

        deploys.forEach { deployVersion.dependsOnSnapshot(it) }
        deployVersion.dependsOnSnapshot(deployUpload) {
            reuseBuilds = ReuseBuilds.NO
        }
        deployVersion.dependsOnSnapshot(deployPublish) {
            reuseBuilds = ReuseBuilds.NO
        }

        buildTypesOrder = listOf(deployVersion, *deploys.toTypedArray(), deployUpload, deployPublish)
    }

    subProject(deploymentProject)

    additionalConfiguration()
}

fun Project.buildVersion() = BuildType {
    id(BUILD_CONFIGURE_VERSION_ID)
    this.name = "Build (Configure Version)"
    commonConfigure()

    params {
        param(versionSuffixParameter, "SNAPSHOT")
        param(teamcitySuffixParameter, "%build.counter%")
    }

    steps {
        gradle {
            name = "Generate build chain version"
            jdkHome = "%env.$jdk%"
            tasks = ""
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$teamcitySuffixParameter=%$teamcitySuffixParameter%"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.also { buildType(it) }

fun Project.buildAll(versionBuild: BuildType) = BuildType {
    id(BUILD_ALL_ID)
    this.name = "Build (All)"
    type = BuildTypeSettings.Type.COMPOSITE

    dependsOnSnapshot(versionBuild)
    buildNumberPattern = versionBuild.depParamRefs.buildNumber.ref

    triggers {
        vcs {
            triggerRules = """
                    -:*.md
                    -:.gitignore
                """.trimIndent()
        }
    }

    commonConfigure()
}.also { buildType(it) }

fun Project.build(platform: Platform, versionBuild: BuildType) = buildType("Build", platform) {

    dependsOnSnapshot(versionBuild)

    params {
        param(versionSuffixParameter, versionBuild.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, versionBuild.depParamRefs[teamcitySuffixParameter].ref)
    }

    steps {
        gradle {
            name = "Build and Test ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            tasks = "clean publishToBuildLocal check"
            // --continue is needed to run tests for all targets even if one target fails
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$teamcitySuffixParameter=%$teamcitySuffixParameter% --continue"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    // What files to publish as build artifacts
    artifactRules = "+:build/maven=>maven\n+:build/api=>api"
}

fun Project.deployVersion() = BuildType {
    id(DEPLOY_CONFIGURE_VERSION_ID)
    this.name = "Deploy [RUN THIS ONE]"
    type = BuildTypeSettings.Type.DEPLOYMENT
    commonConfigure()

    buildNumberPattern = "%reverse.dep.*.$releaseVersionParameter% %build.counter%"

    params {
        // enable editing of this configuration to set up things
        param("teamcity.ui.settings.readOnly", "false")
        param(versionSuffixParameter, "dev-%build.counter%")
        param("reverse.dep.*.$versionSuffixParameter", "%$versionSuffixParameter%")
        text("reverse.dep.*.$releaseVersionParameter", "", label = "Version", description = "Version of artifacts to deploy", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }

    /*
    requirements {
        // Require Linux for configuration build
        contains("teamcity.agent.jvm.os.name", "Linux")
    }
     */

    /*
    steps {
        gradle {
            name = "Verify Gradle Configuration"
            tasks = "clean publishPrepareVersion"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            buildFile = ""
            jdkHome = "%env.$jdk%"
        }
    }
     */
}.also { buildType(it) }

fun Project.deployUpload() = BuildType {
    templates(AbsoluteId("KotlinTools_KotlinLibrariesDeployLocalBundleToCentral"))
    name = "Upload deployment to central portal"
    id(DEPLOY_UPLOAD_ID)

    artifactRules = """
        %LocalDeploymentPaths%
        buildRepo => buildRepo.zip
    """.trimIndent()

    params {
        param("DeploymentName", "kotlinx.collections.immutable %DeployVersion%")
        param("DeployVersion", "%$releaseVersionParameter%")
        password("DeploymentToken", "???", display = ParameterDisplay.HIDDEN)
    }
}.also { buildType(it) }

fun Project.deployPublish() = BuildType {
    id(DEPLOY_PUBLISH_ID)
    templates(PUBLISH_DEPLOYMENT_TEMPLATE_ID)
    name = "Publish deployment"
    type = BuildTypeSettings.Type.DEPLOYMENT

    params {
        password("DeploymentToken", "???", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "%$releaseVersionParameter%")
    }
    commonConfigure()
}.also { buildType(it) }


fun Project.buildArtifacts(platform: Platform) = buildType("Build", platform) {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1

    buildNumberPattern = "%reverse.dep.*.$releaseVersionParameter% %build.counter%"

    vcs {
        cleanCheckout = true
    }

    artifactRules = """
        build/maven/** => buildRepo.zip
    """.trimIndent()

    steps {
        gradle {
            name = "Build ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            tasks = "clean publishAllPublicationsToBuildLocalRepository"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}
