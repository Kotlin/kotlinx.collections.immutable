import kotlinx.team.infra.mavenPublicationsPom
import org.gradle.jvm.tasks.Jar

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
    id("org.jetbrains.dokka")
}

base {
    archivesBaseName = "kotlinx-collections-immutable" // doesn't work
}

mavenPublicationsPom {
    description.set("Kotlin Immutable Collections multiplatform library")
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

    named("javadocJar", Jar::class) {
        dependsOn(dokkaHtml)
        from(buildDir.resolve("dokka/html"))
    }
}
