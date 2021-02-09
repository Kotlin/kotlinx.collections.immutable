/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.Project

fun Project.additionalConfiguration() {
    params {
        param("env.JDK_6", "%env.JDK_16%")
    }
    subProject(benchmarksProject(knownBuilds.buildVersion))
}