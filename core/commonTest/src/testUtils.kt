/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
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