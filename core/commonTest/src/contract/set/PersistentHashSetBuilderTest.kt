/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.persistentHashSetOf
import tests.stress.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashSetBuilderTest {

    @Test
    fun `should correctly iterate after removing integer element`() {
        val removedElement = 0
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, removedElement, 32)
                    as PersistentHashSet<Int>

        validate(set, removedElement)
    }

    @Test
    fun `should correctly iterate after removing IntWrapper element`() {
        val removedElement = IntWrapper(0, 0)
        val set: PersistentHashSet<IntWrapper> = persistentHashSetOf(
            removedElement,
            IntWrapper(1, 0),
            IntWrapper(2, 32),
            IntWrapper(3, 32)
        ) as PersistentHashSet<IntWrapper>

        validate(set, removedElement)
    }

    private fun <E> validate(set: PersistentHashSet<E>, removedElement: E) {
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
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, 0, 32) as PersistentHashSet<Int>
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
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, 0, 32) as PersistentHashSet<Int>
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            while (iterator1.hasNext()) {
                val element1 = iterator1.next()
                iterator2.next()
                if (element1 == 0) iterator1.remove()
                if (element1 == 2) iterator2.remove()
            }
        }
    }

    @Test
    fun `removing element from one iterator and accessing another throws ConcurrentModificationException`() {
        val set = persistentHashSetOf(1, 2, 3)
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            iterator1.next()
            iterator1.remove()
            iterator2.next()
        }
    }
}