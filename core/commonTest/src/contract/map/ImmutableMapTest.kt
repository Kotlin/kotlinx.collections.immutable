/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.*
import tests.contract.collectionBehavior
import tests.contract.compare
import tests.contract.mapBehavior
import tests.contract.setBehavior
import tests.IntWrapper
import kotlin.test.*

class ImmutableHashMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)
    override fun <K, V> testBuilderToPersistentMap(builder: PersistentMap.Builder<K, V>) {
        assertNotSame(builder.build(), builder.toPersistentMap(), "toPersistent shouldn't call build()")
    }

    @Test fun putAllElements() {
        run {
            val x = immutableMapOf<String, Int>() + (11..200).map { "$it" to it }
            compareMaps(x.toMap(), x.puttingAll(immutableMapOf()))
            compareMaps(x.toMap(), immutableMapOf<String, Int>().puttingAll(x))
        }

        run {
            val x = immutableMapOf<Int, Int>() + (11..200).map { it to it }
            val y = immutableMapOf<Int, Int>() + (120..400).map { it to it }
            compareMaps(x.toMap() + y.toMap(), x.puttingAll(y))
        }

        run {
            val x = immutableMapOf<String, Int>() + (11..200).map { "$it" to it }
            val y = immutableMapOf<String, Int>() + (120..400).map { "$it" to it }
            compareMaps(x.toMap() + y.toMap(), x.puttingAll(y))
        }

        run {
            val x = immutableMapOf<IntWrapper, Int>() + (11..200).map { IntWrapper(it, it % 30) to it }
            val y = immutableMapOf<IntWrapper, Int>() + (120..400).map { IntWrapper(it, it % 30) to -it }
            compareMaps(x.toMap() + y.toMap(), x.puttingAll(y))
            compareMaps(y.toMap() + x.toMap(), y.puttingAll(x))
        }

        run {
            val bcase = (1..2000).toList() // to preserve reference equality
            val left = immutableMapOf<String, Int>() + bcase.map { "$it" to it }
            assertSame(left, left + left)
            assertSame(left, left + immutableMapOf())
        }
    }
}

class ImmutableOrderedMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentMapOf(*pairs)
    override fun <K, V> compareMaps(expected: Map<K, V>, actual: Map<K, V>) = compare(expected, actual) { mapBehavior(ordered = true) }
    override fun <K, V> testBuilderToPersistentMap(builder: PersistentMap.Builder<K, V>) {
        assertSame(builder.build(), builder.toPersistentMap(), "toPersistent should call build()")
    }

    @Test fun iterationOrder() {
        var map = immutableMapOf("x" to null, "y" to 1).toPersistentMap()
        compare(setOf("x", "y"), map.keys) { setBehavior(ordered = true) }

        map += "x" to 1
        compare(setOf("x", "y"), map.keys) { setBehavior(ordered = true) }

        map = map.removing("x")
        map += "x" to 2
        compare(setOf("y", "x"), map.keys) { setBehavior(ordered = true) }
        compare(listOf(1, 2), map.values) { collectionBehavior(ordered = true) }
        compare(mapOf("y" to 1, "x" to 2).entries, map.entries) { setBehavior(ordered = true) }
    }
}

abstract class ImmutableMapTest {
    abstract fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V>
    abstract fun <K, V> testBuilderToPersistentMap(builder: PersistentMap.Builder<K, V>)

    open fun <K, V> compareMaps(expected: Map<K, V>, actual: Map<K, V>) = compareMapsUnordered(expected, actual)
    fun <K, V> compareMapsUnordered(expected: Map<K, V>, actual: Map<K, V>) = compare(expected, actual) { mapBehavior(ordered = false) }


    @Test fun empty() {
        val empty1 = immutableMapOf<Int, String>()
        val empty2 = immutableMapOf<String, Int>()
        assertEquals<ImmutableMap<*, *>>(empty1, empty2)
        assertEquals(mapOf<Int, String>(), empty1)
        assertSame<ImmutableMap<*, *>>(empty1, empty2)

        compareMaps(emptyMap(), empty1)
    }


    @Test fun ofPairs() {
        val map0 = mapOf("x" to 1, "y" to null, null to 2)
        val map1 = immutableMapOf("x" to 1, "y" to null, null to 2)
        val map2 = immutableMapOf("x" to 1, "y" to null, null to 2)

        compareMaps(map0, map1)
        compareMaps(map1, map2)
    }


    @Test fun toImmutable() {
        val original = mapOf("x" to 1, "y" to null, null to 2)
        val immOriginal = original.toImmutableMap()
        compareMaps(original, immOriginal)


        val map = HashMap(original) // copy
        var immMap = map.toPersistentMap()
        val immMap2 = immMap.toImmutableMap()
        assertSame(immMap2, immMap)

        compareMapsUnordered(original, immMap)
        compareMapsUnordered(map, immMap)

        map.remove(null)
        assertNotEquals<Map<*, *>>(map, immMap)

        immMap = immMap.removing(null)
        assertEquals<Map<*, *>>(map, immMap) // problem
    }

    @Test fun emptyMapToPersistentMap() {
        val empty = emptyMap<String, Int>()
        val emptyPersistentMap = empty.toPersistentMap()

        assertSame(emptyPersistentMap, empty.toPersistentMap())
    }

