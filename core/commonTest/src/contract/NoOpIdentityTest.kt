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
import tests.trie.collidingKey1
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
 * its empty factory call returns.
 *
 * A write changes nothing when it writes the stored instance or, over a primitive number, an unsigned number, a char
 * or a Boolean, an equal one: NaN over NaN, a number boxed again. An equal instance of a Number subclass is written,
 * as any object. 0.0 and -0.0 are not equal, so one written over the other is stored, also on Kotlin/JS, where
 * 0.0 === -0.0.
 *
 * A new call is a row of its kind's noOps or emptyingCalls, by its participle or through mutate; a new receiver, an
 * entry of [lists], [sets] or [maps]; a new kind of value, a row of [valueWrites].
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
        for ((size, depth) in listDepths) for (index in boundaryIndices(size)) for (write in valueWrites) {
            val stored = write.stored()
            val neighbour = if (index == 0) 1 else index - 1
            val list = wrappersOf(size).toPersistentList<Any?>().replacingAt(index, stored)
            testWrites("$depth, ${write.description} at $index", write, list, read = { this[index] },
                writes = listOf(
                    "replacingAt" to { written -> replacingAt(index, written) },
                    "a builder's set, which returns the stored element" to { written ->
                        mutate { assertSameValue(stored, it.set(index, written), "what set returns") }
                    },
                    "the list iterator's set" to { written -> mutate { it.listIterator(index).run { val _ = next(); set(written) } } },
                ),
                writesElsewhereToo = listOf(
                    "a builder's set after a set at $neighbour" to { written -> mutate { it[neighbour] = NEW; it[index] = written } },
                ),
            )
        }
    }

    @Test
    fun `value writes into a hash map`() = runCells { testValueWrites("a hash map") { it.toPersistentHashMap() } }

    @Test
    fun `value writes into an ordered map`() = runCells { testValueWrites("an ordered map") { it.toPersistentMap() } }

    @Test
    fun `an index out of range throws even with nothing to add or one element to remove`() = runCells {
        for ((origin, list) in lists()) for ((call, function) in outOfRangeCalls) cell("$origin, $call") {
            assertFailsWith<IndexOutOfBoundsException> { list.function() }
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
        for ((call, function) in noOps(receiver)) cell("$origin, $call") { assertSame(receiver, receiver.function(), "the receiver") }
        for ((call, function) in emptyingCalls(receiver)) cell("$origin, $call") { assertSame(empty, receiver.function(), "the empty instance") }
    }
}

/** The library declares mutate for each kind; the calls lists and sets share need it for both. */
private inline fun <E> PersistentCollection<E>.mutate(mutator: (MutableCollection<E>) -> Unit): PersistentCollection<E> =
    builder().apply(mutator).build()

/** An element or a value no fixture holds. */
private const val NEW = "new"

/** A missing key whose path parts from [stored]'s at level 1. */
private fun missingBesideAtLevelOne(stored: Any?): Any = missingKey(stored.hashCode() xor MAX_BRANCHING_FACTOR)

/** Every key wrapped, so that an equal key that is not the stored instance exists. */
private val wrappedShapes: Map<String, List<Any>> = trieShapes.mapValues { (_, keys) -> keys.map { if (it is Int) IntWrapper(it, it) else it } }

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

private fun <C : PersistentCollection<Any?>> collectionNoOps(collection: C): List<Call<C>> = buildList {
    call("a builder that is only built") { mutate { } }
    for ((argument, elements) in noElementOf(collection)) {
        call("removingAll($argument)") { removingAll(elements) }
        call("removeAll($argument)") { mutate { it.removeAll(elements) } }
    }
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        call("retainingAll($argument)") { retainingAll(elements) }
        call("retainAll($argument)") { mutate { it.retainAll(elements) } }
    }
    call("retainingAll(itself)") { retainingAll(this) }
    call("removing(a missing element)") { removing("missing") }
    call("remove(a missing element)") { mutate { it.remove("missing") } }
    call("removingAll { false }") { removingAll { false } }
    call("removeAll { false }") { mutate { it.removeAll { false } } }
    call("retainAll { true }") { mutate { it.retainAll { true } } }
}

