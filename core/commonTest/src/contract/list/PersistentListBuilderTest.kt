/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PersistentListBuilderTest {

    // 3: tail only, 40: the root is the single leaf, 100: a root over leaves, 1100: a root over level-1 nodes over leaves
    private fun builders(size: Int): List<Pair<String, PersistentList.Builder<Int>>> {
        val fromList = List(size) { it }.toPersistentList().builder()
        val fromAdds = persistentListOf<Int>().builder().apply { addAll(List(size) { it }) }
        return listOf("from a list" to fromList, "from adds" to fromAdds)
    }

    @Test
    fun `next after a set of the upcoming index returns the new element at every depth whether the builder came from a list or from adds`() {
        for (size in listOf(3, 40, 100, 1100)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.iterator()
                assertEquals(0, iterator.next(), "$flavour at size $size")

                builder[1] = -1
                assertEquals(-1, iterator.next(), "$flavour at size $size")

                val _ = builder.build()
                builder[2] = -2
                assertEquals(-2, iterator.next(), "$flavour at size $size after build")
            }
        }
    }

    @Test
    fun `next after a set that copies a leaf under an owned root returns the new element`() {
        val builder = List(100) { it }.toPersistentList().builder() as PersistentVectorBuilder<Int>
        builder[40] = 40 // copies the root and the leaf of indices 32 to 63, the leaf of indices 0 to 31 stays unowned
        val root = builder.root
        val iterator = builder.iterator()

        builder[0] = -1

        assertSame(root, builder.root)
        assertEquals(-1, iterator.next())
    }

    @Test
    fun `previous after a set of the first element returns the new element from a cursor at the end at every depth`() {
        for (size in listOf(40, 100, 1100)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.listIterator(size)

                builder[0] = -1

                assertEquals((size - 1 downTo 1) + listOf(-1), List(size) { iterator.previous() }, "$flavour at size $size")
            }
        }
    }

    @Test
    fun `previous after the iterator set of the visited index returns the new element`() {
        for ((flavour, builder) in builders(40)) {
            val iterator = builder.listIterator()
            repeat(5) { val _ = iterator.next() }
            assertEquals(5, iterator.next(), flavour)

            iterator.set(-1)

            assertEquals(-1, iterator.previous(), flavour)
        }
    }

    @Test
    fun `next and previous of two iterators in one leaf return the elements set through the other`() {
        for ((flavour, builder) in builders(100)) {
            val first = builder.listIterator(40)
            val second = builder.listIterator(41)
            assertEquals(40, first.next(), flavour)
            assertEquals(41, second.next(), flavour)

            first.set(-1) // copies the leaf of indices 32 to 63 when the builder came from a list
            second.set(-2)

            assertEquals(-2, first.next(), flavour)
            assertEquals(listOf(-2, -1), List(2) { second.previous() }, flavour)
        }
    }

    @Test
    fun `reverse of the builder reverses all the elements whether the builder came from a list or from adds`() {
        for ((flavour, builder) in builders(40)) {
            builder.reverse()

            assertEquals((39 downTo 0).toList(), builder.toList(), flavour)
        }
    }

    @Test
    fun `an add and a removeAt that cancel out in size still invalidate a live iterator`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.iterator()
            val _ = iterator.next()

            builder.add(-3)
            val _ = builder.removeAt(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }
    }
}
