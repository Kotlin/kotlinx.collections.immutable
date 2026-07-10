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

    @Test
    fun `null keys flow through hash map operations`() {
        // a null key splitting a root entry cell into a sub-node
        val withNull = persistentHashMapOf<Int?, String>(null to "n")
        val split = withNull.putting(32, "x")
        assertEquals("n", split[null])
        assertEquals("x", split[32])

        // two-arg removes with a null key
        assertSame(withNull, withNull.removing(null, "other"))
        assertSame(PersistentHashMap.emptyOf<Int?, String>(), withNull.removing(null, "n"))
        val builder = (withNull as PersistentHashMap<Int?, String>).builder()
        assertFalse(builder.remove(null, "other"))
        assertTrue(builder.remove(null, "n"))
        assertTrue(builder.isEmpty())

        // bulk putAll around null keys, in both directions
        val nullIntoNodes = persistentHashMapOf<Int?, String>(0 to "a", 32 to "b").builder()
        nullIntoNodes.putAll(persistentHashMapOf<Int?, String>(null to "n"))
        assertEquals(mapOf<Int?, String>(0 to "a", 32 to "b", null to "n"), nullIntoNodes.build())
        val nodesIntoNull = persistentHashMapOf<Int?, String>(null to "n").builder()
        nodesIntoNull.putAll(persistentHashMapOf<Int?, String>(0 to "a", 32 to "b"))
        assertEquals(mapOf<Int?, String>(0 to "a", 32 to "b", null to "n"), nodesIntoNull.build())

        // putAll merging two single entries that share a position, null on either side
        val nullMeets32 = persistentHashMapOf<Int?, String>(null to "n").builder()
        nullMeets32.putAll(persistentHashMapOf<Int?, String>(32 to "x"))
        assertEquals(mapOf<Int?, String>(null to "n", 32 to "x"), nullMeets32.build())
        val meets32Null = persistentHashMapOf<Int?, String>(32 to "x").builder()
        meets32Null.putAll(persistentHashMapOf<Int?, String>(null to "n"))
        assertEquals(mapOf<Int?, String>(null to "n", 32 to "x"), meets32Null.build())

        // iterator mutations around a null key that collides with a present hash-zero key
        val colliding = persistentHashMapOf<Any?, String>(0 to "a", null to "b").builder()
        val entryIterator = colliding.entries.iterator()
        while (entryIterator.hasNext()) {
            val entry = entryIterator.next()
            entry.setValue(entry.value + "!")
        }
        assertEquals(mapOf<Any?, String>(0 to "a!", null to "b!"), colliding.build())
        val keyIterator = colliding.keys.iterator()
        val removedKeys = mutableSetOf<Any?>()
        while (keyIterator.hasNext()) {
            removedKeys.add(keyIterator.next())
            keyIterator.remove()
        }
        assertEquals(setOf<Any?>(0, null), removedKeys)
        assertTrue(colliding.isEmpty())
    }

    @Test
    fun `removing by key and value with a same-slot key mismatch is a no-op`() {
        // IntWrapper(9, 1) lands on the slot occupied by IntWrapper(2, 1) without matching it
        val k = IntWrapper(2, 1)
        val map = persistentHashMapOf(k to 2) as PersistentHashMap<IntWrapper, Int>

        assertSame(map, map.removing(IntWrapper(9, 1), 2))

        val builder = map.builder()
        assertFalse(builder.remove(IntWrapper(9, 1), 2))
        assertEquals(1, builder.size)
    }

    @Test
    fun `equal-sized hash maps with different structures are not equal`() {
        // same slot, different keys
        assertNotEquals(persistentHashMapOf(1 to "a"), persistentHashMapOf(33 to "a"))

        // equal entry positions, sub-nodes at different positions
        assertNotEquals(
            persistentHashMapOf(1 to "a", 2 to "b", 3 to "c", 35 to "d"),
            persistentHashMapOf(1 to "a", 2 to "b", 4 to "c", 36 to "d"),
        )

        // equal sizes with differently-sized collision groups
        val g1 = persistentHashMapOf(
            IntWrapper(1, 0) to "1", IntWrapper(2, 0) to "2", IntWrapper(3, 0) to "3",
            IntWrapper(4, 1) to "4", IntWrapper(5, 1) to "5",
        )
        val g2 = persistentHashMapOf(
            IntWrapper(1, 0) to "1", IntWrapper(2, 0) to "2",
            IntWrapper(4, 1) to "4", IntWrapper(5, 1) to "5", IntWrapper(6, 1) to "6",
        )
        assertNotEquals(g1, g2)

        // a hash map builder equals itself
        val builder = persistentHashMapOf(1 to "a").builder()
        assertEquals(builder, builder)
    }

    @Test
    fun `putting all entries of a colliding map takes the incoming values`() {
        // Regression test: the collision-node putAll used to keep the target's stale value
        // for a common key whenever the target had any extra colliding key (or the same keys).
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 0)
        val k3 = IntWrapper(3, 0)
        val incoming = persistentHashMapOf(k1 to "new", k3 to "c")

        // the target holds an extra colliding key: the merged collision node must carry the new value
        val merged = persistentHashMapOf(k1 to "old", k2 to "b").puttingAll(incoming)
        assertEquals(3, merged.size)
        assertEquals("new", merged[k1])
        assertEquals("b", merged[k2])

        // the target's colliding keys are a subset of the incoming ones
        assertEquals("new", persistentHashMapOf(k1 to "old").puttingAll(incoming)[k1])

        // the target holds exactly the same colliding keys
        val sameKeys = persistentHashMapOf(k1 to "old", k3 to "x").puttingAll(incoming)
        assertEquals("new", sameKeys[k1])
        assertEquals("c", sameKeys[k3])

        // fully disjoint collision nodes just merge
        val k4 = IntWrapper(4, 0)
        val disjoint = persistentHashMapOf(k2 to "b", k4 to "d").puttingAll(incoming)
        assertEquals(mapOf(k1 to "new", k2 to "b", k3 to "c", k4 to "d"), disjoint.toMap())

        // an owned builder root grows structurally through putAll
        val grown = persistentHashMapOf<IntWrapper, String>().builder()
        grown[k1] = "a"
        grown.putAll(persistentHashMapOf(IntWrapper(2, 1) to "b"))
        assertEquals(mapOf(k1 to "a", IntWrapper(2, 1) to "b"), grown.build())

        // the builder's optimized putAll is the same code path
        val builder = persistentHashMapOf(k1 to "old", k2 to "b").builder()
        builder.putAll(incoming)
        assertEquals("new", builder[k1])
        // and the fallback for a non-persistent argument agrees
        val builder2 = persistentHashMapOf(k1 to "old", k2 to "b").builder()
        builder2.putAll(mapOf(k1 to "new", k3 to "c"))
        assertEquals("new", builder2[k1])
    }

    @Test
    fun `putting all with nothing to update keeps the existing instances`() {
        val k1 = IntWrapper(1, 0)
        val k3 = IntWrapper(3, 0)
        val a = "a"

        // colliding keys with identical entries: the original map instance is returned
        val map = persistentHashMapOf(k1 to a, k3 to "x")
        assertSame(map, map.puttingAll(persistentHashMapOf(k1 to a, k3 to "x")))

        // a structure-preserving putAll into an owned builder root updates the value in place
        val builder = persistentHashMapOf<IntWrapper, String>().builder()
        builder[k1] = "old"
        builder.putAll(persistentHashMapOf(k1 to a))
        assertEquals(mapOf(k1 to a), builder.build())

        // an unowned builder whose putAll changes nothing builds the original map back
        val base = persistentHashMapOf(k1 to a)
        val noOpBuilder = base.builder()
        noOpBuilder.putAll(persistentHashMapOf(k1 to a))
        assertSame(base, noOpBuilder.build())
    }

}
