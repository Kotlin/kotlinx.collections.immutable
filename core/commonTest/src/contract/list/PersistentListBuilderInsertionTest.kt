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
import kotlin.test.assertTrue

/**
 * Exercises the insertion paths of the persistent list builder that involve the root trie:
 * appending over a filled tail (pushing the tail into the root, promoting the trie one level up),
 * positional insertion that carries elements over the full leaves, and positional bulk insertion
 * that shifts and splits leaf buffers.
 *
 * Layout reminder: a leaf buffer holds 32 elements and the last <= 32 elements live in the tail.
 * A builder grown past 32 elements by plain `add` calls keeps a root with `rootShift = 0`
 * (the root is a single leaf) until the 65th element promotes it to a real trie (`rootShift = 5`),
 * and the 1057th element adds the second trie level (`rootShift = 10`).
 *
 * Every operation is mirrored on an [ArrayList] reference, and the resulting contents
 * are compared element by element.
 */
class PersistentListBuilderInsertionTest {

    /**
     * Builds a builder that exclusively owns all its buffers by adding elements one by one.
     */
    private fun ownedBuilderOf(size: Int): PersistentList.Builder<Int> {
        val builder = persistentListOf<Int>().builder()
        for (element in 0 until size) {
            assertTrue(builder.add(element))
        }
        return builder
    }

    /**
     * Checks [builder] against [expected] through iteration, indexed access and the built list.
     */
    private fun assertBuilderContents(expected: List<Int>, builder: PersistentList.Builder<Int>) {
        assertEquals(expected.size, builder.size)
        assertEquals<List<Int>>(expected, builder)
        for (index in expected.indices) {
            assertEquals(expected[index], builder[index], "element at index $index")
        }
        assertEquals<List<Int>>(expected, builder.build())
    }

    @Test
    fun `add grows the builder through root creation and root pushes and trie height increases`() {
        val expected = List(1100) { it }
        // Sizes right after the interesting transitions:
        // 33 - the filled tail becomes the root leaf, 65 - the single-leaf root is promoted to a trie,
        // 97 - a filled tail is pushed into a root with free slots, 1057 - the second level promotion,
        // 1089 - a push descending two trie levels.
        val checkpoints = setOf(33, 64, 65, 97, 1056, 1057, 1089)

        val builder = persistentListOf<Int>().builder()
        for (element in 0 until 1100) {
            assertTrue(builder.add(element))
            assertEquals(element + 1, builder.size)
            if (builder.size in checkpoints) {
                assertEquals<List<Int>>(expected.subList(0, builder.size), builder)
            }
        }

        assertBuilderContents(expected, builder)
    }

    @Test
    fun `add at index zero of a two-level trie carries elements through the full first block`() {
        val builder = ownedBuilderOf(1057) // root children: a full 1024-element block + one leaf, tail of 1
        val reference = (0 until 1057).toMutableList()

        builder.add(0, -1)
        reference.add(0, -1)

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `add at an index in a full tail pushes the filled tail into the root as a leaf`() {
        // A single full root leaf and an entirely filled tail: the insertion overflows the tail,
        // and pushing it also promotes the single-leaf root to a real trie.
        val builder = ownedBuilderOf(64)
        val reference = (0 until 64).toMutableList()

        builder.add(40, -1)
        reference.add(40, -1)

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `add at an index in the root carries overflowing elements through the leaves into the tail`() {
        val builder = ownedBuilderOf(100) // three full root leaves + tail of four
        val reference = (0 until 100).toMutableList()

        // Inserting at the head pops the last element out of every leaf into the following one,
        // and the element carried out of the last leaf lands at the head of the tail.
        builder.add(0, -1)
        reference.add(0, -1)

        // Inserting into the middle leaf shifts only the leaves after the insertion point.
        builder.add(40, -2)
        reference.add(40, -2)

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the tail region splits elements into new leaf buffers and promotes the full root`() {
        val builder = ownedBuilderOf(40) // single full root leaf + tail of eight
        val reference = (0 until 40).toMutableList()
        val elements = List(70) { 100 + it }

        // The insertion point is inside the tail, and the collection is over two buffers big:
        // the tail is split into new leaves (one of them filled from the collection alone),
        // which no longer fit next to the entirely filled root, so the root is promoted.
        assertTrue(builder.addAll(35, elements))
        assertTrue(reference.addAll(35, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the single-leaf root of an owned builder shifts its tail in place`() {
        val builder = ownedBuilderOf(40) // single full root leaf + owned tail of eight
        val reference = (0 until 40).toMutableList()
        val elements = List(10) { 100 + it }

        // The insertion point is inside the root, which consists of a single leaf,
        // so the leaf iteration starts right at the insertion point; the new tail is bigger
        // than the old one, and the owned tail buffer is shifted right without copying.
        assertTrue(builder.addAll(5, elements))
        assertTrue(reference.addAll(5, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the first of two frozen root leaves shifts the following leaf into new buffers`() {
        val vector = (0 until 70).toPersistentList()
        val builder = vector.builder() // frozen root of two leaves + frozen tail of six
        val reference = (0 until 70).toMutableList()
        val elements = List(10) { 100 + it }

        // The leaf after the insertion point is shifted right into a copy (the builder
        // does not own the vector's buffers), and its overflow prefills the new tail.
        assertTrue(builder.addAll(10, elements))
        assertTrue(reference.addAll(10, elements))

        assertBuilderContents(reference, builder)

        // The source vector must not observe builder modifications.
        assertEquals<List<Int>>((0 until 70).toList(), vector)
    }

    @Test
    fun `addAll at an index in the last root leaf overflows into an extra fresh buffer`() {
        val builder = ownedBuilderOf(70) // two full root leaves + tail of six
        val reference = (0 until 70).toMutableList()
        val elements = List(40) { 100 + it }

        // The insertion point is inside the last leaf and the shifted-out elements
        // do not fit the single remaining split buffer, so an extra buffer is allocated
        // between the leaf remainder and the new tail.
        assertTrue(builder.addAll(40, elements))
        assertTrue(reference.addAll(40, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index preserving the tail size shifts whole leaves to the right`() {
        val builder = ownedBuilderOf(70) // two full root leaves + tail of six
        val reference = (0 until 70).toMutableList()
        val elements = List(32) { 100 + it } // exactly one leaf worth keeps the tail size unchanged

        // The new tail is not bigger than the old one: the tail is copied as is,
        // and the leaf after the insertion point moves into the root wholesale.
        assertTrue(builder.addAll(10, elements))
        assertTrue(reference.addAll(10, elements))

        assertBuilderContents(reference, builder)
    }
}
