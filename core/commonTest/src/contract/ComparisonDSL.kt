/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import tests.assertTypeEquals
import kotlin.test.*

fun <T> compare(expected: T, actual: T, block: CompareContext<T>.() -> Unit) {
    CompareContext(expected, actual).block()
}

class CompareContext<out T>(val expected: T, val actual: T) {

    fun equals(message: String = "") {
        assertEquals(expected, actual, message)
    }
    fun <P> propertyEquals(message: String = "", getter: T.() -> P) {
        assertEquals(expected.getter(), actual.getter(), message)
    }
    fun propertyFails(getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}) }
    inline fun <reified E> propertyFailsWith(noinline getter: T.() -> Unit) = propertyFailsWith({ it is E }, getter)
    fun propertyFailsWith(exceptionPredicate: (Throwable) -> Boolean, getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}, exceptionPredicate) }
    fun <P> compareProperty(getter: T.() -> P, block: CompareContext<P>.() -> Unit) {
        compare(expected.getter(), actual.getter(), block)
    }

    private fun assertFailEquals(expected: () -> Unit, actual: () -> Unit, exceptionPredicate: ((Throwable) -> Boolean)? = null) {
        val expectedFail = assertFails(expected)
        val actualFail = assertFails(actual)

        if (exceptionPredicate != null) {
            assertTrue(exceptionPredicate(expectedFail), "expected fail")
            assertTrue(exceptionPredicate(actualFail), "actual fail")
        } else {
            //assertEquals(expectedFail != null, actualFail != null)
            assertTypeEquals(expectedFail, actualFail)
        }
    }
}