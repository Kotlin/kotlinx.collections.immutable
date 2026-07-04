/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.test.*

class PersistentHashMapExtraTest {

    @Test
    fun `entries of a non-empty persistent hash map contain exactly the map contents`() {
        val map = persistentHashMapOf("a" to 1, "b" to 2, "c" to 3) as PersistentHashMap<String, Int>
        val entries = map.entries
        assertEquals(3, entries.size)
        assertEquals(setOf("a" to 1, "b" to 2, "c" to 3), entries.map { it.key to it.value }.toSet())
    }

    @Test
    fun `removing an absent key returns the same map instance`() {
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 32) // shares the level-0 hash segment with k1, so the root holds a sub-node
        val map = persistentHashMapOf(k1 to 1, k2 to 2)

        // the probe descends into the sub-node and lands on an entry with a different key
        assertSame(map, map.removing(IntWrapper(9, 0)))
        // the probe descends into the sub-node and lands on an empty slot there
        assertSame(map, map.removing(IntWrapper(9, 64)))
        // the probe lands on an empty slot directly in the root
        assertSame(map, map.removing(IntWrapper(9, 1)))
    }

    @Test
    fun `putting into a full hash collision node updates it correctly`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val sharedValue = "a"
        val map = persistentHashMapOf(a to sharedValue, b to "b")

        // putting the very same value instance for an existing colliding key changes nothing
        assertSame(map, map.putting(a, sharedValue))

        val updated = map.putting(a, "a2")
        assertEquals(2, updated.size)
        assertEquals("a2", updated[a])
        assertEquals("b", updated[b])
        assertEquals(sharedValue, map[a]) // the original map is unaffected

        val c = IntWrapper(3, 0)
        val extended = map.putting(c, "c")
        assertEquals(3, extended.size)
        assertEquals("c", extended[c])
        assertEquals(sharedValue, extended[a])

        // removing an absent key that fully collides with the present ones changes nothing
        assertSame(map, map.removing(IntWrapper(9, 0)))
    }

    @Test
    fun `removing by key and value removes only exactly matching root entries`() {
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 1)
        val map = persistentHashMapOf(k1 to 1, k2 to 2)

        assertSame(map, map.removing(k1, 999))             // value mismatch
        assertSame(map, map.removing(IntWrapper(3, 2), 1)) // absent key

        val removed = map.removing(k1, 1)
        assertEquals(1, removed.size)
        assertNull(removed[k1])
        assertEquals(2, removed[k2])
        assertEquals(1, map[k1]) // the original map is unaffected

        val emptied = persistentHashMapOf(k1 to 1).removing(k1, 1)
        assertSame(PersistentHashMap.emptyOf<IntWrapper, Int>(), emptied)
    }

    @Test
    fun `removing by key and value under a full hash collision`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val c = IntWrapper(3, 0)
        val map = persistentHashMapOf(a to "a", b to "b", c to "c")

        assertSame(map, map.removing(a, "x"))                // value mismatch
        assertSame(map, map.removing(IntWrapper(4, 0), "a")) // absent colliding key

        val removed = map.removing(b, "b")
        assertEquals(2, removed.size)
        assertNull(removed[b])
        assertEquals("a", removed[a])
        assertEquals("c", removed[c])
        assertEquals("b", map[b]) // the original map is unaffected
    }

    @Test
    fun `builder removes by key and value under a full hash collision`() {
        val a = IntWrapper(1, 0)
        val b = IntWrapper(2, 0)
        val c = IntWrapper(3, 0)
        val map = persistentHashMapOf(a to "a", b to "b", c to "c") as PersistentHashMap<IntWrapper, String>
        val builder = map.builder()

        assertFalse(builder.remove(a, "x"))                // value mismatch
        assertFalse(builder.remove(IntWrapper(4, 0), "a")) // absent colliding key
        assertNull(builder.remove(IntWrapper(4, 0)))       // removal by an absent colliding key
        assertEquals(3, builder.size)

        assertTrue(builder.remove(b, "b"))
        assertEquals(2, builder.size)
        assertNull(builder[b])
        assertEquals("a", builder[a])
        assertEquals("c", builder[c])
        assertEquals<Map<IntWrapper, String>>(mapOf(a to "a", c to "c"), builder.build())
    }

    @Test
    fun `cleared persistent hash map is the empty map`() {
        val map = persistentHashMapOf("a" to 1, "b" to 2)
        val cleared = map.cleared()
        assertTrue(cleared.isEmpty())
        assertSame(PersistentHashMap.emptyOf<String, Int>(), cleared)
        assertEquals(2, map.size) // the original map is unaffected
    }

}
