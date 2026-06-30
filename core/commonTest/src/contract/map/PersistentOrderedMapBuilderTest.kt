/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    private class TraceKey(val value: Int, private val hash: Int) {
        override fun equals(other: Any?): Boolean =
            other is TraceKey && value == other.value && hash == other.hash

        override fun hashCode(): Int = hash
    }
}
