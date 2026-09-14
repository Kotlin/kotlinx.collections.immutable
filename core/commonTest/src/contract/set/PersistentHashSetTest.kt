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
import tests.trie.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashSetTest {

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
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, lastLevelSibling)

        val union = persistentHashSetOf(collidingKey1, collidingKey2) + persistentHashSetOf(collidingKey1, lastLevelSibling)
        assertEquals(3, union.size)
        assertEquals(3, union.toList().size)
        assertEquals(expected, union)
        assertEquals(union, expected)
        val withoutA1 = union - collidingKey1
        assertEquals(persistentHashSetOf(collidingKey2, lastLevelSibling), withoutA1)
        assertFalse(collidingKey1 in withoutA1)

        val reversedUnion = persistentHashSetOf(collidingKey1, lastLevelSibling) + persistentHashSetOf(collidingKey1, collidingKey2)
        assertEquals(3, reversedUnion.size)
        assertEquals(3, reversedUnion.toList().size)
        assertEquals(expected, reversedUnion)
        assertEquals(persistentHashSetOf(collidingKey2, lastLevelSibling), reversedUnion - collidingKey1)
    }

    @Test
    fun `plus should insert a new element into a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3, lastLevelSibling)

        val union = persistentHashSetOf(collidingKey1, collidingKey2) + persistentHashSetOf(collidingKey3, lastLevelSibling)
        assertEquals(4, union.size)
        assertEquals(expected, union)

        val reversedUnion = persistentHashSetOf(collidingKey3, lastLevelSibling) + persistentHashSetOf(collidingKey1, collidingKey2)
        assertEquals(4, reversedUnion.size)
        assertEquals(expected, reversedUnion)
    }

    @Test
    fun `intersect and minus should handle an element absent from a bottom-level collision node`() {
        assertTrue((persistentHashSetOf(collidingKey1, collidingKey2) intersect persistentHashSetOf(collidingKey3, lastLevelSibling)).isEmpty())
        assertTrue((persistentHashSetOf(collidingKey3, lastLevelSibling) intersect persistentHashSetOf(collidingKey1, collidingKey2)).isEmpty())

        assertEquals(persistentHashSetOf(collidingKey1, collidingKey2), persistentHashSetOf(collidingKey1, collidingKey2) - persistentHashSetOf(collidingKey3, lastLevelSibling))
        assertEquals(persistentHashSetOf(collidingKey3, lastLevelSibling), persistentHashSetOf(collidingKey3, lastLevelSibling) - persistentHashSetOf(collidingKey1, collidingKey2))
    }

    @Test
    fun `minus should remove an element from a bottom-level collision node`() {
        val difference = persistentHashSetOf(collidingKey1, collidingKey2) - persistentHashSetOf(collidingKey1, lastLevelSibling)
        assertEquals(1, difference.size)
        assertEquals(persistentHashSetOf(collidingKey2), difference)

        val reversedDifference = persistentHashSetOf(collidingKey1, lastLevelSibling) - persistentHashSetOf(collidingKey1, collidingKey2)
        assertEquals(1, reversedDifference.size)
        assertEquals(persistentHashSetOf(lastLevelSibling), reversedDifference)
    }

    @Test
    fun `intersect should find the shared element inside a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1)

        val intersection = persistentHashSetOf(collidingKey1, collidingKey2) intersect persistentHashSetOf(collidingKey1, lastLevelSibling)
        assertEquals(expected, intersection)
        assertEquals(intersection, expected)
        assertEquals(listOf(collidingKey1), intersection.toList())

        val reversedIntersection = persistentHashSetOf(collidingKey1, lastLevelSibling) intersect persistentHashSetOf(collidingKey1, collidingKey2)
        assertEquals(expected, reversedIntersection)
        assertEquals(listOf(collidingKey1), reversedIntersection.toList())
    }

    @Test
    fun `containsAll should find elements inside a bottom-level collision node`() {
        assertTrue(persistentHashSetOf(collidingKey1, collidingKey2, lastLevelSibling).containsAll(persistentHashSetOf(collidingKey1, lastLevelSibling)))
        assertTrue(persistentHashSetOf(collidingKey1, collidingKey2, lastLevelSibling).containsAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertFalse(persistentHashSetOf(collidingKey1, lastLevelSibling).containsAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertFalse(persistentHashSetOf(collidingKey1, collidingKey2, lastLevelSibling).containsAll(persistentHashSetOf(collidingKey3, lastLevelSibling)))
    }

    @Test
    fun `addingAll should keep the stored element instance when the argument holds it in a subtree`() {
        val set = persistentHashSetOf(collidingKey1)

        val updated = set.addingAll(persistentHashSetOf(collidingKey1.copy(), levelOneSibling))

        assertEquals(2, updated.size)
        assertSame(collidingKey1, updated.single { it == collidingKey1 })
    }

    @Test
    fun `retainingAll should keep the stored element instance when the receiver holds it in a subtree`() {
        val set = persistentHashSetOf(collidingKey1, levelOneSibling)

        val updated = set.retainingAll(persistentHashSetOf(collidingKey1.copy()))

        assertEquals(1, updated.size)
        assertSame(collidingKey1, updated.single { it == collidingKey1 })
    }

    @Test
    fun `retainingAll should keep every stored instance when the argument's collision node is an equality-subset`() {
        val set = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3)

        val updated = set.retainingAll(persistentHashSetOf(collidingKey1.copy(), collidingKey2.copy()))

        assertEquals(2, updated.size)
        assertSame(collidingKey1, updated.single { it == collidingKey1 })
        assertSame(collidingKey2, updated.single { it == collidingKey2 })
    }

    @Test
    fun `retainingAll and removingAll that change nothing should return the same set emptied by removing`() {
        val empty = persistentHashSetOf(7).removing(7)

        assertSame(empty, empty.retainingAll(persistentHashSetOf(1, 2)))
        assertSame(empty, empty.removingAll(persistentHashSetOf(1, 2)))
    }
}
