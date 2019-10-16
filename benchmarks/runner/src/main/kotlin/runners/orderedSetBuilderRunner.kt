/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package runners

import implementationParam
import orderedSetBuilderOutputFileName
import runBenchmarks


fun main() {
    runBenchmarks(orderedSetBuilderOutputFileName) { this
            .include("immutableSet.builder")
            .param(implementationParam, "ordered")
    }
}
