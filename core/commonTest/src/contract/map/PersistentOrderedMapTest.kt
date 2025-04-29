/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentOrderedMapTest {

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/198
     */
    @Test
    fun `when removing multiple keys with identical hashcodes the remaining key should be correctly promoted`() {
        class ChosenHashCode(
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

        assertEquals(minusAb.entries, cOnly.entries)
        assertEquals(minusAb, cOnly)
    }
}