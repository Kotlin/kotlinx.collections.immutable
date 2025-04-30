/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.collections.iterator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashMapBuilderTest {

    @Test
    fun `should correctly iterate after removing integer key and promotion colliding key during iteration`() {
        val removedKey = 0
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2 to "b", 3 to "c", removedKey to "y", 32 to "z")
                    as PersistentHashMap<Int, String>

        validatePromotion(map, removedKey)
    }

    @Test
    fun `should correctly iterate after removing IntWrapper key and promotion colliding key during iteration`() {
        val removedKey = IntWrapper(0, 0)
        val map: PersistentHashMap<IntWrapper, String> = persistentHashMapOf(
            removedKey to "a",
            IntWrapper(1, 0) to "b",
            IntWrapper(2, 32) to "c",
            IntWrapper(3, 32) to "d"
        ) as PersistentHashMap<IntWrapper, String>

        validatePromotion(map, removedKey)
    }

    private fun <K> validatePromotion(map: PersistentHashMap<K, *>, removedKey: K) {
        val builder = map.builder()
        val iterator = builder.entries.iterator()

        val expectedCount = map.size
        var actualCount = 0

        while (iterator.hasNext()) {
            val (key, _) = iterator.next()
            if (key == removedKey) {
                iterator.remove()
            }
            actualCount++
        }

        val resultMap = builder.build()
        for ((key, value) in map) {
            if (key != removedKey) {
                assertTrue(key in resultMap)
                assertEquals(resultMap[key], value)
            } else {
                assertFalse(key in resultMap)
            }
        }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `removing twice on iterators throws IllegalStateException`() {
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2 to "b", 3 to "c", 0 to "y", 32 to "z") as PersistentHashMap<Int, String>
        val builder = map.builder()
        val iterator = builder.entries.iterator()

        assertFailsWith<IllegalStateException> {
            while (iterator.hasNext()) {
                val (key, _) = iterator.next()
                if (key == 0) iterator.remove()
                if (key == 0) {
                    iterator.remove()
                    iterator.remove()
                }
            }
        }
    }

    @Test
    fun `removing elements from different iterators throws ConcurrentModificationException`() {
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2 to "b", 3 to "c", 0 to "y", 32 to "z") as PersistentHashMap<Int, String>
        val builder = map.builder()
        val iterator1 = builder.entries.iterator()
        val iterator2 = builder.entries.iterator()

        assertFailsWith<ConcurrentModificationException> {
            while (iterator1.hasNext()) {
                val (key, _) = iterator1.next()
                iterator2.next()
                if (key == 0) iterator1.remove()
                if (key == 2) iterator2.remove()
            }
        }
    }

    @Test
    fun `removing element from one iterator and accessing another throws ConcurrentModificationException`() {
        val map = persistentHashMapOf(1 to "a", 2 to "b", 3 to "c")
        val builder = map.builder()
        val iterator1 = builder.entries.iterator()
        val iterator2 = builder.entries.iterator()

        assertFailsWith<ConcurrentModificationException> {
            iterator1.next()
            iterator1.remove()
            iterator2.next()
        }
    }
}