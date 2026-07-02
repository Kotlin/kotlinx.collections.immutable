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
    @Test
    fun `builder entry setValue invalidates cached map`() {
        val builder = persistentMapOf("a" to 1).builder()
        assertEquals(persistentMapOf("a" to 1), builder.build())

        val entry = builder.entries.iterator().next()
        assertEquals(1, entry.setValue(2))

        assertEquals(persistentMapOf("a" to 2), builder.build())
    }

    @Test
    fun `builder entry setValue preserves entry order`() {
        val builder = persistentMapOf("a" to 1, "b" to 2, "c" to 3).builder()
        val iterator = builder.entries.iterator()
        assertEquals("a", iterator.next().key)
        val entry = iterator.next()
        assertEquals(2, entry.setValue(20))

        assertEquals(20, entry.value)
        assertEquals(listOf("a", "b", "c"), builder.build().keys.toList())
        assertEquals(20, builder["b"])
    }

    @Test
    fun `builder entry setValue after a structural change updates the current links`() {
        val builder = persistentMapOf("a" to 1, "b" to 2, "c" to 3).builder()
        val entry = builder.entries.iterator().next()
        assertEquals(2, builder.remove("b"))
        assertEquals(persistentMapOf("a" to 1, "c" to 3), builder.build())

        assertEquals(1, entry.setValue(10))

        val built = builder.build()
        assertEquals(persistentMapOf("a" to 10, "c" to 3), built)
        assertEquals(listOf("a", "c"), built.keys.toList())
    }

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
