/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentListBuilderRemovalTest {

    init {
        check(MAX_BUFFER_SIZE == 32) { "Test sizes assume a trie buffer size of 32" }
    }

    private fun ownedBuilderOf(range: IntRange): PersistentList.Builder<Int> {
        val builder = persistentListOf<Int>().builder()
        for (element in range) {
            assertTrue(builder.add(element))
        }
        return builder
    }

    @Test
    fun `get descends the root trie for indices before the tail`() {
        val builder = ownedBuilderOf(0..1090)
        for (index in 0..1090) {
            assertEquals(index, builder[index])
        }
        assertFailsWith<IndexOutOfBoundsException> { builder[1091] }
        assertFailsWith<IndexOutOfBoundsException> { builder[-1] }
    }

    @Test
    fun `set at an index in the root of an owned builder replaces in place without invalidating iterators`() {
        val builder = ownedBuilderOf(0..99)
        val iterator = builder.listIterator()

        assertEquals(0, builder.set(0, -1))
        assertEquals(40, builder.set(40, -2))
        assertEquals(63, builder.set(63, -3))
        assertEquals(95, builder.set(95, -4))

        assertEquals(-1, iterator.next())
        assertEquals(1, iterator.next())

        val expected = (0..99).toMutableList()
        expected[0] = -1
        expected[40] = -2
        expected[63] = -3
        expected[95] = -4
        assertEquals<List<Int>>(expected, builder)
        assertEquals<List<Int>>(expected, builder.build())
    }

    @Test
    fun `set at an index in the root of a vector-backed builder copies the frozen leaf and invalidates iterators`() {
        val vector = (0..99).toPersistentList()
        val builder = vector.builder()
        val iterator = builder.listIterator()

        assertEquals(10, builder.set(10, -10))
        assertFailsWith<ConcurrentModificationException> { iterator.next() }

        val freshIterator = builder.listIterator()
        assertEquals(11, builder.set(11, -11))
        for (index in 0..9) {
            assertEquals(index, freshIterator.next())
        }
        assertEquals(-10, freshIterator.next())
        assertEquals(-11, freshIterator.next())

        val expected = (0..99).toMutableList()
        expected[10] = -10
        expected[11] = -11
        assertEquals<List<Int>>(expected, builder)
        assertEquals<List<Int>>(expected, builder.build())

        assertEquals(100, vector.size)
        assertEquals(10, vector[10])
        assertEquals(11, vector[11])
    }

    @Test
    fun `removeAt an index in the root shifts elements across leaves like a mutable list`() {
        val builder = ownedBuilderOf(0..99)
        val reference = (0..99).toMutableList()

        assertEquals(reference.removeAt(0), builder.removeAt(0))
        assertEquals(reference.removeAt(40), builder.removeAt(40))

        assertEquals(reference.size, builder.size)
        assertEquals<List<Int>>(reference, builder)
        assertEquals<List<Int>>(reference, builder.build())
    }

    @Test
    fun `removeAt zero on a two-level trie rotates the tail element through all leaves and pulls the emptied tail`() {
        val builder = ownedBuilderOf(0..1056)

        assertEquals(0, builder.removeAt(0))

        assertEquals(1056, builder.size)
        assertEquals(1, builder[0])
        assertEquals(1056, builder[1055])
        assertEquals((1..1056).toList(), builder)
        assertEquals((1..1056).toList(), builder.build())
    }

    @Test
    fun `removing elements from the end pulls leaf buffers back from the trie until the root disappears`() {
        val builder = ownedBuilderOf(0..1056)

        for (element in 1056 downTo 25) {
            assertEquals(element, builder.removeAt(builder.size - 1))
            if (builder.size == 1000) {
                assertEquals((0..999).toList(), builder)
            }
        }

        assertEquals(25, builder.size)
        assertEquals((0..24).toList(), builder)
        assertEquals((0..24).toList(), builder.build())
    }

    @Test
    fun `removeAll matching only tail elements pulls the last leaf when the tail empties`() {
        val builder = ownedBuilderOf(0..99)

        assertFalse(builder.removeAll(listOf(200, 300)))
        assertEquals((0..99).toList(), builder)

        assertTrue(builder.removeAll(listOf(97, 99)))
        assertEquals((0..96).toList() + 98, builder)

        assertTrue(builder.removeAll(listOf(96, 98)))
        assertEquals(96, builder.size)
        assertEquals((0..95).toList(), builder)

        assertTrue(builder.add(96))
        assertEquals((0..96).toList(), builder)
        assertEquals((0..96).toList(), builder.build())
    }

    @Test
    fun `removeAll spanning several owned leaves recycles buffers and trims stale root entries`() {
        val builder = ownedBuilderOf(0..135)

        assertTrue(builder.removeAll((33..100).toList()))

        val expected = (0..32).toList() + (101..135).toList()
        assertEquals(68, builder.size)
        assertEquals(expected, builder)
        assertEquals(expected, builder.build())

        assertTrue(builder.add(999))
        assertEquals(expected + 999, builder)
    }

    @Test
    fun `removeAll trimming a two-level trie nullifies the dropped child on every level`() {
        val ownedBuilder = ownedBuilderOf(0..2145)
        assertTrue(ownedBuilder.removeAll((1057..2145).toList()))
        assertEquals(1057, ownedBuilder.size)
        assertEquals((0..1056).toList(), ownedBuilder)
        assertTrue(ownedBuilder.add(1057))
        assertEquals(1057, ownedBuilder[1057])
        assertEquals((0..1057).toList(), ownedBuilder.build())

        val vector = (0..2145).toPersistentList()
        val vectorBackedBuilder = vector.builder()
        assertTrue(vectorBackedBuilder.removeAll((1057..2145).toList()))
        assertEquals((0..1056).toList(), vectorBackedBuilder)
        assertEquals(2146, vector.size)
        assertEquals(1060, vector[1060])
        assertEquals(2145, vector[2145])
    }

    @Test
    fun `removeAll keeping only tail survivors drops the root entirely`() {
        val builder = ownedBuilderOf(0..99)

        assertTrue(builder.removeAll((0..95).toList()))
        assertEquals(4, builder.size)
        assertEquals(listOf(96, 97, 98, 99), builder)

        assertTrue(builder.add(100))
        assertEquals(listOf(96, 97, 98, 99, 100), builder)
        assertEquals(listOf(96, 97, 98, 99, 100), builder.build())
    }

    @Test
    fun `removeAll on a builder with a single-leaf root scans the root as one leaf buffer`() {
        val builder = ownedBuilderOf(0..<40)
        val reference = (0..<40).toMutableList()

        assertTrue(builder.removeAll(listOf(5, 38, 100)))
        assertTrue(reference.removeAll(listOf(5, 38, 100)))

        assertEquals<List<Int>>(reference, builder)
        assertEquals<List<Int>>(reference, builder.build())
    }
}
