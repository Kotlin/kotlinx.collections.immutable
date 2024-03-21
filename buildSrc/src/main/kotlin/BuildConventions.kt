/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

// Static dependency pins.
val pinnedDependencies = mapOf<String, LibrariesForLibs.() -> Provider<String>>(
    "org.jetbrains.kotlin:kotlin-stdlib" to { versions.kotlin.sdk },
    "org.jetbrains.kotlin:kotlin-stdlib-common" to { versions.kotlin.sdk },
)

object BuildConventions {
    fun Project.baseConventions() {
        val libs = the<LibrariesForLibs>()

        // enable dependency locking
        dependencyLocking {
            lockAllConfigurations()
        }

        listOf(
            Jar::class,
            Zip::class,
            Tar::class,
        ).forEach {
            tasks.withType(it).configureEach {
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true

                when(this) {
                    is Zip -> isZip64 = true
                }
            }
        }

        // pin dependencies which are critical transitively
        configurations.all {
            resolutionStrategy {
                eachDependency {
                    val coordinate = "${requested.module.group}:${requested.module.name}"
                    pinnedDependencies[coordinate]?.let { pin ->
                        useVersion(pin.invoke(libs).get())
                        because("pinned dependency")
                    }
                }
            }
        }
    }
}
