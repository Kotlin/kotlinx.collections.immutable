/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.*

class PersistentOrderedMapExtraTest {

    @Test
    fun `removing by key and value from an ordered map handles absent keys and mismatches`() {
        val map = persistentMapOf("a" to 1)

        assertSame(map, map.removing("b", 1), "absent key")
        assertSame(map, map.removing("a", 999), "value mismatch")
        assertTrue(map.removing("a", 1).isEmpty())

        // an ordered map builder equals itself
        val builder = map.builder()
        assertEquals(builder, builder)
    }

    @Test
    fun `setValue on an entry whose key was removed from the builder re-inserts the key at the end`() {
        val builder = persistentMapOf("a" to 1, "b" to 2, "c" to 3).builder()
        val iterator = builder.entries.iterator()

        val first = iterator.next()
        assertEquals("a", first.key)
        assertEquals(1, builder.remove(first.key))
        assertEquals(2, builder.size)

        // Unlike the hash map builder, whose detached-entry setValue is a silent no-op,
        // the ordered builder falls back to put and appends the key to the iteration order.
        first.setValue(100)
        assertEquals(3, builder.size)
        assertEquals(100, builder["a"])
        assertEquals(listOf("b", "c", "a"), builder.build().keys.toList())
    }
}
