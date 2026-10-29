/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentCollection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import tests.IntWrapper
import tests.equalCopy
import tests.remove
import tests.trie.missingKey
import tests.trie.trieShapes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * A persistent call that changes nothing returns the receiver, and a builder given only what it holds builds the
 * collection it came from. A call or a build that empties a collection returns the empty instance of its kind, the one
 * its empty factory call returns. An index out of range throws IndexOutOfBoundsException first, even when the call
 * would add nothing or remove the only element.
 *
 * A write changes nothing when it writes the stored instance or, over a primitive number, an unsigned number, a char
 * or a Boolean, an equal one: NaN over NaN, a number boxed again. An equal instance of a Number subclass is written,
 * as any object. 0.0 and -0.0 are not equal, so one written over the other is stored, also on Kotlin/JS, where
 * 0.0 === -0.0.
 *
 * A new call is a row of its kind's noOps or emptyingCalls, by its participle or through mutate; a new receiver, an
 * entry of [lists], [sets] or [maps]; a new kind of value, a row of [valueWrites]; a new index check, a row of
 * [outOfRangeCalls].
 */
class NoOpIdentityTest {

    @Test
    fun list() = runCells { testCalls(lists(), persistentListOf(), ::listNoOps, ::listEmptyingCalls) }

    @Test
    fun hashSet() = runCells { testCalls(sets(persistentHashSetOf()), persistentHashSetOf(), ::setNoOps, ::collectionEmptyingCalls) }

    @Test
    fun orderedSet() = runCells { testCalls(sets(persistentSetOf()), persistentSetOf(), ::setNoOps, ::collectionEmptyingCalls) }

    @Test
    fun hashMap() = runCells { testCalls(maps(persistentHashMapOf()), persistentHashMapOf(), ::mapNoOps, ::mapEmptyingCalls) }

    @Test
    fun orderedMap() = runCells { testCalls(maps(persistentMapOf()), persistentMapOf(), ::mapNoOps, ::mapEmptyingCalls) }

    @Test
    fun `value writes into a list`() = runCells {
        for ((size, shape) in listShapes) for (index in boundaryIndices(size)) for (write in valueWrites) {
            val (stored, written) = write.values()
            val neighbour = if (index == 0) 1 else index - 1
            val list = wrappersOf(size).toPersistentList<Any?>().replacingAt(index, stored)
            testWrites("$shape, ${write.description} at $index", write, list, written, read = { this[index] },
                writes = rows {
                    "replacingAt" { replacingAt(index, written) }
                    "a builder's set, which returns the stored element" {
                        mutate { assertSameValue(stored, it.set(index, written), "what set returns") }
                    }
                    "the list iterator's set" { mutate { it.listIterator(index).run { val _ = next(); set(written) } } }
                },
                writesElsewhereToo = rows {
                    "a builder's set after a set at $neighbour" { mutate { it[neighbour] = NEW; it[index] = written } }
                },
            )
        }
    }

    @Test
    fun `value writes into a hash map`() = runCells { testValueWrites("a hash map") { it.toPersistentHashMap() } }

    @Test
    fun `value writes into an ordered map`() = runCells { testValueWrites("an ordered map") { it.toPersistentMap() } }

    @Test
    fun `an index out of range throws even with nothing to add or one element to remove`() = runCells {
        for ((origin, list) in lists()) for ((description, call) in outOfRangeCalls) cell("$origin, $description") {
            assertFailsWith<IndexOutOfBoundsException> { list.call() }
        }
    }
}

/** Every call of [noOps] returns its receiver, and every call of [emptyingCalls] returns [empty], the empty instance. */
private fun <C : Any> Cells.testCalls(
    receivers: List<Pair<String, C>>,
    empty: C,
    noOps: (C) -> List<Call<C>>,
    emptyingCalls: (C) -> List<Call<C>>,
) {
    for ((origin, receiver) in receivers) {
        for ((description, call) in noOps(receiver)) cell("$origin, $description") { assertSame(receiver, receiver.call(), "the receiver") }
        for ((description, call) in emptyingCalls(receiver)) cell("$origin, $description") { assertSame(empty, receiver.call(), "the empty instance") }
    }
}

