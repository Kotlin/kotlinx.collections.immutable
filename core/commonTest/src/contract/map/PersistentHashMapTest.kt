/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
