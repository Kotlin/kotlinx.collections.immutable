@file:OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)

import BuildConventions.baseConventions
import Java9Modularity.configureJava9ModuleInfo
import kotlinx.team.infra.mavenPublicationsPom

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

base {
    archivesName = "kotlinx-collections-immutable"
}

mavenPublicationsPom {
    description.set("Kotlin Immutable Collections multiplatform library")
}

kotlin {
    applyDefaultHierarchyTemplate()
    explicitApi()

    // According to https://kotlinlang.org/docs/native-target-support.html
    // Tier 1
    linuxX64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()

    // Tier 2
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    iosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30000"
                }
            }
        }
        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
                metaInfo = true
            }
        }
    }

    wasmJs {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30000"
                }
            }
        }
    }

    wasmWasi {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30000"
                }
            }
        }
    }

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
        languageSettings.apply {
            //            progressiveMode = true
            optIn("kotlin.RequiresOptIn")
        }
    }

    sourceSets {
        val commonMain by getting {
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.google.guava:guava-testlib:18.0")
            }
        }

        val jsMain by getting {
        }
        val jsTest by getting {
        }

        val wasmMain by creating {
            dependsOn(commonMain)
        }
        val wasmTest by creating {
            dependsOn(commonTest)
        }

        val wasmJsMain by getting {
            dependsOn(wasmMain)
        }
        val wasmJsTest by getting {
            dependsOn(wasmTest)
        }

        val wasmWasiMain by getting {
            dependsOn(wasmMain)
        }
        val wasmWasiTest by getting {
            dependsOn(wasmTest)
        }

        val nativeMain by getting {
        }
        val nativeTest by getting {
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
        maxHeapSize = "1024m"
    }
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
}

// Drop this when node js version become stable
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

// configure baseline conventions (dependency verification, locking, etc.)
baseConventions()

afterEvaluate {
    // Build a multi-release JAR with a Java `module-info.java` spec
    configureJava9ModuleInfo(multiRelease = true)
}
