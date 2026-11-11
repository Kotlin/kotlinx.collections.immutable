/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.plus
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashMapTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `if the collision is of size 2 and one of the keys is removed the remaining key must be promoted`() {
        val map1: PersistentHashMap<Int, String> =
            persistentHashMapOf(-1 to "a", 0 to "b", 32 to "c") as PersistentHashMap<Int, String>
        val builder = map1.builder()
        val map2 = builder.build()

        assertTrue(map1.equals(builder))
        assertEquals(map1, map2.toMap())
        assertEquals(map1, map2)

        val map3 = map1.removing(0)
        builder.remove(0)
        val map4 = builder.build()

        assertTrue(map3.equals(builder))
        assertEquals(map3, map4.toMap())
        assertEquals(map3, map4)
    }

    @Test
    fun `builder should correctly handle multiple element removals in case of full collision`() {
        val a = IntWrapper(0, 0)
        val b = IntWrapper(1, 0)
        val c = IntWrapper(2, 0)

        val original: PersistentHashMap<IntWrapper, String> =
            persistentHashMapOf(a to "a", b to "b", c to "c") as PersistentHashMap<IntWrapper, String>

        val onlyA: PersistentHashMap<IntWrapper, String> =
            persistentHashMapOf(a to "a") as PersistentHashMap<IntWrapper, String>

        val builder = original.builder()
        builder.remove(b)
        builder.remove(c)
        val removedBC = builder.build()

        assertEquals(onlyA, removedBC)
    }

    @Test
    fun `builder should correctly handle multiple element removals in case of partial collision`() {
        val a = IntWrapper(0, 0)
        val b = IntWrapper(1, 0)
        val c = IntWrapper(2, 0)
        val d = IntWrapper(3, 11)

        val original: PersistentHashMap<IntWrapper, String> =
            persistentHashMapOf(a to "a", b to "b", c to "c", d to "d") as PersistentHashMap<IntWrapper, String>

        val afterImmutableRemoving = original.removing(b).removing(c)

        val builder = original.builder()
        builder.remove(b)
        builder.remove(c)
        val afterMutableRemoving = builder.build()

        assertEquals(afterImmutableRemoving, afterMutableRemoving)
    }

    @Test
    fun `putAll should not duplicate a key stored in a bottom-level collision node`() {
        val sum = persistentHashMapOf(a1 to 1, a2 to 2) + persistentHashMapOf(a1 to 10, sibling to 3)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(a1 to 10, a2 to 2, sibling to 3), sum)
        assertEquals(10, sum[a1])

        val reversedSum = persistentHashMapOf(a1 to 10, sibling to 3) + persistentHashMapOf(a1 to 1, a2 to 2)
        assertEquals(3, reversedSum.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 2, sibling to 3), reversedSum)
        assertEquals(1, reversedSum[a1])
    }

    @Test
    fun `putAll should insert a new key into a bottom-level collision node`() {
        val expected = persistentHashMapOf(a1 to 1, a2 to 2, a3 to 4, sibling to 3)

        val sum = persistentHashMapOf(a1 to 1, a2 to 2) + persistentHashMapOf(a3 to 4, sibling to 3)
        assertEquals(4, sum.size)
        assertEquals(expected, sum)

        val reversedSum = persistentHashMapOf(a3 to 4, sibling to 3) + persistentHashMapOf(a1 to 1, a2 to 2)
        assertEquals(4, reversedSum.size)
        assertEquals(expected, reversedSum)
    }

    @Test
    fun `putAll should take the value of the argument for a key held by both collision nodes`() {
        val receiver = persistentHashMapOf(a1 to 1, a2 to 2)
        val argument = persistentHashMapOf(a1 to 10, a2 to 20)

        assertEquals(persistentHashMapOf(a1 to 10, a2 to 20), receiver.puttingAll(argument))
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 2), argument.puttingAll(receiver))
    }

    @Test
    fun `putAll should merge partially overlapping collision nodes`() {
        val receiver = persistentHashMapOf(a1 to 1, a2 to 2)
        val argument = persistentHashMapOf(a2 to 20, a3 to 4)

        val sum = receiver.puttingAll(argument)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 20, a3 to 4), sum)

        val reversedSum = argument.puttingAll(receiver)
        assertEquals(3, reversedSum.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 2, a3 to 4), reversedSum)
    }

    @Test
    fun `putAll should return a new map when the argument only replaces values of a collision node`() {
        val receiver = persistentHashMapOf(a1 to 1, a2 to 2, a3 to 4, sibling to 3)
        val argument = persistentHashMapOf(a1 to 10, a2 to 20)

        val sum = receiver.puttingAll(argument)
        assertNotSame(receiver, sum)
        assertEquals(4, sum.size)
        assertEquals(persistentHashMapOf(a1 to 10, a2 to 20, a3 to 4, sibling to 3), sum)

        val reversedSum = argument.puttingAll(receiver)
        assertEquals(4, reversedSum.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 2, a3 to 4, sibling to 3), reversedSum)
    }

    @Test
    fun `putAll should keep a null value the argument adds to a collision node`() {
        val receiver = persistentHashMapOf(a1 to 1, a2 to null)
        val argument = persistentHashMapOf(a2 to 20, a3 to null)

        val sum = receiver.puttingAll(argument)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(a1 to 1, a2 to 20, a3 to null), sum)
        assertNull(sum[a3])
    }

    @Test
    fun `putAll should take a value of the argument that is equal but not the same instance`() {
        data class Value<T>(val value: T)

        val value = Value(1)
        val equalValue = Value(1)
        val receiver = persistentHashMapOf(a1 to value, a2 to value)
        val argument = persistentHashMapOf(a1 to equalValue, a2 to value)

        val sum = receiver.puttingAll(argument)
        assertNotSame(receiver, sum)
        assertEquals(receiver, sum)
        assertSame(equalValue, sum[a1])
    }

    @Test
    fun `putAll should return the same map when the argument brings no new values`() {
        val one = "one"
        val two = "two"
        val receiver = persistentHashMapOf(a1 to one, a2 to two, sibling to "three")
        val argument = persistentHashMapOf(a1 to one, a2 to two)

        assertSame(receiver, receiver.puttingAll(argument))
    }

    @Test
    fun `entries of a non-empty persistent hash map contain exactly the map contents`() {
        val map = persistentHashMapOf("a" to 1, "b" to 2, "c" to 3)
        val entries = map.entries
        assertEquals(3, entries.size)
        assertEquals(setOf("a" to 1, "b" to 2, "c" to 3), entries.map { it.key to it.value }.toSet())
    }

    @Test
    fun `removing an absent key returns the same map instance`() {
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 32)
        val map = persistentHashMapOf(k1 to 1, k2 to 2)

        assertSame(map, map.removing(IntWrapper(9, 0)))
        assertSame(map, map.removing(IntWrapper(9, 64)))
        assertSame(map, map.removing(IntWrapper(9, 1)))
    }

    @Test
    fun `putting into a full hash collision node updates it correctly`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val sharedValue = "a"
        val map = persistentHashMapOf(a to sharedValue, b to "b")

        assertSame(map, map.putting(a, sharedValue))

        val updated = map.putting(a, "a2")
        assertEquals(2, updated.size)
        assertEquals("a2", updated[a])
        assertEquals("b", updated[b])
        assertEquals(sharedValue, map[a])

        val c = IntWrapper(3, 0)
        val extended = map.putting(c, "c")
        assertEquals(3, extended.size)
        assertEquals("c", extended[c])
        assertEquals(sharedValue, extended[a])

        assertSame(map, map.removing(IntWrapper(9, 0)))
    }

    @Test
    fun `removing by key and value removes only exactly matching root entries`() {
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 1)
        val map = persistentHashMapOf(k1 to 1, k2 to 2)

        assertSame(map, map.removing(k1, 999))
        assertSame(map, map.removing(IntWrapper(3, 2), 1))

        val removed = map.removing(k1, 1)
        assertEquals(1, removed.size)
        assertNull(removed[k1])
        assertEquals(2, removed[k2])
        assertEquals(1, map[k1])

        val emptied = persistentHashMapOf(k1 to 1).removing(k1, 1)
        assertSame(PersistentHashMap.emptyOf(), emptied)
    }

    @Test
    fun `removing by key and value under a full hash collision`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val c = IntWrapper(3, 0)
        val map = persistentHashMapOf(a to "a", b to "b", c to "c")

        assertSame(map, map.removing(a, "x"))
        assertSame(map, map.removing(IntWrapper(4, 0), "a"))

        val removed = map.removing(b, "b")
        assertEquals(2, removed.size)
        assertNull(removed[b])
        assertEquals("a", removed[a])
        assertEquals("c", removed[c])
        assertEquals("b", map[b])
    }

    @Test
    fun `builder removes by key and value under a full hash collision`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val c = IntWrapper(3, 0)
        val map = persistentHashMapOf(a to "a", b to "b", c to "c") as PersistentHashMap<IntWrapper, String>
        val builder = map.builder()

        assertFalse(builder.remove(a, "x"))
        assertFalse(builder.remove(IntWrapper(4, 0), "a"))
        assertNull(builder.remove(IntWrapper(4, 0)))
        assertEquals(3, builder.size)

        assertTrue(builder.remove(b, "b"))
        assertEquals(2, builder.size)
        assertNull(builder[b])
        assertEquals("a", builder[a])
        assertEquals("c", builder[c])
        assertEquals(mapOf(a to "a", c to "c"), builder.build())
    }

    @Test
    fun `cleared persistent hash map is the empty map`() {
        val map = persistentHashMapOf("a" to 1, "b" to 2)
        val cleared = map.cleared()
        assertTrue(cleared.isEmpty())
        assertSame(PersistentHashMap.emptyOf(), cleared)
        assertEquals(2, map.size)
    }
}
