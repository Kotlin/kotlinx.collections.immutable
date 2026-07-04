/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the root-trie paths of the persistent list builder: reading and replacing elements
 * stored in the root, removing elements that shift the tail through the root leaves,
 * pulling leaf buffers back out of the trie when removals empty the tail,
 * and bulk removals that compact, trim or entirely drop the root.
 *
 * Layout reminder: a leaf buffer holds 32 elements and the last <= 32 elements live in the tail;
 * sizes 33..64 keep a single-leaf root (rootShift = 0), sizes 65..1056 use one trie level
 * (rootShift = 5), and sizes 1057+ use two levels (rootShift = 10).
 */
class PersistentListBuilderRemovalTest {

    /**
     * Builds a builder that exclusively owns all its buffers by adding elements one by one.
     */
    private fun ownedBuilderOf(range: IntRange): PersistentList.Builder<Int> {
        val builder = persistentListOf<Int>().builder()
        for (element in range) {
            assertTrue(builder.add(element))
        }
        return builder
    }

    @Test
    fun `get descends the root trie for indices before the tail`() {
        // Two-level trie: root -> node -> leaf, so bufferFor loops twice for root indices.
        val builder = ownedBuilderOf(0..1090) // root holds 0..1087, tail holds 1088..1090
        for (index in 0..1090) {
            assertEquals(index, builder[index])
        }
        assertFailsWith<IndexOutOfBoundsException> { builder[1091] }
        assertFailsWith<IndexOutOfBoundsException> { builder[-1] }
    }

    @Test
    fun `set at an index in the root of an owned builder replaces in place without invalidating iterators`() {
        val builder = ownedBuilderOf(0..99) // root holds 0..95 in three leaves, tail holds 96..99
        val iterator = builder.listIterator()

        assertEquals(0, builder.set(0, -1))
        assertEquals(40, builder.set(40, -2))
        assertEquals(63, builder.set(63, -3))
        assertEquals(95, builder.set(95, -4))

        // The builder owns all leaves, so no structural change happened
        // and the previously created iterator must still be usable.
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

        // The first replacement copies the frozen root path, which is a structural change.
        assertEquals(10, builder.set(10, -10))
        assertFailsWith<ConcurrentModificationException> { iterator.next() }

        // The second replacement hits the now-owned leaf: no structural change anymore.
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

        // The source vector must not observe builder modifications.
        assertEquals(100, vector.size)
        assertEquals(10, vector[10])
        assertEquals(11, vector[11])
    }

    @Test
    fun `removeAt an index in the root shifts elements across leaves like a mutable list`() {
        val builder = ownedBuilderOf(0..99) // three root leaves + tail of 4
        val reference = (0..99).toMutableList()

        // Removing the first element rotates the first tail element through every root leaf.
        assertEquals(reference.removeAt(0), builder.removeAt(0))
        // Removing from the middle shifts only the leaves after the removal point.
        assertEquals(reference.removeAt(40), builder.removeAt(40))

        assertEquals(reference.size, builder.size)
        assertEquals<List<Int>>(reference, builder)
        assertEquals<List<Int>>(reference, builder.build())
    }

    @Test
    fun `removeAt zero on a two-level trie rotates the tail element through all leaves and pulls the emptied tail`() {
        val builder = ownedBuilderOf(0..1056) // two-level root of 1056 elements + tail of 1

        // The single tail element rotates into the root, the emptied tail is replaced
        // by the last leaf pulled from the trie, and the trie height decreases.
        assertEquals(0, builder.removeAt(0))

        assertEquals(1056, builder.size)
        assertEquals(1, builder[0])
        assertEquals(1056, builder[1055])
        assertEquals<List<Int>>((1..1056).toList(), builder)
        assertEquals<List<Int>>((1..1056).toList(), builder.build())
    }

    @Test
    fun `removing elements from the end pulls leaf buffers back from the trie until the root disappears`() {
        val builder = ownedBuilderOf(0..1056) // rootShift = 10 right after the height promotion

        // Walks the size back through every boundary: 1057 -> height decrease to one level,
        // each multiple of 32 -> pull of the last leaf, 64 -> root demoted to a single leaf,
        // 33 -> root dropped completely, then plain tail removals.
        for (element in 1056 downTo 25) {
            assertEquals(element, builder.removeAt(builder.size - 1))
            if (builder.size == 1000) {
                assertEquals<List<Int>>((0..999).toList(), builder)
            }
        }

        assertEquals(25, builder.size)
        assertEquals<List<Int>>((0..24).toList(), builder)
        assertEquals<List<Int>>((0..24).toList(), builder.build())
    }

