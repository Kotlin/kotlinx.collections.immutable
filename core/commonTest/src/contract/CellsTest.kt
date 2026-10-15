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

    @Test
    fun `a builder that throws fails its own cell and the table goes on`() {
        val builders: List<Pair<String, () -> MutableSet<Int>>> = listOf(
            "a builder that throws" to { error("no builder") },
            "a set of 2" to { mutableSetOf(1, 2) },
        )
        val views: List<Pair<String, MutableSet<Int>.() -> MutableIterator<Int>>> = listOf("iterator" to { iterator() })
        val failure = assertFailsWith<AssertionError> {
            runCells {
                testIterator(builders, views, BuilderOperation("no call", throwsCME = false) { }, IteratorOperation("next()") { next() })
            }
        }
        assertTrue(failure.message!!.startsWith("1 of 4 cells failed:\na builder that throws, iterator: "), failure.message)
        assertEquals("no builder", failure.cause?.message)
    }

    @Test
    fun `a table without cells fails`() {
        assertFailsWith<AssertionError> { runCells { } }
    }
}
