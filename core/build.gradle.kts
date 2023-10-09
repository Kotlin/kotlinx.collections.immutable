import kotlinx.team.infra.mavenPublicationsPom
import java.net.URI

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
    infra {
        // According to https://kotlinlang.org/docs/native-target-support.html

        // Tier 1
        target("linuxX64")
        target("macosX64")
        target("macosArm64")
        target("iosSimulatorArm64")
        target("iosX64")

        // Tier 2
        target("linuxArm64")
        target("watchosSimulatorArm64")
        target("watchosX64")
        target("watchosArm32")
        target("watchosArm64")
        target("tvosSimulatorArm64")
        target("tvosX64")
        target("tvosArm64")
        target("iosArm64")

        // Tier 3
        target("androidNativeArm32")
        target("androidNativeArm64")
        target("androidNativeX86")
        target("androidNativeX64")
        target("mingwX64")
        target("watchosDeviceArm64")
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

        val wasmJsMain by getting {
        }
        val wasmJsTest by getting {
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

publishing {
    repositories {
        maven {
            url = URI("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
            credentials {
                username = ""
                password = ""
            }
        }
    }
}
