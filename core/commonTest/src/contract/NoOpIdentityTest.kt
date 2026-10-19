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
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import tests.IntWrapper
import tests.equalCopy
import tests.remove
import tests.trie.collidingKey1
import tests.trie.missingKey
import tests.trie.trieShapes
import kotlin.test.Test
import kotlin.test.assertEquals
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
            val other = if (index == 0) 1 else index - 1
            val list = wrappersOf(size).toPersistentList<Any?>().replacingAt(index, stored)
            testWrites("$depth, ${write.description} at $index", write, list, { this[index] },
                writes = listOf(
                    "replacingAt" to { replacingAt(index, it) },
                    "a builder's set" to { written -> mutate { assertSameValue(stored, it.set(index, written)) } },
                    "the list iterator's set" to { written -> mutate { it.listIterator(index).run { val _ = next(); set(written) } } },
                ),
                writesWithOthers = listOf(
                    "a builder's set after a set at $other" to { written -> mutate { it[other] = NEW; it[index] = written } },
                ),
            )
        }
    }

    @Test
    fun `value writes into a hash map`() = runCells { testValueWrites("a hash map") { persistentHashMapOf(*it.toTypedArray()) } }

    @Test
    fun `value writes into an ordered map`() = runCells { testValueWrites("an ordered map") { persistentMapOf(*it.toTypedArray()) } }
}

private fun <C : Any> Cells.testCalls(
    receivers: List<Pair<String, C>>,
    empty: C,
    noOps: (C) -> List<Call<C>>,
    emptyingCalls: (C) -> List<Call<C>>,
) {
    for ((origin, receiver) in receivers) {
        for ((call, function) in noOps(receiver)) cell("$origin, $call") { assertSame(receiver, receiver.function()) }
        for ((call, function) in emptyingCalls(receiver)) cell("$origin, $call") { assertSame(empty, receiver.function()) }
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
    add("a builder that is only built" to { mutate { } })
    for ((argument, elements) in noElementOf(collection)) {
        add("removingAll($argument)" to { removingAll(elements) })
        add("removeAll($argument)" to { mutate { it.removeAll(elements) } })
    }
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        add("retainingAll($argument)" to { retainingAll(elements) })
        add("retainAll($argument)" to { mutate { it.retainAll(elements) } })
    }
    add("retainingAll(itself)" to { retainingAll(this) })
    add("removing(a missing element)" to { removing("missing") })
    add("remove(a missing element)" to { mutate { it.remove("missing") } })
    add("removingAll { false }" to { removingAll { false } })
    add("removeAll { false }" to { mutate { it.removeAll { false } } })
    add("retainAll { true }" to { mutate { it.retainAll { true } } })
}

private fun <C : PersistentCollection<Any?>> collectionEmptyingCalls(collection: C): List<Call<C>> = buildList {
    for ((argument, elements) in everyElementOf(collection) + supersetsOf(collection)) {
        add("removingAll($argument)" to { removingAll(elements) })
        add("removeAll($argument)" to { mutate { it.removeAll(elements) } })
    }
    for ((argument, elements) in noElementOf(collection)) {
        add("retainingAll($argument)" to { retainingAll(elements) })
        add("retainAll($argument)" to { mutate { it.retainAll(elements) } })
    }
    add("cleared()" to { cleared() })
    add("clear()" to { mutate { it.clear() } })
    add("removingAll { true }" to { removingAll { true } })
    add("removeAll { true }" to { mutate { it.removeAll { true } } })
    add("retainAll { false }" to { mutate { it.retainAll { false } } })
    add("removing every element" to { collection.fold<Any?, PersistentCollection<Any?>>(this) { rest, element -> rest.removing(element) } })
    add("remove of every element" to { mutate { builder -> collection.forEach { builder.remove(it) } } })
    add("the iterator's remove of every element" to { mutate { it.iterator().run { while (hasNext()) { val _ = next(); remove() } } } })
    add("minus an array of every element" to { this - collection.toTypedArray() })
    add("minus a sequence of every element" to { this - collection.asSequence() })
    add("minus an iterable that is not a collection of every element" to { this - Iterable { collection.iterator() } })
}

private fun lists(): List<Pair<String, PersistentList<Any?>>> = listOf("the empty list" to persistentListOf<Any?>()) +
        listDepths.map { (size, depth) -> "a list of $size, $depth" to wrappersOf(size).toPersistentList() }

