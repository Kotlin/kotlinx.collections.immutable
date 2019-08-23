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

package kotlinx.collections.immutable.contractTests

import kotlin.test.*

public fun <T> compare(expected: T, actual: T, block: CompareContext<T>.() -> Unit) {
    CompareContext(expected, actual).block()
}

public class CompareContext<out T>(public val expected: T, public val actual: T) {

    public fun equals(message: String = "") {
        assertEquals(expected, actual, message)
    }
    public fun <P> propertyEquals(message: String = "", getter: T.() -> P) {
        assertEquals(expected.getter(), actual.getter(), message)
    }
    public fun propertyFails(getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}) }
    public inline fun <reified E> propertyFailsWith(noinline getter: T.() -> Unit) = propertyFailsWith({ it is E }, getter)
    public fun propertyFailsWith(exceptionPredicate: (Throwable) -> Boolean, getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}, exceptionPredicate) }
    public fun <P> compareProperty(getter: T.() -> P, block: CompareContext<P>.() -> Unit) {
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

    public fun assertTypeEquals(expected: Any?, actual: Any?) {
        assertEquals(expected?.javaClass, actual?.javaClass)
    }
}