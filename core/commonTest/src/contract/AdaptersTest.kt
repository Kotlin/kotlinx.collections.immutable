/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.adapters.ImmutableCollectionAdapter
import kotlinx.collections.immutable.adapters.ImmutableListAdapter
import kotlinx.collections.immutable.adapters.ImmutableMapAdapter
import kotlinx.collections.immutable.adapters.ImmutableSetAdapter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptersTest {

    @Test
    fun `collection adapter delegates queries and equality to the wrapped collection`() {
        val impl: Collection<String> = listOf("a", "b")
        val adapter = ImmutableCollectionAdapter(impl)

        assertEquals(2, adapter.size)
        assertFalse(adapter.isEmpty())
        assertTrue(adapter.contains("a"))
        assertFalse(adapter.contains("c"))
        assertTrue(adapter.containsAll(listOf("a", "b")))
        assertContentEquals(impl, adapter)

        // equals is delegated to the wrapped list, so the adapter equals an equal plain list
        assertTrue(adapter == listOf("a", "b"))
        assertFalse(adapter == listOf("b", "a"))
        assertEquals(impl.hashCode(), adapter.hashCode())
        assertEquals("[a, b]", adapter.toString())
    }

    @Test
    fun `list adapter delegates to the wrapped list and equals equal lists in both directions`() {
        val impl = listOf(1, 2, 3)
        val adapter = ImmutableListAdapter(impl)

        assertEquals(3, adapter.size)
        assertEquals(2, adapter[1])
        assertEquals(2, adapter.indexOf(3))
        assertTrue(adapter.contains(3))
        assertFalse(adapter.contains(4))
        assertContentEquals(impl, adapter)

        assertEquals(impl, adapter)
        assertEquals<List<Int>>(adapter, impl)
        assertEquals(impl.hashCode(), adapter.hashCode())
        assertEquals("[1, 2, 3]", adapter.toString())
        assertFalse(adapter == listOf(1, 2))
        assertFalse(adapter == listOf(3, 2, 1))

        val persistent = persistentListOf(1, 2, 3)
        assertEquals<List<Int>>(persistent, adapter)
        assertEquals<List<Int>>(adapter, persistent)
        assertEquals(persistent.hashCode(), adapter.hashCode())
    }

    @Test
    fun `list adapter subList is an immutable adapter over the backing subList`() {
        val adapter = ImmutableListAdapter(listOf(1, 2, 3, 4, 5))

        val sub: ImmutableList<Int> = adapter.subList(1, 4)
        assertTrue(sub is ImmutableListAdapter<*>)
        assertEquals(listOf(2, 3, 4), sub)
        assertEquals(3, sub.size)
        assertEquals(2, sub[0])

        // the returned adapter supports taking sub-lists further
        assertEquals(listOf(3, 4), sub.subList(1, 3))

        // out-of-bounds ranges are rejected by the backing subList implementation
        assertFailsWith<IndexOutOfBoundsException> { adapter.subList(2, 6) }
    }

    @Test
    fun `set adapter delegates to the wrapped set and equals equal sets in both directions`() {
        val impl = setOf(1, 2, 3)
        val adapter = ImmutableSetAdapter(impl)

        assertEquals(3, adapter.size)
        assertTrue(adapter.contains(2))
        assertFalse(adapter.contains(5))
        assertTrue(adapter.containsAll(listOf(1, 3)))
        assertEquals(listOf(1, 2, 3), adapter.toList())

        assertEquals(impl, adapter)
        assertEquals<Set<Int>>(adapter, impl)
        assertEquals(impl.hashCode(), adapter.hashCode())
        assertEquals(impl.toString(), adapter.toString())
        assertFalse(adapter == setOf(1, 2))

        val persistent = persistentSetOf(1, 2, 3)
        assertEquals<Set<Int>>(persistent, adapter)
        assertEquals<Set<Int>>(adapter, persistent)
        assertEquals(persistent.hashCode(), adapter.hashCode())
    }

    @Test
    fun `map adapter delegates to the wrapped map and exposes immutable views`() {
        val impl = mapOf(1 to "a", 2 to "b", 3 to "c")
        val adapter = ImmutableMapAdapter(impl)

        assertEquals(3, adapter.size)
        assertFalse(adapter.isEmpty())
        assertEquals("b", adapter[2])
        assertNull(adapter[4])
        assertTrue(adapter.containsKey(1))
        assertFalse(adapter.containsKey(5))
        assertTrue(adapter.containsValue("c"))
        assertFalse(adapter.containsValue("z"))

        // views are immutable-typed adapters over the wrapped map's views
        val keys: ImmutableSet<Int> = adapter.keys
        assertTrue(keys is ImmutableSetAdapter<*>)
        assertEquals(impl.keys, keys)

        val values: ImmutableCollection<String> = adapter.values
        assertTrue(values is ImmutableCollectionAdapter<*>)
        assertEquals(listOf("a", "b", "c"), values.toList())

        val entries: ImmutableSet<Map.Entry<Int, String>> = adapter.entries
        assertTrue(entries is ImmutableSetAdapter<*>)
        assertEquals(3, entries.size)
        assertEquals(listOf(1 to "a", 2 to "b", 3 to "c"), entries.map { it.key to it.value })

        assertEquals(impl, adapter)
        assertEquals<Map<Int, String>>(adapter, impl)
        assertEquals(impl.hashCode(), adapter.hashCode())
        assertEquals(impl.toString(), adapter.toString())
        assertFalse(adapter == mapOf(1 to "a"))

        val persistent = persistentMapOf(1 to "a", 2 to "b", 3 to "c")
        assertEquals<Map<Int, String>>(persistent, adapter)
        assertEquals<Map<Int, String>>(adapter, persistent)
        assertEquals(persistent.hashCode(), adapter.hashCode())
    }
}
