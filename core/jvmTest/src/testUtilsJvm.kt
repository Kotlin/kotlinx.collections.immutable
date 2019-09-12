/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests

import kotlin.test.assertEquals

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    assertEquals(expected?.javaClass, actual?.javaClass)
}

public actual val currentPlatform: TestPlatform get() = TestPlatform.JVM

actual object NForAlgorithmComplexity {
    actual val O_N: Int = 1_000_000
    actual val O_NlogN: Int = 200_000
    actual val O_NN: Int = 10_000
    actual val O_NNlogN: Int = 5_000
}