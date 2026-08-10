/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The matrix only tests what it actually instantiates. Both halves are pinned here: that the six
 * representations really are the six runtime types `equals` dispatches on, and that comparing equal
 * content the matrix calls "different" is reported rather than passed over.
 */
class EqualityMatrixTest {

    private val entries = mapOf<IntWrapper, Value?>(
        IntWrapper(1, 7) to Value(1),
        IntWrapper(2, 7) to Value(2),
        IntWrapper(3, 3) to null,
    )

    @Test
    fun theOrderedBuilderIsTheTypeEqualsDispatchesOn() {
        // The other three representations are pinned by their declared return types, so asserting
        // them would be dead. This one is not: `PersistentOrderedMap.builder()` is declared as
        // `PersistentMap.Builder`, and `equals` dispatches on the concrete class.
        val ordered = entries.map { it.key to it.value }

        assertTrue(buildOrderedMap(ordered).builder() is PersistentOrderedMapBuilder)
    }

    @Test
    fun equalContentPassesTheMatrix() {
        val different = differentContent(entries)!!

        assertEquals(emptyList(), equalityFailures(entries, different))
    }

    @Test
    fun theUnequalHalfReportsWhenTheContentIsNotActuallyDifferent() {
        // Feeding the same content in as the "different" side must light up every pair, or the
        // unequal half of the matrix is not comparing anything.
        val failures = equalityFailures(entries, entries)

        assertTrue(failures.isNotEmpty(), "the unequal half never fired")
        assertTrue(failures.all { it.property == "equality.different" }, "$failures")
        assertEquals(6 * 6 * 2, failures.size)
    }

    @Test
    fun differentContentIsNullOnlyForAnEmptyMap() {
        assertEquals(null, differentContent(emptyMap()))
        assertTrue(differentContent(entries) != null)
    }
}
