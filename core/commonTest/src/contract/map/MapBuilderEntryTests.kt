/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.PersistentMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A call made through the builder behind a live entry, as a table row of stdlib's ConcurrentModificationTest. */
class EntryOperation(
    val description: String,
    /** true when the call moves the size at least once, which invalidates the entries iterator. */
    val throwsCME: Boolean = true,
    /** false when the call leaves the builder without the entry's key. */
    val keySurvives: Boolean = true,
    val function: PersistentMap.Builder<Any, String>.(key: Any) -> Unit,
)

/**
 * The live entry contract of the map builders: an entry the entries iterator returned is a window on the builder's
 * slot for its key, and once the key is gone it keeps the last value it read. A subclass passes the ways of
 * building a map of a few distinct keys and values, as stdlib's IterableTests passes a factory.
 */
abstract class MapBuilderEntryTests(private val builders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>>) {

    @Test
    fun entryReadsItsSlot() {
        val operations = listOf(
            EntryOperation("no call", throwsCME = false) { },
            EntryOperation("put(new value)", throwsCME = false) { key -> put(key, "x") },
            EntryOperation("put(new value) twice", throwsCME = false) { key -> put(key, "x"); put(key, "y") },
            EntryOperation("put(the stored value)", throwsCME = false) { key -> put(key, getValue(key)) },
            EntryOperation("put(new value for another key)", throwsCME = false) { key -> put(keys.first { it != key }, "o") },
            EntryOperation("putAll(new values)", throwsCME = false) { putAll(keys.associateWith { "n" }) },
            EntryOperation("build()", throwsCME = false) { val _ = build() },
            EntryOperation("put(new key)") { put("new", "n") },
            EntryOperation("remove(another key)") { key -> remove(keys.first { it != key }) },
            EntryOperation("remove(the key)", keySurvives = false) { key -> remove(key) },
            EntryOperation("put(new value) then remove(the key)", keySurvives = false) { key -> put(key, "x"); remove(key) },
            EntryOperation("remove(the key) then put it back") { key -> remove(key); put(key, "p") },
            EntryOperation("clear()", keySurvives = false) { clear() },
        )
        for (operation in operations) for ((origin, newBuilder) in builders) {
            val keys = newBuilder().keys.toList()
            for (position in keys.indices) {
                val builder = newBuilder()
                val iterator = builder.entries.iterator()
                repeat(position) { val _ = iterator.next() }
                val entry = iterator.next()
                val key = entry.key
                val read = entry.value
                val message = "$origin, ${operation.description}, entry $position of $keys"

                operation.function(builder, key)

                val value = if (operation.keySurvives) builder.getValue(key) else read
                assertEquals(value, entry.value, message)
                assertEquals("$key=$value", entry.toString(), message)
                assertEquals(key.hashCode() xor value.hashCode(), entry.hashCode(), message)
                assertEquals(operation.keySurvives, entry in builder.entries, message)

                assertEquals(value, entry.setValue("z"), message)
                assertEquals("z", entry.value, message)
                assertEquals(if (operation.keySurvives) "z" else null, builder[key], message)
                assertEquals(if (operation.keySurvives) "z" else null, builder.build()[key], message)

                if (operation.throwsCME) {
                    val _ = assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
                } else {
                    // a write through the entry leaves the iteration where it was
                    assertEquals(keys.drop(position + 1), List(keys.size - position - 1) { iterator.next().key }, message)
                }
            }
        }
    }

    @Test
    fun entryOfARemovedKeyKeepsTheValueItRead() {
        for ((origin, newBuilder) in builders) {
            val keys = newBuilder().keys.toList()
            for (position in keys.indices) {
                val builder = newBuilder()
                val iterator = builder.entries.iterator()
                repeat(position) { val _ = iterator.next() }
                val entry = iterator.next()
                val key = entry.key
                val message = "$origin, entry $position of $keys"

                builder[key] = "x"
                assertEquals("x", entry.value, message)
                assertEquals("x", builder.remove(key), message)

                // the key is gone, so the entry answers from what it read and writes nothing
                val snapshot = builder.build()
                assertEquals("x", entry.value, message)
                assertEquals("x", entry.setValue("z"), message)
                assertEquals("z", entry.setValue("y"), message)
                assertSame(snapshot, builder.build(), message)
                assertEquals(keys.size - 1, builder.size, message)

                // the key put back brings the entry back to its slot
                assertEquals(null, builder.put(key, "p"), message)
                assertEquals("p", entry.value, message)
                assertEquals("p", entry.setValue("q"), message)
                assertEquals("q", builder[key], message)
            }
        }
    }

    @Test
    fun entryAfterTheIteratorsRemove() {
        for ((origin, newBuilder) in builders) {
            val keys = newBuilder().keys.toList()
            for (position in keys.indices) {
                val builder = newBuilder()
                val iterator = builder.entries.iterator()
                repeat(position) { val _ = iterator.next() }
                val entry = iterator.next()
                val read = entry.value
                val message = "$origin, entry $position of $keys"

                iterator.remove()

                assertEquals(read, entry.setValue("z"), message)
                assertTrue(entry.key !in builder, message)
                assertEquals(keys.size - 1, builder.size, message)
                assertEquals(keys.drop(position + 1), List(keys.size - position - 1) { iterator.next().key }, message)
                assertTrue(entry.key !in builder.build(), message)
            }
        }
    }

    /** The entry of a visited key is still a window on its slot once the iterator has moved on to a later entry. */
    @Test
    fun twoLiveEntries() {
        for ((origin, newBuilder) in builders) {
            val keys = newBuilder().keys.toList()
            for (position in keys.indices) for (later in position + 1..keys.lastIndex) {
                val builder = newBuilder()
                val iterator = builder.entries.iterator()
                repeat(position) { val _ = iterator.next() }
                val entry = iterator.next()
                val message = "$origin, entries $position and $later of $keys"

                repeat(later - position - 1) { val _ = iterator.next() }
                val laterEntry = iterator.next()

                assertEquals(keys[later], laterEntry.key, message)
                assertEquals(builder.getValue(keys[later]), laterEntry.setValue("y"), message)
                assertEquals(builder.getValue(keys[position]), entry.setValue("x"), message)

                assertEquals("x", entry.setValue("xx"), message)
                assertEquals("xx", builder[keys[position]], message)
                assertEquals("y", builder[keys[later]], message)
                assertEquals(keys.size, builder.size, message)

                // the iterator's remove of the later entry leaves the earlier one on its slot
                iterator.remove()

                assertEquals("xx", entry.setValue("w"), message)
                assertEquals("w", builder[keys[position]], message)
                assertTrue(keys[later] !in builder, message)
                assertEquals(keys.size - 1, builder.size, message)
                assertEquals(keys.drop(later + 1), List(keys.size - later - 1) { iterator.next().key }, message)
            }
        }
    }

    @Test
    fun setValueOnEveryEntryWithABuildInBetween() {
        for ((origin, newBuilder) in builders) {
            val builder = newBuilder()
            val keys = builder.keys.toList()
            val values = keys.map { builder.getValue(it) }
            val iterator = builder.entries.iterator()

            val first = iterator.next()
            assertEquals(values[0], first.setValue(values[0] + "!"), origin)
            val snapshot = builder.build()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                assertEquals(entry.value, entry.setValue(entry.value + "!"), origin)
            }

            assertEquals(keys, builder.build().keys.toList(), origin)
            assertEquals(values.map { "$it!" }, keys.map { builder.build().getValue(it) }, origin)
            assertEquals(listOf("${values[0]}!") + values.drop(1), keys.map { snapshot.getValue(it) }, origin)
        }
    }
}
