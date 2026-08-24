/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.collections.LinkedHashSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentOrderedSetBuilderTest {

    @Test
    fun `builder cache remains consistent after repeated removals and rebuilds`() {
        var persistent = persistentSetOf<TraceKey>()
        var builder = persistentSetOf<TraceKey>().builder()

        var expectedPersistent = linkedSetOf<TraceKey>()
        var expectedBuilder = linkedSetOf<TraceKey>()

        fun builderAdd(value: Int) {
            builder.add(key(value))
            expectedBuilder.add(key(value))
        }

        fun builderAddAll(vararg values: Int) {
            val keys = keys(*values)
            builder.addAll(keys)
            expectedBuilder.addAll(keys)
        }

        fun builderRemove(value: Int) {
            builder.remove(key(value))
            expectedBuilder.remove(key(value))
        }

        fun builderRemoveAll(vararg values: Int) {
            val keys = keys(*values).toSet()
            builder.removeAll(keys)
            expectedBuilder.removeAll(keys)
        }

        fun persistentAdd(value: Int) {
            val key = key(value)
            persistent = persistent.adding(key)
            expectedPersistent.add(key)
        }

        fun persistentAddAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.addingAll(keys)
            expectedPersistent.addAll(keys)
        }

        fun persistentRemove(value: Int) {
            val key = key(value)
            persistent = persistent.removing(key)
            expectedPersistent.remove(key)
        }

        fun persistentRemoveAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.removingAll(keys)
            expectedPersistent.removeAll(keys.toSet())
        }

        fun rebuildBuilderFromPersistent() {
            builder = persistent.builder()
            expectedBuilder = LinkedHashSet(expectedPersistent)
        }

        fun rebuildPersistentFromBuilder() {
            persistent = builder.build()
            expectedPersistent = LinkedHashSet(expectedBuilder)
        }

        builderAdd(348)
        builderRemoveAll(348, 348, 64)
        persistentAddAll(368, 274, 483, 445)
        rebuildBuilderFromPersistent()
        rebuildPersistentFromBuilder()
        builderAddAll(368, 368, 368, 368)
        persistentAdd(457)
        builderRemove(368)
        builderRemoveAll(49, 274)
        builderAdd(302)
        persistentRemoveAll(346, 43, 169, 368)
        builderRemoveAll(483, 211, 348, 442, 211)
        persistentAddAll(400)
        builderAdd(158)
        persistentAdd(164)
        persistentAddAll(277, 90, 274)
        persistentAddAll(274, 27)
        rebuildPersistentFromBuilder()
        persistentRemoveAll(197, 342, 438, 287, 498)
        rebuildPersistentFromBuilder()
        builderRemoveAll(445, 445, 312)
        rebuildPersistentFromBuilder()
        builderAddAll(302)
        rebuildBuilderFromPersistent()
        persistentAddAll(302)
        persistentRemoveAll(155, 434, 206)
        persistentRemoveAll(15, 96, 22, 302)
        builderRemove(302)
        builderAdd(243)
        persistentAddAll(158, 286)
        builderRemoveAll(155, 74, 61, 158, 186)
        persistentAdd(298)
        persistentRemove(85)
        builderRemove(243)
        persistentAdd(44)
        persistentRemoveAll(406)

        assertEquals(expectedPersistent, LinkedHashSet(persistent.toList()))
        assertEquals(expectedBuilder, LinkedHashSet(builder.build().toList()))
        assertEquals(expectedBuilder.toList(), builder.build().toList())
    }

    @Test
    fun `iterator remove after a remove of a different element throws ConcurrentModificationException`() {
        val builder = persistentSetOf(1, 2).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())

        assertTrue(builder.remove(2))

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(1), builder.build().toList())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `iterator remove after a remove of an already visited element throws ConcurrentModificationException`() {
        val builder = persistentSetOf(1, 2, 3).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())
        assertEquals(2, iterator.next())

        assertTrue(builder.remove(1))

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(2, 3), builder.build().toList())
    }

    @Test
    fun `iterator remove after external changes that cancel out in size throws ConcurrentModificationException`() {
        val builder = persistentSetOf(1, 2, 3).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())

        assertTrue(builder.remove(3))
        assertTrue(builder.add(4))

        assertTrue(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(1, 2, 4), builder.build().toList())
    }

    @Test
    fun `iterator remove on an exhausted iterator after an external add throws ConcurrentModificationException`() {
        val builder = persistentSetOf(1).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())
        assertFalse(iterator.hasNext())

        assertTrue(builder.add(2))

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
        assertEquals(listOf(1, 2), builder.build().toList())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `iterator remove after an external clear throws ConcurrentModificationException`() {
        val builder = persistentSetOf(1, 2).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())

        builder.clear()

        assertFailsWith<ConcurrentModificationException> { iterator.remove() }
    }

    @Test
    fun `iterator remove without a preceding next after an external remove throws IllegalStateException`() {
        val builder = persistentSetOf(1, 2).builder()
        val iterator = builder.iterator()

        assertTrue(builder.remove(2))

        assertFailsWith<IllegalStateException> { iterator.remove() }
        assertEquals(listOf(1), builder.build().toList())
    }

    @Test
    fun `iterator remove after external calls that change nothing does not throw`() {
        val builder = persistentSetOf(1, 2).builder()
        val iterator = builder.iterator()
        assertEquals(1, iterator.next())

        assertFalse(builder.add(2))
        assertFalse(builder.remove(3))

        iterator.remove()
        assertEquals(listOf(2), builder.build().toList())
    }

    private fun key(value: Int): TraceKey = TraceKey(value, hashForValue(value))

    private fun keys(vararg values: Int): List<TraceKey> = values.map(::key)

    private fun hashForValue(value: Int): Int =
        when (value.mod(10)) {
            0, 1, 2 -> 0
            3, 4, 5 -> 13 or ((value and 31) shl 5)
            6, 7 -> 13 or (7 shl 5) or ((value and 31) shl 10)
            else -> (value * 0x9E3779B9.toInt()).rotateLeft(7)
        }

    private class TraceKey(val value: Int, private val hash: Int) {
        override fun equals(other: Any?): Boolean =
            other is TraceKey && value == other.value && hash == other.hash

        override fun hashCode(): Int = hash
    }
}
