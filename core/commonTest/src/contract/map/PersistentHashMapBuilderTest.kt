/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.collections.iterator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
    fun `putAll that only replaces values should invalidate a live iterator`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(1 to "x", 2 to "y"))

        assertEquals("x", builder[1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that only replaces values in a bottom-level collision node should invalidate a live iterator`() {
        val builder = persistentHashMapOf(a1 to "a", a2 to "b").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(a1 to "x", a2 to "y"))

        assertEquals("x", builder[a1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that only replaces values should invalidate an iterator that descended into the replaced node`() {
        val builder = persistentHashMapOf(1 to "a", 0 to "y", 32 to "z").builder()
        builder[1] = "A"

        val iterator = builder.entries.iterator()
        val _ = iterator.next()
        builder.putAll(persistentHashMapOf(0 to "Y", 32 to "Z"))

        assertEquals("Y", builder[0])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that replaces values in an unowned collision node under an owned root should invalidate a live iterator`() {
        val builder = (persistentHashMapOf(a1 to "a", a2 to "b", sibling to "c")
                as PersistentHashMap<IntWrapper, String>).builder()
        builder[sibling] = "C"
        val nodeBefore = builder.node

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(a1 to "x", a2 to "y"))

        assertSame(nodeBefore, builder.node)
        assertEquals("x", builder[a1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that replaces the value of a key stored in a two-entry node should invalidate a live iterator`() {
        val neighbor = IntWrapper(2, 32)
        val builder = persistentHashMapOf(a1 to "a", neighbor to "b").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(a1 to "x"))

        assertEquals("x", builder[a1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
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
    fun `putAll should keep the stored key instance when the argument holds the key one level down`() {
        val storedKey = IntWrapper(1, 32)
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 32) to "x", IntWrapper(2, 0) to "y"))

        assertEquals(2, builder.size)
        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should keep the stored key instance when the argument holds the key in a bottom-level collision node`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 0) to "y"))

        assertEquals(2, builder.size)
        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should keep the stored key instance when the collision node is reached at the last level`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a", sibling to "c").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 0) to "y"))

        assertEquals(3, builder.size)
        assertEquals("x", builder[storedKey])
        assertEquals("c", builder[sibling])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should keep the stored key instance when the argument holds the key several levels down`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 1 shl 10) to "y"))

        assertEquals(2, builder.size)
        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should reuse the argument's subtree when it holds the stored key instance`() {
        val argument = persistentHashMapOf(a1 to "x", IntWrapper(2, 32) to "y")
                as PersistentHashMap<IntWrapper, String>
        val builder = (persistentHashMapOf(a1 to "a") as PersistentHashMap<IntWrapper, String>).builder()

        builder.putAll(argument)

        assertEquals(2, builder.size)
        assertEquals("x", builder[a1])
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `putAll should reuse the argument's collision node when its key is already the receiver's instance`() {
        val argument = persistentHashMapOf(a1 to "x", a2 to "y") as PersistentHashMap<IntWrapper, String>
        val builder = (persistentHashMapOf(a1 to "old") as PersistentHashMap<IntWrapper, String>).builder()

        builder.putAll(argument)

        assertEquals(2, builder.size)
        assertEquals("x", builder[a1])
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `putAll should insert the entry when the argument's subtree lacks the key`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(2, 32) to "y", IntWrapper(3, 64) to "z"))

        assertEquals(3, builder.size)
        assertEquals("a", builder[storedKey])
        assertEquals("y", builder[IntWrapper(2, 32)])
        assertEquals("z", builder[IntWrapper(3, 64)])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should push the entry deeper when it collides with an argument entry inside the subtree`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(2, 1 shl 10) to "y", IntWrapper(3, 32) to "z"))

        assertEquals(3, builder.size)
        assertEquals("a", builder[storedKey])
        assertEquals("y", builder[IntWrapper(2, 1 shl 10)])
        assertEquals("z", builder[IntWrapper(3, 32)])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should keep the stored key instance when the receiver holds the key in a subtree`() {
        val storedKey = IntWrapper(1, 0)
        val builder = persistentHashMapOf(storedKey to "a", IntWrapper(2, 32) to "b").builder()

        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x"))

        assertEquals(2, builder.size)
        assertEquals("x", builder[storedKey])
        assertSame(storedKey, builder.keys.single { it == storedKey })
    }

    @Test
    fun `putAll should not write the receiver's key into the argument's map`() {
        val storedKey = IntWrapper(1, 0)
        val argumentKey = IntWrapper(1, 0)
        val argument = persistentHashMapOf(argumentKey to "x", IntWrapper(2, 32) to "y")
        val builder = persistentHashMapOf(storedKey to "a").builder()

        builder.putAll(argument)

        assertSame(storedKey, builder.keys.single { it == storedKey })
        assertSame(argumentKey, argument.keys.single { it == argumentKey })
        assertEquals("x", argument[argumentKey])
    }

    @Test
    fun `putAll should invalidate a live iterator when the argument holds the key in a subtree`() {
        val builder = persistentHashMapOf(a1 to "a").builder()

        val iterator = builder.entries.iterator()
        builder.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 32) to "y"))

        assertEquals("x", builder[a1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `putAll that merges an entry into the argument's subtree should count one size change`() {
        val overlapping = (persistentHashMapOf(IntWrapper(1, 0) to "a")
                as PersistentHashMap<IntWrapper, String>).builder()
        val sizeModCount = overlapping.sizeModCount
        overlapping.putAll(persistentHashMapOf(IntWrapper(1, 0) to "x", IntWrapper(2, 32) to "y"))
        assertEquals(sizeModCount + 1, overlapping.sizeModCount)

        val disjoint = (persistentHashMapOf(IntWrapper(1, 0) to "a")
                as PersistentHashMap<IntWrapper, String>).builder()
        val disjointSizeModCount = disjoint.sizeModCount
        disjoint.putAll(persistentHashMapOf(IntWrapper(2, 32) to "y", IntWrapper(3, 64) to "z"))
        assertEquals(disjointSizeModCount + 1, disjoint.sizeModCount)

        val collision = (persistentHashMapOf(IntWrapper(1, 0) to "a", sibling to "c")
                as PersistentHashMap<IntWrapper, String>).builder()
        val collisionSizeModCount = collision.sizeModCount
        collision.putAll(persistentHashMapOf(IntWrapper(2, 0) to "y", IntWrapper(4, 0) to "z"))
        assertEquals(4, collision.size)
        assertEquals(collisionSizeModCount + 1, collision.sizeModCount)
    }

    @Test
    fun `putAll of random colliding maps should keep the stored key instances and the argument's values`() {
        val hashes = intArrayOf(0, 1, 32, 33, 1 shl 10, 1 shl 30, (1 shl 30) or 32)
        val random = Random(313)
        repeat(200) { iteration ->
            val receiverKeys = mutableMapOf<Int, IntWrapper>()
            val builder = persistentHashMapOf<IntWrapper, String>().builder()
            for (id in 0..<14) {
                if (random.nextBoolean()) {
                    val key = IntWrapper(id, hashes[id % hashes.size])
                    receiverKeys[id] = key
                    builder[key] = "r$id"
                }
            }
            val argumentKeys = mutableMapOf<Int, IntWrapper>()
            val argumentBuilder = persistentHashMapOf<IntWrapper, String>().builder()
            for (id in 0..<14) {
                if (random.nextBoolean()) {
                    val key = IntWrapper(id, hashes[id % hashes.size])
                    argumentKeys[id] = key
                    argumentBuilder[key] = "a$id"
                }
            }

            builder.putAll(argumentBuilder.build())

            val shape = "iteration $iteration, receiver ${receiverKeys.keys}, argument ${argumentKeys.keys}"
            assertEquals((receiverKeys.keys + argumentKeys.keys).size, builder.size, shape)
            for ((id, key) in receiverKeys) {
                assertSame(key, builder.keys.singleOrNull { it == key }, "$shape, id $id")
                assertEquals(if (id in argumentKeys) "a$id" else "r$id", builder[key], "$shape, id $id")
            }
            for ((id, key) in argumentKeys) {
                if (id in receiverKeys) continue
                assertSame(key, builder.keys.singleOrNull { it == key }, "$shape, id $id")
                assertEquals("a$id", builder[key], "$shape, id $id")
            }
        }
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
    fun `entry setValue after a remove of a different key writes the value and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value
        val otherKey = if (entry.key == 1) 2 else 1

        builder.remove(otherKey)

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals(1, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after a remove of a key other than the upcoming one writes the value and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val orderedKeys = builder.keys.toList()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value

        assertEquals(orderedKeys[0], entry.key)
        builder.remove(orderedKeys[2])

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals(2, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after an external remove of its own key writes nothing and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value

        builder.remove(entry.key)

        assertEquals(oldValue, entry.setValue("z"))
        assertFalse(builder.containsKey(entry.key))
        assertEquals(1, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after an external put of a new key writes the value and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value

        builder[3] = "c"

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals(3, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after an external value overwrite of a different key writes the value and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value
        val otherKey = (setOf(1, 2, 3) - entry.key).first()

        builder[otherKey] = "overwritten"

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals("overwritten", builder[otherKey])
        assertEquals(3, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue on the exhausted iterator of a single entry map after an external put writes the value and the following next throws`() {
        val builder = persistentHashMapOf(1 to "a").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()

        assertFalse(iterator.hasNext())
        builder[2] = "b"

        assertEquals("a", entry.setValue("z"))
        assertEquals("z", builder[1])
        assertEquals(2, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after a remove of a colliding key writes the value and the following next throws`() {
        val builder = persistentHashMapOf(
            IntWrapper(1, 0) to "a",
            IntWrapper(2, 0) to "b",
            IntWrapper(3, 0) to "c"
        ).builder()
        val orderedKeys = builder.keys.toList()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        val oldValue = entry.value

        builder.remove(orderedKeys[1])

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals(2, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after a remove that shrinks the collision node writes the value and the following next throws`() {
        val builder = persistentHashMapOf(
            IntWrapper(1, 0) to "a",
            IntWrapper(2, 0) to "b",
            IntWrapper(3, 1) to "c"
        ).builder()
        val orderedKeys = builder.keys.toList()
        val iterator = builder.entries.iterator()
        val _ = iterator.next()
        val entry = iterator.next()
        val oldValue = entry.value

        assertEquals(IntWrapper(3, 1), orderedKeys[0])
        builder.remove(orderedKeys[2])

        assertEquals(oldValue, entry.setValue("z"))
        assertEquals("z", builder[entry.key])
        assertEquals(2, builder.size)
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `setting values through the iterator updates every entry and preserves the iteration order`() {
        val builder = persistentHashMapOf(
            IntWrapper(1, 0) to "a",
            IntWrapper(2, 0) to "b",
            IntWrapper(3, 1) to "c",
            IntWrapper(4, 32) to "d"
        ).builder()
        val orderedKeys = builder.keys.toList()
        val visited = mutableListOf<IntWrapper>()
        val iterator = builder.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            visited.add(entry.key)
            entry.setValue(entry.value + "!")
        }

        assertEquals(orderedKeys, visited)
        assertEquals(4, builder.size)
        assertEquals("a!", builder[IntWrapper(1, 0)])
        assertEquals("b!", builder[IntWrapper(2, 0)])
        assertEquals("c!", builder[IntWrapper(3, 1)])
        assertEquals("d!", builder[IntWrapper(4, 32)])
    }

    @Test
    fun `setting the value of an earlier entry after a later one keeps the iterator valid`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val orderedKeys = builder.keys.toList()
        val iterator = builder.entries.iterator()
        val first = iterator.next()
        val second = iterator.next()
        val firstValue = first.value
        val secondValue = second.value

        assertEquals(secondValue, second.setValue("y"))
        assertEquals(firstValue, first.setValue("x"))
        assertEquals("x", first.setValue("xx"))

        assertEquals("xx", builder[orderedKeys[0]])
        assertEquals("y", builder[orderedKeys[1]])
        assertEquals(3, builder.size)
        assertEquals(orderedKeys[2], iterator.next().key)
        assertFalse(iterator.hasNext())
    }

    @Test
    fun `iterator remove after a remove of a different key throws ConcurrentModificationException`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val orderedKeys = builder.keys.toList()
        val iterator = builder.entries.iterator()
        assertEquals(orderedKeys[0], iterator.next().key)

        builder.remove(orderedKeys[1])

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(1, builder.size)
        assertTrue(builder.containsKey(orderedKeys[0]))
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `iterator remove on the exhausted iterator of a single entry map after an external put throws ConcurrentModificationException`() {
        val builder = persistentHashMapOf(1 to "a").builder()
        val iterator = builder.entries.iterator()
        val _ = iterator.next()
        assertFalse(iterator.hasNext())

        builder[2] = "b"

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(2, builder.size)
        assertEquals("a", builder[1])
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `iterator remove without a preceding next after an external remove throws IllegalStateException`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()

        builder.remove(1)

        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertEquals(1, builder.size)
    }
}
