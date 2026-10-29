/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.implementations.immutableList.rootSize
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import tests.equalCopy
import tests.trie.trieShapes

/**
 * Fresh builders, each named after where it comes from. A factory such as [listBuildersOf] or [hashMapBuildersOf] gives
 * a builder from a persistent collection, which shares every node with it, one from adds, which owns every node (an
 * addAll or putAll of a persistent hash collection would adopt that collection's nodes instead), and one from a
 * persistent collection with a write undone, which owns the path of that write and shares the rest; all of them hold
 * the content in the same order. A new bug is covered on every origin of its kind, through [forEachBuilder] or, for a
 * call through the builder or one of its views, as a new row of [listOperations], [mapOperations] or [setOperations].
 */
typealias Origins<B> = List<Pair<String, () -> B>>

/** The iterators a builder opens, each named after its view, with the calls [testIterator] makes on them. */
class Views<B, I : MutableIterator<*>>(val calls: List<Call<I>>, val openers: List<Pair<String, B.() -> I>>)

fun <B, I : MutableIterator<*>> views(calls: List<Call<I>>, openers: Rows<B.() -> I>.() -> Unit): Views<B, I> = Views(calls, rows(openers))

typealias ListBuilder = PersistentList.Builder<Any>
typealias SetBuilder = PersistentSet.Builder<Any?>
typealias MapBuilder = PersistentMap.Builder<Any?, String>

/** Runs [test], the assertions of one bug, on a fresh builder of every origin, one cell per origin. */
fun <B> forEachBuilder(builders: Origins<B>, test: (B) -> Unit): Unit = runCells {
    for ((origin, newBuilder) in builders) cell(origin) { test(newBuilder()) }
}

const val TAIL_ONLY = 3
const val FULL_TAIL = MAX_BUFFER_SIZE
const val ONE_LEAF = MAX_BUFFER_SIZE + 8
const val LEAVES = 3 * MAX_BUFFER_SIZE + 4
const val NODES = MAX_BUFFER_SIZE * MAX_BUFFER_SIZE + 76

val listShapes: Map<Int, String> = mapOf(
    TAIL_ONLY to "the tail only",
    FULL_TAIL to "a full tail",
    FULL_TAIL + 1 to "a root that is one leaf and a tail of one",
    ONE_LEAF to "a root that is one leaf",
    2 * FULL_TAIL to "a root that is one leaf and a full tail",
    2 * FULL_TAIL + 1 to "a root over two leaves and a tail of one",
    LEAVES to "a root over leaves",
    NODES to "a root over level-1 nodes",
)

/** How many elements of a list of [size] its trie holds; up to a full tail, the tail holds them all. */
fun trieSize(size: Int): Int = if (size <= MAX_BUFFER_SIZE) 0 else rootSize(size)

/** The ends of the first leaf, the first level-1 node and the last leaf, the index past each, and the last index. */
fun boundaryIndices(size: Int): List<Int> {
    val trieSize = trieSize(size)
    return listOf(
        0, MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE,
        trieSize - MAX_BUFFER_SIZE, trieSize - 1, trieSize, size - 1,
    ).filter { it in 0..<size }.distinct().sorted()
}

/** The origins of [list], whose undone write puts [other] at the middle index and then the element that was there. */
fun <E> listBuildersOf(list: PersistentList<E>, other: E): Origins<PersistentList.Builder<E>> {
    val size = list.size
    val shape = listShapes[size]?.let { ", $it" }.orEmpty()
    val middle = size / 2
    return rows {
        "from a list of $size$shape" { list.builder() }
        "from adds of $size$shape" { persistentListOf<E>().builder().apply { addAll(list) } }
        if (list.isNotEmpty()) "from a list of $size with the element at $middle overwritten and restored$shape" {
            list.builder().apply { this[middle] = other; this[middle] = list[middle] }
        }
    }
}

fun listBuildersOf(size: Int): Origins<ListBuilder> = listBuildersOf(List<Any>(size) { it }.toPersistentList(), "overwritten")

