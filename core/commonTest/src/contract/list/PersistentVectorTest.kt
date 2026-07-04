/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the pure persistent operations of the vector implementations.
 *
 * Every list here is grown with element-by-element [PersistentList.adding] calls, without builders,
 * so that the operations are performed on trie-backed vectors made of shared exactly-sized buffers.
 */
class PersistentVectorTest {

    /** Builds a vector of the given [size] containing elements `0 until size` using only persistent `adding` calls. */
    private fun vectorOfSize(size: Int): PersistentList<Int> {
        var vector: PersistentList<Int> = persistentListOf()
        for (element in 0 until size) {
            vector = vector.adding(element)
        }
        return vector
    }

    /** Compares element by element through `get` to also exercise the trie descent for every index. */
    private fun assertElementsEqual(expected: List<Int>, actual: List<Int>) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(expected[index], actual[index], "element at index $index")
        }
    }

    /** The elements of `vectorOfSize(size).addingAt(index, -1)` if the insertion is correct. */
    private fun insertedReference(size: Int, index: Int): List<Int> =
        (0 until index) + listOf(-1) + (index until size)

    @Test
    fun `appending elements one by one grows the vector through the buffer and trie height boundaries`() {
        val boundarySizes = setOf(32, 33, 64, 65, 96, 1056, 1057)
        val snapshots = mutableListOf<PersistentList<Int>>()

        var vector: PersistentList<Int> = persistentListOf()
        for (element in 0 until 1200) {
            vector = vector.adding(element)
            assertEquals(element + 1, vector.size)
            assertEquals(element, vector[element])
            if (vector.size in boundarySizes) snapshots += vector
        }

        assertElementsEqual(List(1200) { it }, vector)

        // the earlier versions are not affected by the later additions
        assertEquals(boundarySizes.size, snapshots.size)
        for (snapshot in snapshots) {
            assertElementsEqual(List(snapshot.size) { it }, snapshot)
        }
    }

    @Test
    fun `addingAt inserts into the trie root carrying elements over the full buffers`() {
        // two full leaves in the root + entirely filled 32-element tail
        val fullTailBase = vectorOfSize(96)
        for (index in listOf(0, 5, 40, 70, 96)) {
            assertElementsEqual(insertedReference(96, index), fullTailBase.addingAt(index, -1))
        }

        // three full leaves in the root + single-element tail
        val shortTailBase = vectorOfSize(97)
        for (index in listOf(0, 96, 97)) {
            assertElementsEqual(insertedReference(97, index), shortTailBase.addingAt(index, -1))
        }

        assertFailsWith<IndexOutOfBoundsException> { fullTailBase.addingAt(97, -1) }

        // the insertions do not modify the original vectors
        assertElementsEqual(List(96) { it }, fullTailBase)
        assertElementsEqual(List(97) { it }, shortTailBase)
    }

    @Test
    fun `removingAt shifts elements from the trie root through the tail`() {
        // three full leaves in the root + four elements in the tail
        val base = vectorOfSize(100)
        for (index in listOf(0, 40, 97, 99)) {
            assertElementsEqual((0 until 100).filter { it != index }, base.removingAt(index))
        }

        assertFailsWith<IndexOutOfBoundsException> { base.removingAt(100) }

        // the removals do not modify the original vector
        assertElementsEqual(List(100) { it }, base)
    }

    @Test
    fun `removingAt the only tail element pulls the last leaf buffer out of the trie`() {
        // the root consists of a single leaf: the result becomes a tail-only small vector
        val singleLeaf = vectorOfSize(33)
        assertElementsEqual((0 until 32).toList(), singleLeaf.removingAt(32))
        assertElementsEqual((1 until 33).toList(), singleLeaf.removingAt(0))

        // the root keeps other leaves: the pulled leaf becomes the new tail
        val multiLeaf = vectorOfSize(97)
        assertElementsEqual((0 until 96).toList(), multiLeaf.removingAt(96))

        // the last leaf of the second trie level is pulled out: the root is demoted one level down
        val twoLevels = vectorOfSize(1057)
        assertElementsEqual((0 until 1056).toList(), twoLevels.removingAt(1056))
        assertElementsEqual((0 until 1057).toList(), twoLevels)
    }

    @Test
    fun `replacingAt updates elements in the root and in the tail creating a new vector`() {
        val base = vectorOfSize(100)

        val rootReplaced = base.replacingAt(10, -1)
        assertElementsEqual((0 until 100).map { if (it == 10) -1 else it }, rootReplaced)

        val tailReplaced = base.replacingAt(98, -2)
        assertElementsEqual((0 until 100).map { if (it == 98) -2 else it }, tailReplaced)

        assertEquals(10, base[10])
        assertEquals(98, base[98])
        assertFailsWith<IndexOutOfBoundsException> { base.replacingAt(100, 0) }
    }

    @Test
    fun `bulk additions to a trie-backed vector append and insert collections`() {
        val base = vectorOfSize(40)

        assertSame(base, base.addingAll(emptyList()))
        assertSame(base, base.addingAllAt(20, emptyList()))

        assertElementsEqual((0 until 40) + listOf(-1, -2, -3), base.addingAll(listOf(-1, -2, -3)))
        assertElementsEqual((0 until 20) + listOf(-1, -2, -3) + (20 until 40), base.addingAllAt(20, listOf(-1, -2, -3)))

        assertFailsWith<IndexOutOfBoundsException> { base.addingAllAt(41, listOf(-1)) }
        assertElementsEqual(List(40) { it }, base)
    }

    @Test
    fun `retainingAll keeps matching elements and returns the empty vector for an empty argument`() {
        val base = vectorOfSize(40)

        assertElementsEqual(listOf(3, 7), base.retainingAll(listOf(3, 7, 100)))

        val emptied = base.retainingAll(emptyList())
        assertSame<PersistentList<Int>>(persistentListOf(), emptied)
        assertTrue(emptied.isEmpty())

        assertElementsEqual(List(40) { it }, base)
    }

    @Test
    fun `small vector operations at the buffer size boundaries`() {
        // the insertion overflowing the buffer capacity goes through the builder
        val small = vectorOfSize(30)
        assertElementsEqual((0 until 15) + (100 until 110) + (15 until 30), small.addingAllAt(15, (100 until 110).toList()))

        // the insertion at the size position appends the element
        assertElementsEqual((0 until 30) + listOf(99), small.addingAt(30, 99))

        // the positional insertion into an entirely filled buffer creates a trie-backed vector
        val full = vectorOfSize(32)
        assertElementsEqual((0 until 10) + listOf(-1) + (10 until 32), full.addingAt(10, -1))
        assertElementsEqual(List(32) { it }, full)

        // removing the only element returns the empty vector
        val emptied = persistentListOf<Int>().adding(7).removingAt(0)
        assertSame<PersistentList<Int>>(persistentListOf(), emptied)
    }
}
