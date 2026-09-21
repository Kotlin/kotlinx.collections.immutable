/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.intersect
import kotlinx.collections.immutable.persistentUnorderedSetOf
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentUnorderedSet
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashSetTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `persistentUnorderedSet and their builder should be equal before and after modification`() {
        val set1 = persistentUnorderedSetOf(-1, 0, 32)
        val builder = set1.builder()

        assertTrue(set1.equals(builder))
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())

        val set2 = set1.removing(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/144
     */
    @Test
    fun `removing multiple batches should leave only remaining elements`() {
        val firstBatch = listOf(4554, 9380, 4260, 6602)
        val secondBatch = listOf(1188, 14794)
        val extraElement = 7450

        val set = firstBatch.plus(secondBatch).plus(extraElement).toPersistentUnorderedSet()
        val result = set.minus(firstBatch.toPersistentUnorderedSet()).minus(secondBatch)
        assertEquals(1, result.size)
        assertEquals(extraElement, result.first())
    }

    @Test
    fun `after removing elements from one collision the remaining one element must be promoted to the root`() {
        val set1 = persistentUnorderedSetOf(0, 32768, 65536)
        val set2 = persistentUnorderedSetOf(0, 32768)

        val expected = persistentUnorderedSetOf(65536)
        val actual = set1 - set2

        assertEquals(expected, actual)
    }

    @Test
    fun `intersect should promote the only remaining element to the root`() {
        val intersection = persistentUnorderedSetOf(1, 33) intersect persistentUnorderedSetOf(1, 65)
        val expected = persistentUnorderedSetOf(1)

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
        assertEquals(1, intersection.size)
        assertTrue(1 in intersection)
        assertEquals(listOf(1), intersection.toList())
    }

    @Test
    fun `intersect should promote the only remaining element through multiple levels`() {
        val intersection = persistentUnorderedSetOf(1, 1 + (1 shl 10)) intersect persistentUnorderedSetOf(1, 1 + (1 shl 11))
        val expected = persistentUnorderedSetOf(1)

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `intersect should keep a single remaining sub-node on its level`() {
        val intersection = persistentUnorderedSetOf(1, 1 + (1 shl 10), 33) intersect
                persistentUnorderedSetOf(1, 1 + (1 shl 10), 65)
        val expected = persistentUnorderedSetOf(1, 1 + (1 shl 10))

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `intersect of colliding elements should promote the only remaining element to the root`() {
        val intersection = persistentUnorderedSetOf(IntWrapper(1, 0), IntWrapper(2, 0)) intersect
                persistentUnorderedSetOf(IntWrapper(1, 0), IntWrapper(3, 0))
        val expected = persistentUnorderedSetOf(IntWrapper(1, 0))

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `removing the only remaining element after intersect should result in an empty set`() {
        val intersection = persistentUnorderedSetOf(1, 33) intersect persistentUnorderedSetOf(1, 65)
        val empty = intersection - 1

        assertEquals(persistentUnorderedSetOf<Int>(), empty)
        assertEquals(empty, persistentUnorderedSetOf())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `plus should not duplicate an element shared with a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1, a2, sibling)

        val union = persistentUnorderedSetOf(a1, a2) + persistentUnorderedSetOf(a1, sibling)
        assertEquals(3, union.size)
        assertEquals(3, union.toList().size)
        assertEquals(expected, union)
        assertEquals(union, expected)
        val withoutA1 = union - a1
        assertEquals(persistentUnorderedSetOf(a2, sibling), withoutA1)
        assertFalse(a1 in withoutA1)

        val reversedUnion = persistentUnorderedSetOf(a1, sibling) + persistentUnorderedSetOf(a1, a2)
        assertEquals(3, reversedUnion.size)
        assertEquals(3, reversedUnion.toList().size)
        assertEquals(expected, reversedUnion)
        assertEquals(persistentUnorderedSetOf(a2, sibling), reversedUnion - a1)
    }

    @Test
    fun `plus should insert a new element into a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1, a2, a3, sibling)

        val union = persistentUnorderedSetOf(a1, a2) + persistentUnorderedSetOf(a3, sibling)
        assertEquals(4, union.size)
        assertEquals(expected, union)

        val reversedUnion = persistentUnorderedSetOf(a3, sibling) + persistentUnorderedSetOf(a1, a2)
        assertEquals(4, reversedUnion.size)
        assertEquals(expected, reversedUnion)
    }

    @Test
    fun `intersect and minus should handle an element absent from a bottom-level collision node`() {
        assertTrue((persistentUnorderedSetOf(a1, a2) intersect persistentUnorderedSetOf(a3, sibling)).isEmpty())
        assertTrue((persistentUnorderedSetOf(a3, sibling) intersect persistentUnorderedSetOf(a1, a2)).isEmpty())

        assertEquals(persistentUnorderedSetOf(a1, a2), persistentUnorderedSetOf(a1, a2) - persistentUnorderedSetOf(a3, sibling))
        assertEquals(persistentUnorderedSetOf(a3, sibling), persistentUnorderedSetOf(a3, sibling) - persistentUnorderedSetOf(a1, a2))
    }

    @Test
    fun `minus should remove an element from a bottom-level collision node`() {
        val difference = persistentUnorderedSetOf(a1, a2) - persistentUnorderedSetOf(a1, sibling)
        assertEquals(1, difference.size)
        assertEquals(persistentUnorderedSetOf(a2), difference)

        val reversedDifference = persistentUnorderedSetOf(a1, sibling) - persistentUnorderedSetOf(a1, a2)
        assertEquals(1, reversedDifference.size)
        assertEquals(persistentUnorderedSetOf(sibling), reversedDifference)
    }

    @Test
    fun `intersect should find the shared element inside a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1)

        val intersection = persistentUnorderedSetOf(a1, a2) intersect persistentUnorderedSetOf(a1, sibling)
        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(listOf(a1), intersection.toList())

        val reversedIntersection = persistentUnorderedSetOf(a1, sibling) intersect persistentUnorderedSetOf(a1, a2)
        assertEquals(expected, reversedIntersection)
        assertEquals(listOf(a1), reversedIntersection.toList())
    }

    @Test
    fun `containsAll should find elements inside a bottom-level collision node`() {
        assertTrue(persistentUnorderedSetOf(a1, a2, sibling).containsAll(persistentUnorderedSetOf(a1, sibling)))
        assertTrue(persistentUnorderedSetOf(a1, a2, sibling).containsAll(persistentUnorderedSetOf(a1, a2)))
        assertFalse(persistentUnorderedSetOf(a1, sibling).containsAll(persistentUnorderedSetOf(a1, a2)))
        assertFalse(persistentUnorderedSetOf(a1, a2, sibling).containsAll(persistentUnorderedSetOf(a3, sibling)))
    }
}
