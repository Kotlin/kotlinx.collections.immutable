/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

private class Assertions

internal val ASSERTIONS_ENABLED = Assertions::class.java.desiredAssertionStatus()

internal actual inline fun assert(condition: () -> Boolean) {
    if (ASSERTIONS_ENABLED && !condition()) throw AssertionError("Assertion failed")
}
