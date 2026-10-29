/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * A table runs every cell even after one fails, then fails once naming the failed cells; a table that ran no cell
 * fails too. The first three tests are of [testIterator].
 */
class CellsTest {
    private val setOfTwo: Origins<MutableSet<Int>> = rows { "a set of 2" { mutableSetOf(1, 2) } }
    private val noCall = operations<MutableSet<Int>>(thatKeepTheSize = { "no call" { } })
    private val nextOnly: Views<MutableSet<Int>, MutableIterator<Int>> = views(rows { "next()" { next() } }) { "iterator" { iterator() } }

    @Test
    fun `a builder that throws fails its own cells and the table goes on`() {
        val throwing: Origins<MutableSet<Int>> = rows { "a builder that throws" { error("no builder") } }
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(throwing + setOfTwo, nextOnly, noCall) } }
        assertTrue(failure.message!!.startsWith("2 of 4 cells failed:\na builder that throws, iterator, next() after no call"), failure.message)
        assertEquals("no builder", failure.cause?.message)
    }

    @Test
    fun `a call that moves the size but is marked as keeping it or the other way round fails its cells`() {
        val misplaced = operations<MutableSet<Int>>(
            thatKeepTheSize = { "add(3)" { add(3) } },
            thatMoveTheSize = { "no call" { } },
        )
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(setOfTwo, nextOnly, misplaced) } }
        assertTrue(failure.message!!.startsWith("4 of 4 cells failed:"), failure.message)
    }

    @Test
    fun `an iterator call whose own assertion fails fails its cells`() {
        val failing: Views<MutableSet<Int>, MutableIterator<Int>> = views(rows { "an assertion that fails" { assertTrue(false) } }) { "iterator" { iterator() } }
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(setOfTwo, failing, noCall) } }
        assertTrue(failure.message!!.startsWith("2 of 2 cells failed:"), failure.message)
    }

    @Test
    fun `a table without cells fails`() {
        assertFailsWith<AssertionError> { runCells { } }
    }
}