private fun <K, V> mapBuildersOfKind(
    kind: String,
    entries: List<Pair<K, V>>,
    newMap: (List<Pair<K, V>>) -> PersistentMap<K, V>,
): Origins<PersistentMap.Builder<K, V>> = rows {
    "$kind from a map" { newMap(entries).builder() }
    "$kind from puts" { newMap(emptyList()).builder().apply { for ((key, value) in entries) put(key, value) } }
    if (entries.isNotEmpty()) "$kind from a map with its last key removed and put back" {
        val (key, value) = entries.last()
        newMap(entries).builder().apply { remove(key); put(key, value) }
    }
}

private fun <E> setBuildersOfKind(
    kind: String,
    elements: List<E>,
    newSet: (List<E>) -> PersistentSet<E>,
): Origins<PersistentSet.Builder<E>> = rows {
    "$kind from a set" { newSet(elements).builder() }
    "$kind from adds" { newSet(emptyList()).builder().apply { addAll(elements) } }
    if (elements.isNotEmpty()) "$kind from a set with its last element removed and added back" {
        val last = elements.last()
        newSet(elements).builder().apply { remove(last); add(last) }
    }
}

fun <K, V> hashMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOfKind("a hash map builder", entries) { persistentHashMapOf(*it.toTypedArray()) }
fun <K, V> orderedMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOfKind("an ordered map builder", entries) { persistentMapOf(*it.toTypedArray()) }
fun <E> hashSetBuildersOf(elements: List<E>) = setBuildersOfKind("a hash set builder", elements) { it.toPersistentHashSet() }
fun <E> orderedSetBuildersOf(elements: List<E>) = setBuildersOfKind("an ordered set builder", elements) { it.toPersistentSet() }

/** Both kinds of map builder, for a bug of neither kind in particular. */
fun <K, V> mapBuildersOf(vararg entries: Pair<K, V>): Origins<PersistentMap.Builder<K, V>> =
    hashMapBuildersOf(entries.toList()) + orderedMapBuildersOf(entries.toList())

/**
 * Lists of the tail alone and of a leaf and a tail; the last origin of a list over several leaves, which owns its root
 * but not its first leaf, so that a set there copies a shared leaf under an owned root; and the lists where a write
 * changes the shape of the trie, each from a persistent list: a remove from a tail of one pulls a leaf back into the
 * tail and drops or demotes the root, an add to a full tail pushes it into the trie and creates or promotes the root.
 */
val listBuilders: Origins<ListBuilder> = listBuildersOf(TAIL_ONLY) + listBuildersOf(ONE_LEAF) + listBuildersOf(LEAVES).last() +
    listOf(FULL_TAIL, FULL_TAIL + 1, 2 * FULL_TAIL, 2 * FULL_TAIL + 1).map { listBuildersOf(it).first() }

private fun <B> ofEveryShape(buildersOf: (keys: List<Any?>) -> Origins<B>): Origins<B> =
    trieShapes.flatMap { (shape, keys) -> buildersOf(keys).map { (origin, newBuilder) -> "$origin, $shape" to newBuilder } }

private fun valuesFor(keys: List<Any?>): List<Pair<Any?, String>> = keys.mapIndexed { index, key -> key to "v$index" }

val hashMapBuilders = ofEveryShape { keys -> hashMapBuildersOf(valuesFor(keys)) }
val orderedMapBuilders = ofEveryShape { keys -> orderedMapBuildersOf(valuesFor(keys)) }
val hashSetBuilders = ofEveryShape(::hashSetBuildersOf)
val orderedSetBuilders = ofEveryShape(::orderedSetBuildersOf)

private val removedElements = List<Any>(ONE_LEAF) { it }

/**
 * Builders of [kind] holding nothing: one never filled, and ones emptied by removes, by a removeAll and by a clear
 * through [elements], the collection the elements leave by: the builder itself, or the keys of a map builder. They held
 * a leaf and a tail, so that a list's removeAll empties a trie too.
 */
private fun <B> emptyBuildersOf(kind: String, elements: B.() -> MutableCollection<in Any>, builderOf: (List<Any>) -> B): Origins<B> = rows {
    "$kind never filled" { builderOf(emptyList()) }
    "$kind emptied by remove" { builderOf(removedElements).apply { for (element in removedElements) elements().remove(element) } }
    "$kind emptied by removeAll" { builderOf(removedElements).apply { elements().removeAll(removedElements) } }
    "$kind emptied by clear" { builderOf(removedElements).apply { elements().clear() } }
}

