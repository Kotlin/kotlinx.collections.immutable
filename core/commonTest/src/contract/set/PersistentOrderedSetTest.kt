/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import tests.contract.drain
import tests.contract.runCells
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ELEMENT_MADE_EQUAL = "An element has become equal to another element after it was added to the persistent set."

class PersistentOrderedSetTest {

    private val iterators: List<Pair<String, PersistentSet<Element>.() -> Iterator<Element>>> = listOf(
        "the set's iterator" to { iterator() },
        "the builder's iterator" to { builder().iterator() },
    )

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/204
     */
    @Test
    fun `persistentOrderedSet and their builder should be equal before and after modification`() {
        val set1 = persistentSetOf(-486539264, 16777216, 0, 67108864)
        val builder = set1.builder()

        assertTrue(set1.equals(builder))
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())

        val set2 = set1.removing(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    @Test
    fun `next after the hash of the next element changed throws ConcurrentModificationException and goes on from that element once the hash is back`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val element = Element(2)
            val iterator = persistentSetOf(Element(1), element, Element(3)).open()
            val _ = iterator.next()

            element.id = 9
            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            element.id = 2

            assertEquals(listOf(element, Element(3)), iterator.drain())
        }
    }

    @Test
    fun `an element made equal to an earlier one makes the last next throw ConcurrentModificationException`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val element = Element(3)
            val set = persistentSetOf(Element(1), Element(2), element)
            element.id = 1
            val iterator = set.open()
            repeat(set.size - 1) { val _ = iterator.next() }

            val failure = assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertEquals(ELEMENT_MADE_EQUAL, failure.message)
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `an element made equal to a later one makes the next that ends the chain early throw ConcurrentModificationException`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val element = Element(2)
            val set = persistentSetOf(Element(1), element, Element(3))
            element.id = 3
            val iterator = set.open()
            val _ = iterator.next()

            val failure = assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertEquals(ELEMENT_MADE_EQUAL, failure.message)
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `remove through the iterator of an element whose hashCode changed after next throws ConcurrentModificationException`() {
        val element = Element(2)
        val builder = persistentSetOf(Element(1), element, Element(3)).builder()
        val iterator = builder.iterator()
        repeat(2) { val _ = iterator.next() }
        element.id = 4

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(3, builder.size)
    }

    private class Element(var id: Int) {
        override fun hashCode(): Int = id
        override fun equals(other: Any?): Boolean = other is Element && other.id == id
        override fun toString(): String = "Element($id)"
    }
}
