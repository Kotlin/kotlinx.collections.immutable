/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
