/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests

import kotlin.test.assertEquals

actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.javaClass, actual?.javaClass)
}

actual val currentPlatform: TestPlatform get() = TestPlatform.JVM

actual object NForAlgorithmComplexity {
    actual val O_N: Int = 1_000_000
    actual val O_NlogN: Int = 200_000
    actual val O_NN: Int = 10_000
    actual val O_NNlogN: Int = 5_000
}