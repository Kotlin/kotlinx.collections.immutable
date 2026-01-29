import kotlinx.team.infra.mavenPublicationsPom
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.dokka")
    `maven-publish`
}

base {
    archivesName = "kotlinx-collections-immutable" // doesn't work
}

mavenPublicationsPom {
    description = "Kotlin Immutable Collections multiplatform library"
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
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

    jvmToolchain(21)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xjdk-release=8")
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
        compilerOptions {
            sourceMap = true
            moduleKind.set(JsModuleKind.MODULE_UMD)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs {
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

dokka {
    pluginsConfiguration.html {
        templatesDir.set(projectDir.resolve("dokka-templates"))
    }

    dokkaPublications.html {
        failOnWarning.set(true)
        // Enum members and undocumented toString()
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.configureEach {
        val platform = name.dropLast(4)
        samples.from("$platform/test")
        skipDeprecated.set(true)
        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl("https://github.com/Kotlin/kotlinx.collections.immutable/tree/v0.4.0")
            remoteLineSuffix.set("#L")
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
        maxHeapSize = "1024m"
    }

    // See https://youtrack.jetbrains.com/issue/KT-61313
    withType<Sign>().configureEach {
        val pubName = name.removePrefix("sign").removeSuffix("Publication")
        findByName("linkDebugTest$pubName")?.let {
            mustRunAfter(it)
        }
        findByName("compileTestKotlin$pubName")?.let {
            mustRunAfter(it)
        }
    }
}
