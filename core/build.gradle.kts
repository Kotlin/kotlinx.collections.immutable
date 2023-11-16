import kotlinx.team.infra.mavenPublicationsPom

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

base {
    archivesBaseName = "kotlinx-collections-immutable" // doesn't work
}

mavenPublicationsPom {
    description.set("Kotlin Immutable Collections multiplatform library")
}

kotlin {
    applyDefaultHierarchyTemplate {

        // According to https://kotlinlang.org/docs/native-target-support.html
        // Tier 1
        this@kotlin.linuxX64()
        this@kotlin.macosX64()
        this@kotlin.macosArm64()
        this@kotlin.iosSimulatorArm64()
        this@kotlin.iosX64()

        // Tier 2
        this@kotlin.watchosSimulatorArm64()
        this@kotlin.watchosX64()
        this@kotlin.watchosArm32()
        this@kotlin.watchosArm64()
        this@kotlin.tvosSimulatorArm64()
        this@kotlin.tvosX64()
        this@kotlin.tvosArm64()
        this@kotlin.iosArm64()
        this@kotlin.linuxArm64()

        // Tier 3
        this@kotlin.mingwX64()
        this@kotlin.androidNativeArm32()
        this@kotlin.androidNativeArm64()
        this@kotlin.androidNativeX86()
        this@kotlin.androidNativeX64()
        this@kotlin.watchosDeviceArm64()
    }

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
        }
        val wasmTest by creating {
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