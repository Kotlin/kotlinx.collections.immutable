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

import kotlin.native.concurrent.ThreadLocal


internal fun Char.isUpperCase(): Boolean = this in 'A'..'Z'
internal fun Char.isDigit(): Boolean = this in '0'..'9'

internal fun <K, V> MutableMap<K, V>.remove(key: K, value: V): Boolean =
        if (key in this && this[key] == value) {
            remove(key)
            true
        } else {
            false
        }

public expect fun assertTypeEquals(expected: Any?, actual: Any?)

public enum class TestPlatform {
    JVM,
    JS,
    Native
}
public expect val currentPlatform: TestPlatform

public inline fun testOn(platform: TestPlatform, action: () -> Unit) {
    if (platform == currentPlatform) action()
}

@ThreadLocal
private var stringValues = mutableListOf<String>()

internal fun distinctStringValues(size: Int): List<String> {
    if (size <= stringValues.size) {
        return stringValues.subList(0, size)
    }
    for (index in stringValues.size until size) {
        stringValues.add(index.toString())
    }
    return stringValues
}

expect object NForAlgorithmComplexity {
    val O_N: Int
    val O_NlogN: Int
    val O_NN: Int
    val O_NNlogN: Int
}