/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.*
import tests.contract.collectionBehavior
import tests.contract.compare
import tests.contract.mapBehavior
import tests.contract.setBehavior
import tests.stress.IntWrapper
import kotlin.test.*

class ImmutableHashMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)
    override fun <K, V> testBuilderToPersistentMap(builder: PersistentMap.Builder<K, V>) {
        assertNotSame(builder.build(), builder.toPersistentMap(), "toPersistent shouldn't call build()")
    }

    @Test fun putAllElements() {
        run {
            val x = immutableMapOf<String, Int>() + (11..200).map { "$it" to it }
            compareMaps(x.toMap(), x.putAll(immutableMapOf()))
            compareMaps(x.toMap(), immutableMapOf<String, Int>().putAll(x))
        }

        run {
            val x = immutableMapOf<Int, Int>() + (11..200).map { it to it }
            val y = immutableMapOf<Int, Int>() + (120..400).map { it to it }
            compareMaps(x.toMap() + y.toMap(), x.putAll(y))
        }

        run {
            val x = immutableMapOf<String, Int>() + (11..200).map { "$it" to it }
            val y = immutableMapOf<String, Int>() + (120..400).map { "$it" to it }
            compareMaps(x.toMap() + y.toMap(), x.putAll(y))
        }

        run {
            val x = immutableMapOf<IntWrapper, Int>() + (11..200).map { IntWrapper(it, it % 30) to it }
            val y = immutableMapOf<IntWrapper, Int>() + (120..400).map { IntWrapper(it, it % 30) to it }
            compareMaps(x.toMap() + y.toMap(), x.putAll(y))
            compareMaps(y.toMap() + x.toMap(), y.putAll(x))
        }

        run {
            val bcase = (1..2000).toList() // to preserve reference equality
            val left = immutableMapOf<String, Int>() + bcase.map { "$it" to it }
            assertSame(left, left + left)
            assertSame(left, left + immutableMapOf())
        }
    }

    @Test fun putAllElementsFromBuilder() {
        val builder1 = immutableMapOf<String, Int>().builder()
        val builder2 = immutableMapOf<String, Int>().builder()
        val expected = mutableMapOf<String, Int>()
        for (i in 300..400) {
            builder1.put("$i", i)
            expected.put("$i", i)
        }
        for (i in 0..200) {
            builder2.put("$i", i)
            expected.put("$i", i)
        }
        builder1.putAll(builder2)

        // make sure we work with current state of builder2, not previous
        compareMaps(expected, builder1)
        builder2.put("200", 1000)
        // make sure we can't modify builder1 through builder2
        compareMaps(expected, builder1)
        // make sure builder builds correct map
        compareMaps(expected, builder1.build())
    }

    @Test fun regressionGithubIssue109() {
        // https://github.com/Kotlin/kotlinx.collections.immutable/issues/109
        val map0 = immutableMapOf<Int, Int>().put(0, 0).put(1, 1).put(32, 32)
        val map1 = map0.mutate { it.remove(0) }
        val map2 = map1.mutate {
            it.remove(1)
            it.remove(0)
        }

        assertTrue(map1.containsKey(32))
    }

    @Test
    fun regressionGithubIssue114() {
        // https://github.com/Kotlin/kotlinx.collections.immutable/issues/114
        val p = persistentMapOf(99 to 1)
        val e = Array(101) { it }.map { it to it }
        val c = persistentMapOf(*e.toTypedArray())
        val n = p.builder().apply { putAll(c) }.build()
        assertEquals(99, n[99])
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

        map = map.remove("x")
        map += "x" to 2
        compare(setOf("y", "x"), map.keys) { setBehavior(ordered = true) }
        compare(listOf(1, 2), map.values) { collectionBehavior(ordered = true) }
        compare(mapOf("y" to 1, "x" to 2).entries, map.entries) { setBehavior(ordered = true) }
    }

    @Test fun keyHashCodeChanged() {
        val changing = mutableSetOf("ok")
        val persistent: PersistentMap<Any, Any> = immutableMapOf("constant" to "fixed", changing to "modified")
        assertEquals(1, persistent.keys.filter { it === changing }.size)
        changing.add("break iteration")
        assertFailsWith<ConcurrentModificationException> { persistent.keys.filter { it === changing } }
    }

    @Test fun builderKeyHashCodeChanged() {
        val changing = mutableSetOf("ok")
        val builder: PersistentMap.Builder<Any, Any> = immutableMapOf<Any, Any>().builder().apply {
            putAll(listOf("constant" to "fixed", changing to "modified"))
        }
        assertEquals(1, builder.filter { it.key === changing }.size)
        changing.add("break iteration")
        assertFailsWith<ConcurrentModificationException> { builder.filter { it.key === changing } }
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

        immMap = immMap.remove(null)
        assertEquals<Map<*, *>>(map, immMap) // problem
    }


    @Test fun putElements() {
        var map = immutableMapOf<String, Int?>().toPersistentMap()
        map = map.put("x", 0)
        map = map.put("x", 1)
        map = map.putAll(arrayOf("x" to null))
        map = map + ("y" to null)
        map += "y" to 1
        assertEquals(mapOf("x" to null, "y" to 1), map)

        map += map
        map += map.map { it.key + "!" to it.value }

        assertEquals(map.size, map.entries.size)

        assertEquals(mapOf("x" to null, "y" to 1, "x!" to null, "y!" to 1), map)
    }

    @Test fun putEqualButNotSameValue() {
        data class Value<T>(val value: T)
        val map = immutableMapOf("x" to Value(1))

        val newValue = Value(1)
        val newMap = map.put("x", newValue)
        assertNotSame(map, newMap)
        assertEquals(map, newMap)

        val sameMap = newMap.put("x", newValue)
        assertSame(newMap, sameMap)
    }

    @Test fun removeElements() {
        val map = immutableMapOf("x" to 1, null to "x").toPersistentMap()

        fun <K, V> assertEquals(expected: Map<out K, V>, actual: Map<out K, V>) = kotlin.test.assertEquals(expected, actual)

        assertEquals(mapOf("x" to 1), map.remove(null))
        assertEquals(mapOf("x" to 1), map.remove(null, "x"))
        assertEquals(map, map.remove("x", 2))

        assertEquals(emptyMap(), map.clear())
        assertEquals(emptyMap(), map.remove("x").remove(null))
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
        "abcxaxyz12".associateTo(builder) { it to it.toInt() }
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

    @Test fun noOperation() {
        immutableMapOf<Int, String>().toPersistentMap().testNoOperation({ clear() }, { clear() })

        val map = immutableMapOf("x" to 1, null to "x").toPersistentMap()
        with(map) {
            testNoOperation({ remove("y") }, { remove("y") })
            testNoOperation({ remove("x", 2) }, { remove("x", 2) })
            testNoOperation({ put("x", 1) }, { put("x", 1) })     // does not hold
            testNoOperation({ putAll(this) }, { putAll(this) })   // does not hold
            testNoOperation({ putAll(emptyMap()) }, { putAll(emptyMap()) })
        }
    }

    fun <K, V> PersistentMap<K, V>.testNoOperation(persistent: PersistentMap<K, V>.() -> PersistentMap<K, V>, mutating: MutableMap<K, V>.() -> Unit) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure non-mutating operations return the same instance
        assertSame(this, result)
        assertSame(this, buildResult)
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