/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

internal expect fun assert(condition: AssertScope.() -> Boolean)

internal object AssertScope {
    inline infix fun Boolean.otherwise(lazyMessage: () -> Any): Boolean {
        if (!this) assertionFailed(lazyMessage())
        return true
    }

    fun assertionFailed(message: Any?): Nothing {
        throw AssertionError(message)
    }
}
