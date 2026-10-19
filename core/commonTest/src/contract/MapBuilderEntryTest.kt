/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import tests.trie.missingKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A map builder's entry is a window on the builder's slot for its key: whatever is read from it or set on it first
 * after a write through the builder, in place or on a copied path, sees the slot, and a key put back after a remove
 * brings the entry back to its slot. Once the key is gone, the entry writes nothing and keeps the last value it read or
 * got from its own setValue. setValue of the value it holds changes nothing, so build() returns the same map. The
 * iterator that returned it goes on after its setValue calls, unless the write through the builder moved the size.
 *
 * A new call through the builder is a row of [mapOperations], or of [slotMoves] when it moves the key's slot; a new
 * first call on an entry is a row of [firstReads]; a scenario with assertions of its own is a [forEachBuilder] test
 * over [mapBuildersOf].
 */
class MapBuilderEntryTest {

    @Test
    fun hashMap() = runCells { testEntries(hashMapBuilders, mapOperations + slotMoves) }

    @Test
    fun orderedMap() = runCells { testEntries(orderedMapBuilders, mapOperations + slotMoves) }

    @Test
    fun `entry value follows an overwrite of its key to null and a re-put of its key as null`() =
        forEachBuilder(mapBuildersOf<Int, String?>(1 to "a", 2 to null, 3 to "c")) { builder ->
            val (entryOfString, entryOfNull, entryOfRePut) = builder.entries.toList()

            builder[1] = null
            builder[2] = "b"
            assertEquals("c", builder.remove(3))
            assertNull(builder.put(3, null))

            assertNull(entryOfString.value)
            assertEquals("b", entryOfNull.value)
            assertNull(entryOfRePut.setValue("z"))
            assertNull(entryOfString.setValue("x"))
            assertEquals("b", entryOfNull.setValue(null))
            assertEquals(mapOf(1 to "x", 2 to null, 3 to "z"), builder.toMap())
        }

    @Test
    fun `setValue of a detached entry writes nothing and a re-put of its key brings the entry back`() =
        forEachBuilder(mapBuildersOf(1 to "a", 2 to "b")) { builder ->
            val entry = builder.entries.iterator().next()
            assertEquals("a", builder.remove(1))
            val built = builder.build()

            assertEquals("a", entry.setValue("z"))
            assertEquals("z", entry.setValue("y"))
            assertEquals("y", entry.value)
            assertFalse(entry in builder.entries)
            assertSame(built, builder.build())

            assertNull(builder.put(1, "p"))
            assertEquals("p", entry.value)
            assertEquals("p", entry.setValue("q"))
            assertEquals(mapOf(1 to "q", 2 to "b"), builder.toMap())
        }

    @Test
    fun `setValue of an equal value that is not the stored instance writes it`() {
        val stored = IntWrapper(1, 1)
        val equal = stored.copy()
        forEachBuilder(mapBuildersOf(1 to stored)) { builder ->
            val entry = builder.entries.iterator().next()

            assertSame(stored, entry.setValue(equal))

            assertSame(equal, builder[1])
            assertSame(equal, entry.value)
        }
    }

    @Test
    fun `the keys view removes a key with a null value and says so`() =
        forEachBuilder(mapBuildersOf<Int, String?>(1 to null, 2 to "b")) { builder ->
            assertTrue(builder.keys.remove(1))
            assertFalse(builder.keys.remove(1))
            assertEquals(mapOf(2 to "b"), builder.toMap())
        }
}

private typealias MapBuilder = PersistentMap.Builder<Any, String>
private typealias Entry = MutableMap.MutableEntry<Any, String>

/** The calls beyond [mapOperations] that move a key's slot, take it away or bring it back. */
private val slotMoves: List<BuilderOperation<MapBuilder>> = listOf(
    BuilderOperation("put(a key with the hash of the first key)") { put(missingKey(keys.first().hashCode()), "c") },
    BuilderOperation("remove(the first key) then put(a key with its hash)") { val key = keys.first(); remove(key); put(missingKey(key.hashCode()), "c") },
    BuilderOperation("remove(the first key) then put it back") { val key = keys.first(); remove(key); put(key, "p") },
    BuilderOperation("remove(the last key) then put it back") { val key = keys.last(); remove(key); put(key, "p") },
    BuilderOperation("clear() then put(the first key) back") { val key = keys.first(); clear(); put(key, "p") },
    BuilderOperation("clear() then putAll(a hash map holding the first key)") { val key = keys.first(); clear(); putAll(persistentHashMapOf(key to "p")) },
)

/** The first call on an entry after a call through the builder, with nothing read from the entry in between. */
private val firstReads: List<Pair<String, Entry.(expected: String) -> Unit>> = listOf(
    "value" to { expected -> assertEquals(expected, value) },
    "toString" to { expected -> assertEquals("$key=$expected", toString()) },
    "hashCode" to { expected -> assertEquals(key.hashCode() xor expected.hashCode(), hashCode()) },
    "equals" to { expected -> assertTrue(this == mapOf(key to expected).entries.single()) },
    "setValue of the value it holds" to { expected -> assertSame(expected, setValue(expected)) },
)

private fun Cells.testEntries(builders: Origins<MapBuilder>, operations: List<BuilderOperation<MapBuilder>>) {
    for (operation in operations) for ((read, check) in firstReads) for ((origin, newBuilder) in builders) {
        cell("$origin, $read first after ${operation.description}") {
            val keys = newBuilder().keys.toList()
            for (position in keys.indices) at("entry $position") {
                val builder = newBuilder()
                val iterator = builder.entries.iterator()
                repeat(position) { val _ = iterator.next() }
                val entry = iterator.next()
                val before = entry.value
                operation.function(builder)
                val kept = entry.key in builder
                val value = if (kept) builder.getValue(entry.key) else before
                val built = builder.build()

                at("the first read") { entry.check(value) }
                at("setValue of the value it holds") {
                    assertSame(value, entry.setValue(value))
                    assertSame(built, builder.build())
                }
                at("setValue of a new value") {
                    assertEquals(value, entry.setValue("z"))
                    assertEquals("z", entry.value)
                    val expected = built.toMutableMap().apply { if (kept) this[entry.key] = "z" }
                    assertEquals(expected.toList(), builder.toList())
                }
                at("the iterator after the setValue calls") {
                    if (operation.throwsCME) assertFailsWith<ConcurrentModificationException> { iterator.next() }
                    else assertEquals(keys.drop(position + 1), iterator.asSequence().map { it.key }.toList())
                }
            }
        }
    }
}