private fun <B : MutableCollection<in Any>> emptyBuildersOf(kind: String, builderOf: (List<Any>) -> B): Origins<B> = emptyBuildersOf(kind, { this }, builderOf)

val emptyListBuilders = emptyBuildersOf("a list builder") { it.toPersistentList().builder() }
val emptySetBuilders = emptyBuildersOf("a hash set builder") { it.toPersistentHashSet<Any?>().builder() } +
    emptyBuildersOf("an ordered set builder") { it.toPersistentSet<Any?>().builder() }
val emptyMapBuilders = emptyBuildersOf("a hash map builder", elements = { keys }) { persistentHashMapOf(*valuesFor(it).toTypedArray()).builder() } +
    emptyBuildersOf("an ordered map builder", elements = { keys }) { persistentMapOf(*valuesFor(it).toTypedArray()).builder() }

val listViews: Views<ListBuilder, MutableIterator<*>> = views(iteratorCalls) { "iterator" { iterator() } }
val listIteratorViews: Views<ListBuilder, MutableListIterator<Any>> = views(listIteratorCalls) { "listIterator" { listIterator() } }
val setViews: Views<SetBuilder, MutableIterator<*>> = views(iteratorCalls) { "iterator" { iterator() } }
val mapViews: Views<MapBuilder, MutableIterator<*>> = views(iteratorCalls) {
    "entries" { entries.iterator() }
    "keys" { keys.iterator() }
    "values" { values.iterator() }
}

/** For builders of two elements or more, which set(1) and the second removeAt(lastIndex) need. */
val listOperations: List<BuilderOperation<ListBuilder>> = operations(
    thatKeepTheSize = {
        "no call" { }
        "set(1)" { this[1] = "changed" }
        "set(lastIndex)" { this[lastIndex] = "changed" }
        "set(the stored element)" { this[1] = this[1] }
        "addAll(an empty list)" { addAll(emptyList()) }
        "removeAll(missing elements)" { removeAll(listOf("missing")) }
        "retainAll(a superset)" { retainAll(toList() + "missing") }
        "build()" { val _ = build() }
        "build() then set(1)" { val _ = build(); this[1] = "changed" }
    },
    thatMoveTheSize = {
        "add()" { add("new") }
        "add(0)" { add(0, "new") }
        "removeAt(0)" { removeAt(0) }
        "removeAt(lastIndex)" { removeAt(lastIndex) }
        "removeAt(lastIndex) twice" { removeAt(lastIndex); removeAt(lastIndex) }
        "clear()" { clear() }
        "addAll()" { addAll(listOf("new", "newer")) }
        "removeAll(the first element)" { removeAll(listOf(first())) }
        "retainAll(the first element)" { retainAll(listOf(first())) }
        "add() then removeAt(lastIndex)" { add("new"); removeAt(lastIndex) }
        "iterator().remove()" { iterator().apply { val _ = next(); remove() } }
    },
)

private fun newValuesFor(keys: List<Any?>): PersistentMap<Any?, String> =
    persistentHashMapOf(*keys.mapIndexed { index, key -> key to "new $index" }.toTypedArray())

/** An entry of its own with equal copies of [key] and [value], since entries.remove takes a MutableEntry and not a pair. */
private fun entryOf(key: Any?, value: String): MutableMap.MutableEntry<Any?, String> =
    mutableMapOf(key.equalCopy() to value.toCharArray().concatToString()).entries.first()

