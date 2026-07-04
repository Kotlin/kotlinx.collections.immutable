/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.toPersistentList
import kotlin.test.*

class PersistentListIteratorTest {

    @Test
    fun `persistent list iterator walks backward from the end across tail and trie`() {
        // 70 elements: root trie of two leaves (64 elements, height 2) plus a tail of 6
        val list = List(70) { it }.toPersistentList()
        val iterator = list.listIterator(list.size)

        assertFalse(iterator.hasNext())
        assertTrue(iterator.hasPrevious())

        // backward through the tail, across the tail -> trie boundary,
        // and across the leaf boundary inside the trie
        for (i in 69 downTo 0) {
            assertEquals(i, iterator.previousIndex())
            assertEquals(i, iterator.previous())
            assertEquals(i, iterator.nextIndex())
        }
        assertFalse(iterator.hasPrevious())
        assertFailsWith<NoSuchElementException> { iterator.previous() }

        // the same iterator instance walks forward again over the whole list
        for (i in 0..69) {
            assertEquals(i, iterator.next())
        }
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun `builder list iterator supports mutation mid-iteration across trie and tail`() {
        // 40 elements: root trie of a single leaf (32 elements) plus a tail of 8
        val builder = List(40) { it }.toPersistentList().builder()
        val expected = MutableList(40) { it }
        val actual = builder.listIterator()
        val expectedIterator = expected.listIterator()

        // forward inside the trie part
        repeat(10) {
            assertEquals(expectedIterator.next(), actual.next())
        }

        // replace the last returned element; the builder iterator re-syncs its trie iterator
        expectedIterator.set(-10)
        actual.set(-10)

        // forward across the trie -> tail boundary to the end
        while (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), actual.next())
        }
        assertFalse(actual.hasNext())

        // backward through the tail part
        repeat(8) {
            assertEquals(expectedIterator.previous(), actual.previous())
        }

        // structural mutations through the iterator force a full iterator reset
        expectedIterator.remove()
        actual.remove()
        expectedIterator.add(100)
        actual.add(100)

        // backward across the tail -> trie boundary all the way to the beginning
        while (expectedIterator.hasPrevious()) {
            assertEquals(expectedIterator.previous(), actual.previous())
        }
        assertFalse(actual.hasPrevious())
        assertFailsWith<NoSuchElementException> { actual.previous() }

        assertEquals<List<Int>>(expected, builder)
        assertEquals<List<Int>>(expected, builder.build())
    }

    @Test
    fun `builder list iterator add grows the trie and keeps iterating after resets`() {
        val builder = List(33) { it }.toPersistentList().builder()
        val iterator = builder.listIterator(builder.size)

        // append through the iterator; each add re-syncs the trie iterator,
        // and the root trie height grows twice on the way (at sizes 65 and 1057)
        while (builder.size < 1057) {
            iterator.add(builder.size)
        }
        assertEquals(1057, builder.size)
        assertFalse(iterator.hasNext())
        assertEquals(1057, iterator.nextIndex())

        // the same iterator instance walks all the way back through the grown trie,
        // crossing leaf and node boundaries at every level
        for (i in 1056 downTo 0) {
            assertEquals(i, iterator.previous())
        }
        assertFalse(iterator.hasPrevious())
        assertEquals(0, iterator.next())

        assertEquals(List(1057) { it }, builder.build())
    }
}
