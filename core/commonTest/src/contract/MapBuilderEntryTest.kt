/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A map builder's entry is a window on the builder's slot for its key: value and setValue follow every write through
 * the builder, in place or on a copied path, and a key put back after a remove brings the entry back to its slot. Once
 * the key is gone, the entry writes nothing and keeps the last value it read or got from its own setValue. setValue of
 * the value it holds changes nothing, so build() returns the same map.
 */
class MapBuilderEntryTest {

    @Test
    fun hashMap() = runCells { testEntries(hashMapBuilders) }

    @Test
    fun orderedMap() = runCells { testEntries(orderedMapBuilders) }

    @Test
    fun `entry value follows an overwrite of its key to null and of a null value to a string`() =
        forEachBuilder(mapBuildersOf<Int, String?>(1 to "a", 2 to null)) { message, builder ->
            val iterator = builder.entries.iterator()
            val entryOfString = iterator.next()
            val entryOfNull = iterator.next()
            assertEquals(1, entryOfString.key, message)

            builder[1] = null
            builder[2] = "b"

            assertNull(entryOfString.value, message)
            assertEquals("b", entryOfNull.value, message)
            assertNull(entryOfString.setValue("x"), message)
            assertEquals("b", entryOfNull.setValue(null), message)
            assertEquals("x", builder[1], message)
            assertNull(builder[2], message)
            assertTrue(builder.containsKey(2), message)
        }

    @Test
    fun `setValue of an equal value that is not the stored instance writes it`() {
        val stored = IntWrapper(1, 1)
        val equal = stored.copy()
        forEachBuilder(mapBuildersOf(1 to stored)) { message, builder ->
            val entry = builder.entries.iterator().next()

            assertSame(stored, entry.setValue(equal), message)

            assertSame(equal, builder[1], message)
            assertSame(equal, entry.value, message)
        }
    }

    @Test
    fun `setValue after a remove and a re-put of its key as null writes the re-put slot`() =
        forEachBuilder(mapBuildersOf<Int, String?>(1 to "a", 2 to "b", 3 to "c")) { message, builder ->
            val entry = builder.entries.iterator().next()
            assertEquals("a", builder.remove(1), message)
            assertNull(builder.put(1, null), message)
            val content = builder.toList()

            assertNull(entry.setValue("z"), message)

            assertEquals("z", entry.value, message)
            assertEquals(content.map { (key, value) -> key to (if (key == 1) "z" else value) }, builder.build().toList(), message)
        }

    @Test
    fun `a re-put key moves to the end of the ordered map and its entry follows`() {
        val builder = persistentMapOf(1 to "a", 2 to "b").builder()
        val entry = builder.entries.iterator().next()
        assertEquals("a", builder.remove(1))

        assertNull(builder.put(1, "p"))

        assertEquals("p", entry.setValue("q"))
        assertEquals(listOf(2 to "b", 1 to "q"), builder.build().toList())
    }
}

private typealias MapBuilders = List<Pair<String, () -> PersistentMap.Builder<Any, String>>>

private fun <K, V> mapBuildersOf(vararg entries: Pair<K, V>): List<Pair<String, () -> PersistentMap.Builder<K, V>>> =
    hashMapBuildersOf(entries.toList()).map { (origin, newBuilder) -> "hash map $origin" to newBuilder } +
        orderedMapBuildersOf(entries.toList()).map { (origin, newBuilder) -> "ordered map $origin" to newBuilder }

private class EntryOperation(
    val description: String,
    /** true when the call moves the size at least once, which invalidates the entries iterator. */
    val throwsCME: Boolean = true,
    val keySurvives: Boolean = true,
    val function: PersistentMap.Builder<Any, String>.(key: Any) -> Unit,
)

/** A key no fixture holds, with the hash of [key], so it takes the slot of a removed [key]. */
private fun keyWithTheHashOf(key: Any): Any = IntWrapper(99, key.hashCode())

