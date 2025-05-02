/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.toPersistentHashSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentHashSetTest {

    @Test
    fun `persistentHashSet and their builder should be equal before and after modification`() {
        val set1 = persistentHashSetOf(-1, 0, 32)
        val builder = set1.builder()

        assertTrue(set1.equals(builder))
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())

        val set2 = set1.remove(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/144
     */
    @Test
    fun reproducer() {
        val firstBatch = listOf(
            0b0_00100_01110_01010,
            0b0_00110_01110_01010,
            0b0_01001_00101_00100,
            0b0_00100_00101_00100
        )
        val secondBatch = listOf(
            0b0_00001_00101_00100,
            0b0_01110_01110_01010
        )
        val extraElement =
            0b0_00111_01000_11010

        val set = firstBatch.plus(secondBatch).plus(extraElement).toPersistentHashSet()
        val result = set.minus(firstBatch.toPersistentHashSet()).minus(secondBatch)
        assertEquals(1, result.size)
        assertEquals(extraElement, result.first())
    }
}