private fun <C : PersistentCollection<Any?>> collectionEmptyingCalls(collection: C): List<Call<C>> = buildList {
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        call("removingAll($argument)") { removingAll(elements) }
        call("removeAll($argument)") { mutate { it.removeAll(elements) } }
    }
    for ((argument, elements) in noElementOf(collection)) {
        call("retainingAll($argument)") { retainingAll(elements) }
        call("retainAll($argument)") { mutate { it.retainAll(elements) } }
    }
    call("cleared()") { cleared() }
    call("clear()") { mutate { it.clear() } }
    call("removingAll { true }") { removingAll { true } }
    call("removeAll { true }") { mutate { it.removeAll { true } } }
    call("retainAll { false }") { mutate { it.retainAll { false } } }
    call("removing every element") { collection.fold<Any?, PersistentCollection<Any?>>(this) { rest, element -> rest.removing(element) } }
    call("remove of every element") { mutate { builder -> collection.forEach { builder.remove(it) } } }
    call("the iterator's remove of every element") { mutate { it.iterator().run { while (hasNext()) { val _ = next(); remove() } } } }
    call("minus an array of every element") { this - collection.toTypedArray() }
    call("minus a sequence of every element") { this - collection.asSequence() }
    call("minus an iterable that is not a collection of every element") { this - Iterable { collection.iterator() } }
}

private fun lists(): List<Pair<String, PersistentList<Any?>>> = listOf(
    "the empty list" to persistentListOf<Any?>(),
    "a list of one element" to wrappersOf(1).toPersistentList(),
) + listDepths.map { (size, depth) -> "a list of $size, $depth" to wrappersOf(size).toPersistentList() }

