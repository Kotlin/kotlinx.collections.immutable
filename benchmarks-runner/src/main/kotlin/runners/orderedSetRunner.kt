/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package runners

import implementationParam
import orderedSetOutputFileName
import runBenchmarks


fun main() {
    runBenchmarks(orderedSetOutputFileName) { this
            .include("immutableSet")
            .exclude("builder")
            .param(implementationParam, "ordered")
    }
}
