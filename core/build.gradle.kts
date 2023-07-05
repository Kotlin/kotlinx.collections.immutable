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

    js(BOTH) {
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

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
        languageSettings.apply {
            //            progressiveMode = true
            optIn("kotlin.RequiresOptIn")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-common")
                api("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")

            }
        }
        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
                implementation("com.google.guava:guava-testlib:18.0")
            }
        }

        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-js")
            }
        }

        val jsTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
            }
        }

        val nativeMain by getting {
            dependsOn(commonMain.get())
        }
        val nativeTest by getting {
            dependsOn(commonTest.get())
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
        maxHeapSize = "1024m"
    }
}