private fun listNoOps(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionNoOps(list) + buildList {
    call("addingAll(nothing)") { addingAll(emptyList()) }
    call("addAll(nothing)") { mutate { it.addAll(emptyList()) } }
    call("addingAllAt(0, nothing)") { addingAllAt(0, emptyList()) }
    call("addAll(0, nothing)") { mutate { it.addAll(0, emptyList()) } }
    for (index in boundaryIndices(list.size)) {
        call("replacingAt($index, the stored element)") { replacingAt(index, list[index]) }
        call("set($index, the stored element)") { mutate { it[index] = list[index] } }
        call("listIterator($index).set(next())") { mutate { it.listIterator(index).run { set(next()) } } }
    }
}

private fun listEmptyingCalls(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionEmptyingCalls(list) + buildList {
    call("subList(0, size).clear()") { mutate { it.subList(0, it.size).clear() } }
    call("removingAt(0) of every element") { list.fold(this) { rest, _ -> rest.removingAt(0) } }
    call("removeAt(0) of every element") { mutate { while (it.isNotEmpty()) it.removeAt(0) } }
    call("removingAt(lastIndex) of every element") { list.fold(this) { rest, _ -> rest.removingAt(rest.lastIndex) } }
    call("removeAt(lastIndex) of every element") { mutate { while (it.isNotEmpty()) it.removeAt(it.lastIndex) } }
}

/** The shortcuts for nothing to add and for removing the only element come after the index check. */
private val outOfRangeCalls: List<Call<PersistentList<Any?>>> = buildList {
    call("removingAt(size)") { removingAt(size) }
    call("removeAt(size)") { mutate { it.removeAt(it.size) } }
    call("addingAllAt(size + 1, nothing)") { addingAllAt(size + 1, emptyList()) }
    call("addAll(size + 1, nothing)") { mutate { it.addAll(it.size + 1, emptyList()) } }
}

private fun sets(empty: PersistentSet<Any?>): List<Pair<String, PersistentSet<Any?>>> = listOf(
    "the empty set" to empty,
    "a set of one element" to empty.adding(IntWrapper(1, 1)),
) + wrappedShapes.map { (shape, elements) -> "a set of $shape" to empty.addingAll(elements) }

private fun setNoOps(set: PersistentSet<Any?>): List<Call<PersistentSet<Any?>>> = collectionNoOps(set) + buildList {
    for ((argument, elements) in everyElementOf(set) + nothing) {
        call("addingAll($argument)") { addingAll(elements) }
        call("addAll($argument)") { mutate { it.addAll(elements) } }
    }
    call("addingAll(itself)") { addingAll(this) }
    call("addAll(the set it was made from)") { mutate { it.addAll(this) } }
    for (element in set) {
        call("adding($element)") { adding(element) }
        call("add($element)") { mutate { it.add(element) } }
        call("adding(a copy of $element)") { adding(element.equalCopy()) }
        call("add(a copy of $element)") { mutate { it.add(element.equalCopy()) } }
        call("removing(a missing element with the hash of $element)") { removing(missingKey(element.hashCode())) }
        call("remove(a missing element with the hash of $element)") { mutate { it.remove(missingKey(element.hashCode())) } }
        call("removing(a missing element beside $element at level 1)") { removing(missingBesideAtLevelOne(element)) }
        call("remove(a missing element beside $element at level 1)") { mutate { it.remove(missingBesideAtLevelOne(element)) } }
    }
}

private fun maps(empty: PersistentMap<Any?, Any?>): List<Pair<String, PersistentMap<Any?, Any?>>> = listOf(
    "the empty map" to empty,
    "a map of one key" to empty.putting(IntWrapper(1, 1), IntWrapper(0, 0)),
    "a map with a null key" to empty.puttingAll(mapOf(null to IntWrapper(0, 0), collidingKey1 to IntWrapper(1, 1))),
    "a map with a NaN key" to empty.puttingAll(mapOf(Double.NaN to "v", 1 to "w")),
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

private fun mapNoOps(map: PersistentMap<Any?, Any?>): List<Call<PersistentMap<Any?, Any?>>> = buildList {
    call("a builder that is only built") { mutate { } }
    for ((argument, argumentEntries) in storedEntriesOf(map)) {
        call("puttingAll($argument)") { puttingAll(argumentEntries) }
        call("putAll($argument)") { mutate { it.putAll(argumentEntries) } }
    }
    call("puttingAll(itself)") { puttingAll(this) }
    call("putAll(the map it was made from)") { mutate { it.putAll(this) } }
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        call("minus($argument)") { this - argumentKeys }
        call("keys.removeAll($argument)") { mutate { it.keys.removeAll(argumentKeys) } }
    }
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        call("keys.retainAll($argument)") { mutate { it.keys.retainAll(argumentKeys) } }
    }
    call("removing(a missing key)") { removing("missing") }
    call("remove(a missing key)") { mutate { it.remove("missing") } }
    call("entries.removeAll { false }") { mutate { it.entries.removeAll { false } } }
    call("entries.retainAll { true }") { mutate { it.entries.retainAll { true } } }
    for ((key, value) in map) for ((keyName, sameKey) in listOf("$key" to key, "a copy of $key" to key.equalCopy())) {
        call("putting($keyName, its value)") { putting(sameKey, value) }
        call("put($keyName, its value)") { mutate { it[sameKey] = value } }
        call("puttingAll(a hash map of $keyName and its value)") { puttingAll(persistentHashMapOf(sameKey to value)) }
        call("putAll(a hash map of $keyName and its value)") { mutate { it.putAll(persistentHashMapOf(sameKey to value)) } }
    }
    for (key in map.keys) {
        call("removing($key, a new value)") { removing(key, NEW) }
        call("remove($key, a new value)") { mutate { val _ = it.remove(key, NEW) } }
        call("removing(a missing key with the hash of $key)") { removing(missingKey(key.hashCode())) }
        call("remove(a missing key with the hash of $key)") { mutate { it.remove(missingKey(key.hashCode())) } }
        call("removing(a missing key beside $key at level 1)") { removing(missingBesideAtLevelOne(key)) }
        call("remove(a missing key beside $key at level 1)") { mutate { it.remove(missingBesideAtLevelOne(key)) } }
    }
}

private fun mapEmptyingCalls(map: PersistentMap<Any?, Any?>): List<Call<PersistentMap<Any?, Any?>>> = buildList {
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        call("minus($argument)") { this - argumentKeys }
        call("keys.removeAll($argument)") { mutate { it.keys.removeAll(argumentKeys) } }
    }
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        call("keys.retainAll($argument)") { mutate { it.keys.retainAll(argumentKeys) } }
    }
    call("cleared()") { cleared() }
    call("clear()") { mutate { it.clear() } }
    call("values.clear()") { mutate { it.values.clear() } }
    call("entries.removeAll { true }") { mutate { it.entries.removeAll { true } } }
    call("entries.retainAll { false }") { mutate { it.entries.retainAll { false } } }
    call("the entries' iterator's remove of every entry") { mutate { it.entries.iterator().run { while (hasNext()) { val _ = next(); remove() } } } }
    call("removing every key") { map.keys.fold(this) { rest, key -> rest.removing(key) } }
    call("remove of every key") { mutate { builder -> map.keys.forEach { builder.remove(it) } } }
    call("removing every key with its value") { map.entries.fold(this) { rest, (key, value) -> rest.removing(key, value) } }
    call("remove of every key with its value") { mutate { builder -> map.forEach { (key, value) -> val _ = builder.remove(key, value) } } }
    call("minus an array of every key") { this - map.keys.toTypedArray() }
    call("minus a sequence of every key") { this - map.keys.asSequence() }
    call("minus an iterable that is not a collection of every key") { this - Iterable { map.keys.iterator() } }
}

