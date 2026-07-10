/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.test.*

class PersistentMapEntryIteratorTest {

    @Test
    fun `setValue on a builder entry in the middle of iteration updates the builder and keeps iterating`() {
        val k1 = IntWrapper(1, 1)
        val k2 = IntWrapper(2, 2)
        val k3 = IntWrapper(3, 3)
        val k4 = IntWrapper(4, 4)
        val builder = persistentHashMapOf(k1 to 1, k2 to 2, k3 to 3, k4 to 4).builder()
        val iterator = builder.entries.iterator()

        val first = iterator.next()
        val firstOldValue = first.value
        assertEquals(firstOldValue, first.setValue(firstOldValue + 100))
        assertEquals(firstOldValue + 100, first.value)
        assertEquals(firstOldValue + 100, builder[first.key])

        // collect into a list so a broken resetPath re-yielding visited entries is caught
        val seen = mutableListOf(first.key to first.value)
        while (iterator.hasNext()) {
            val entry = iterator.next()
            seen.add(entry.key to entry.value)
        }
        assertEquals(4, seen.size)
        val expected = mutableMapOf(k1 to 1, k2 to 2, k3 to 3, k4 to 4)
        expected[first.key] = firstOldValue + 100
        assertEquals(expected, seen.toMap())
        assertEquals<Map<IntWrapper, Int>>(expected, builder.build())
    }

    @Test
    fun `setValue on the last builder entry works when iteration is finished`() {
        val k1 = IntWrapper(1, 1)
        val k2 = IntWrapper(2, 2)
        val builder = persistentHashMapOf(k1 to 1, k2 to 2).builder()
        val iterator = builder.entries.iterator()

        var last = iterator.next()
        while (iterator.hasNext()) {
            last = iterator.next()
        }

        val oldValue = last.value
        assertEquals(oldValue, last.setValue(50))
        assertFalse(iterator.hasNext())
        assertEquals(50, builder[last.key])

        val expected = mutableMapOf(k1 to 1, k2 to 2)
        expected[last.key] = 50
        assertEquals<Map<IntWrapper, Int>>(expected, builder.build())
    }

    @Test
    fun `setValue on an entry whose key was removed from the builder directly is a no-op`() {
        val k1 = IntWrapper(1, 1)
        val k2 = IntWrapper(2, 2)
        val k3 = IntWrapper(3, 3)
        val builder = persistentHashMapOf(k1 to 1, k2 to 2, k3 to 3).builder()
        val iterator = builder.entries.iterator()

        val entry = iterator.next()
        val oldValue = entry.value
        assertEquals(oldValue, builder.remove(entry.key))
        assertEquals(2, builder.size)

        assertEquals(oldValue, entry.setValue(100))
        assertEquals(100, entry.value) // only the detached entry object is updated
        assertFalse(builder.containsKey(entry.key)) // the builder does not get the key back
        assertEquals(2, builder.size)
    }

    @Test
    fun `setValue during iteration over colliding keys repositions the iterator inside the collision node`() {
        val k1 = IntWrapper(1, 0)
        val k2 = IntWrapper(2, 0)
        val k3 = IntWrapper(3, 0)
        val builder = persistentHashMapOf(k1 to "a", k2 to "b", k3 to "c").builder()
        val iterator = builder.entries.iterator()

        val first = iterator.next()
        val firstOldValue = first.value
        assertEquals(firstOldValue, first.setValue(firstOldValue + "!"))
        assertEquals(firstOldValue + "!", builder[first.key])

        // collect into a list so a broken resetPath re-yielding visited entries is caught
        val seen = mutableListOf(first.key to first.value)
        while (iterator.hasNext()) {
            val entry = iterator.next()
            seen.add(entry.key to entry.value)
        }
        assertEquals(3, seen.size)
        val expected = mutableMapOf(k1 to "a", k2 to "b", k3 to "c")
        expected[first.key] = firstOldValue + "!"
        assertEquals(expected, seen.toMap())
        assertEquals<Map<IntWrapper, String>>(expected, builder.build())
    }
}
