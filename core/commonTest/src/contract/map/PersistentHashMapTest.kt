/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.plus
import tests.IntWrapper
import tests.trie.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashMapTest {

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
        val sum = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2) + persistentHashMapOf(collidingKey1 to 10, sibling to 3)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 2, sibling to 3), sum)
        assertEquals(10, sum[collidingKey1])

        val reversedSum = persistentHashMapOf(collidingKey1 to 10, sibling to 3) + persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2)
        assertEquals(3, reversedSum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, sibling to 3), reversedSum)
        assertEquals(1, reversedSum[collidingKey1])
    }

    @Test
    fun `putAll should insert a new key into a bottom-level collision node`() {
        val expected = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, collidingKey3 to 4, sibling to 3)

        val sum = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2) + persistentHashMapOf(collidingKey3 to 4, sibling to 3)
        assertEquals(4, sum.size)
        assertEquals(expected, sum)

        val reversedSum = persistentHashMapOf(collidingKey3 to 4, sibling to 3) + persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2)
        assertEquals(4, reversedSum.size)
        assertEquals(expected, reversedSum)
    }

    @Test
    fun `putAll should take the value of the argument for a key held by both collision nodes`() {
        val receiver = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2)
        val argument = persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 20)

        assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 20), receiver.puttingAll(argument))
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2), argument.puttingAll(receiver))
    }

    @Test
    fun `putAll should merge partially overlapping collision nodes`() {
        val receiver = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2)
        val argument = persistentHashMapOf(collidingKey2 to 20, collidingKey3 to 4)

        val sum = receiver.puttingAll(argument)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 20, collidingKey3 to 4), sum)

        val reversedSum = argument.puttingAll(receiver)
        assertEquals(3, reversedSum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, collidingKey3 to 4), reversedSum)
    }

    @Test
    fun `putAll should return a new map when the argument only replaces values of a collision node`() {
        val receiver = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, collidingKey3 to 4, sibling to 3)
        val argument = persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 20)

        val sum = receiver.puttingAll(argument)
        assertNotSame(receiver, sum)
        assertEquals(4, sum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 20, collidingKey3 to 4, sibling to 3), sum)

        val reversedSum = argument.puttingAll(receiver)
        assertEquals(4, reversedSum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, collidingKey3 to 4, sibling to 3), reversedSum)
    }

    @Test
    fun `putAll should keep a null value the argument adds to a collision node`() {
        val receiver = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to null)
        val argument = persistentHashMapOf(collidingKey2 to 20, collidingKey3 to null)

        val sum = receiver.puttingAll(argument)
        assertEquals(3, sum.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 20, collidingKey3 to null), sum)
        assertNull(sum[collidingKey3])
    }

    @Test
    fun `putAll should take a value of the argument that is equal but not the same instance`() {
        data class Value<T>(val value: T)

        val value = Value(1)
        val equalValue = Value(1)
        val receiver = persistentHashMapOf(collidingKey1 to value, collidingKey2 to value)
        val argument = persistentHashMapOf(collidingKey1 to equalValue, collidingKey2 to value)

        val sum = receiver.puttingAll(argument)
        assertNotSame(receiver, sum)
        assertEquals(receiver, sum)
        assertSame(equalValue, sum[collidingKey1])
    }

    @Test
    fun `puttingAll should keep the stored key instance when the argument holds the key in a subtree`() {
        val map = persistentHashMapOf(collidingKey1 to "a")

        val updated = map.puttingAll(persistentHashMapOf(collidingKey1.copy() to "x", levelOneSibling to "y"))

        assertEquals(2, updated.size)
        assertEquals("x", updated[collidingKey1])
        assertSame(collidingKey1, updated.keys.single { it == collidingKey1 })
    }

    @Test
    fun `putAll should return the same map when the argument brings no new values`() {
        val one = "one"
        val two = "two"
        val receiver = persistentHashMapOf(collidingKey1 to one, collidingKey2 to two, sibling to "three")
        val argument = persistentHashMapOf(collidingKey1 to one, collidingKey2 to two)

        assertSame(receiver, receiver.puttingAll(argument))
    }
}