    @Test
    fun `removeAll matching only tail elements pulls the last leaf when the tail empties`() {
        val builder = ownedBuilderOf(0..99) // three root leaves + tail of 96..99

        // Nothing matches: no elements are removed at all.
        assertFalse(builder.removeAll(listOf(200, 300)))
        assertEquals<List<Int>>((0..99).toList(), builder)

        // Only part of the tail matches: the root is untouched, the tail shrinks.
        assertTrue(builder.removeAll(listOf(97, 99)))
        assertEquals<List<Int>>((0..96).toList() + 98, builder)

        // The rest of the tail matches: the emptied tail is refilled
        // with the last leaf pulled from the root.
        assertTrue(builder.removeAll(listOf(96, 98)))
        assertEquals(96, builder.size)
        assertEquals<List<Int>>((0..95).toList(), builder)

        // The builder stays fully functional after the pull.
        assertTrue(builder.add(96))
        assertEquals<List<Int>>((0..96).toList(), builder)
        assertEquals<List<Int>>((0..96).toList(), builder.build())
    }

    @Test
    fun `removeAll spanning several owned leaves recycles buffers and trims stale root entries`() {
        val builder = ownedBuilderOf(0..135) // four root leaves + tail of 128..135

        // Removes the suffix of leaf 1, all of leaf 2 and the prefix of leaf 3:
        // survivors are compacted into fewer leaves reusing the builder's own buffers,
        // and the trimmed root drops its now-unused children.
        assertTrue(builder.removeAll((33..100).toList()))

        val expected = (0..32).toList() + (101..135).toList()
        assertEquals(68, builder.size)
        assertEquals<List<Int>>(expected, builder)
        assertEquals<List<Int>>(expected, builder.build())

        assertTrue(builder.add(999))
        assertEquals<List<Int>>(expected + 999, builder)
    }

    @Test
    fun `removeAll trimming a two-level trie nullifies the dropped child on every level`() {
        // Owned builder: the trimmed nodes are already mutable and are cleaned up in place.
        val ownedBuilder = ownedBuilderOf(0..1090) // root children: 1024 elements + 2 leaves, tail of 3
        assertTrue(ownedBuilder.removeAll((1057..1090).toList()))
        assertEquals(1057, ownedBuilder.size)
        assertEquals<List<Int>>((0..1056).toList(), ownedBuilder)
        assertTrue(ownedBuilder.add(1057))
        assertEquals(1057, ownedBuilder[1057])
        assertEquals<List<Int>>((0..1057).toList(), ownedBuilder.build())

        // Vector-backed builder: the same removal must copy the frozen nodes
        // and leave the source vector intact.
        val vector = (0..1090).toPersistentList()
        val vectorBackedBuilder = vector.builder()
        assertTrue(vectorBackedBuilder.removeAll((1057..1090).toList()))
        assertEquals<List<Int>>((0..1056).toList(), vectorBackedBuilder)
        assertEquals(1091, vector.size)
        assertEquals(1060, vector[1060])
        assertEquals(1090, vector[1090])
    }

    @Test
    fun `removeAll keeping only tail survivors drops the root entirely`() {
        val builder = ownedBuilderOf(0..99)

        // Every root element matches, only the four tail elements survive:
        // the root becomes null and the builder degrades to a tail-only list.
        assertTrue(builder.removeAll((0..95).toList()))
        assertEquals(4, builder.size)
        assertEquals<List<Int>>(listOf(96, 97, 98, 99), builder)

        assertTrue(builder.add(100))
        assertEquals<List<Int>>(listOf(96, 97, 98, 99, 100), builder)
        assertEquals<List<Int>>(listOf(96, 97, 98, 99, 100), builder.build())
    }

    @Test
    fun `removeAll on a builder with a single-leaf root scans the root as one leaf buffer`() {
        // The root with rootShift = 0 is iterated through the single-element leaf iterator,
        // the same iterator the bulk insertion uses to shift leaves.
        val builder = ownedBuilderOf(0 until 40)
        val reference = (0 until 40).toMutableList()

        assertTrue(builder.removeAll(listOf(5, 38, 100)))
        assertTrue(reference.removeAll(listOf(5, 38, 100)))

        assertEquals<List<Int>>(reference, builder)
        assertEquals<List<Int>>(reference, builder.build())
    }
}
