/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.collections.LinkedHashSet
import kotlin.test.Test
import kotlin.test.assertEquals

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
            persistent = persistent.add(key)
            expectedPersistent.add(key)
        }

        fun persistentAddAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.addAll(keys)
            expectedPersistent.addAll(keys)
        }

        fun persistentRemove(value: Int) {
            val key = key(value)
            persistent = persistent.remove(key)
            expectedPersistent.remove(key)
        }

        fun persistentRemoveAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.removeAll(keys)
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

    private fun key(value: Int): TraceKey = TraceKey(value, hashForValue(value))

    private fun keys(vararg values: Int): List<TraceKey> = values.map(::key)

    private fun hashForValue(value: Int): Int =
        when (value.mod(10)) {
            0, 1, 2 -> 0
            3, 4, 5 -> 13 or ((value and 31) shl 5)
            6, 7 -> 13 or (7 shl 5) or ((value and 31) shl 10)
            else -> (value * 0x9E3779B9.toInt()).rotateLeft(7)
        }

    private data class TraceKey(
        val value: Int,
        private val hashCode: Int,
    ) {
        override fun hashCode(): Int = hashCode
    }
}
