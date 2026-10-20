/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.intersect
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentHashSet
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashSetTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `persistentHashSet and their builder should be equal before and after modification`() {
        val set1 = persistentHashSetOf(-1, 0, 32)
        val builder = set1.builder()

        assertTrue(set1.equals(builder))
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())

        val set2 = set1.removing(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/144
     */
    @Test
    fun `removing multiple batches should leave only remaining elements`() {
        val firstBatch = listOf(4554, 9380, 4260, 6602)
        val secondBatch = listOf(1188, 14794)
        val extraElement = 7450

        val set = firstBatch.plus(secondBatch).plus(extraElement).toPersistentHashSet()
        val result = set.minus(firstBatch.toPersistentHashSet()).minus(secondBatch)
        assertEquals(1, result.size)
        assertEquals(extraElement, result.first())
    }
}
