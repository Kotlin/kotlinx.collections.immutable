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
}
