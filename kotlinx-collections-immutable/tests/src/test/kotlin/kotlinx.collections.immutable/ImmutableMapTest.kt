package kotlinx.collections.immutable

import org.junit.Test
import test.collections.behaviors.*
import test.collections.compare
import java.util.*
import kotlin.test.*

class ImmutableHashMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = kotlinx.collections.immutable.immutableHashMapOf(*pairs)
}
class ImmutableOrderedMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = kotlinx.collections.immutable.immutableMapOf(*pairs)
    override fun <K, V> compareMaps(expected: Map<K, V>, actual: Map<K, V>) = compare(expected, actual) { mapBehavior(ordered = true) }

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
}

abstract class ImmutableMapTest {

    abstract fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V>

    open fun <K, V> compareMaps(expected: Map<K, V>, actual: Map<K, V>) = compareMapsUnordered(expected, actual)
    fun <K, V> compareMapsUnordered(expected: Map<K, V>, actual: Map<K, V>) = compare(expected, actual) { mapBehavior(ordered = false) }


    @Test fun empty() {
        val empty1 = immutableMapOf<Int, String>()
        val empty2 = immutableMapOf<String, Int>()
        assertEquals<ImmutableMap<*, *>>(empty1, empty2)
        assertEquals(mapOf<Int, String>(), empty1)
        assertTrue(empty1 === empty2)

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
        assertTrue(immMap2 === immMap)

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
        assertTrue(map === builder.build(), "Building the same list without modifications")

        val map2 = builder.toImmutableMap()
        assertTrue(map2 === map, "toImmutable calls build()")

        with(map) {
            testMutation { put('K', null) }
            testMutation { putAll("kotlin".associate { it to 0 }) }
            testMutation { this['a'] = null }
            testMutation { remove('x') }
            testMutation { clear() }
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
        assertTrue(this === result)
        assertTrue(this === buildResult)
    }


    @Test
    fun covariantTyping() {
        val mapNothing = immutableMapOf<Nothing, Nothing>()
        val mapSI: PersistentMap<String, Int> = mapNothing + ("x" to 1)
        val mapSNI: PersistentMap<String, Int?> = mapSI + mapOf("y" to null)
        val mapANA: PersistentMap<Any, Any?> = mapSNI + listOf(1 to "x")

        assertEquals(mapOf(1 to "x", "x" to 1, "y" to null), mapANA)
    }
}