/** The library declares mutate for each kind; the calls lists and sets share need it for both. */
private inline fun <E> PersistentCollection<E>.mutate(mutator: (MutableCollection<E>) -> Unit): PersistentCollection<E> =
    builder().apply(mutator).build()

/** An element or a value no fixture holds. */
private const val NEW = "new"

/** A missing key whose path parts from [stored]'s at level 1. */
private fun missingBesideAtLevelOne(stored: Any?): Any = missingKey(stored.hashCode() xor MAX_BRANCHING_FACTOR)

/** Every Int key wrapped, so that an equal key that is not the stored instance exists. */
private val wrappedShapes: Map<String, List<Any?>> = trieShapes.mapValues { (_, keys) -> keys.map { if (it is Int) IntWrapper(it, it) else it } }

private fun wrappersOf(size: Int): List<Any> = List(size) { IntWrapper(it, it) }

/** As a list and as hash and ordered sets, which a hash set merges trie by trie. */
private fun everyElementOf(elements: Collection<Any?>): List<Pair<String, Collection<Any?>>> = listOf(
    "the stored elements" to elements.toList(),
    "an equal hash set" to elements.toPersistentHashSet(),
    "an equal ordered set" to elements.toPersistentSet(),
    "a hash set of equal copies" to elements.map { it.equalCopy() }.toPersistentHashSet(),
)

private fun supersetsOf(elements: Collection<Any?>): List<Pair<String, Collection<Any?>>> = listOf(
    "a superset" to elements + "missing",
    "a hash set of a superset" to (elements + "missing").toPersistentHashSet(),
)

private val nothing: List<Pair<String, Collection<Any?>>> = listOf("nothing" to emptyList(), "an empty hash set" to persistentHashSetOf())

private fun noElementOf(elements: Collection<Any?>): List<Pair<String, Collection<Any?>>> = nothing + listOf(
    "missing elements" to listOf("missing", "absent"),
    "a hash set of missing elements" to persistentHashSetOf("missing", "absent"),
    "a hash set of missing elements with the stored hashes" to elements.map { missingKey(it.hashCode()) }.toPersistentHashSet(),
)

private fun <C : PersistentCollection<Any?>> collectionNoOps(collection: C): List<Call<C>> = rows {
    "a builder that is only built" { mutate { } }
    for ((argument, elements) in noElementOf(collection)) {
        "removingAll($argument)" { removingAll(elements) }
        "removeAll($argument)" { mutate { it.removeAll(elements) } }
    }
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        "retainingAll($argument)" { retainingAll(elements) }
        "retainAll($argument)" { mutate { it.retainAll(elements) } }
    }
    "retainingAll(itself)" { retainingAll(this) }
    "removing(a missing element)" { removing("missing") }
    "remove(a missing element)" { mutate { it.remove("missing") } }
    "removingAll { false }" { removingAll { false } }
    "removeAll { false }" { mutate { it.removeAll { false } } }
    "retainAll { true }" { mutate { it.retainAll { true } } }
}

