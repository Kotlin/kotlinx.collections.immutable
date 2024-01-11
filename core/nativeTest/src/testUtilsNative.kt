/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests

import kotlin.test.assertTrue

actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    if (expected != null && actual != null) {
        assertTrue(expected::class.isInstance(actual) || actual::class.isInstance(expected),
                "Expected: $expected,  Actual: $actual")
    } else {
        assertTrue(expected == null && actual == null)
    }
}

actual val currentPlatform: TestPlatform get() = TestPlatform.Native

actual object NForAlgorithmComplexity {
    actual val O_N: Int = 100_000
    actual val O_NlogN: Int = 20_000
    actual val O_NN: Int = 1_000
    actual val O_NNlogN: Int = 500
}