private fun listNoOps(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionNoOps(list) + buildList {
    add("addingAll(nothing)" to { addingAll(emptyList()) })
    add("addAll(nothing)" to { mutate { it.addAll(emptyList()) } })
    add("addingAllAt(0, nothing)" to { addingAllAt(0, emptyList()) })
    add("addAll(0, nothing)" to { mutate { it.addAll(0, emptyList()) } })
    for (index in boundaryIndices(list.size)) {
        add("replacingAt($index, the stored element)" to { replacingAt(index, list[index]) })
        add("set($index, the stored element)" to { mutate { it[index] = list[index] } })
        add("listIterator($index).set(next())" to { mutate { it.listIterator(index).run { set(next()) } } })
    }
}

private fun listEmptyingCalls(list: PersistentList<Any?>): List<Call<PersistentList<Any?>>> = collectionEmptyingCalls(list) + buildList {
    add("subList(0, size).clear()" to { mutate { it.subList(0, it.size).clear() } })
    add("removingAt(0) of every element" to { list.fold(this) { rest, _ -> rest.removingAt(0) } })
    add("removeAt(0) of every element" to { mutate { while (it.isNotEmpty()) it.removeAt(0) } })
    add("removingAt(lastIndex) of every element" to { list.fold(this) { rest, _ -> rest.removingAt(rest.lastIndex) } })
    add("removeAt(lastIndex) of every element" to { mutate { while (it.isNotEmpty()) it.removeAt(it.lastIndex) } })
}

private fun sets(empty: PersistentSet<Any?>): List<Pair<String, PersistentSet<Any?>>> = listOf("the empty set" to empty) +
        wrappedShapes.map { (shape, elements) -> "a set of $shape" to empty.addingAll(elements) }

private fun setNoOps(set: PersistentSet<Any?>): List<Call<PersistentSet<Any?>>> = collectionNoOps(set) + buildList {
    for ((argument, elements) in everyElementOf(set) + nothing) {
        add("addingAll($argument)" to { addingAll(elements) })
        add("addAll($argument)" to { mutate { it.addAll(elements) } })
    }
    add("addingAll(itself)" to { addingAll(this) })
    add("addAll(the set it was made from)" to { mutate { it.addAll(this) } })
    for (element in set) {
        add("adding($element)" to { adding(element) })
        add("add($element)" to { mutate { it.add(element) } })
        add("adding(a copy of $element)" to { adding(element.equalCopy()) })
        add("add(a copy of $element)" to { mutate { it.add(element.equalCopy()) } })
        add("removing(a missing element with the hash of $element)" to { removing(missingKey(element.hashCode())) })
        add("remove(a missing element with the hash of $element)" to { mutate { it.remove(missingKey(element.hashCode())) } })
        add("removing(a missing element beside $element at level 1)" to { removing(missingBesideAtLevelOne(element)) })
        add("remove(a missing element beside $element at level 1)" to { mutate { it.remove(missingBesideAtLevelOne(element)) } })
    }
}

private fun maps(empty: PersistentMap<Any?, Any?>): List<Pair<String, PersistentMap<Any?, Any?>>> = listOf(
    "the empty map" to empty,
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
    add("a builder that is only built" to { mutate { } })
    for ((argument, entries) in storedEntriesOf(map)) {
        add("puttingAll($argument)" to { puttingAll(entries) })
        add("putAll($argument)" to { mutate { it.putAll(entries) } })
    }
    add("puttingAll(itself)" to { puttingAll(this) })
    add("putAll(the map it was made from)" to { mutate { it.putAll(this) } })
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        add("minus($argument)" to { this - argumentKeys })
        add("keys.removeAll($argument)" to { mutate { it.keys.removeAll(argumentKeys) } })
    }
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        add("keys.retainAll($argument)" to { mutate { it.keys.retainAll(argumentKeys) } })
    }
    add("removing(a missing key)" to { removing("missing") })
    add("remove(a missing key)" to { mutate { it.remove("missing") } })
    add("entries.removeAll { false }" to { mutate { it.entries.removeAll { false } } })
    add("entries.retainAll { true }" to { mutate { it.entries.retainAll { true } } })
    for ((key, value) in map) for ((keyName, sameKey) in listOf("$key" to key, "a copy of $key" to key.equalCopy())) {
        add("putting($keyName, its value)" to { putting(sameKey, value) })
        add("put($keyName, its value)" to { mutate { it[sameKey] = value } })
        add("puttingAll(a hash map of $keyName and its value)" to { puttingAll(persistentHashMapOf(sameKey to value)) })
        add("putAll(a hash map of $keyName and its value)" to { mutate { it.putAll(persistentHashMapOf(sameKey to value)) } })
    }
    for (key in map.keys) {
        add("removing($key, a new value)" to { removing(key, NEW) })
        add("remove($key, a new value)" to { mutate { val _ = it.remove(key, NEW) } })
        add("removing(a missing key with the hash of $key)" to { removing(missingKey(key.hashCode())) })
        add("remove(a missing key with the hash of $key)" to { mutate { it.remove(missingKey(key.hashCode())) } })
        add("removing(a missing key beside $key at level 1)" to { removing(missingBesideAtLevelOne(key)) })
        add("remove(a missing key beside $key at level 1)" to { mutate { it.remove(missingBesideAtLevelOne(key)) } })
    }
}