private fun <C : PersistentCollection<Any?>> collectionEmptyingCalls(collection: C): List<Call<C>> = rows {
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        "removingAll($argument)" { removingAll(elements) }
        "removeAll($argument)" { mutate { it.removeAll(elements) } }
    }
    for ((argument, elements) in noElementOf(collection)) {
        "retainingAll($argument)" { retainingAll(elements) }
        "retainAll($argument)" { mutate { it.retainAll(elements) } }
    }
    "cleared()" { cleared() }
    "clear()" { mutate { it.clear() } }
    "removingAll { true }" { removingAll { true } }
    "removeAll { true }" { mutate { it.removeAll { true } } }
    "retainAll { false }" { mutate { it.retainAll { false } } }
    "removing every element" { collection.fold<Any?, PersistentCollection<Any?>>(this) { rest, element -> rest.removing(element) } }
    "remove of every element" { mutate { builder -> collection.forEach { builder.remove(it) } } }
    "the iterator's remove of every element" { mutate { it.iterator().run { while (hasNext()) { val _ = next(); remove() } } } }
    "minus an array of every element" { this - collection.toTypedArray() }
    "minus a sequence of every element" { this - collection.asSequence() }
    "minus an iterable that is not a collection of every element" { this - Iterable { collection.iterator() } }
}

private fun lists(): List<Pair<String, PersistentList<Any?>>> = listOf(
    "the empty list" to persistentListOf<Any?>(),
    "a list of one element" to wrappersOf(1).toPersistentList(),
) + listShapes.map { (size, shape) -> "a list of $size, $shape" to wrappersOf(size).toPersistentList() }

