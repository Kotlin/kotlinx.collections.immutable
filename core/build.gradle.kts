plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

base {
    archivesBaseName = "kotlinx-collections-immutable" // doesn't work
}

val JDK_6: String by project

kotlin {
    infra {
        target("macosX64")
        target("iosX64")
        target("iosArm64")
        target("iosArm32")
        target("linuxX64")
        target("mingwX64")
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.6"
                jdkHome = JDK_6
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

    sourceSets.all {
        val platformIndex = name.indexOfLast { it.isUpperCase() }
        val platform = name.substring(0, platformIndex)
        val subSrcSet = name.substring(platformIndex).decapitalize().takeUnless { it == "main" }
        kotlin.setSrcDirs(listOf("$platform/${subSrcSet ?: "src"}"))
        resources.setSrcDirs(listOf("$platform/${subSrcSet?.let { "$it-resources" } ?: "resources"}"))
        languageSettings.apply {
            //            progressiveMode = true
            useExperimentalAnnotation("kotlin.Experimental")
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
            dependencies {

            }
        }

    }
}

tasks {
    named("jvmTest", Test::class) {
        maxHeapSize = "1024m"
        executable = "$JDK_6/bin/java"
    }
}