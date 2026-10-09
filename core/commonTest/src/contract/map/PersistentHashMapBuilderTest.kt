/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import tests.contract.BuilderOperation
import tests.contract.iteratorOperations
import tests.contract.testIterationContinues
import tests.contract.testIterator
import tests.trie.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Builders of every trie shape holding "v0", "v1", ...: from a map, which shares its nodes with the builder, so the
 * first write copies them, which is where an iterator loses its path; from puts, which own their nodes and write in
 * place; and from a map with a put, which owns the path to the first key and shares the rest.
 */
private val hashMapBuilders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>> = trieShapes.flatMap { (shape, keys) ->
    val entries = keys.mapIndexed { index, key -> key to "v$index" }
    listOf(
        "from a map of $shape" to { persistentHashMapOf(*entries.toTypedArray()).builder() },
        "from puts of $shape" to { persistentHashMapOf<Any, String>().builder().apply { for ((key, value) in entries) set(key, value) } },
        "from a map of $shape with a put" to { persistentHashMapOf(*entries.toTypedArray()).builder().apply { set(keys.first(), "put") } },
    )
}

/** A hash map of [keys] in the given order, with a new value for each. */
private fun newValuesFor(keys: List<Any>): PersistentMap<Any, String> =
    persistentHashMapOf(*keys.mapIndexed { index, key -> key to "new $index" }.toTypedArray())

/** Runs [test] on a builder of [entries] from a map, which shares its nodes, and from puts, which own theirs. */
private fun <K, V> forEachBuilder(entries: List<Pair<K, V>>, test: (message: String, builder: PersistentHashMapBuilder<K, V>) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    test("from a map", persistentHashMapOf(*entries.toTypedArray()).builder() as PersistentHashMapBuilder<K, V>)
    @Suppress("UNCHECKED_CAST")
    test("from puts", persistentHashMapOf<K, V>().builder().apply { for ((key, value) in entries) set(key, value) } as PersistentHashMapBuilder<K, V>)
}

/** A receiver and an argument of putAll, as the pairs their maps are built from. */
private class PutAllRow(val description: String, val receiver: List<Pair<IntWrapper, String>>, val argument: List<Pair<IntWrapper, String>>)

/** After a putAll: every key keeps the instance the receiver stored, or the argument's when new, and holds the argument's value when it has one. */
private fun assertMerged(receiver: Map<IntWrapper, String>, argument: Map<IntWrapper, String>, builder: Map<IntWrapper, String>, message: String) {
    val keys = receiver.keys + argument.keys
    assertEquals(keys.size, builder.size, message)
    for (key in keys) {
        assertSame(key, builder.storedKey(key), "$message, key ${key.obj}")
        assertSame(if (key in argument) argument.getValue(key) else receiver.getValue(key), builder[key], "$message, key ${key.obj}")
    }
}

/** The remaining entries of the iterator, as "key=value". */
private fun Iterator<*>.remainingStrings(): List<String> = buildList { while (hasNext()) add(next().toString()) }