private fun listNoOps(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionNoOps(list) + rows {
    "addingAll(nothing)" { addingAll(emptyList()) }
    "addAll(nothing)" { mutate { it.addAll(emptyList()) } }
    "addingAllAt(0, nothing)" { addingAllAt(0, emptyList()) }
    "addAll(0, nothing)" { mutate { it.addAll(0, emptyList()) } }
    for (index in boundaryIndices(list.size)) {
        "replacingAt($index, the stored element)" { replacingAt(index, list[index]) }
        "set($index, the stored element)" { mutate { it[index] = list[index] } }
        "listIterator($index).set(next())" { mutate { it.listIterator(index).run { set(next()) } } }
    }
}

private fun listEmptyingCalls(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionEmptyingCalls(list) + rows {
    "subList(0, size).clear()" { mutate { it.subList(0, it.size).clear() } }
    "removingAt(0) of every element" { list.fold(this) { rest, _ -> rest.removingAt(0) } }
    "removeAt(0) of every element" { mutate { while (it.isNotEmpty()) it.removeAt(0) } }
    "removingAt(lastIndex) of every element" { list.fold(this) { rest, _ -> rest.removingAt(rest.lastIndex) } }
    "removeAt(lastIndex) of every element" { mutate { while (it.isNotEmpty()) it.removeAt(it.lastIndex) } }
}

/** The shortcuts for nothing to add and for removing the only element come after the index check. */
private val outOfRangeCalls: List<Call<PersistentList<Any?>>> = rows {
    "removingAt(-1)" { removingAt(-1) }
    "removeAt(-1)" { mutate { it.removeAt(-1) } }
    "removingAt(size)" { removingAt(size) }
    "removeAt(size)" { mutate { it.removeAt(it.size) } }
    "addingAllAt(-1, nothing)" { addingAllAt(-1, emptyList()) }
    "addAll(-1, nothing)" { mutate { it.addAll(-1, emptyList()) } }
    "addingAllAt(size + 1, nothing)" { addingAllAt(size + 1, emptyList()) }
    "addAll(size + 1, nothing)" { mutate { it.addAll(it.size + 1, emptyList()) } }
}

private fun sets(empty: PersistentSet<Any?>): List<Pair<String, PersistentSet<Any?>>> = listOf(
    "the empty set" to empty,
    "a set of one element" to empty.adding(IntWrapper(1, 1)),
) + wrappedShapes.map { (shape, elements) -> "a set of $shape" to empty.addingAll(elements) }

private fun setNoOps(set: PersistentSet<Any?>): List<Call<PersistentSet<Any?>>> = collectionNoOps(set) + rows {
    for ((argument, elements) in everyElementOf(set) + nothing) {
        "addingAll($argument)" { addingAll(elements) }
        "addAll($argument)" { mutate { it.addAll(elements) } }
    }
    "addingAll(itself)" { addingAll(this) }
    "addAll(the set it was made from)" { mutate { it.addAll(this) } }
    for (element in set) {
        "adding($element)" { adding(element) }
        "add($element)" { mutate { it.add(element) } }
        "adding(a copy of $element)" { adding(element.equalCopy()) }
        "add(a copy of $element)" { mutate { it.add(element.equalCopy()) } }
        "removing(a missing element with the hash of $element)" { removing(missingKey(element.hashCode())) }
        "remove(a missing element with the hash of $element)" { mutate { it.remove(missingKey(element.hashCode())) } }
        "removing(a missing element beside $element at level 1)" { removing(missingBesideAtLevelOne(element)) }
        "remove(a missing element beside $element at level 1)" { mutate { it.remove(missingBesideAtLevelOne(element)) } }
    }
}

private fun maps(empty: PersistentMap<Any?, Any?>): List<Pair<String, PersistentMap<Any?, Any?>>> = listOf(
    "the empty map" to empty,
    "a map of one key" to empty.putting(IntWrapper(1, 1), IntWrapper(0, 0)),
) + wrappedShapes.map { (shape, keys) ->
    "a map of $shape" to empty.puttingAll(keys.withIndex().associate { (index, key) -> key to IntWrapper(index, index) })
}

/** Only stored entries, as maps and as hash maps, which a hash map merges trie by trie. */
private fun storedEntriesOf(map: Map<Any?, Any?>): List<Pair<String, Map<Any?, Any?>>> = listOf(
    "nothing" to emptyMap(),
    "an empty hash map" to persistentHashMapOf(),
    "the stored entries" to map.toMap(),
    "an equal hash map" to persistentHashMapOf(*map.toList().toTypedArray()),
    "an equal ordered map" to persistentMapOf(*map.toList().toTypedArray()),
    "a hash map of the stored values under equal keys" to persistentHashMapOf(*map.map { (key, value) -> key.equalCopy() to value }.toTypedArray()),
)

private fun mapNoOps(map: PersistentMap<Any?, Any?>): List<Call<PersistentMap<Any?, Any?>>> = rows {
    "a builder that is only built" { mutate { } }
    for ((argument, argumentEntries) in storedEntriesOf(map)) {
        "puttingAll($argument)" { puttingAll(argumentEntries) }
        "putAll($argument)" { mutate { it.putAll(argumentEntries) } }
    }
    "puttingAll(itself)" { puttingAll(this) }
    "putAll(the map it was made from)" { mutate { it.putAll(this) } }
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        "minus($argument)" { this - argumentKeys }
        "keys.removeAll($argument)" { mutate { it.keys.removeAll(argumentKeys) } }
    }
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        "keys.retainAll($argument)" { mutate { it.keys.retainAll(argumentKeys) } }
    }
    "removing(a missing key)" { removing("missing") }
    "remove(a missing key)" { mutate { it.remove("missing") } }
    "entries.removeAll { false }" { mutate { it.entries.removeAll { false } } }
    "entries.retainAll { true }" { mutate { it.entries.retainAll { true } } }
    for ((key, value) in map) for ((keyName, sameKey) in listOf("$key" to key, "a copy of $key" to key.equalCopy())) {
        "putting($keyName, its value)" { putting(sameKey, value) }
        "put($keyName, its value)" { mutate { it[sameKey] = value } }
        "puttingAll(a hash map of $keyName and its value)" { puttingAll(persistentHashMapOf(sameKey to value)) }
        "putAll(a hash map of $keyName and its value)" { mutate { it.putAll(persistentHashMapOf(sameKey to value)) } }
    }
    for (key in map.keys) {
        "removing($key, a new value)" { removing(key, NEW) }
        "remove($key, a new value)" { mutate { val _ = it.remove(key, NEW) } }
        "removing(a missing key with the hash of $key)" { removing(missingKey(key.hashCode())) }
        "remove(a missing key with the hash of $key)" { mutate { it.remove(missingKey(key.hashCode())) } }
        "removing(a missing key beside $key at level 1)" { removing(missingBesideAtLevelOne(key)) }
        "remove(a missing key beside $key at level 1)" { mutate { it.remove(missingBesideAtLevelOne(key)) } }
    }
}

