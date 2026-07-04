/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PersistentOrderedSetExtraTest {

    @Test
    fun `retainingAll with an empty collection returns the empty ordered set singleton`() {
        val set = persistentSetOf(1, 2, 3)

        assertSame(persistentSetOf<Int>(), set.retainingAll(emptyList()))

        // a non-empty argument goes through the builder and preserves the original order
        val retained = set.retainingAll(listOf(3, 2, 5))
        assertEquals(listOf(2, 3), retained.toList())

        assertEquals(listOf(1, 2, 3), set.toList())
    }

    @Test
    fun `removing the first middle last and sole elements preserves the order of the rest`() {
        val set = persistentSetOf(1, 2, 3)

        assertEquals(listOf(2, 3), set.removing(1).toList())
        assertEquals(listOf(1, 3), set.removing(2).toList())
        assertEquals(listOf(1, 2), set.removing(3).toList())
        assertEquals(emptyList(), persistentSetOf(7).removing(7).toList())
        assertEquals(listOf(1, 2, 3), set.toList())
    }
}
