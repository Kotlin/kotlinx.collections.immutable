/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.persistentListOf
import tests.contract.forEachBuilder
import tests.contract.listBuilders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private val PersistentList.Builder<*>.modCount: Int get() = (this as PersistentVectorBuilder<*>).getModCount()

/** The list builder's clear drops the root and the tail in one step: one modification, and the builder goes on from the empty list. */
class PersistentListBuilderTest {
    @Test
    fun `add and build after a clear`() = forEachBuilder(listBuilders) { builder ->
        val size = builder.size
        val built = builder.build()
        val modCount = builder.modCount

        builder.clear()

        assertEquals(modCount + 1, builder.modCount)
        assertSame(persistentListOf(), builder.build())
        repeat(size) { builder.add(size + it) }
        assertEquals(List<Any>(size) { size + it }, builder.build())
        assertEquals(List<Any>(size) { it }, built)
    }
}