private fun mapEmptyingCalls(map: PersistentMap<Any?, Any?>): List<Call<PersistentMap<Any?, Any?>>> = rows {
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        "minus($argument)" { this - argumentKeys }
        "keys.removeAll($argument)" { mutate { it.keys.removeAll(argumentKeys) } }
    }
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        "keys.retainAll($argument)" { mutate { it.keys.retainAll(argumentKeys) } }
    }
    "cleared()" { cleared() }
    "clear()" { mutate { it.clear() } }
    "values.clear()" { mutate { it.values.clear() } }
    "entries.removeAll { true }" { mutate { it.entries.removeAll { true } } }
    "entries.retainAll { false }" { mutate { it.entries.retainAll { false } } }
    "the entries' iterator's remove of every entry" { mutate { it.entries.iterator().run { while (hasNext()) { val _ = next(); remove() } } } }
    "removing every key" { map.keys.fold(this) { rest, key -> rest.removing(key) } }
    "remove of every key" { mutate { builder -> map.keys.forEach { builder.remove(it) } } }
    "removing every key with its value" { map.entries.fold(this) { rest, (key, value) -> rest.removing(key, value) } }
    "remove of every key with its value" { mutate { builder -> map.forEach { (key, value) -> val _ = builder.remove(key, value) } } }
    "minus an array of every key" { this - map.keys.toTypedArray() }
    "minus a sequence of every key" { this - map.keys.asSequence() }
    "minus an iterable that is not a collection of every key" { this - Iterable { map.keys.iterator() } }
}

/** [values] makes `stored to written` anew on every call, so that an equal number is boxed again. */
private class ValueWrite(val description: String, val changes: Boolean, val values: () -> Pair<Any?, Any?>)

private fun writes(changes: Boolean, section: Rows<() -> Pair<Any?, Any?>>.() -> Unit): List<ValueWrite> =
    rows(section).map { (description, values) -> ValueWrite(description, changes, values) }

/** A Number that is no primitive box: equal by [value], and written over an equal instance as any object. */
private data class ValueNumber(val value: Int) : Number() {
    override fun toByte(): Byte = value.toByte()
    override fun toDouble(): Double = value.toDouble()
    override fun toFloat(): Float = value.toFloat()
    override fun toInt(): Int = value
    override fun toLong(): Long = value.toLong()
    override fun toShort(): Short = value.toShort()
}

private val valueWrites = writes(changes = true) {
    "-0.0 over 0.0" { 0.0 to -0.0 }
    "0.0 over -0.0" { -0.0 to 0.0 }
    "an equal Number object" { ValueNumber(1) to ValueNumber(1) }
} + writes(changes = false) {
    "NaN over NaN" { Double.NaN to Double.NaN }
    "an equal Double" { 1.5 to 1.5 }
    "an equal Int" { 1000 to 1000 }
    "an equal Long" { 1000L to 1000L }
    "an equal Float" { 1.5f to 1.5f }
    "an equal Short" { 1000.toShort() to 1000.toShort() }
    "an equal Byte, which only Kotlin/Wasm boxes again" { 1.toByte() to 1.toByte() }
    "an equal Char" { '€' to '€' }
    "an equal UInt" { 1u to 1u }
    "an equal ULong" { 1uL to 1uL }
    "an equal UByte" { 1.toUByte() to 1.toUByte() }
    "an equal UShort" { 1.toUShort() to 1.toUShort() }
    ValueNumber(1).let { same -> "the stored Number object" { same to same } }
    "null over the null of arrayOfNulls, which Kotlin/JS holds as undefined" { arrayOfNulls<Any>(1)[0] to null }
}

