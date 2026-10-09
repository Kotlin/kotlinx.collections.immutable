/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import tests.IntWrapper
import tests.contract.BuilderOperation
import tests.contract.iteratorOperations
import tests.contract.testIterationContinues
import tests.contract.testIterator
import tests.trie.collidingKey1
import tests.trie.levelOneSibling
import tests.trie.otherLevelOneSibling
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Builders of three entries built from a map, which shares its trie, and from puts, which own theirs. */
private val orderedMapBuilders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>> = listOf(
    "from a map" to { persistentMapOf<Any, String>(1 to "a", 2 to "b", 3 to "c").builder() },
    "from puts" to { persistentMapOf<Any, String>().builder().apply { this[1] = "a"; this[2] = "b"; this[3] = "c" } },
)

class PersistentOrderedMapBuilderTest {

    @Test
    fun iterators() {
        val views = listOf<Pair<String, PersistentMap.Builder<Any, String>.() -> MutableIterator<*>>>(
            "entries" to { entries.iterator() },
            "keys" to { keys.iterator() },
            "values" to { values.iterator() },
        )
        val operations = listOf<BuilderOperation<PersistentMap.Builder<Any, String>>>(
            BuilderOperation("no call", throwsCME = false) { },

            BuilderOperation("put(new key)") { put("new", "n") },
            BuilderOperation("remove(the first key)") { remove(keys.first()) },
            BuilderOperation("remove(the last key)") { remove(keys.last()) },
            BuilderOperation("clear()") { clear() },
            BuilderOperation("putAll(a map with a new key)") { putAll(persistentMapOf("new" to "n")) },
            BuilderOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },

            BuilderOperation("put(a new value for the first key)", throwsCME = false) { put(keys.first(), "changed") },
            BuilderOperation("put(a new value for the last key)", throwsCME = false) { put(keys.last(), "changed") },
            BuilderOperation("put(the stored value of the first key)", throwsCME = false) { keys.first().let { put(it, getValue(it)) } },
            BuilderOperation("put(the stored value of the last key)", throwsCME = false) { keys.last().let { put(it, getValue(it)) } },
            BuilderOperation("putAll(new values)", throwsCME = false) { putAll(mapValues { it.value + "!" }) },
            BuilderOperation("putAll(the stored values)", throwsCME = false) { putAll(toMap()) },
            BuilderOperation("putAll(the built map)", throwsCME = false) { putAll(build()) },
            BuilderOperation("putAll(the builder itself)", throwsCME = false) { putAll(this) },
            BuilderOperation("putAll(an empty map)", throwsCME = false) { putAll(emptyMap()) },
            BuilderOperation("remove(a missing key)", throwsCME = false) { remove("missing") },
        )

        for (operation in operations) {
            for (iteratorOp in iteratorOperations) testIterator(orderedMapBuilders, views, operation, iteratorOp)
            if (!operation.throwsCME) testIterationContinues(orderedMapBuilders, views, operation)
        }
    }

    @Test
    fun `no-op remove keeps the builder cache valid`() {
        // The absent key's path enters the level-1 node the stored keys share, so the no-op remove is recursive.
        val builder = persistentMapOf(collidingKey1 to 1, levelOneSibling to 2).builder()

        assertNull(builder.remove(otherLevelOneSibling))

        assertEquals(persistentMapOf(collidingKey1 to 1, levelOneSibling to 2), builder.build())
        assertEquals(listOf(collidingKey1, levelOneSibling), builder.build().keys.toList())
    }

    @Test
    fun `put on the iterator of an empty builder keeps it false and next throws`() {
        val builder = persistentMapOf<Int, String>().builder()
        val iterator = builder.entries.iterator()

        builder[1] = "a"

        assertFalse(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `removes of every remaining key keep hasNext true and next throws`() {
        for ((origin, newBuilder) in orderedMapBuilders) {
            val builder = newBuilder()
            val keys = builder.keys.toList()
            val iterator = builder.entries.iterator()
            assertEquals(keys.take(2), List(2) { iterator.next().key }, origin)

            for (key in keys) assertNotNull(builder.remove(key), origin)

            assertTrue(iterator.hasNext(), origin)
            assertFailsWith<ConcurrentModificationException>(origin) { iterator.next() }
        }
    }

    @Test
    fun `setValue after a remove and a re-put of its key as null writes at the new position`() {
        val builder = persistentMapOf<Int, String?>(1 to "a", 2 to "b", 3 to "c").builder()
        val entry = builder.entries.iterator().next()
        assertEquals("a", builder.remove(1))
        assertNull(builder.put(1, null))

        assertNull(entry.setValue("z"))

        val built = builder.build()
        assertEquals(listOf(2, 3, 1), built.keys.toList())
        assertEquals(listOf("b", "c", "z"), built.values.toList())
    }

    @Test
    fun `setValue of an equal value that is not the stored instance writes it`() {
        val stored = IntWrapper(1, 1)
        val equal = stored.copy()
        val builder = persistentMapOf(1 to stored).builder()
        val entry = builder.entries.iterator().next()

        assertSame(stored, entry.setValue(equal))

        assertSame(equal, builder[1])
        assertSame(equal, entry.value)
    }

    @Test
    fun `a re-put key moves to the end and its entry follows`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val entry = builder.entries.iterator().next()
        assertEquals("a", builder.remove(1))

        assertNull(builder.put(1, "p"))

        assertEquals("p", entry.setValue("q"))
        assertEquals(listOf(2 to "b", 1 to "q"), builder.build().entries.map { it.key to it.value })
    }
}

/** The live entry contract on the ordered map builders, as stdlib declares an IterableTests subclass. */
class PersistentOrderedMapBuilderEntryTest : MapBuilderEntryTests(orderedMapBuilders)
