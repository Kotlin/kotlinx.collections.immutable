/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests

import kotlin.test.assertEquals

actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.let { it::class.js }, actual?.let { it::class.js })
}

actual val currentPlatform: TestPlatform get() = TestPlatform.JS

actual object NForAlgorithmComplexity {
    actual val O_N: Int = 500_000
    actual val O_NlogN: Int = 100_000
    actual val O_NN: Int = 5_000
    actual val O_NNlogN: Int = 1_000
}