/** A Double by its bits: only they tell -0.0 from 0.0 on every platform, and on Kotlin/JS NaN is not === itself. */
private fun assertSameValue(expected: Any?, actual: Any?, message: String) {
    if (expected is Double && actual is Double) assertEquals(expected.toRawBits(), actual.toRawBits(), message)
    else assertSame(expected, actual, message)
}

/**
 * Every call leaves [written] where [read] reads when [write] changes something, and the stored value otherwise. A call
 * of [writes] returns the receiver exactly when the write changes nothing; a call of [writesElsewhereToo] writes other
 * elements or keys as well, so only the value read back tells.
 */
@Suppress("UNCHECKED_CAST")
private fun <C : Any> Cells.testWrites(
    name: String,
    write: ValueWrite,
    receiver: C,
    written: Any?,
    read: C.() -> Any?,
    writes: List<Call<C>>,
    writesElsewhereToo: List<Call<C>>,
) {
    val readBack = if (write.changes) written else receiver.read()
    for ((description, call) in writes) cell("$name by $description") {
        val result = receiver.call() as C
        if (write.changes) assertNotSame(receiver, result, "the result of a write that changes the value")
        else assertSame(receiver, result, "the result of a write that changes nothing")
        assertSameValue(readBack, result.read(), "the value read back")
    }
    for ((description, call) in writesElsewhereToo) cell("$name by $description") {
        assertSameValue(readBack, (receiver.call() as C).read(), "the value read back")
    }
}

/**
 * Writes under every key of every trie shape by a put, by an entry's setValue and by putAlls. A putAll that writes
 * other keys too, brings the key in a subtree beside a new key, or holds every key and a new one with the key's hash,
 * keeps the stored value when the write changes nothing.
 */
private fun Cells.testValueWrites(kind: String, newMap: (Map<Any?, Any?>) -> PersistentMap<Any?, Any?>) {
    for ((shape, keys) in trieShapes) for ((position, key) in keys.withIndex()) for (write in valueWrites) {
        val (stored, written) = write.values()
        val content = keys.withIndex().associate { (index, shapeKey) -> shapeKey to IntWrapper(index, index) } + (key to stored)
        val everyKey = content + (key to written)
        testWrites("$kind of $shape, ${write.description} under the key at $position", write, newMap(content), written, read = { this[key] },
            writes = rows {
                "putting" { putting(key, written) }
                "a builder's put, which returns the stored value" {
                    mutate { assertSameValue(stored, it.put(key, written), "what put returns") }
                }
                "an entry's setValue, which returns the stored value" {
                    mutate { builder -> assertSameValue(stored, builder.entries.first { it.key == key }.setValue(written), "what setValue returns") }
                }
                putAllCalls("a hash map of the key" to mapOf(key to written), "a hash map of every key" to everyKey)
            },
            writesElsewhereToo = rows {
                putAllCalls(
                    "new values for the other keys" to content.mapValues { NEW } + (key to written),
                    "a new key beside it at level 1" to mapOf(key to written, missingBesideAtLevelOne(key) to NEW),
                    "every key and a new key with its hash" to everyKey + (missingKey(key.hashCode()) to NEW),
                )
            },
        )
    }
}

/** Rows of a puttingAll and a builder's putAll of each argument as a hash map, which a hash map merges trie by trie. */
private fun Rows<PersistentMap<Any?, Any?>.() -> Any?>.putAllCalls(vararg arguments: Pair<String, Map<Any?, Any?>>) {
    for ((argument, entries) in arguments) {
        "puttingAll of $argument" { puttingAll(entries.toPersistentHashMap()) }
        "a builder's putAll of $argument" { mutate { it.putAll(entries.toPersistentHashMap()) } }
    }
}