/** For the hash and the ordered map builders; a hash map argument takes the hash builder's trie-by-trie path. */
val mapOperations: List<BuilderOperation<MapBuilder>> = operations(
    thatKeepTheSize = {
        "no call" { }
        "put(a new value for the first key)" { put(keys.first(), "changed") }
        "put(a new value for the last key)" { put(keys.last(), "changed") }
        "put(the stored value of the first key)" { put(keys.first(), getValue(keys.first())) }
        "put(the stored value of the last key)" { put(keys.last(), getValue(keys.last())) }
        "putAll(a new value for the first key)" { putAll(persistentHashMapOf(keys.first() to "changed")) }
        "putAll(a new value for the last key)" { putAll(persistentHashMapOf(keys.last() to "changed")) }
        "putAll(new values)" { putAll(newValuesFor(keys.toList())) }
        "putAll(new values in the reverse key order)" { putAll(newValuesFor(keys.reversed())) }
        "putAll(new values with the last two keys swapped)" { putAll(newValuesFor(keys.toList().let { it.dropLast(2) + it.takeLast(2).reversed() })) }
        "putAll(new values as a map)" { putAll(mapValues { it.value + "!" }) }
        "putAll(the stored values)" { putAll(persistentHashMapOf(*toList().toTypedArray())) }
        "putAll(the stored values as a map)" { putAll(toMap()) }
        "putAll(the built map)" { putAll(build()) }
        "putAll(the builder itself)" { putAll(this) }
        "putAll(an empty map)" { putAll(emptyMap()) }
        "putAll(an empty hash map)" { putAll(persistentHashMapOf<Any?, String>()) }
        "remove(a missing key)" { remove("missing") }
        "entries.remove(the first key with another value)" { entries.remove(entryOf(keys.first(), "other")) }
        "build() then put(a new value for the first key)" { val _ = build(); put(keys.first(), "changed") }
    },
    thatMoveTheSize = {
        "put(new key)" { put("new", "n") }
        "remove(the first key)" { remove(keys.first()) }
        "remove(the last key)" { remove(keys.last()) }
        "clear()" { clear() }
        "putAll(a map with a new key)" { putAll(mapOf("new" to "n")) }
        "putAll(a hash map with a new key)" { putAll(persistentHashMapOf("new" to "n")) }
        "put(new key) then remove(it)" { put("new", "n"); remove("new") }
        "remove(every key)" { for (key in keys.toList()) remove(key) }
        "entries.iterator().remove()" { entries.iterator().apply { val _ = next(); remove() } }
        "keys.iterator().remove()" { keys.iterator().apply { val _ = next(); remove() } }
        "entries.remove(an equal copy of the first entry)" { entries.remove(entryOf(keys.first(), getValue(keys.first()))) }
        "keys.remove(the first key)" { keys.remove(keys.first()) }
    },
)

/**
 * For the hash and the ordered set builders of two elements or more, which the rows that retain only the first element
 * need; a hash set argument takes the hash builder's trie-by-trie path.
 */
val setOperations: List<BuilderOperation<SetBuilder>> = operations(
    thatKeepTheSize = {
        "no call" { }
        "add(a stored element)" { add(first()) }
        "add(an equal copy of a stored element)" { add(first().equalCopy()) }
        "remove(a missing element)" { remove("missing") }
        "addAll(the built set)" { addAll(build()) }
        "addAll(the stored elements as a list)" { addAll(toList()) }
        "addAll(an equal hash set)" { addAll(toList().toPersistentHashSet()) }
        "removeAll(a hash set of missing elements)" { removeAll(persistentHashSetOf("missing")) }
        "removeAll(a list of missing elements)" { removeAll(listOf("missing")) }
        "retainAll(a superset as a hash set)" { retainAll((toList() + "missing").toPersistentHashSet()) }
        "retainAll(a superset as a list)" { retainAll(toList() + "missing") }
    },
    thatMoveTheSize = {
        "add(a new element)" { add("new") }
        "remove(the first element)" { remove(first()) }
        "remove(the last element)" { remove(last()) }
        "clear()" { clear() }
        "addAll(a hash set with a new element)" { addAll(persistentHashSetOf("new")) }
        "addAll(a list with a stored and a new element)" { addAll(listOf(first(), "new")) }
        "removeAll(a hash set holding the first element)" { removeAll(persistentHashSetOf(first())) }
        "removeAll(a list holding the first element)" { removeAll(listOf(first())) }
        "removeAll(the built set)" { removeAll(build()) }
        "retainAll(a hash set holding only the first element)" { retainAll(persistentHashSetOf(first())) }
        "retainAll(a list holding only the first element)" { retainAll(listOf(first())) }
        "retainAll(a hash set of missing elements)" { retainAll(persistentHashSetOf("missing")) }
        "add(a new element) then remove(it)" { add("new"); remove("new") }
        "remove(every element)" { for (element in toList()) remove(element) }
        "iterator().remove()" { iterator().apply { val _ = next(); remove() } }
    },
)