/** [stored] and [written] box or make the value anew on every call; [changes] tells whether the write is stored. */
private class ValueWrite(val description: String, val changes: Boolean, val stored: () -> Any?, val written: () -> Any?)

/** A Number that is no primitive box: equal by [value], and written over an equal instance as any object. */
private data class ValueNumber(val value: Int) : Number() {
    override fun toByte(): Byte = value.toByte()
    override fun toDouble(): Double = value.toDouble()
    override fun toFloat(): Float = value.toFloat()
    override fun toInt(): Int = value
    override fun toLong(): Long = value.toLong()
    override fun toShort(): Short = value.toShort()
}

private val valueWrites = listOf(
    ValueWrite("-0.0 over 0.0", changes = true, { 0.0 }, { -0.0 }),
    ValueWrite("0.0 over -0.0", changes = true, { -0.0 }, { 0.0 }),
    ValueWrite("NaN over NaN", changes = false, { Double.NaN }, { Double.NaN }),
    ValueWrite("an equal Double", changes = false, { 1.5 }, { 1.5 }),
    ValueWrite("an equal Int", changes = false, { 1000 }, { 1000 }),
    ValueWrite("an equal Long", changes = false, { 1000L }, { 1000L }),
    ValueWrite("an equal Float", changes = false, { 1.5f }, { 1.5f }),
    ValueWrite("an equal Short", changes = false, { 1000.toShort() }, { 1000.toShort() }),
    ValueWrite("an equal Byte", changes = false, { 1.toByte() }, { 1.toByte() }),
    ValueWrite("an equal Char", changes = false, { '€' }, { '€' }),
    ValueWrite("an equal UInt", changes = false, { 1u }, { 1u }),
    ValueWrite("an equal ULong", changes = false, { 1uL }, { 1uL }),
    ValueWrite("an equal UByte", changes = false, { 1.toUByte() }, { 1.toUByte() }),
    ValueWrite("an equal UShort", changes = false, { 1.toUShort() }, { 1.toUShort() }),
    ValueWrite("an equal Number object", changes = true, { ValueNumber(1) }, { ValueNumber(1) }),
    ValueNumber(1).let { same -> ValueWrite("the stored Number object", changes = false, { same }, { same }) },
    ValueWrite("null over the null of arrayOfNulls, which Kotlin/JS holds as undefined", changes = false, { arrayOfNulls<Any>(1)[0] }, { null }),
)

/** A Double by its bits: only they tell -0.0 from 0.0 on every platform, and on Kotlin/JS NaN is not === itself. */
private fun assertSameValue(expected: Any?, actual: Any?, message: String) {
    if (expected is Double && actual is Double) assertEquals(expected.toRawBits(), actual.toRawBits(), message)
    else assertSame(expected, actual, message)
}

