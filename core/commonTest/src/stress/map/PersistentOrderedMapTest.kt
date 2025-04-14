/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.map

import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentOrderedMapTest {

    @Test
    fun equalsTest() {
        val expected = persistentMapOf("a" to 1, "b" to 2, "c" to 3)
        val actual = persistentMapOf<String, Int>().put("a", 1).put("b", 2).put("c", 3)
        assertEquals(expected, actual)
    }

    private class ChosenHashCode(
        private val hashCode: Int,
        private val name: String,
    ) {
        override fun equals(other: Any?): Boolean {
            return other is ChosenHashCode && (other.name == name)
        }

        override fun hashCode(): Int {
            return hashCode
        }

        override fun toString(): String {
            return name
        }
    }

    @Test
    fun testFromGitHubIssue() {
        val a = ChosenHashCode(123, "A")
        val b = ChosenHashCode(123, "B")
        val c = ChosenHashCode(123, "C")

        val abc = persistentMapOf(
            a to "x",
            b to "y",
            c to "z",
        )

        val minusAb = abc.minus(arrayOf(a, b))
        val cOnly = persistentMapOf(c to "z")

        assertEquals(cOnly.entries, minusAb.entries)
        assertEquals(cOnly, minusAb)
        assertEquals(minusAb, cOnly)
    }
}