private val entryOperations: List<EntryOperation> = listOf(
    EntryOperation("no call", throwsCME = false) { },

    EntryOperation("put(new value)", throwsCME = false) { key -> put(key, "x") },
    EntryOperation("put(new value) twice", throwsCME = false) { key -> put(key, "x"); put(key, "y") },
    EntryOperation("put(the stored value)", throwsCME = false) { key -> put(key, getValue(key)) },
    EntryOperation("put(new value for another key)", throwsCME = false) { key -> put(keys.first { it != key }, "o") },
    EntryOperation("putAll(new values)", throwsCME = false) { putAll(keys.associateWith { "n" }) },
    EntryOperation("putAll(a hash map of new values)", throwsCME = false) { putAll(persistentHashMapOf(*keys.map { it to "n" }.toTypedArray())) },
    EntryOperation("build()", throwsCME = false) { val _ = build() },
    EntryOperation("build() then put(new value)", throwsCME = false) { key -> val _ = build(); put(key, "x") },
    EntryOperation("build() then put(new value for another key)", throwsCME = false) { key -> val _ = build(); put(keys.first { it != key }, "o") },

    EntryOperation("put(new key)") { put("new", "n") },
    EntryOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },
    EntryOperation("put(a key with its hash)") { key -> put(keyWithTheHashOf(key), "c") },
    EntryOperation("putAll(new values and a new key)") { putAll(keys.associateWith { "n" } + ("new" to "n")) },
    EntryOperation("putAll(a hash map of new values and a new key)") { putAll(persistentHashMapOf(*(keys.map { it to "n" } + ("new" to "n")).toTypedArray())) },
    EntryOperation("remove(another key)") { key -> remove(keys.first { it != key }) },
    EntryOperation("remove(the last other key)") { key -> remove(keys.last { it != key }) },
    EntryOperation("remove(another key) then build()") { key -> remove(keys.first { it != key }); val _ = build() },
    EntryOperation("remove(the key) then put it back") { key -> remove(key); put(key, "p") },
    EntryOperation("remove(the key) then putAll(a hash map holding it)") { key -> remove(key); putAll(persistentHashMapOf(key to "p")) },
    EntryOperation("remove(every key) then putAll(a hash map holding the key)") { key -> for (other in keys.toList()) remove(other); putAll(persistentHashMapOf(key to "p")) },
    EntryOperation("clear() then put(the key) back") { key -> clear(); put(key, "p") },

    EntryOperation("remove(the key)", keySurvives = false) { key -> remove(key) },
    EntryOperation("put(new value) then remove(the key)", keySurvives = false) { key -> put(key, "x"); remove(key) },
    EntryOperation("remove(the key) then put(a key with its hash)", keySurvives = false) { key -> remove(key); put(keyWithTheHashOf(key), "c") },
    EntryOperation("remove(every key)", keySurvives = false) { for (key in keys.toList()) remove(key) },
    EntryOperation("clear()", keySurvives = false) { clear() },
)

private val whileTheKeyIsGone: List<Pair<String, PersistentMap.Builder<Any, String>.(key: Any) -> Unit>> = listOf(
    "no call" to { },
    "put(a key with its hash)" to { key -> put(keyWithTheHashOf(key), "c") },
)

private fun Cells.testEntries(builders: MapBuilders) {
    for (operation in entryOperations) {
        testEntryReadsItsSlot(builders, operation)
        testSetValueBeforeARead(builders, operation)
        testSetValueOfTheValueItHolds(builders, operation)
    }
    testEntryOfARemovedKey(builders)
    testEntryAfterTheIteratorsRemove(builders)
    testTwoLiveEntries(builders)
    testSetValueOnEveryEntry(builders)
}

private class LiveEntry(
    val builder: PersistentMap.Builder<Any, String>,
    val iterator: MutableIterator<MutableMap.MutableEntry<Any, String>>,
    val entry: MutableMap.MutableEntry<Any, String>,
    val keys: List<Any>,
    val position: Int,
) {
    val key: Any get() = entry.key

    /** Runs to the end over the keys the builder held when the iterator was opened. */
    fun assertIterationGoesOn() {
        assertEquals(keys.drop(position + 1), List(keys.size - position - 1) { iterator.next().key })
        assertFalse(iterator.hasNext())
    }
}

private fun Cells.keysOf(origin: String, newBuilder: () -> PersistentMap.Builder<Any, String>): List<Any>? =
    try {
        newBuilder().keys.toList()
    } catch (e: Throwable) {
        cell("$origin, the keys") { throw e }
        null
    }

private fun Cells.forEachEntry(builders: MapBuilders, what: String, check: LiveEntry.() -> Unit) {
    for ((origin, newBuilder) in builders) {
        val keys = keysOf(origin, newBuilder) ?: continue
        for (position in keys.indices) cell("$origin, entry $position of $keys, $what") {
            val builder = newBuilder()
            val iterator = builder.entries.iterator()
            repeat(position) { val _ = iterator.next() }
            LiveEntry(builder, iterator, iterator.next(), keys, position).check()
        }
    }
}

private fun Cells.testEntryReadsItsSlot(builders: MapBuilders, operation: EntryOperation) = forEachEntry(builders, operation.description) {
    val read = entry.value

    operation.function(builder, key)
    val content = builder.toList()

    val value = if (operation.keySurvives) builder.getValue(key) else read
    assertEquals("$key=$value", entry.toString())
    assertEquals(value, entry.value)
    assertEquals(key.hashCode() xor value.hashCode(), entry.hashCode())
    assertEquals(operation.keySurvives, entry in builder.entries)

    assertEquals(value, entry.setValue("z"))
    assertEquals("z", entry.value)
    val expected = content.map { (k, v) -> k to (if (operation.keySurvives && k == key) "z" else v) }
    assertEquals(expected, builder.toList())
    assertEquals(expected, builder.build().toList())

    if (operation.throwsCME) {
        val _ = assertFailsWith<ConcurrentModificationException> { iterator.next() }
    } else {
        assertIterationGoesOn()
    }
}

