/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations

import kotlinx.collections.immutable.internal.ListImplementation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InternalUtilitiesTest {

    @Test
    fun `orderedHashCode implements the standard ordered collection hash algorithm`() {
        assertEquals(1, ListImplementation.orderedHashCode(emptyList<Int>()))

        // h = (1 * 31 + 3) * 31 + 7 = 1061
        assertEquals(1061, ListImplementation.orderedHashCode(listOf(3, 7)))

        // null elements contribute 0, and the result matches List.hashCode() of an equal list
        val elements = listOf(3, null, 7)
        assertEquals(elements.hashCode(), ListImplementation.orderedHashCode(elements))
    }

    @Test
    fun `orderedEquals compares collections by size and element order`() {
        assertTrue(ListImplementation.orderedEquals(emptyList<Int>(), emptyList<String>()))
        assertTrue(ListImplementation.orderedEquals(listOf(1, null, 3), listOf(1, null, 3)))

        // size mismatch short-circuits
        assertFalse(ListImplementation.orderedEquals(listOf(1, 2, 3), listOf(1, 2)))

        // element mismatch in the middle of iteration
        assertFalse(ListImplementation.orderedEquals(listOf(1, 2, 3), listOf(1, 5, 3)))

        // same elements in a different order are not equal
        assertFalse(ListImplementation.orderedEquals(listOf(1, 2, 3), listOf(3, 2, 1)))
    }
}