private fun mapEmptyingCalls(map: PersistentMap<Any?, Any?>): List<Call<PersistentMap<Any?, Any?>>> = buildList {
    for ((argument, argumentKeys) in everyElementOf(map.keys) + supersetsOf(map.keys)) {
        add("minus($argument)" to { this - argumentKeys })
        add("keys.removeAll($argument)" to { mutate { it.keys.removeAll(argumentKeys) } })
    }
    for ((argument, argumentKeys) in noElementOf(map.keys)) {
        add("keys.retainAll($argument)" to { mutate { it.keys.retainAll(argumentKeys) } })
    }
    add("cleared()" to { cleared() })
    add("clear()" to { mutate { it.clear() } })
    add("values.clear()" to { mutate { it.values.clear() } })
    add("entries.removeAll { true }" to { mutate { it.entries.removeAll { true } } })
    add("entries.retainAll { false }" to { mutate { it.entries.retainAll { false } } })
    add("the entries' iterator's remove of every entry" to { mutate { it.entries.iterator().run { while (hasNext()) { val _ = next(); remove() } } } })
    add("removing every key" to { map.keys.fold(this) { rest, key -> rest.removing(key) } })
    add("remove of every key" to { mutate { builder -> map.keys.forEach { builder.remove(it) } } })
    add("removing every key with its value" to { map.entries.fold(this) { rest, (key, value) -> rest.removing(key, value) } })
    add("remove of every key with its value" to { mutate { builder -> map.forEach { (key, value) -> val _ = builder.remove(key, value) } } })
    add("minus an array of every key" to { this - map.keys.toTypedArray() })
    add("minus a sequence of every key" to { this - map.keys.asSequence() })
    add("minus an iterable that is not a collection of every key" to { this - Iterable { map.keys.iterator() } })
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
private fun assertSameValue(expected: Any?, actual: Any?) {
    if (expected is Double && actual is Double) assertEquals(expected.toRawBits(), actual.toRawBits()) else assertSame(expected, actual)
}

/**
 * A call of [writes] returns the receiver when [write] changes nothing, and a new instance holding the written value
 * otherwise. A call of [writesWithOthers] writes elsewhere too, so only what it leaves where [read] reads tells.
 */
private fun <C : Any> Cells.testWrites(
    name: String,
    write: ValueWrite,
    receiver: C,
    read: C.() -> Any?,
    writes: List<Pair<String, C.(written: Any?) -> C>>,
    writesWithOthers: List<Pair<String, C.(written: Any?) -> C>>,
) {
    for ((call, function) in writes) cell("$name by $call") {
        val written = write.written()
        val result = receiver.function(written)
        if (write.changes) {
            assertNotSame(receiver, result)
            assertSameValue(written, result.read())
        } else {
            assertSame(receiver, result)
        }
    }
    for ((call, function) in writesWithOthers) cell("$name by $call") {
        val written = write.written()
        assertSameValue(if (write.changes) written else receiver.read(), receiver.function(written).read())
    }
}

/**
 * Writes under every key of every trie shape by a put, by a putAll of a hash map, which a hash map merges trie by
 * trie, and by an entry's setValue. A putAll that writes other keys too, or brings the key in a subtree beside a new
 * key, keeps the stored value when the write changes nothing.
 */
private fun Cells.testValueWrites(kind: String, newMap: (List<Pair<Any, Any?>>) -> PersistentMap<Any, Any?>) {
    for ((shape, keys) in trieShapes) for ((position, key) in keys.withIndex()) for (write in valueWrites) {
        val stored = write.stored()
        val pairs = keys.mapIndexed { index, shapeKey -> shapeKey to if (index == position) stored else IntWrapper(index, index) }
        fun everyKey(written: Any?) =
            persistentHashMapOf(*pairs.mapIndexed { index, pair -> if (index == position) key to written else pair }.toTypedArray())
        fun newValuesForTheOthers(written: Any?) = persistentHashMapOf(*keys.map { if (it == key) key to written else it to NEW }.toTypedArray())
        fun aNewKeyBeside(written: Any?) = persistentHashMapOf(key to written, missingBesideAtLevelOne(key) to NEW)
        testWrites("$kind of $shape, ${write.description} under the key at $position", write, newMap(pairs), { this[key] },
            writes = listOf(
                "putting" to { putting(key, it) },
                "puttingAll of a hash map of the key" to { puttingAll(persistentHashMapOf(key to it)) },
                "puttingAll of a hash map of every key" to { puttingAll(everyKey(it)) },
                "a builder's put" to { written -> mutate { assertSameValue(stored, it.put(key, written)) } },
                "a builder's putAll of a hash map of the key" to { written -> mutate { it.putAll(persistentHashMapOf(key to written)) } },
                "a builder's putAll of a hash map of every key" to { written -> mutate { it.putAll(everyKey(written)) } },
                "an entry's setValue" to { written ->
                    mutate { builder -> assertSameValue(stored, builder.entries.first { it.key == key }.setValue(written)) }
                },
            ),
            writesWithOthers = listOf(
                "puttingAll of new values for the other keys" to { puttingAll(newValuesForTheOthers(it)) },
                "a builder's putAll of new values for the other keys" to { written -> mutate { it.putAll(newValuesForTheOthers(written)) } },
                "puttingAll of a new key beside it at level 1" to { puttingAll(aNewKeyBeside(it)) },
                "a builder's putAll of a new key beside it at level 1" to { written -> mutate { it.putAll(aNewKeyBeside(written)) } },
            ),
        )
    }
}
