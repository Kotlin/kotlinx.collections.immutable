/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.intersect
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentHashSet
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashSetTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `persistentHashSet and their builder should be equal before and after modification`() {
        val set1 = persistentHashSetOf(-1, 0, 32)
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

        val set = firstBatch.plus(secondBatch).plus(extraElement).toPersistentHashSet()
        val result = set.minus(firstBatch.toPersistentHashSet()).minus(secondBatch)
        assertEquals(1, result.size)
        assertEquals(extraElement, result.first())
    }

    @Test
    fun `after removing elements from one collision the remaining one element must be promoted to the root`() {
        val set1: PersistentHashSet<Int> = persistentHashSetOf(0, 32768, 65536) as PersistentHashSet<Int>
        val set2: PersistentHashSet<Int> = persistentHashSetOf(0, 32768) as PersistentHashSet<Int>

        val expected = persistentHashSetOf(65536)
        val actual = set1 - set2

        assertEquals(expected, actual)
    }

    @Test
    fun `intersect should promote the only remaining element to the root`() {
        val intersection = persistentHashSetOf(1, 33) intersect persistentHashSetOf(1, 65)
        val expected = persistentHashSetOf(1)

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
        assertEquals(1, intersection.size)
        assertTrue(1 in intersection)
        assertEquals(listOf(1), intersection.toList())
    }

    @Test
    fun `intersect should promote the only remaining element through multiple levels`() {
        val intersection = persistentHashSetOf(1, 1 + (1 shl 10)) intersect persistentHashSetOf(1, 1 + (1 shl 11))
        val expected = persistentHashSetOf(1)

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `intersect should keep a single remaining sub-node on its level`() {
        val intersection = persistentHashSetOf(1, 1 + (1 shl 10), 33) intersect
                persistentHashSetOf(1, 1 + (1 shl 10), 65)
        val expected = persistentHashSetOf(1, 1 + (1 shl 10))

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `intersect of colliding elements should promote the only remaining element to the root`() {
        val intersection = persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 0)) intersect
                persistentHashSetOf(IntWrapper(1, 0), IntWrapper(3, 0))
        val expected = persistentHashSetOf(IntWrapper(1, 0))

        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(expected.hashCode(), intersection.hashCode())
    }

    @Test
    fun `removing the only remaining element after intersect should result in an empty set`() {
        val intersection = persistentHashSetOf(1, 33) intersect persistentHashSetOf(1, 65)
        val empty = intersection - 1

        assertEquals(persistentHashSetOf<Int>(), empty)
        assertEquals(empty, persistentHashSetOf())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `plus should not duplicate an element shared with a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1, a2, sibling)

        val union = persistentHashSetOf(a1, a2) + persistentHashSetOf(a1, sibling)
        assertEquals(3, union.size)
        assertEquals(3, union.toList().size)
        assertEquals(expected, union)
        assertEquals(union, expected)
        val withoutA1 = union - a1
        assertEquals(persistentHashSetOf(a2, sibling), withoutA1)
        assertFalse(a1 in withoutA1)

        val reversedUnion = persistentHashSetOf(a1, sibling) + persistentHashSetOf(a1, a2)
        assertEquals(3, reversedUnion.size)
        assertEquals(3, reversedUnion.toList().size)
        assertEquals(expected, reversedUnion)
        assertEquals(persistentHashSetOf(a2, sibling), reversedUnion - a1)
    }

    @Test
    fun `plus should insert a new element into a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1, a2, a3, sibling)

        val union = persistentHashSetOf(a1, a2) + persistentHashSetOf(a3, sibling)
        assertEquals(4, union.size)
        assertEquals(expected, union)

        val reversedUnion = persistentHashSetOf(a3, sibling) + persistentHashSetOf(a1, a2)
        assertEquals(4, reversedUnion.size)
        assertEquals(expected, reversedUnion)
    }

    @Test
    fun `intersect and minus should handle an element absent from a bottom-level collision node`() {
        assertTrue((persistentHashSetOf(a1, a2) intersect persistentHashSetOf(a3, sibling)).isEmpty())
        assertTrue((persistentHashSetOf(a3, sibling) intersect persistentHashSetOf(a1, a2)).isEmpty())

        assertEquals(persistentHashSetOf(a1, a2), persistentHashSetOf(a1, a2) - persistentHashSetOf(a3, sibling))
        assertEquals(persistentHashSetOf(a3, sibling), persistentHashSetOf(a3, sibling) - persistentHashSetOf(a1, a2))
    }

    @Test
    fun `minus should remove an element from a bottom-level collision node`() {
        val difference = persistentHashSetOf(a1, a2) - persistentHashSetOf(a1, sibling)
        assertEquals(1, difference.size)
        assertEquals(persistentHashSetOf(a2), difference)

        val reversedDifference = persistentHashSetOf(a1, sibling) - persistentHashSetOf(a1, a2)
        assertEquals(1, reversedDifference.size)
        assertEquals(persistentHashSetOf(sibling), reversedDifference)
    }

    @Test
    fun `intersect should find the shared element inside a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1)

        val intersection = persistentHashSetOf(a1, a2) intersect persistentHashSetOf(a1, sibling)
        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(listOf(a1), intersection.toList())

        val reversedIntersection = persistentHashSetOf(a1, sibling) intersect persistentHashSetOf(a1, a2)
        assertEquals(expected, reversedIntersection)
        assertEquals(listOf(a1), reversedIntersection.toList())
    }

    @Test
    fun `containsAll should find elements inside a bottom-level collision node`() {
        assertTrue(persistentHashSetOf(a1, a2, sibling).containsAll(persistentHashSetOf(a1, sibling)))
        assertTrue(persistentHashSetOf(a1, a2, sibling).containsAll(persistentHashSetOf(a1, a2)))
        assertFalse(persistentHashSetOf(a1, sibling).containsAll(persistentHashSetOf(a1, a2)))
        assertFalse(persistentHashSetOf(a1, a2, sibling).containsAll(persistentHashSetOf(a3, sibling)))
    }

    @Test
    fun `addingAll should keep the stored element instance when the argument holds it in a subtree`() {
        val storedElement = IntWrapper(1, 0)
        val set = persistentHashSetOf(storedElement)

        val updated = set.addingAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 32)))

        assertEquals(2, updated.size)
        assertSame(storedElement, updated.single { it == storedElement })
    }

    @Test
    fun `retainingAll should keep the stored element instance when the receiver holds it in a subtree`() {
        val storedElement = IntWrapper(1, 0)
        val set = persistentHashSetOf(storedElement, IntWrapper(2, 32))

        val updated = set.retainingAll(persistentHashSetOf(IntWrapper(1, 0)))

        assertEquals(1, updated.size)
        assertSame(storedElement, updated.single { it == storedElement })
    }

    @Test
    fun `retainingAll should keep every stored instance when the argument's collision node is an equality-subset`() {
        val storedElement1 = IntWrapper(1, 0)
        val storedElement2 = IntWrapper(2, 0)
        val set = persistentHashSetOf(storedElement1, storedElement2, IntWrapper(3, 0))

        val updated = set.retainingAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 0)))

        assertEquals(2, updated.size)
        assertSame(storedElement1, updated.single { it == storedElement1 })
        assertSame(storedElement2, updated.single { it == storedElement2 })
    }

    @Test
    fun `retainingAll and removingAll that change nothing should return the same set emptied by removing`() {
        val empty = persistentHashSetOf(7).removing(7)

        assertSame(empty, empty.retainingAll(persistentHashSetOf(1, 2)))
        assertSame(empty, empty.removingAll(persistentHashSetOf(1, 2)))
    }
}