class PersistentHashMapBuilderTest {

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
            BuilderOperation("putAll(a map with a new key)") { putAll(mapOf("new" to "n")) },
            BuilderOperation("putAll(a hash map with a new key)") { putAll(persistentHashMapOf("new" to "n")) },
            BuilderOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },

            BuilderOperation("put(a new value for the first key)", throwsCME = false) { put(keys.first(), "changed") },
            BuilderOperation("put(a new value for the last key)", throwsCME = false) { put(keys.last(), "changed") },
            BuilderOperation("put(the stored value of the first key)", throwsCME = false) { put(keys.first(), getValue(keys.first())) },
            BuilderOperation("put(the stored value of the last key)", throwsCME = false) { put(keys.last(), getValue(keys.last())) },
            BuilderOperation("putAll(a new value for the first key)", throwsCME = false) { putAll(persistentHashMapOf(keys.first() to "changed")) },
            BuilderOperation("putAll(a new value for the last key)", throwsCME = false) { putAll(persistentHashMapOf(keys.last() to "changed")) },
            BuilderOperation("putAll(new values)", throwsCME = false) { putAll(newValuesFor(keys.toList())) },
            // A collision node of the argument then lists its keys in another order than the receiver's, which keeps its own.
            BuilderOperation("putAll(new values in the reverse key order)", throwsCME = false) { putAll(newValuesFor(keys.reversed())) },
            BuilderOperation("putAll(the stored values)", throwsCME = false) { putAll(persistentHashMapOf(*entries.map { it.key to it.value }.toTypedArray())) },
            BuilderOperation("putAll(the built map)", throwsCME = false) { putAll(build()) },
            BuilderOperation("putAll(the builder itself)", throwsCME = false) { putAll(this) },
            BuilderOperation("putAll(an empty map)", throwsCME = false) { putAll(persistentHashMapOf<Any, String>()) },
            BuilderOperation("remove(a missing key)", throwsCME = false) { remove("missing") },
        )

        for (operation in operations) {
            for (iteratorOp in iteratorOperations) testIterator(hashMapBuilders, views, operation, iteratorOp)
            if (!operation.throwsCME) testIterationContinues(hashMapBuilders, views, operation)
        }
    }

    @Test
    fun `put on the iterator of an empty builder keeps hasNext false and next throws`() {
        val builder = persistentHashMapOf<Int, String>().builder()
        val iterator = builder.entries.iterator()

        builder[1] = "a"

        assertFalse(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `removes of every remaining key keep hasNext true and next throws`() {
        for ((origin, newBuilder) in hashMapBuilders) {
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
    fun `next after a promoting remove returns the value written after the remove`() {
        val keys = trieShapes.getValue("a level-1 node next to root entries")
        forEachBuilder(keys.map { it to "v$it" }) { message, builder ->
            val iterator = builder.entries.iterator()
            repeat(3) { val _ = iterator.next() }
            assertEquals(0, iterator.next().key, message)
            iterator.remove()

            builder[32] = "Z"

            assertEquals("Z", iterator.next().value, message)
            assertFalse(iterator.hasNext(), message)
        }
    }

    @Test
    fun `next after a promoting remove and a build returns the value written after the build`() {
        val keys = trieShapes.getValue("a level-1 node next to root entries")
        forEachBuilder(keys.map { it to "v$it" }) { message, builder ->
            val iterator = builder.entries.iterator()
            repeat(3) { val _ = iterator.next() }
            assertEquals(0, iterator.next().key, message)
            iterator.remove()
            val built = builder.build()

            builder[32] = "Z"

            assertEquals("Z", iterator.next().value, message)
            assertFalse(iterator.hasNext(), message)
            assertEquals("v32", built[32], message)
        }
    }

    @Test
    fun `overwrite in an unowned node after a promoting remove keeps the iterator valid`() {
        // root entries 10 and 11, nodes {7, 39}, {5, 37} and {0, 32} at the root cells 7, 5 and 0
        val builder = persistentHashMapOf(10 to "j", 11 to "k", 0 to "v0", 32 to "v32", 5 to "v5", 37 to "v37", 7 to "v7", 39 to "v39").builder()
        val iterator = builder.entries.iterator()
        assertEquals(listOf(10, 11, 7, 39, 5), List(5) { iterator.next().key })
        iterator.remove()

        builder[0] = "Y"

        assertEquals(listOf("37=v37", "0=Y", "32=v32"), iterator.remainingStrings())
        assertEquals(7, builder.size)
    }

    @Test
    fun `overwrites after a promoting remove and a build keep the iterator valid`() {
        // 195 and 1219 share the root cell 3 and the level-1 cell 6, 67 and 1091 the root cell 3 and the level-1 cell 2, 1 and 33 the root cell 1
        val builder = persistentHashMapOf(195 to "x", 1219 to "y", 67 to "p", 1091 to "q", 1 to "a", 33 to "b").builder()
        val iterator = builder.entries.iterator()
        assertEquals(195, iterator.next().key)
        iterator.remove()

        val _ = builder.build()
        builder[67] = "P"
        builder[1] = "A"

        assertEquals(listOf("1219=y", "67=P", "1091=q", "1=A", "33=b"), iterator.remainingStrings())
        assertEquals(5, builder.size)
    }

    @Test
    fun `overwrite in a later node while the iterator is inside another node is seen`() {
        // root entries 1, 2 and 3, nodes {5, 37} and {0, 32} at the root cells 5 and 0
        val builder = persistentHashMapOf(1 to "a", 2 to "b", 3 to "c", 0 to "y", 32 to "z", 5 to "e", 37 to "f").builder()
        val iterator = builder.entries.iterator()
        repeat(3) { val _ = iterator.next() }
        assertEquals(5, iterator.next().key)

        builder[0] = "Y"

        assertEquals(listOf("37=f", "0=Y", "32=z"), iterator.remainingStrings())
    }

    @Test
    fun `putAll of new values in another collision order keeps the receiver's order`() {
        val builder = persistentHashMapOf(collidingKey1 to "a", collidingKey2 to "b", collidingKey1.colliding(11) to "c", collidingKey1.colliding(12) to "d").builder()
        val orderedKeys = builder.keys.toList()
        // The argument holds the same key instances with the first two swapped, so its last key keeps its index and its first does not.
        val argument = persistentHashMapOf(orderedKeys[2] to "z", orderedKeys[3] to "w", orderedKeys[0] to "x", orderedKeys[1] to "y")
        assertEquals(listOf(orderedKeys[1], orderedKeys[0], orderedKeys[2], orderedKeys[3]), argument.keys.toList())
        val iterator = builder.entries.iterator()
        assertEquals(orderedKeys[0], iterator.next().key)

        builder.putAll(argument)

        assertEquals(listOf(orderedKeys[1] to "y", orderedKeys[2] to "z", orderedKeys[3] to "w"), List(3) { iterator.next().let { it.key to it.value } })
        assertFalse(iterator.hasNext())
        assertEquals(orderedKeys, builder.keys.toList())
    }

    @Test
    fun `putAll holding the stored key in a subtree invalidates the iterator`() = forEachBuilder(listOf(collidingKey1 to "a")) { message, builder ->
        val iterator = builder.entries.iterator()

        builder.putAll(persistentHashMapOf(collidingKey1.copy() to "x", levelOneSibling to "y"))

        assertEquals("x", builder[collidingKey1], message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `entry value follows an overwrite of its key to null and of a null value to a string`() {
        val builder = persistentHashMapOf<Int, String?>(1 to "a", 2 to null).builder()
        val iterator = builder.entries.iterator()
        val entryOfString = iterator.next()
        val entryOfNull = iterator.next()
        assertEquals(1, entryOfString.key)

        builder[1] = null
        builder[2] = "b"

        assertNull(entryOfString.value)
        assertEquals("b", entryOfNull.value)
        assertNull(entryOfString.setValue("x"))
        assertEquals("b", entryOfNull.setValue(null))
        assertEquals("x", builder[1])
        assertNull(builder[2])
        assertTrue(builder.containsKey(2))
    }

    @Test
    fun `an entry whose key was removed stays detached when another key takes its slot`() {
        val builder = persistentHashMapOf(1 to "a", 2 to "b").builder()
        val entry = builder.entries.iterator().next()
        assertEquals(1, entry.key)
        builder[1] = "x"
        assertEquals("x", entry.value)

        assertEquals("x", builder.remove(1))
        builder[33] = "c" // 33 shares root cell 1 with 1

        assertEquals("x", entry.value)
        assertEquals("x", entry.setValue("z"))
        assertFalse(builder.containsKey(1))
        assertEquals("c", builder[33])
        assertEquals(2, builder.size)
    }

    @Test
    fun `setValue of an equal value that is not the stored instance writes it`() {
        val stored = IntWrapper(1, 1)
        val equal = stored.copy()
        val builder = persistentHashMapOf(1 to stored).builder()
        val entry = builder.entries.iterator().next()

        assertSame(stored, entry.setValue(equal))

        assertSame(equal, builder[1])
        assertSame(equal, entry.value)
    }

    @Test
    fun `putAll does not duplicate a key stored in a bottom-level collision node`() {
        val builder = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2).builder()
        builder.putAll(persistentHashMapOf(collidingKey1 to 10, lastLevelSibling to 3))
        assertEquals(3, builder.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 2, lastLevelSibling to 3), builder.build())

        val reversedBuilder = persistentHashMapOf(collidingKey1 to 10, lastLevelSibling to 3).builder()
        reversedBuilder.putAll(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2))
        assertEquals(3, reversedBuilder.size)
        assertEquals(persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2, lastLevelSibling to 3), reversedBuilder.build())
    }

    @Test
    fun `putAll takes the values of the argument builder without sharing storage`() {
        val argument = persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 20).builder()
        val builder = persistentHashMapOf(collidingKey1 to 1, collidingKey2 to 2).builder()
        builder.putAll(argument)
        assertEquals(2, builder.size)

        builder[collidingKey1] = 100
        argument[collidingKey2] = 200

        assertEquals(persistentHashMapOf(collidingKey1 to 100, collidingKey2 to 20), builder.build())
        assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 200), argument.build())
    }

    @Test
    fun `putAll keeps the stored key instance and takes the argument's value`() {
        val same = "a"
        val rows = listOf(
            PutAllRow("an equal key", listOf(collidingKey1 to "a", rootSibling to "b"), listOf(collidingKey1.copy() to "x")),
            PutAllRow("collision values", listOf(collidingKey1 to "a", collidingKey2 to "b"), listOf(collidingKey1.copy() to "x", collidingKey2.copy() to "y")),
            PutAllRow("the key one level down in the argument", listOf(levelOneSibling to "a"), listOf(levelOneSibling.copy() to "x", collidingKey2 to "y")),
            PutAllRow("the key in the argument's collision node", listOf(collidingKey1 to "a"), listOf(collidingKey1.copy() to "x", collidingKey2 to "y")),
            PutAllRow("the same value", listOf(collidingKey1 to same), listOf(collidingKey1.copy() to same, collidingKey2 to "y")),
            PutAllRow("the collision node reached at the last level", listOf(collidingKey1 to "a", lastLevelSibling to "c"), listOf(collidingKey1.copy() to "x", collidingKey2 to "y")),
            PutAllRow("the key several levels down in the argument", listOf(collidingKey1 to "a"), listOf(collidingKey1.copy() to "x", levelTwoSibling to "y")),
            PutAllRow("the key in the receiver's subtree", listOf(collidingKey1 to "a", levelOneSibling to "b"), listOf(collidingKey1.copy() to "x")),
            PutAllRow("a subtree lacking the key", listOf(collidingKey1 to "a"), listOf(levelOneSibling to "y", otherLevelOneSibling to "z")),
            PutAllRow("an entry pushed deeper by the subtree", listOf(collidingKey1 to "a"), listOf(levelTwoSibling to "y", levelOneSibling to "z")),
        )
        for (row in rows) forEachBuilder(row.receiver) { message, builder ->
            builder.putAll(persistentHashMapOf(*row.argument.toTypedArray()))

            assertMerged(row.receiver.toMap(), row.argument.toMap(), builder, "${row.description} $message")
        }
    }

    @Test
    fun `putAll counts one size change when it adds keys and none when it replaces values`() {
        val rows = listOf(
            PutAllRow("new values", listOf(collidingKey1 to "a", collidingKey2 to "b", lastLevelSibling to "c"), listOf(collidingKey1 to "x", collidingKey2 to "y", lastLevelSibling to "z")) to 0,
            PutAllRow("an entry merged into the argument's subtree", listOf(collidingKey1 to "a"), listOf(collidingKey1.copy() to "x", levelOneSibling to "y")) to 1,
            PutAllRow("a disjoint subtree", listOf(collidingKey1 to "a"), listOf(levelOneSibling to "y", otherLevelOneSibling to "z")) to 1,
            PutAllRow("keys added to a collision node", listOf(collidingKey1 to "a", lastLevelSibling to "c"), listOf(collidingKey2 to "y", collidingKey3 to "z")) to 1,
        )
        for ((row, sizeChanges) in rows) forEachBuilder(row.receiver) { message, builder ->
            val sizeModCount = builder.sizeModCount

            builder.putAll(persistentHashMapOf(*row.argument.toTypedArray()))

            assertEquals((row.receiver.toMap() + row.argument.toMap()).size, builder.size, "${row.description} $message")
            assertEquals(sizeModCount + sizeChanges, builder.sizeModCount, "${row.description} $message")
        }
    }

    @Test
    fun `putAll reuses the argument's subtree holding the stored key instance`() = forEachBuilder(listOf(collidingKey1 to "a")) { message, builder ->
        @Suppress("UNCHECKED_CAST")
        val argument = persistentHashMapOf(collidingKey1 to "x", levelOneSibling to "y") as PersistentHashMap<IntWrapper, String>

        builder.putAll(argument)

        assertEquals(2, builder.size, message)
        assertEquals("x", builder[collidingKey1], message)
        assertSame(argument.node, builder.node, message)
    }

    @Test
    fun `putAll reuses the argument's collision node holding the stored key instance`() = forEachBuilder(listOf(collidingKey1 to "old")) { message, builder ->
        @Suppress("UNCHECKED_CAST")
        val argument = persistentHashMapOf(collidingKey1 to "x", collidingKey2 to "y") as PersistentHashMap<IntWrapper, String>

        builder.putAll(argument)

        assertEquals(2, builder.size, message)
        assertEquals("x", builder[collidingKey1], message)
        assertSame(argument.node, builder.node, message)
    }

    @Test
    fun `putAll leaves the argument its own key instance`() = forEachBuilder(listOf(collidingKey1 to "a")) { message, builder ->
        val argumentKey = collidingKey1.copy()
        val argument = persistentHashMapOf(argumentKey to "x", levelOneSibling to "y")

        builder.putAll(argument)

        assertSame(collidingKey1, builder.storedKey(collidingKey1), message)
        assertSame(argumentKey, argument.storedKey(argumentKey), message)
        assertEquals("x", argument[argumentKey], message)
    }

    @Test
    fun `putAll of new values in an unowned collision node under an owned root keeps the root`() {
        @Suppress("UNCHECKED_CAST")
        val builder = persistentHashMapOf(collidingKey1 to "a", collidingKey2 to "b", lastLevelSibling to "c").builder() as PersistentHashMapBuilder<IntWrapper, String>
        builder[lastLevelSibling] = "C" // copies the path down to the last level, the collision node stays shared with the map
        val root = builder.node
        val iterator = builder.entries.iterator()
        assertEquals("C", iterator.next().value)

        builder.putAll(persistentHashMapOf(collidingKey1 to "x", collidingKey2 to "y"))

        assertSame(root, builder.node)
        assertEquals("x", builder[collidingKey1])
        assertEquals(listOf("x", "y"), List(2) { iterator.next().value }.sorted())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun `put of the stored value keeps the built map`() {
        val map = persistentHashMapOf(collidingKey1 to "a", collidingKey2 to "b", lastLevelSibling to "c")
        val stored = map.getValue(collidingKey1)
        val builder = map.builder()

        assertSame(stored, builder.put(collidingKey1, stored))

        assertSame(map, builder.build())
    }

    @Test
    fun `putAll of the stored values keeps the built map`() {
        val map = persistentHashMapOf(collidingKey1 to "a", collidingKey2 to "b", lastLevelSibling to "c")
        val builder = map.builder()

        builder.putAll(persistentHashMapOf(*map.map { it.key to it.value }.toTypedArray()))

        assertSame(map, builder.build())
    }

    @Test
    fun `putAll of an equal key with the stored value keeps the built map`() {
        val map = persistentHashMapOf(collidingKey1 to "a", rootSibling to "b")
        val builder = map.builder()
        val iterator = builder.entries.iterator()

        builder.putAll(persistentHashMapOf(collidingKey1.copy() to map.getValue(collidingKey1)))

        assertSame(map, builder.build())
        val _ = iterator.next()
    }

    @Test
    fun `putAll of random maps keeps the stored key instances and takes the argument's values`() {
        val random = Random(313)
        repeat(200) { iteration ->
            val receiver = randomTrieKeys(random, 0..<14).values.associateWith { "r${it.obj}" }
            val argument = randomTrieKeys(random, 0..<14).values.associateWith { "a${it.obj}" }
            val builder = persistentHashMapOf<IntWrapper, String>().builder().apply { for ((key, value) in receiver) set(key, value) }

            builder.putAll(persistentHashMapOf(*argument.toList().toTypedArray()))

            assertMerged(receiver, argument, builder, "iteration $iteration, receiver ${receiver.keys.map { it.obj }}, argument ${argument.keys.map { it.obj }}")
        }
    }
}

/** The live entry contract on the hash map builders, as stdlib declares an IterableTests subclass. */
class PersistentHashMapBuilderEntryTest : MapBuilderEntryTests(hashMapBuilders)
