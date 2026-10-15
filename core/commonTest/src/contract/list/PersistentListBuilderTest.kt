/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import tests.contract.LEAVES
import tests.contract.ONE_LEAF
import tests.contract.TAIL_ONLY
import tests.contract.forEachBuilder
import tests.contract.listBuildersOf
import tests.contract.listDepths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val listBuildersOfEveryDepth = listDepths.keys.flatMap(::listBuildersOf)

private val PersistentList.Builder<*>.modCount: Int get() = (this as PersistentVectorBuilder<*>).getModCount()

class PersistentListBuilderTest {
    @Test
    fun `set and remove from a cursor at the end without a preceding move throw ISE`() = forEachBuilder(listBuildersOf(TAIL_ONLY)) { message, builder ->
        val size = builder.size
        val iterator = builder.listIterator(size)

        builder.add(size)

        assertFailsWith<IllegalStateException>(message) { iterator.set(-1) }
        assertFailsWith<IllegalStateException>(message) { iterator.remove() }
        assertTrue(iterator.hasPrevious(), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.previous() }
    }

    /** The sweep makes the iterator's own moves after the builder's call; this one makes them before it. */
    @Test
    fun `set and remove after the iterator's own remove and add and an external add throw ISE`() = forEachBuilder(listBuildersOf(TAIL_ONLY)) { message, builder ->
        val iterator = builder.listIterator()
        val _ = iterator.next()
        iterator.remove()
        iterator.add(-1)

        builder.add(9)

        assertFailsWith<IllegalStateException>(message) { iterator.set(-2) }
        assertFailsWith<IllegalStateException>(message) { iterator.remove() }
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `next after a set of the upcoming index`() = forEachBuilder(listBuildersOfEveryDepth) { message, builder ->
        val iterator = builder.iterator()
        assertEquals(0, iterator.next(), message)

        builder[1] = -1
        assertEquals(-1, iterator.next(), message)

        val _ = builder.build()
        builder[2] = -2
        assertEquals(-2, iterator.next(), "$message after build")
    }

    @Test
    fun `next after a set that copies a leaf under an owned root`() {
        val builder = List(LEAVES) { it }.toPersistentList().builder() as PersistentVectorBuilder<Int>
        builder[40] = -40 // copies the root and the leaf of index 40, the leaf of index 0 stays shared
        val root = builder.root
        val iterator = builder.iterator()

        builder[0] = -1

        assertSame(root, builder.root)
        assertEquals(-1, iterator.next())
    }

    @Test
    fun `previous after a set of the first element from a cursor at the end`() = forEachBuilder(listBuildersOfEveryDepth) { message, builder ->
        val size = builder.size
        val iterator = builder.listIterator(size)

        builder[0] = -1

        assertEquals((size - 1 downTo 1) + listOf(-1), List(size) { iterator.previous() }, message)
    }

    @Test
    fun `previous after the iterator's set`() = forEachBuilder(listBuildersOf(ONE_LEAF)) { message, builder ->
        val iterator = builder.listIterator()
        repeat(5) { val _ = iterator.next() }
        assertEquals(5, iterator.next(), message)

        iterator.set(-1)

        assertEquals(-1, iterator.previous(), message)
    }

    @Test
    fun `set through two iterators in one leaf`() = forEachBuilder(listBuildersOf(LEAVES)) { message, builder ->
        val first = builder.listIterator(40)
        val second = builder.listIterator(41)
        assertEquals(40, first.next(), message)
        assertEquals(41, second.next(), message)

        first.set(-1) // copies the leaf of both cursors when the builder came from a list
        second.set(-2)

        assertEquals(-2, first.next(), message)
        assertEquals(listOf(-2, -1), List(2) { second.previous() }, message)
    }

    @Test
    fun `add without a preceding next inserts the element`() = forEachBuilder(listBuildersOf(TAIL_ONLY)) { message, builder ->
        val iterator = builder.listIterator()

        iterator.add(-1)

        assertEquals(listOf(-1, 0, 1, 2), builder.toList(), message)
    }

    @Test
    fun `clear of an empty builder fails live iterators and builds the canonical empty list`() = forEachBuilder(
        listOf(
            "from persistentListOf" to { persistentListOf<Int>().builder() },
            "emptied by removeAt" to { persistentListOf(0).builder().apply { removeAt(0) } },
            "the tail emptied by removeAll" to { persistentListOf(1).builder().apply { assertTrue(removeAll(listOf(1))) } },
            "a leaf and the tail emptied by removeAll" to { List(ONE_LEAF) { it }.toPersistentList().builder().apply { assertTrue(removeAll(List(ONE_LEAF) { it })) } },
        )
    ) { message, builder ->
        val iterator = builder.iterator()

        builder.clear()

        assertSame(persistentListOf(), builder.build(), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `add and build after a clear`() = forEachBuilder(listBuildersOfEveryDepth) { message, builder ->
        val size = builder.size
        val built = builder.build()
        val modCount = builder.modCount

        builder.clear()

        assertEquals(modCount + 1, builder.modCount, message)
        assertSame(persistentListOf(), builder.build(), message)
        repeat(size) { builder.add(size + it) }
        assertEquals(List(size) { size + it }, builder.build(), message)
        assertEquals(List(size) { it }, built, message)
    }
}
