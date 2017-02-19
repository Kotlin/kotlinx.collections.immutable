package test.collections

import kotlin.test.*

public fun <T> compare(expected: T, actual: T, block:CompareContext<T>.() -> Unit) {
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