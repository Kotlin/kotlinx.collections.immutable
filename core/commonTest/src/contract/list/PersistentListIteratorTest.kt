/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.*

class PersistentListIteratorTest {

    init {
        check(MAX_BUFFER_SIZE == 32) { "Test sizes assume a trie buffer size of 32" }
    }

    @Test
    fun `persistent list iterator walks backward from the end across tail and trie`() {
        val list = List(70) { it }.toPersistentList()
        val iterator = list.listIterator(list.size)

        assertFalse(iterator.hasNext())
        assertTrue(iterator.hasPrevious())

        for (i in 69 downTo 0) {
            assertEquals(i, iterator.previousIndex())
            assertEquals(i, iterator.previous())
            assertEquals(i, iterator.nextIndex())
        }
        assertFalse(iterator.hasPrevious())
        assertFailsWith<NoSuchElementException> { iterator.previous() }

        for (i in 0..69) {
            assertEquals(i, iterator.next())
        }
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun `builder list iterator supports mutation mid-iteration across trie and tail`() {
        val builder = List(40) { it }.toPersistentList().builder()
        val expected = MutableList(40) { it }
        val actual = builder.listIterator()
        val expectedIterator = expected.listIterator()

        repeat(10) {
            assertEquals(expectedIterator.next(), actual.next())
        }

        expectedIterator.set(-10)
        actual.set(-10)

        while (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), actual.next())
        }
        assertFalse(actual.hasNext())

        repeat(8) {
            assertEquals(expectedIterator.previous(), actual.previous())
        }

        expectedIterator.remove()
        actual.remove()
        expectedIterator.add(100)
        actual.add(100)

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

        while (builder.size < 1057) {
            iterator.add(builder.size)
        }
        assertEquals(1057, builder.size)
        assertFalse(iterator.hasNext())
        assertEquals(1057, iterator.nextIndex())

        for (i in 1056 downTo 0) {
            assertEquals(i, iterator.previous())
        }
        assertFalse(iterator.hasPrevious())
        assertEquals(0, iterator.next())

        assertEquals(List(1057) { it }, builder.build())
    }

    @Test
    fun `builder list iterator remove and set throw IllegalStateException without a preceding next or previous`() {
        val builder = List(40) { it }.toPersistentList().builder()
        val iterator = builder.listIterator()

        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertFailsWith<IllegalStateException> { iterator.set(-1) }

        assertEquals(0, iterator.next())
        iterator.remove()
        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertFailsWith<IllegalStateException> { iterator.set(-1) }

        assertEquals(1, iterator.next())
        iterator.add(100)
        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertFailsWith<IllegalStateException> { iterator.set(-2) }

        val expected = listOf(1, 100) + (2..<40)
        assertEquals(expected, builder)
        assertEquals(expected, builder.build())
    }
}
