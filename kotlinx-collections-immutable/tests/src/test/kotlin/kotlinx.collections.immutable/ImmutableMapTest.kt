package kotlinx.collections.immutable

import org.junit.Test
import java.util.*
import kotlin.test.*

class ImmutableHashMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = kotlinx.collections.immutable.immutableHashMapOf(*pairs)
}
class ImmutableOrderedMapTest : ImmutableMapTest() {
    override fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = kotlinx.collections.immutable.immutableMapOf(*pairs)

    @Test fun iterationOrder() {
        var map = immutableMapOf("x" to null, "y" to 1)
        assertEquals(listOf("x", "y"), map.keys.toList())

        map += "x" to 1
        assertEquals(listOf("x", "y"), map.keys.toList())

        map = map.remove("x")
        map += "x" to 2
        assertEquals(listOf("y", "x"), map.keys.toList())
        assertEquals(listOf(1, 2), map.values.toList())
        assertEquals(listOf("y" to 1, "x" to 2), map.toList())
    }
}

abstract class ImmutableMapTest {

    abstract fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V>


    @Test fun empty() {
        val empty1 = immutableMapOf<Int, String>()
        val empty2 = immutableMapOf<String, Int>()
        assertEquals<ImmutableMap<*, *>>(empty1, empty2)
        assertEquals(mapOf<Int, String>(), empty1)
        assertTrue(empty1 === empty2)
    }


    @Test fun ofPairs() {
        val map0 = mapOf("x" to 1, "y" to null, null to 2)
        val map1 = immutableMapOf("x" to 1, "y" to null, null to 2)
        val map2 = immutableMapOf("x" to 1, "y" to null, null to 2)

        assertEquals(map0, map1)
        assertEquals(map1, map2)
    }


    @Test fun toImmutable() {
        val original = mapOf("x" to 1, "y" to null, null to 2)

        val map = HashMap(original) // copy
        var immMap = map.toImmutableMap()
        val immMap2 = immMap.toImmutableMap()
        assertTrue(immMap2 === immMap)

        assertEquals<Map<*, *>>(map, immMap) // problem
//        assertEquals(map.toString(), immMap.toString()) // order sensitive
        assertEquals(map.hashCode(), immMap.hashCode())
        assertEquals<Set<*>>(map.keys, immMap.keys)
        assertEquals<Set<*>>(map.entries, immMap.entries)
        assertEquals(map.values.toSet(), immMap.values.toSet())

        map.remove(null)
        assertNotEquals<Map<*, *>>(map, immMap)

        immMap = immMap.remove(null)
        assertEquals<Map<*, *>>(map, immMap) // problem
    }


    @Test fun putElements() {
        var map = immutableMapOf<String, Int?>()
        map = map.put("x", 0)
        map = map.put("x", 1)
        map = map.putAll(arrayOf("x" to null))
        map = map + ("y" to null)
        map += "y" to 1
        map += map
        map += map.map { it.key + "!"  to it.value }

        assertEquals(map.size, map.entries.size)

        assertEquals(mapOf("x" to null, "y" to 1, "x!" to null, "y!" to 1), map)
    }

    @Test fun removeElements() {
        val map = immutableMapOf("x" to 1, null to "x")

        fun <K, V> assertEquals(expected: Map<out K, V>, actual: Map<out K, V>) = kotlin.test.assertEquals(expected, actual)

        assertEquals(mapOf("x" to 1), map.remove(null))
        assertEquals(mapOf("x" to 1), map.remove(null, "x"))
        assertEquals(map, map.remove("x", 2))

        assertEquals(emptyMap(), map.clear())
        assertEquals(emptyMap(), map.remove("x").remove(null))
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

    fun <K, V> ImmutableMap<K, V>.testMutation(operation: MutableMap<K, V>.() -> Unit) {
        val mutable = HashMap(this) as MutableMap<K, V>
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        assertEquals(mutable, builder)
        assertEquals<Map<*, *>>(mutable, builder.build())
    }

    @Test fun noOperation() {
        immutableMapOf<Int, String>().testNoOperation({ clear() }, { clear() })

        val map = immutableMapOf("x" to 1, null to "x")
        with(map) {
            testNoOperation({ remove("y") }, { remove("y") })
            testNoOperation({ remove("x", 2) }, { remove("x", 2) })
//            testNoOperation({ put("x", 1) }, { put("x", 1) })     // does not hold
//            testNoOperation({ putAll(this) }, { putAll(this) })   // does not hold
            testNoOperation({ putAll(emptyMap()) }, { putAll(emptyMap()) })
        }
    }

    fun <K, V> ImmutableMap<K, V>.testNoOperation(persistent: ImmutableMap<K, V>.() -> ImmutableMap<K, V>, mutating: MutableMap<K, V>.() -> Unit) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure non-mutating operations return the same instance
        assertTrue(this === result)
        assertTrue(this === buildResult)
    }


    @Test
    fun covariantTyping() {
        val mapNothing = immutableMapOf<Nothing, Nothing>()
        val mapSI: ImmutableMap<String, Int> = mapNothing + ("x" to 1)
        val mapSNI: ImmutableMap<String, Int?> = mapSI + mapOf("y" to null)
        val mapANA: ImmutableMap<Any, Any?> = mapSNI + listOf(1 to "x")

        assertEquals(mapOf(1 to "x", "x" to 1, "y" to null), mapANA)
    }
}