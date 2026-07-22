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

class PersistentListBuilderInsertionTest {

    init {
        checkTrieShapeAssumptions()
    }

    private fun ownedBuilderOf(size: Int): PersistentList.Builder<Int> {
        val builder = persistentListOf<Int>().builder()
        for (element in 0..<size) {
            assertTrue(builder.add(element))
        }
        return builder
    }

    private fun assertBuilderContents(expected: List<Int>, builder: PersistentList.Builder<Int>) {
        assertEquals(expected.size, builder.size)
        assertEquals(expected, builder)
        for (index in expected.indices) {
            assertEquals(expected[index], builder[index], "element at index $index")
        }
        assertEquals(expected, builder.build())
    }

    @Test
    fun `add grows the builder through root creation and root pushes and trie height increases`() {
        val expected = List(1100) { it }
        val checkpoints = setOf(33, 64, 65, 97, 1056, 1057, 1089)

        val builder = persistentListOf<Int>().builder()
        for (element in 0..<1100) {
            assertTrue(builder.add(element))
            assertEquals(element + 1, builder.size)
            if (builder.size in checkpoints) {
                assertEquals(expected.subList(0, builder.size), builder)
            }
        }

        assertBuilderContents(expected, builder)
    }

    @Test
    fun `add at an index in a full tail pushes the filled tail into the root as a leaf`() {
        val builder = ownedBuilderOf(64)
        val reference = (0..<64).toMutableList()

        builder.add(40, -1)
        reference.add(40, -1)

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `add at an index in the root carries overflowing elements through the leaves into the tail`() {
        val builder = ownedBuilderOf(100)
        val reference = (0..<100).toMutableList()

        builder.add(0, -1)
        reference.add(0, -1)

        builder.add(40, -2)
        reference.add(40, -2)

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the tail region splits elements into new leaf buffers and promotes the full root`() {
        val builder = ownedBuilderOf(40)
        val reference = (0..<40).toMutableList()
        val elements = List(70) { 100 + it }

        assertTrue(builder.addAll(35, elements))
        assertTrue(reference.addAll(35, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the single-leaf root of an owned builder shifts its tail in place`() {
        val builder = ownedBuilderOf(40)
        val reference = (0..<40).toMutableList()
        val elements = List(10) { 100 + it }

        assertTrue(builder.addAll(5, elements))
        assertTrue(reference.addAll(5, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index in the first of two frozen root leaves shifts the following leaf into new buffers`() {
        val vector = (0..<70).toPersistentList()
        val builder = vector.builder()
        val reference = (0..<70).toMutableList()
        val elements = List(10) { 100 + it }

        assertTrue(builder.addAll(10, elements))
        assertTrue(reference.addAll(10, elements))

        assertBuilderContents(reference, builder)

        assertEquals((0..<70).toList(), vector)
    }

    @Test
    fun `addAll at an index in the last root leaf overflows into an extra fresh buffer`() {
        val builder = ownedBuilderOf(70)
        val reference = (0..<70).toMutableList()
        val elements = List(40) { 100 + it }

        assertTrue(builder.addAll(40, elements))
        assertTrue(reference.addAll(40, elements))

        assertBuilderContents(reference, builder)
    }

    @Test
    fun `addAll at an index preserving the tail size shifts whole leaves to the right`() {
        val builder = ownedBuilderOf(70)
        val reference = (0..<70).toMutableList()
        val elements = List(32) { 100 + it }

        assertTrue(builder.addAll(10, elements))
        assertTrue(reference.addAll(10, elements))

        assertBuilderContents(reference, builder)
    }
}
