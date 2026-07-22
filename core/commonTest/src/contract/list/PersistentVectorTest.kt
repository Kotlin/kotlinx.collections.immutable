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

class PersistentVectorTest {

    init {
        checkTrieShapeAssumptions()
    }

    private fun vectorOfSize(size: Int): PersistentList<Int> {
        var vector: PersistentList<Int> = persistentListOf()
        for (element in 0..<size) {
            vector = vector.adding(element)
        }
        return vector
    }

    private fun assertElementsEqual(expected: List<Int>, actual: List<Int>) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(expected[index], actual[index], "element at index $index")
        }
    }

    private fun insertedReference(size: Int, index: Int): List<Int> =
        (0..<index) + listOf(-1) + (index..<size)

    @Test
    fun `appending elements one by one grows the vector through the buffer and trie height boundaries`() {
        val boundarySizes = setOf(32, 33, 64, 65, 96, 1056, 1057)
        val snapshots = mutableListOf<PersistentList<Int>>()

        var vector: PersistentList<Int> = persistentListOf()
        for (element in 0..<1200) {
            vector = vector.adding(element)
            assertEquals(element + 1, vector.size)
            assertEquals(element, vector[element])
            if (vector.size in boundarySizes) snapshots += vector
        }

        assertElementsEqual(List(1200) { it }, vector)

        assertEquals(boundarySizes.size, snapshots.size)
        for (snapshot in snapshots) {
            assertElementsEqual(List(snapshot.size) { it }, snapshot)
        }
    }

    @Test
    fun `addingAt inserts into the trie root carrying elements over the full buffers`() {
        val fullTailBase = vectorOfSize(96)
        for (index in listOf(0, 5, 40, 70, 96)) {
            assertElementsEqual(insertedReference(96, index), fullTailBase.addingAt(index, -1))
        }

        val shortTailBase = vectorOfSize(97)
        for (index in listOf(0, 96, 97)) {
            assertElementsEqual(insertedReference(97, index), shortTailBase.addingAt(index, -1))
        }

        assertFailsWith<IndexOutOfBoundsException> { fullTailBase.addingAt(97, -1) }

        assertElementsEqual(List(96) { it }, fullTailBase)
        assertElementsEqual(List(97) { it }, shortTailBase)
    }

    @Test
    fun `removingAt shifts elements from the trie root through the tail`() {
        val base = vectorOfSize(100)
        for (index in listOf(0, 40, 97, 99)) {
            assertElementsEqual((0..<100).filter { it != index }, base.removingAt(index))
        }

        assertFailsWith<IndexOutOfBoundsException> { base.removingAt(100) }

        assertElementsEqual(List(100) { it }, base)
    }

    @Test
    fun `removingAt the only tail element pulls the last leaf buffer out of the trie`() {
        val singleLeaf = vectorOfSize(33)
        assertElementsEqual((0..<32).toList(), singleLeaf.removingAt(32))
        assertElementsEqual((1..<33).toList(), singleLeaf.removingAt(0))

        val multiLeaf = vectorOfSize(97)
        assertElementsEqual((0..<96).toList(), multiLeaf.removingAt(96))

        val twoLevels = vectorOfSize(1057)
        assertElementsEqual((0..<1056).toList(), twoLevels.removingAt(1056))
        assertElementsEqual((0..<1057).toList(), twoLevels)
    }

    @Test
    fun `replacingAt updates elements in the root and in the tail creating a new vector`() {
        val base = vectorOfSize(100)

        val rootReplaced = base.replacingAt(10, -1)
        assertElementsEqual((0..<100).map { if (it == 10) -1 else it }, rootReplaced)

        val tailReplaced = base.replacingAt(98, -2)
        assertElementsEqual((0..<100).map { if (it == 98) -2 else it }, tailReplaced)

        assertEquals(10, base[10])
        assertEquals(98, base[98])
        assertFailsWith<IndexOutOfBoundsException> { base.replacingAt(100, 0) }
    }

    @Test
    fun `bulk additions to a trie-backed vector append and insert collections`() {
        val base = vectorOfSize(40)

        assertSame(base, base.addingAll(emptyList()))
        assertSame(base, base.addingAllAt(20, emptyList()))

        assertElementsEqual((0..<40) + listOf(-1, -2, -3), base.addingAll(listOf(-1, -2, -3)))
        assertElementsEqual((0..<20) + listOf(-1, -2, -3) + (20..<40), base.addingAllAt(20, listOf(-1, -2, -3)))

        assertFailsWith<IndexOutOfBoundsException> { base.addingAllAt(41, listOf(-1)) }
        assertElementsEqual(List(40) { it }, base)
    }

    @Test
    fun `retainingAll keeps matching elements and returns an empty vector for an empty argument`() {
        val base = vectorOfSize(40)

        assertElementsEqual(listOf(3, 7), base.retainingAll(listOf(3, 7, 100)))

        val emptied = base.retainingAll(emptyList())
        assertTrue(emptied.isEmpty())
        assertEquals(emptyList(), emptied)

        assertElementsEqual(List(40) { it }, base)
    }

    @Test
    fun `small vector operations at the buffer size boundaries`() {
        val small = vectorOfSize(30)
        assertElementsEqual((0..<15) + (100..<110) + (15..<30), small.addingAllAt(15, (100..<110).toList()))

        assertElementsEqual((0..<30) + listOf(99), small.addingAt(30, 99))

        val full = vectorOfSize(32)
        assertElementsEqual((0..<10) + listOf(-1) + (10..<32), full.addingAt(10, -1))
        assertElementsEqual(List(32) { it }, full)

        val emptied = persistentListOf<Int>().adding(7).removingAt(0)
        assertTrue(emptied.isEmpty())
    }
}
