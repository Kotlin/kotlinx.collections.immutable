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
 * fails too.
 */
class CellsTest {
    private val setOfTwo: Origins<MutableSet<Int>> = listOf("a set of 2" to { mutableSetOf(1, 2) })
    private val iterator: Views<MutableSet<Int>, MutableIterator<Int>> = listOf("iterator" to { iterator() })
    private val noCall = BuilderOperation<MutableSet<Int>>("no call", throwsCME = false) { }

    @Test
    fun `a builder that throws fails its own cell and the table goes on`() {
        val throwing: Origins<MutableSet<Int>> = listOf("a builder that throws" to { error("no builder") })
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(throwing + setOfTwo, iterator, listOf(noCall), listOf(next)) } }
        assertTrue(failure.message!!.startsWith("2 of 4 cells failed:\na builder that throws, iterator, next() after no call: "), failure.message)
        assertEquals("no builder", failure.cause?.message)
    }

    @Test
    fun `a call that moves the size but is marked as keeping it fails its cell`() {
        val add = BuilderOperation<MutableSet<Int>>("add(3)", throwsCME = false) { add(3) }
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(setOfTwo, iterator, listOf(add), listOf(next)) } }
        assertTrue(failure.message!!.startsWith("2 of 2 cells failed:"), failure.message)
    }

    @Test
    fun `a call that keeps the size but is marked as moving it fails its cell`() {
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(setOfTwo, iterator, listOf(BuilderOperation("no call") { }), listOf(next)) } }
        assertTrue(failure.message!!.startsWith("2 of 2 cells failed:"), failure.message)
    }

    @Test
    fun `an iterator call whose own assertion fails fails its cell`() {
        val failing: Call<MutableIterator<Int>> = "an assertion that fails" to { assertTrue(false) }
        val failure = assertFailsWith<AssertionError> { runCells { testIterator(setOfTwo, iterator, listOf(noCall), listOf(failing)) } }
        assertTrue(failure.message!!.startsWith("2 of 2 cells failed:"), failure.message)
    }

    @Test
    fun `an exception between the cells fails the table and keeps the cells before it`() {
        val failure = assertFailsWith<AssertionError> {
            runCells {
                cell("a failing cell") { error("in the cell") }
                error("between the cells")
            }
        }
        assertTrue(failure.message!!.startsWith("2 of 2 cells failed:\na failing cell: "), failure.message)
        assertEquals("in the cell", failure.cause?.message)
    }

    @Test
    fun `a table without cells fails`() {
        assertFailsWith<AssertionError> { runCells { } }
    }
}
