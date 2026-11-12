/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentOrderedSetTest {

    @Test
    fun `iterator preserves remaining elements when hashCode throws`() {
        val elements = List(3) { ThrowingHashCode(it) }
        val iterator = persistentSetOf(*elements.toTypedArray()).iterator()

        assertEquals(elements[0], iterator.next())
        elements[1].failHashCode = true
        repeat(2) {
            assertFailsWith<IllegalStateException> { iterator.next() }
        }
        elements[1].failHashCode = false

        assertEquals(elements.drop(1), iterator.asSequence().toList())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun `builder iterator preserves remaining elements when hashCode throws`() {
        val elements = List(3) { ThrowingHashCode(it) }
        val iterator = persistentSetOf(*elements.toTypedArray()).builder().iterator()

        assertEquals(elements[0], iterator.next())
        elements[1].failHashCode = true
        assertFailsWith<IllegalStateException> { iterator.next() }
        elements[1].failHashCode = false

        assertEquals(elements.drop(1), iterator.asSequence().toList())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun `builder iterator removes last returned element after hashCode throws`() {
        val elements = List(3) { ThrowingHashCode(it) }
        val builder = persistentSetOf(*elements.toTypedArray()).builder()
        val iterator = builder.iterator()

        assertEquals(elements[0], iterator.next())
        elements[1].failHashCode = true
        assertFailsWith<IllegalStateException> { iterator.next() }
        elements[1].failHashCode = false
        iterator.remove()

        assertEquals(elements.drop(1), builder.toList())
        assertEquals(elements.drop(1), iterator.asSequence().toList())
        assertFalse(iterator.hasNext())
    }

    private class ThrowingHashCode(private val hash: Int) {
        var failHashCode = false

        override fun hashCode(): Int {
            check(!failHashCode)
            return hash
        }
    }

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
}
