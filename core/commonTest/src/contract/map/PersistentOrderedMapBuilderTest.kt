/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentOrderedMapBuilderTest {

    @Test
    fun `no-op remove keeps the builder cache valid`() {
        val a = TraceKey(1, hash = 1)
        val b = TraceKey(2, hash = 1 or (1 shl 5))
        val absent = TraceKey(3, hash = 1 or (2 shl 5))

        val builder = persistentMapOf(a to 1, b to 2).builder()

        assertNull(builder.remove(absent))
        assertEquals(persistentMapOf(a to 1, b to 2), builder.build())
        assertEquals(listOf(a, b), builder.build().keys.toList())
    }

    @Test
    fun `entry setValue during iteration keeps the iterator valid`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()

        val visitedKeys = mutableListOf<Int>()
        for (entry in builder.entries) {
            visitedKeys.add(entry.key)
            assertEquals(entry.value, entry.setValue(entry.value + "!"))
        }

        assertEquals(listOf(1, 2, 3), visitedKeys)
        val built = builder.build()
        assertEquals(listOf(1, 2, 3), built.keys.toList())
        assertEquals(listOf("a!", "b!", "c!"), built.values.toList())
    }

    @Test
    fun `entry setValue during iteration survives an intervening build`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        assertEquals("a", iterator.next().setValue("a!"))

        val snapshot = builder.build()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            assertEquals(entry.value, entry.setValue(entry.value + "!"))
        }

        assertEquals(persistentMapOf(1 to "a!", 2 to "b", 3 to "c"), snapshot)
        val built = builder.build()
        assertEquals(listOf(1, 2, 3), built.keys.toList())
        assertEquals(listOf("a!", "b!", "c!"), built.values.toList())
    }

    @Test
    fun `iterator remove after entry setValue keeps the iterator valid`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        assertEquals("a", iterator.next().setValue("a!"))
        val _ = iterator.next()

        iterator.remove()

        assertEquals(3, iterator.next().key)
        val built = builder.build()
        assertEquals(listOf(1, 3), built.keys.toList())
        assertEquals(listOf("a!", "c"), built.values.toList())
    }

    @Test
    fun `put of a new value for a stored key during iteration keeps the iterator valid`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        val _ = iterator.next()

        assertEquals("b", builder.put(2, "b!"))

        assertEquals(2, iterator.next().key)
        assertEquals(3, iterator.next().key)
        assertEquals(listOf("a", "b!", "c"), builder.build().values.toList())
    }

    @Test
    fun `put of a new value for a stored key during iteration keeps the keys and values iterators valid`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val keys = builder.keys.iterator()
        val values = builder.values.iterator()
        assertEquals(1, keys.next())
        assertEquals("a", values.next())

        assertEquals("b", builder.put(2, "b!"))

        assertEquals(2, keys.next())
        assertEquals("b!", values.next())
    }

    @Test
    fun `put of a new key during iteration throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        val _ = iterator.next()

        builder[4] = "d"

        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `remove of a key during iteration throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        val _ = iterator.next()

        assertEquals("c", builder.remove(3))

        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after iterator remove does not re-add the key`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        iterator.remove()

        assertEquals("a", entry.setValue("z"))

        assertEquals(1, builder.size)
        assertNull(builder[1])
        assertEquals(2, iterator.next().key)
        assertEquals(listOf(2), builder.build().keys.toList())
    }

    @Test
    fun `entry setValue after remove of its key from the builder does not re-add the key`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        val entry = iterator.next()
        assertEquals("a", builder.remove(1))
        val snapshot = builder.build()

        assertEquals("a", entry.setValue("z"))
        assertEquals("z", entry.setValue("y"))

        assertEquals(1, builder.size)
        assertNull(builder[1])
        assertSame(snapshot, builder.build())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `entry setValue after remove and re-put of its key updates the value at the new position`() {
        val builder = persistentMapOf<Int, String?>(1 to "a", 2 to "b", 3 to "c").builder()
        val entry = builder.entries.iterator().next()
        assertEquals("a", builder.remove(1))
        assertNull(builder.put(1, null))

        assertEquals("a", entry.setValue("z"))

        val built = builder.build()
        assertEquals(listOf(2, 3, 1), built.keys.toList())
        assertEquals(listOf("b", "c", "z"), built.values.toList())
    }

    @Test
    fun `entry setValue of the stored value after remove and re-put keeps the builder cache valid`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val entry = builder.entries.iterator().next()
        assertEquals("a", builder.remove(1))
        assertNull(builder.put(1, "x"))
        val stored = builder[1]!!
        val snapshot = builder.build()

        assertEquals("a", entry.setValue(stored))

        assertSame(snapshot, builder.build())
    }

    @Test
    fun `entry setValue after clear does not re-add the key`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val entry = builder.entries.iterator().next()
        builder.clear()

        assertEquals("a", entry.setValue("z"))

        assertEquals(0, builder.size)
        assertEquals(persistentMapOf(), builder.build())
    }

    @Test
    fun `iterator remove after a remove of a different key throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()
        assertEquals(1, iterator.next().key)

        assertEquals("b", builder.remove(2))

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(1), builder.build().keys.toList())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `iterator remove after a remove of an already visited key throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        assertEquals(1, iterator.next().key)
        assertEquals(2, iterator.next().key)

        assertEquals("a", builder.remove(1))

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        val built = builder.build()
        assertEquals(listOf(2, 3), built.keys.toList())
        assertEquals(listOf("b", "c"), built.values.toList())
    }

    @Test
    fun `iterator remove after external changes that cancel out in size throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val iterator = builder.entries.iterator()
        assertEquals(1, iterator.next().key)

        assertEquals("c", builder.remove(3))
        assertNull(builder.put(4, "d"))

        assertTrue(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(1, 2, 4), builder.build().keys.toList())
    }

    @Test
    fun `iterator remove without a preceding next after an external remove throws IllegalStateException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val iterator = builder.entries.iterator()

        assertEquals("b", builder.remove(2))

        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertEquals(listOf(1), builder.build().keys.toList())
    }

    @Test
    fun `keys and values iterator remove after a remove of a different key throws ConcurrentModificationException`() {
        val builder = persistentMapOf(1 to "a", 2 to "b", 3 to "c").builder()
        val keys = builder.keys.iterator()
        val values = builder.values.iterator()
        assertEquals(1, keys.next())
        assertEquals("a", values.next())

        assertEquals("c", builder.remove(3))

        assertFailsWith<ConcurrentModificationException> { keys.remove() }
        assertFailsWith<ConcurrentModificationException> { values.remove() }
        assertEquals(listOf(1, 2), builder.build().keys.toList())
    }

    private class TraceKey(val value: Int, private val hash: Int) {
        override fun equals(other: Any?): Boolean =
            other is TraceKey && value == other.value && hash == other.hash

        override fun hashCode(): Int = hash
    }
}