/** setValue with no read since [operation], so it has to look the slot up itself. */
private fun Cells.testSetValueBeforeARead(builders: MapBuilders, operation: EntryOperation) =
    forEachEntry(builders, "${operation.description}, setValue before a read") {
        val read = entry.value

        operation.function(builder, key)
        val value = if (operation.keySurvives) builder.getValue(key) else read
        val expected = builder.toList().map { (k, v) -> k to (if (operation.keySurvives && k == key) "z" else v) }

        assertEquals(value, entry.setValue("z"))
        assertEquals("z", entry.value)
        assertEquals(expected, builder.toList())
        assertEquals(expected, builder.build().toList())
    }

private fun Cells.testSetValueOfTheValueItHolds(builders: MapBuilders, operation: EntryOperation) =
    forEachEntry(builders, "${operation.description} then build(), setValue(the value it holds), #366") {
        val read = entry.value

        operation.function(builder, key)
        val built = builder.build()
        val value = if (operation.keySurvives) builder.getValue(key) else read
        assertEquals(key.hashCode() xor value.hashCode(), entry.hashCode())

        assertSame(value, entry.setValue(value))

        assertSame(value, entry.value)
        assertSame(built, builder.build())
        if (operation.throwsCME) {
            val _ = assertFailsWith<ConcurrentModificationException> { iterator.next() }
        } else {
            assertIterationGoesOn()
        }
    }

private fun Cells.testEntryOfARemovedKey(builders: MapBuilders) {
    for ((meanwhile, call) in whileTheKeyIsGone) forEachEntry(builders, "put(new value), read, remove(the key), $meanwhile") {
        builder[key] = "x"
        assertEquals(entry, mapOf(key to "x").entries.single())
        assertEquals("x", entry.value)
        assertEquals("x", builder.remove(key))
        call(builder, key)
        val built = builder.build()

        assertEquals("x", entry.value)
        assertEquals("x", entry.setValue("z"))
        assertEquals("z", entry.setValue("y"))
        assertEquals("y", entry.value)
        assertFalse(entry in builder.entries)
        assertSame(built, builder.build())

        assertNull(builder.put(key, "p"))
        assertEquals("p", entry.value)
        assertEquals("p", entry.setValue("q"))
        assertEquals(built.toMap() + (key to "q"), builder.toMap())
    }
}

private fun Cells.testEntryAfterTheIteratorsRemove(builders: MapBuilders) = forEachEntry(builders, "the iterator's remove") {
    val read = entry.value

    iterator.remove()

    assertEquals(read, entry.value)
    assertEquals(read, entry.setValue("z"))
    assertTrue(key !in builder)
    assertEquals(keys.size - 1, builder.size)
    assertIterationGoesOn()
    assertTrue(key !in builder.build())
}

/** Two entries of one iterator: the iterator's remove of the later key detaches that entry alone. */
private fun Cells.testTwoLiveEntries(builders: MapBuilders) {
    for ((origin, newBuilder) in builders) {
        val keys = keysOf(origin, newBuilder) ?: continue
        for (position in keys.indices) for (later in position + 1..keys.lastIndex) cell("$origin, entries $position and $later of $keys") {
            val builder = newBuilder()
            val iterator = builder.entries.iterator()
            repeat(position) { val _ = iterator.next() }
            val entry = iterator.next()
            repeat(later - position - 1) { val _ = iterator.next() }
            val laterEntry = iterator.next()
            assertEquals(keys[later], laterEntry.key)

            assertEquals(builder.getValue(keys[later]), laterEntry.setValue("y"))
            assertEquals(builder.getValue(keys[position]), entry.setValue("x"))
            assertEquals("x", entry.setValue("xx"))
            assertEquals("xx", builder[keys[position]])
            assertEquals("y", builder[keys[later]])
            assertEquals(keys.size, builder.size)

            iterator.remove()

            assertEquals("xx", entry.setValue("w"))
            assertEquals("w", builder[keys[position]])
            assertEquals("y", laterEntry.setValue("v"))
            assertTrue(keys[later] !in builder)
            assertEquals(keys.size - 1, builder.size)
            LiveEntry(builder, iterator, laterEntry, keys, later).assertIterationGoesOn()
        }
    }
}

private fun Cells.testSetValueOnEveryEntry(builders: MapBuilders) {
    for ((origin, newBuilder) in builders) cell("$origin, setValue on every entry with a build after the first") {
        val builder = newBuilder()
        val keys = builder.keys.toList()
        val values = keys.map { builder.getValue(it) }
        val iterator = builder.entries.iterator()

        val first = iterator.next()
        assertEquals(values[0], first.setValue(values[0] + "!"))
        val snapshot = builder.build()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            assertEquals(entry.value, entry.setValue(entry.value + "!"))
        }

        assertEquals(keys.zip(values.map { "$it!" }), builder.build().toList())
        assertEquals(keys.zip(listOf("${values[0]}!") + values.drop(1)), snapshot.toList())
    }
}