    @Test fun putElements() {
        var map = immutableMapOf<String, Int?>().toPersistentMap()
        map = map.putting("x", 0)
        map = map.putting("x", 1)
        map = map.puttingAll(arrayOf("x" to null))
        map = map + ("y" to null)
        map += "y" to 1
        assertEquals(mapOf("x" to null, "y" to 1), map)

        map += map
        map += map.map { it.key + "!" to it.value }

        assertEquals(map.size, map.entries.size)

        assertEquals(mapOf("x" to null, "y" to 1, "x!" to null, "y!" to 1), map)
    }

    @Test fun removeElements() {
        val map = immutableMapOf("x" to 1, null to "x").toPersistentMap()

        fun <K, V> assertEquals(expected: Map<out K, V>, actual: Map<out K, V>) = kotlin.test.assertEquals(expected, actual)

        assertEquals(mapOf("x" to 1), map.removing(null))
        assertEquals(mapOf("x" to 1), map.removing(null, "x"))
        assertEquals(map, map.removing("x", 2))

        assertEquals(emptyMap(), map.cleared())
        assertEquals(emptyMap(), map.removing("x").removing(null))
    }

    @Test
    fun removeCollection() {
        val map = immutableMapOf(0 to "a", 1 to "B", 2 to "c")

        val newMap = map - setOf(2) - sequenceOf(1)
        assertEquals(mapOf(0 to "a"), newMap)
    }

    @Test fun removeMatching() {
        val map = immutableMapOf(0 to "a", 1 to "B", 2 to "c")
        val newMap = map.mutate { it.entries.removeAll { it.key % 2 == 0 } }
        assertEquals(mapOf(1 to "B"), newMap)
    }


    @Test fun builder() {

        val builder = immutableMapOf<Char, Int?>().builder()
        "abcxaxyz12".associateTo(builder) { it to it.code }
        val map = builder.build()
        assertEquals<Map<*, *>>(map, builder)
        assertSame(map, builder.build(), "Building the same list without modifications")

        val map2 = builder.toImmutableMap()
        assertSame(map2, map, "toImmutable calls build()")

        testBuilderToPersistentMap(builder)

        with(map) {
            testMutation { put('K', null) }
            testMutation { putAll("kotlin".associate { it to 0 }) }
            testMutation { this['a'] = null }
            testMutation { remove('x') }
            testMutation { clear() }
            testMutation { entries.remove(null as Any?) }
        }
    }

    fun <K, V> PersistentMap<K, V>.testMutation(operation: MutableMap<K, V>.() -> Unit) {
        val mutable = HashMap(this) as MutableMap<K, V>
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        compareMapsUnordered(mutable, builder)
        compareMapsUnordered(mutable, builder.build())
    }


    @Test
    fun covariantTyping() {
        val mapNothing = immutableMapOf<Nothing, Nothing>()
        val mapSI: PersistentMap<String, Int> = mapNothing + ("x" to 1)
        val mapSNI: PersistentMap<String, Int?> = mapSI + mapOf("y" to null)
        val mapANA: PersistentMap<Any, Any?> = mapSNI + listOf(1 to "x")

        assertEquals<Map<*, *>>(mapOf(1 to "x", "x" to 1, "y" to null), mapANA)
    }

    private fun <K, V> testSpecializedEquality(map: Map<K, V>, pairs: Array<Pair<K, V>>, isEqual: Boolean) {
        fun testEqualsAndHashCode(lhs: Any, rhs: Any) {
            assertEquals(isEqual, lhs == rhs)
            if (isEqual) assertEquals(lhs.hashCode(), rhs.hashCode())
        }

        testEqualsAndHashCode(map, mapOf(*pairs))
        testEqualsAndHashCode(map, persistentHashMapOf(*pairs))
        testEqualsAndHashCode(map, persistentMapOf(*pairs))
        testEqualsAndHashCode(map, persistentHashMapOf<K, V>().builder().apply { putAll(pairs) })
        testEqualsAndHashCode(map, persistentMapOf<K, V>().builder().apply { putAll(pairs) })
    }

    private fun <K, V> testEquality(data: Array<Pair<K, V>>, changed: Array<Pair<K, V>>) {
        val base = immutableMapOf(*data)
        testSpecializedEquality(base, data, isEqual = true)
        testSpecializedEquality(base, changed, isEqual = false)

        val builder = immutableMapOf<K, V>().builder().apply { putAll(data) }
        testSpecializedEquality(builder, data, isEqual = true)
        testSpecializedEquality(builder, changed, isEqual = false)

        testSpecializedEquality(base, data.copyOf().apply { shuffle() }, isEqual = true)
        testSpecializedEquality(builder, data.copyOf().apply { shuffle() }, isEqual = true)
    }

    @Test
    fun equality() {
        val data = (0..200).map { i -> i to "$i" }.toTypedArray()
        val changed = data.copyOf().apply { this[42] = 42 to "Invalid" }

        testEquality(data, changed)
    }

    @Test
    fun collisionEquality() {
        val data = (0..200).map { i -> IntWrapper(i, i % 50) to "$i" }.toTypedArray()
        val changed = data.copyOf().apply { this[42] = IntWrapper(42, 42) to "Invalid" }

        testEquality(data, changed)
    }
}
