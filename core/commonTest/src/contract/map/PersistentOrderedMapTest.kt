/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.persistentMapOf
import tests.contract.drain
import tests.contract.mapViews
import tests.contract.runCells
import tests.trie.MutableKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

private const val KEY_MADE_EQUAL = "A key has become equal to another key after it was added to the persistent map."

class PersistentOrderedMapTest {

    private val iterators: List<Pair<String, PersistentMap<MutableKey, String>.() -> Iterator<*>>> = listOf(
        "the map's entries iterator" to { entries.iterator() },
        "the map's keys iterator" to { keys.iterator() },
        "the map's values iterator" to { values.iterator() },
        "the builder's entries iterator" to { builder().entries.iterator() },
        "the builder's keys iterator" to { builder().keys.iterator() },
        "the builder's values iterator" to { builder().values.iterator() },
    )

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

    @Test
    fun `next after the hash of the next key changed throws ConcurrentModificationException and goes on from that key once the hash is back`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val key = MutableKey(2)
            val map = persistentMapOf(MutableKey(1) to "a", key to "b", MutableKey(3) to "c")
            val iterator = map.open()
            val _ = iterator.next()

            key.id = 9
            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            key.id = 2

            assertEquals(map.open().drain().drop(1), iterator.drain())
        }
    }

    @Test
    fun `a key made equal to an earlier one makes the last next throw ConcurrentModificationException`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val key = MutableKey(3)
            val map = persistentMapOf(MutableKey(1) to "a", MutableKey(2) to "b", key to "c")
            key.id = 1
            val iterator = map.open()
            repeat(map.size - 1) { val _ = iterator.next() }

            val failure = assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertEquals(KEY_MADE_EQUAL, failure.message)
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `a key made equal to a later one makes the next that ends the chain early throw ConcurrentModificationException`() = runCells {
        for ((name, open) in iterators) cell(name) {
            val key = MutableKey(2)
            val map = persistentMapOf(MutableKey(1) to "a", key to "b", MutableKey(3) to "c")
            key.id = 3
            val iterator = map.open()
            val _ = iterator.next()

            val failure = assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertEquals(KEY_MADE_EQUAL, failure.message)
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `remove through the iterator of a key whose hashCode changed after next throws ConcurrentModificationException`() = runCells {
        for ((viewName, open) in mapViews) cell(viewName) {
            val key = MutableKey(2)
            val builder = persistentMapOf<Any, String>(MutableKey(1) to "a", key to "b", MutableKey(3) to "c").builder()
            val iterator = builder.open()
            repeat(2) { val _ = iterator.next() }
            key.id = 4

            assertFailsWith<ConcurrentModificationException> { iterator.remove() }
            assertEquals(3, builder.size)
        }
    }
}
