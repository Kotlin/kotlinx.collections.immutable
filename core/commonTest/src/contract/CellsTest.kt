/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CellsTest {
    private val setOfTwo: List<Pair<String, () -> MutableSet<Int>>> = listOf("a set of 2" to { mutableSetOf(1, 2) })
    private val iterator: List<Pair<String, MutableSet<Int>.() -> MutableIterator<Int>>> = listOf("iterator" to { iterator() })
    private val next = IteratorOperation<MutableIterator<Int>>("next()") { next() }

    @Test
    fun `a builder that throws fails its own cell and the table goes on`() {
        val builders = listOf<Pair<String, () -> MutableSet<Int>>>("a builder that throws" to { error("no builder") }) + setOfTwo
        val failure = assertFailsWith<AssertionError> {
            runCells { testIterator(builders, iterator, BuilderOperation("no call", throwsCME = false) { }, next) }
        }
        assertTrue(failure.message!!.startsWith("1 of 4 cells failed:\na builder that throws, iterator: "), failure.message)
        assertEquals("no builder", failure.cause?.message)
    }

    @Test
    fun `a call that moves the size but is marked as keeping it fails every cell`() {
        val add = BuilderOperation<MutableSet<Int>>("add(3)", throwsCME = false) { add(3) }
        val failure = assertFailsWith<AssertionError> {
            runCells {
                testIterator(setOfTwo, iterator, add, next)
                testIterationContinues(setOfTwo, iterator, add)
            }
        }
        assertTrue(failure.message!!.startsWith("8 of 8 cells failed:"), failure.message)
    }

    @Test
    fun `a call that keeps the size but is marked as moving it fails every cell`() {
        val failure = assertFailsWith<AssertionError> {
            runCells { testIterator(setOfTwo, iterator, BuilderOperation("no call") { }, next) }
        }
        assertTrue(failure.message!!.startsWith("3 of 3 cells failed:"), failure.message)
    }

    @Test
    fun `a table without cells fails`() {
        assertFailsWith<AssertionError> { runCells { } }
    }
}