/**
 * A call of [writes] returns the receiver when [write] changes nothing, and a new instance holding the written value
 * otherwise. A call of [writesElsewhereToo] writes other elements or keys as well, so only what it leaves where [read]
 * reads tells.
 */
private fun <C : Any> Cells.testWrites(
    name: String,
    write: ValueWrite,
    receiver: C,
    read: C.() -> Any?,
    writes: List<Pair<String, C.(written: Any?) -> C>>,
    writesElsewhereToo: List<Pair<String, C.(written: Any?) -> C>>,
) {
    for ((call, function) in writes) cell("$name by $call") {
        val written = write.written()
        val result = receiver.function(written)
        if (write.changes) {
            assertNotSame(receiver, result, "the result")
            assertSameValue(written, result.read(), "the value read back")
        } else {
            assertSame(receiver, result, "the result")
        }
    }
    for ((call, function) in writesElsewhereToo) cell("$name by $call") {
        val written = write.written()
        assertSameValue(if (write.changes) written else receiver.read(), receiver.function(written).read(), "the value read back")
    }
}

/**
 * Writes under every key of every trie shape by a put, by a putAll of a hash map, which a hash map merges trie by
 * trie, and by an entry's setValue. A putAll that writes other keys too, brings the key in a subtree beside a new key,
 * or holds every key and a new one with the key's hash, keeps the stored value when the write changes nothing.
 */
private fun Cells.testValueWrites(kind: String, newMap: (Map<Any, Any?>) -> PersistentMap<Any, Any?>) {
    for ((shape, keys) in trieShapes) for ((position, key) in keys.withIndex()) for (write in valueWrites) {
        val stored = write.stored()
        val content = keys.withIndex().associate { (index, shapeKey) -> shapeKey to IntWrapper(index, index) } + (key to stored)
        testWrites("$kind of $shape, ${write.description} under the key at $position", write, newMap(content), read = { this[key] },
            writes = listOf(
                "putting" to { written -> putting(key, written) },
                "puttingAll of a hash map of the key" to { written -> puttingAll(persistentHashMapOf(key to written)) },
                "puttingAll of a hash map of every key" to { written -> puttingAll((content + (key to written)).toPersistentHashMap()) },
                "a builder's put, which returns the stored value" to { written ->
                    mutate { assertSameValue(stored, it.put(key, written), "what put returns") }
                },
                "a builder's putAll of a hash map of the key" to { written -> mutate { it.putAll(persistentHashMapOf(key to written)) } },
                "a builder's putAll of a hash map of every key" to { written ->
                    mutate { it.putAll((content + (key to written)).toPersistentHashMap()) }
                },
                "an entry's setValue, which returns the stored value" to { written ->
                    mutate { builder -> assertSameValue(stored, builder.entries.first { it.key == key }.setValue(written), "what setValue returns") }
                },
            ),
            writesElsewhereToo = listOf(
                "puttingAll of new values for the other keys" to { written ->
                    puttingAll((keys.associateWith { NEW } + (key to written)).toPersistentHashMap())
                },
                "a builder's putAll of new values for the other keys" to { written ->
                    mutate { it.putAll((keys.associateWith { NEW } + (key to written)).toPersistentHashMap()) }
                },
                "puttingAll of a new key beside it at level 1" to { written ->
                    puttingAll(persistentHashMapOf(key to written, missingBesideAtLevelOne(key) to NEW))
                },
                "a builder's putAll of a new key beside it at level 1" to { written ->
                    mutate { it.putAll(persistentHashMapOf(key to written, missingBesideAtLevelOne(key) to NEW)) }
                },
                "puttingAll of every key and a new key with its hash" to { written ->
                    puttingAll((content + (key to written) + (missingKey(key.hashCode()) to NEW)).toPersistentHashMap())
                },
                "a builder's putAll of every key and a new key with its hash" to { written ->
                    mutate { it.putAll((content + (key to written) + (missingKey(key.hashCode()) to NEW)).toPersistentHashMap()) }
                },
            ),
        )
    }
}
