/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import tests.contract.drain
import tests.contract.runCells
import tests.trie.*
import kotlin.collections.iterator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A bug of a key whose hash changed after it was stored is a test with [MutableKey] here; a new call through the
 * builder is a row of [tests.contract.mapOperations].
 */
class PersistentHashMapBuilderTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

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
                val _ = iterator2.next()
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
            val _ = iterator1.next()
            iterator1.remove()
            iterator2.next()
        }
    }

    @Test
    fun `putAll should not duplicate a key stored in a bottom-level collision node`() {
        val builder = persistentHashMapOf(a1 to 1, a2 to 2).builder()
        builder.putAll(persistentHashMapOf(a1 to 10, sibling to 3))
        assertEquals(3, builder.size)
        assertEquals(persistentHashMapOf(a1 to 10, a2 to 2, sibling to 3), builder.build())

        val reversedBuilder = persistentHashMapOf(a1 to 10, sibling to 3).builder()
        reversedBuilder.putAll(persistentHashMapOf(a1 to 1, a2 to 2))
        assertEquals(3, reversedBuilder.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 2, sibling to 3), reversedBuilder.build())
    }

    @Test
    fun `putAll should take the values of the argument builder without the two builders sharing storage`() {
        val argument = persistentHashMapOf(a1 to 10, a2 to 20).builder()
        val builder = persistentHashMapOf(a1 to 1, a2 to 2).builder()
        builder.putAll(argument)
        assertEquals(2, builder.size)

        builder[a1] = 100
        argument[a2] = 200

        assertEquals(persistentHashMapOf(a1 to 100, a2 to 20), builder.build())
        assertEquals(persistentHashMapOf(a1 to 10, a2 to 200), argument.build())
    }

    @Test
    fun `put of a stored value should not rebuild a map whose key is in a bottom-level collision node`() {
        val map = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")
        val stored = map[a1]!!

        val builder = map.builder()
        assertSame(stored, builder.put(a1, stored))
        assertSame(map, builder.build())
    }

    @Test
    fun `put of a stored value should not invalidate an iterator when the collision node is shared`() {
        val builder = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c").builder()
        builder[sibling] = "C"
        val stored = builder[a1]!!

        val iterator = builder.keys.iterator()
        val visited = mutableListOf(iterator.next())
        builder[a1] = stored
        while (iterator.hasNext()) {
            visited.add(iterator.next())
        }

        assertEquals(listOf(a1, a2, sibling), visited.sorted())
    }

    @Test
    fun `putAll that adds a key should invalidate a live iterator`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(3 to "c"))

        assertEquals("c", builder[3])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that rewrites values in place should not invalidate a live iterator`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        builder[1] = "A"

        val iterator = builder.values.iterator()
        builder.putAll(persistentHashMapOf(1 to "x", 2 to "y"))

        assertEquals(listOf("x", "y"), listOf(iterator.next(), iterator.next()).sorted())
    }

    @Test
    fun `putAll of the stored values should not invalidate an iterator`() {
        val map = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")
        val builder = map.builder()

        val iterator = builder.keys.iterator()
        val visited = mutableListOf(iterator.next())
        builder.putAll(persistentHashMapOf(a1 to map[a1]!!, a2 to map[a2]!!, sibling to map[sibling]!!))
        while (iterator.hasNext()) {
            visited.add(iterator.next())
        }

        assertEquals(listOf(a1, a2, sibling), visited.sorted())
    }

    @Test
    fun `putAll of the stored values should not rebuild the map`() {
        val map = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")

        val builder = map.builder()
        builder.putAll(persistentHashMapOf(a1 to map[a1]!!, a2 to map[a2]!!, sibling to map[sibling]!!))

        assertSame(map, builder.build())
    }

    @Test
    fun `putAll of the builder itself should not invalidate an iterator`() {
        val builder = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c").builder()

        val iterator = builder.keys.iterator()
        val visited = mutableListOf(iterator.next())
        builder.putAll(builder)
        while (iterator.hasNext()) {
            visited.add(iterator.next())
        }

        assertEquals(listOf(a1, a2, sibling), visited.sorted())
    }

    @Test
    fun `putAll of the map this builder was built from should keep a live iterator valid`() {
        val map = persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")
        val builder = map.builder()

        val iterator = builder.entries.iterator()
        builder.putAll(map)
        val visited = mutableListOf<IntWrapper>()
        while (iterator.hasNext()) {
            visited.add(iterator.next().key)
        }

        assertEquals(listOf(a1, a2, sibling), visited.sorted())
    }

    @Test
    fun `putAll of an empty map should keep a live iterator valid`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf())
        val visited = mutableListOf<Int>()
        while (iterator.hasNext()) {
            visited.add(iterator.next().key)
        }

        assertEquals(listOf(1, 2), visited.sorted())
    }

    @Test
    fun `putAll that only replaces values does not count as a size change`() {
        val builder = (persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")
                as PersistentHashMap<IntWrapper, String>).builder()
        val sizeModCount = builder.sizeModCount

        builder.putAll(persistentHashMapOf(a1 to "x", a2 to "y", sibling to "z"))

        assertEquals(3, builder.size)
        assertEquals(sizeModCount, builder.sizeModCount)
    }

    @Test
    fun `putAll of an equal key with the stored value should not rebuild the map`() {
        val map = persistentHashMapOf(IntWrapper(1, 1) to "a", IntWrapper(2, 2) to "b")
        val builder = map.builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(IntWrapper(1, 1) to map[IntWrapper(1, 1)]!!))

        assertSame(map, builder.build())
        val _ = iterator.next()
    }

    @Test
    fun `putAll that replaces collision values should keep the stored key instances`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a", a2 to "b").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 0) to "y"))

        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll that replaces the value of an equal key should keep the stored key instance`() {
        val storedKey = IntWrapper(1, 1)
        val builder = persistentHashMapOf(storedKey to "a", IntWrapper(2, 2) to "b").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 1) to "x"))

        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `entry setValue after iterator remove does not re-add the key`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value
        iterator.remove()

        assertEquals(oldValue, entry.setValue("z"))

        assertEquals(1, builder.size)
        assertFalse(builder.containsKey(entry.key))
        assertEquals(if (entry.key == 1) 2 else 1, iterator.next().key)
        assertFalse(iterator.hasNext())
        assertFalse(builder.build().containsKey(entry.key))
    }

    @Test
    fun `next throws until the changed hash of the next key is back after an overwrite copied its path`() = runCells {
        fun check(case: String, hashes: List<Int>, changed: Int, newHash: Int) = cell(case) {
            val keys = hashes.mapIndexed { id, hash -> MutableKey(id, hash) }
            val builder = persistentHashMapOf(*keys.map { it to "old" }.toTypedArray()).builder()
            val order = builder.keys.toList()
            val returned = order.indexOf(keys[changed])
            val iterator = builder.entries.iterator()
            repeat(returned) { val _ = iterator.next() }

            keys[changed].hash = newHash
            builder[keys[0]] = "new"

            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            keys[changed].hash = hashes[changed]
            assertEquals(order.drop(returned).map { it to builder[it] }, iterator.drain())
        }
        check("onto another entry", hashes = listOf(0, 1, 2), changed = 1, newHash = 2)
        check("onto an empty cell", hashes = listOf(0, 1, 2), changed = 1, newHash = 5)
        check("onto another collision node", hashes = listOf(0, 0, 32, 32), changed = 2, newHash = 0)
        check("onto an empty cell of a subnode", hashes = listOf(0, 2, 1, 1 + 32), changed = 1, newHash = 1 + 2 * 32)
    }

    @Test
    fun `remove takes out only keys that next returned when the hash of the next key changed`() {
        val keys = List(3) { MutableKey(it) }
        val builder = persistentHashMapOf(*keys.map { it to "old" }.toTypedArray()).builder()
        val iterator = builder.keys.iterator()
        assertEquals(keys[0], iterator.next())
        keys[1].hash = 5

        iterator.remove()

        assertEquals(2, builder.size)
        assertFailsWith<ConcurrentModificationException>("next() while the hash is changed") { iterator.next() }
        keys[1].hash = 1
        assertFailsWith<IllegalStateException>("remove() again, after the next that threw") { iterator.remove() }
        assertEquals(keys.drop(1), iterator.drain())
    }
}
