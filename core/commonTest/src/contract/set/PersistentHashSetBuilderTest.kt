/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.PersistentUnorderedSet
import kotlinx.collections.immutable.persistentUnorderedSetOf
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashSetBuilderTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `should correctly iterate after removing integer element`() {
        val removedElement = 0
        val set = persistentUnorderedSetOf(1, 2, 3, removedElement, 32)

        validate(set, removedElement)
    }

    @Test
    fun `should correctly iterate after removing IntWrapper element`() {
        val removedElement = IntWrapper(0, 0)
        val set = persistentUnorderedSetOf(
            removedElement,
            IntWrapper(1, 0),
            IntWrapper(2, 32),
            IntWrapper(3, 32)
        )

        validate(set, removedElement)
    }

    private fun <E> validate(set: PersistentUnorderedSet<E>, removedElement: E) {
        val builder = set.builder()
        val iterator = builder.iterator()

        val expectedCount = set.size
        var actualCount = 0

        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element == removedElement) {
                iterator.remove()
            }
            actualCount++
        }

        val resultSet = builder.build()
        for (element in set) {
            if (element != removedElement) {
                assertTrue(element in resultSet)
            } else {
                assertFalse(element in resultSet)
            }
        }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `removing twice on iterators throws IllegalStateException`() {
        val set = persistentUnorderedSetOf(1, 2, 3, 0, 32)
        val builder = set.builder()
        val iterator = builder.iterator()

        assertFailsWith<IllegalStateException> {
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element == 0) iterator.remove()
                if (element == 0) {
                    iterator.remove()
                    iterator.remove()
                }
            }
        }
    }

    @Test
    fun `removing elements from different iterators throws ConcurrentModificationException`() {
        val set = persistentUnorderedSetOf(1, 2, 3, 0, 32)
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            while (iterator1.hasNext()) {
                val element1 = iterator1.next()
                val _ = iterator2.next()
                if (element1 == 0) iterator1.remove()
                if (element1 == 2) iterator2.remove()
            }
        }
    }

    @Test
    fun `removing element from one iterator and accessing another throws ConcurrentModificationException`() {
        val set = persistentUnorderedSetOf(1, 2, 3)
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            val _ = iterator1.next()
            iterator1.remove()
            iterator2.next()
        }
    }

    @Test
    fun `retainAll should promote the only remaining element to the root`() {
        val builder = persistentUnorderedSetOf(1, 33).builder()
        builder.retainAll(persistentUnorderedSetOf(1, 65))
        val expected = persistentUnorderedSetOf(1)

        assertTrue(expected.equals(builder))
        assertEquals(expected, builder.build())
        assertEquals(builder.build(), expected)
    }

    @Test
    fun `addAll should not duplicate an element shared with a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1, a2, sibling)

        val builder = persistentUnorderedSetOf(a1, a2).builder()
        assertTrue(builder.addAll(persistentUnorderedSetOf(a1, sibling)))
        assertEquals(3, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentUnorderedSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentUnorderedSetOf(a1, a2)))
        assertEquals(3, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `addAll should insert a new element into a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1, a2, a3, sibling)

        val builder = persistentUnorderedSetOf(a1, a2).builder()
        assertTrue(builder.addAll(persistentUnorderedSetOf(a3, sibling)))
        assertEquals(4, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentUnorderedSetOf(a3, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentUnorderedSetOf(a1, a2)))
        assertEquals(4, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `retainAll should find elements inside a bottom-level collision node`() {
        val expected = persistentUnorderedSetOf(a1)

        val builder = persistentUnorderedSetOf(a1, a2).builder()
        assertTrue(builder.retainAll(persistentUnorderedSetOf(a1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentUnorderedSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.retainAll(persistentUnorderedSetOf(a1, a2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `removeAll should remove elements stored in a bottom-level collision node`() {
        val builder = persistentUnorderedSetOf(a1, a2).builder()
        assertTrue(builder.removeAll(persistentUnorderedSetOf(a1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(persistentUnorderedSetOf(a2), builder.build())

        val reversedBuilder = persistentUnorderedSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.removeAll(persistentUnorderedSetOf(a1, a2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(persistentUnorderedSetOf(sibling), reversedBuilder.build())
    }
}
