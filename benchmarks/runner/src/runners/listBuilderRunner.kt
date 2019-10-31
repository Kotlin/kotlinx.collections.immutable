/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package runners

import listBuilderOutputFileName
import runBenchmarks


fun main() {
    runBenchmarks(listBuilderOutputFileName) { this
            .include("immutableList.builder")
    }
}
