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

/** The iterators a builder opens, each named after its view. */
typealias Views<B, I> = List<Pair<String, B.() -> I>>

/** Runs [test], the assertions of one bug, on a fresh builder of every origin, one cell per origin. */
fun <B> forEachBuilder(builders: Origins<B>, test: (B) -> Unit): Unit = runCells {
    for ((origin, newBuilder) in builders) cell(origin) { test(newBuilder()) }
}

const val TAIL_ONLY = 3
const val FULL_TAIL = MAX_BUFFER_SIZE
const val ONE_LEAF = MAX_BUFFER_SIZE + 8
const val LEAVES = 3 * MAX_BUFFER_SIZE + 4
const val NODES = MAX_BUFFER_SIZE * MAX_BUFFER_SIZE + 76

val listDepths: Map<Int, String> = mapOf(
    TAIL_ONLY to "the tail only",
    FULL_TAIL to "a full tail",
    ONE_LEAF to "a root that is one leaf",
    LEAVES to "a root over leaves",
    NODES to "a root over level-1 nodes",
)

/** The ends of the first leaf, the first level-1 node and the last leaf, the index past each, and the last index. */
fun boundaryIndices(size: Int): List<Int> {
    val trieSize = if (size <= MAX_BUFFER_SIZE) 0 else rootSize(size)
    return listOf(
        0, MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE,
        trieSize - MAX_BUFFER_SIZE, trieSize - 1, trieSize, size - 1,
    ).filter { it in 0..<size }.distinct().sorted()
}

/** The origins of [list], whose undone write puts [other] at the middle index and then the element that was there. */
fun <E> listBuildersOf(list: PersistentList<E>, other: E): Origins<PersistentList.Builder<E>> {
    val size = list.size
    val shape = listDepths[size]?.let { ", $it" }.orEmpty()
    val middle = size / 2
    return listOf(
        "from a list of $size$shape" to { list.builder() },
        "from adds of $size$shape" to { persistentListOf<E>().builder().apply { addAll(list) } },
    ) + if (list.isEmpty()) emptyList() else listOf(
        "from a list of $size with the element at $middle overwritten and restored$shape" to {
            list.builder().apply { this[middle] = other; this[middle] = list[middle] }
        },
    )
}

fun listBuildersOf(size: Int): Origins<PersistentList.Builder<Any>> = listBuildersOf(List<Any>(size) { it }.toPersistentList(), "overwritten")

private fun <K, V> mapBuildersOfKind(
    kind: String,
    entries: List<Pair<K, V>>,
    newMap: (List<Pair<K, V>>) -> PersistentMap<K, V>,
): Origins<PersistentMap.Builder<K, V>> = listOf(
    "$kind from a map" to { newMap(entries).builder() },
    "$kind from puts" to { newMap(emptyList()).builder().apply { for ((key, value) in entries) put(key, value) } },
) + if (entries.isEmpty()) emptyList() else listOf(
    "$kind from a map with its last key removed and put back" to {
        val (key, value) = entries.last()
        newMap(entries).builder().apply { remove(key); put(key, value) }
    },
)

private fun <E> setBuildersOfKind(
    kind: String,
    elements: List<E>,
    newSet: (List<E>) -> PersistentSet<E>,
): Origins<PersistentSet.Builder<E>> = listOf(
    "$kind from a set" to { newSet(elements).builder() },
    "$kind from adds" to { newSet(emptyList()).builder().apply { addAll(elements) } },
) + if (elements.isEmpty()) emptyList() else listOf(
    "$kind from a set with its last element removed and added back" to {
        val last = elements.last()
        newSet(elements).builder().apply { remove(last); add(last) }
    },
)

fun <K, V> hashMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOfKind("a hash map builder", entries) { persistentHashMapOf(*it.toTypedArray()) }
fun <K, V> orderedMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOfKind("an ordered map builder", entries) { persistentMapOf(*it.toTypedArray()) }
fun <E> hashSetBuildersOf(elements: List<E>) = setBuildersOfKind("a hash set builder", elements) { it.toPersistentHashSet() }
fun <E> orderedSetBuildersOf(elements: List<E>) = setBuildersOfKind("an ordered set builder", elements) { it.toPersistentSet() }

/** Both kinds of map builder, for a bug of neither kind in particular. */
fun <K, V> mapBuildersOf(vararg entries: Pair<K, V>): Origins<PersistentMap.Builder<K, V>> =
    hashMapBuildersOf(entries.toList()) + orderedMapBuildersOf(entries.toList())

/**
 * Lists of the tail alone and of a leaf and a tail, and one over several leaves that owns its root but not its first
 * leaf, so that a set there copies a shared leaf under an owned root.
 */
val listBuilders: Origins<PersistentList.Builder<Any>> = listBuildersOf(TAIL_ONLY) + listBuildersOf(ONE_LEAF) + listBuildersOf(LEAVES).last()

private fun <B> ofEveryShape(buildersOf: (keys: List<Any>) -> Origins<B>): Origins<B> =
    trieShapes.flatMap { (shape, keys) -> buildersOf(keys).map { (origin, newBuilder) -> "$origin, $shape" to newBuilder } }

val hashMapBuilders = ofEveryShape { keys -> hashMapBuildersOf(keys.mapIndexed { index, key -> key to "v$index" }) }
val orderedMapBuilders = ofEveryShape { keys -> orderedMapBuildersOf(keys.mapIndexed { index, key -> key to "v$index" }) }
val hashSetBuilders = ofEveryShape(::hashSetBuildersOf)
val orderedSetBuilders = ofEveryShape(::orderedSetBuildersOf)

/*
 * Builders holding nothing: one never filled, and ones whose elements went by removes, by a removeAll and by a clear.
 * They held a leaf and a tail, so that a list's removeAll empties a trie too.
 */

private val removedElements = List<Any>(ONE_LEAF) { it }

private fun <B : MutableCollection<Any>> emptyBuildersOf(kind: String, builderOf: (List<Any>) -> B): Origins<B> = listOf(
    "$kind never filled" to { builderOf(emptyList()) },
    "$kind emptied by remove" to { builderOf(removedElements).apply { for (element in removedElements) remove(element) } },
    "$kind emptied by removeAll" to { builderOf(removedElements).apply { removeAll(removedElements) } },
    "$kind emptied by clear" to { builderOf(removedElements).apply { clear() } },
)

private fun emptyMapBuildersOf(
    kind: String,
    newMap: (List<Pair<Any, String>>) -> PersistentMap<Any, String>,
): Origins<PersistentMap.Builder<Any, String>> {
    val entries = removedElements.map { it to "v$it" }
    return listOf(
        "$kind never filled" to { newMap(emptyList()).builder() },
        "$kind emptied by remove" to { newMap(entries).builder().apply { for (key in removedElements) remove(key) } },
        "$kind emptied by the keys' removeAll" to { newMap(entries).builder().apply { keys.removeAll(removedElements) } },
        "$kind emptied by clear" to { newMap(entries).builder().apply { clear() } },
    )
}

val emptyListBuilders = emptyBuildersOf("a list builder") { it.toPersistentList().builder() }
val emptyHashSetBuilders = emptyBuildersOf("a hash set builder") { it.toPersistentHashSet().builder() }
val emptyOrderedSetBuilders = emptyBuildersOf("an ordered set builder") { it.toPersistentSet().builder() }
val emptyHashMapBuilders = emptyMapBuildersOf("a hash map builder") { persistentHashMapOf(*it.toTypedArray()) }
val emptyOrderedMapBuilders = emptyMapBuildersOf("an ordered map builder") { persistentMapOf(*it.toTypedArray()) }

val listViews: Views<PersistentList.Builder<Any>, MutableIterator<*>> = listOf("iterator" to { iterator() })
val listIteratorViews: Views<PersistentList.Builder<Any>, MutableListIterator<Any>> = listOf("listIterator" to { listIterator() })
val setViews: Views<PersistentSet.Builder<Any>, MutableIterator<*>> = listOf("iterator" to { iterator() })
val mapViews: Views<PersistentMap.Builder<Any, String>, MutableIterator<*>> = listOf(
    "entries" to { entries.iterator() },
    "keys" to { keys.iterator() },
    "values" to { values.iterator() },
)

/** The first row of every table: it leaves the builder alone, so that the calls after it are tested on their own. */
val noCall: BuilderOperation<Any?> = BuilderOperation("no call", throwsCME = false) { }

val listOperations: List<BuilderOperation<PersistentList.Builder<Any>>> = listOf(
    noCall,

    BuilderOperation("add()") { add("new") },
    BuilderOperation("add(0)") { add(0, "new") },
    BuilderOperation("removeAt(0)") { removeAt(0) },
    BuilderOperation("removeAt(lastIndex)") { removeAt(lastIndex) },
    BuilderOperation("removeAt(lastIndex) twice") { removeAt(lastIndex); removeAt(lastIndex) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("addAll()") { addAll(listOf("new", "newer")) },
    BuilderOperation("removeAll(the first element)") { removeAll(listOf(first())) },
    BuilderOperation("retainAll(the first element)") { retainAll(listOf(first())) },
    BuilderOperation("add() then removeAt(lastIndex)") { add("new"); removeAt(lastIndex) },
    BuilderOperation("iterator().remove()") { iterator().apply { val _ = next(); remove() } },

    BuilderOperation("set(1)", throwsCME = false) { this[1] = "changed" },
    BuilderOperation("set(lastIndex)", throwsCME = false) { this[lastIndex] = "changed" },
    BuilderOperation("set(the stored element)", throwsCME = false) { this[1] = this[1] },
    BuilderOperation("addAll(an empty list)", throwsCME = false) { addAll(emptyList()) },
    BuilderOperation("removeAll(missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
    BuilderOperation("retainAll(a superset)", throwsCME = false) { retainAll(toList() + "missing") },
    BuilderOperation("build()", throwsCME = false) { val _ = build() },
    BuilderOperation("build() then set(1)", throwsCME = false) { val _ = build(); this[1] = "changed" },
)

private fun newValuesFor(keys: List<Any>): PersistentMap<Any, String> =
    persistentHashMapOf(*keys.mapIndexed { index, key -> key to "new $index" }.toTypedArray())

/** For the hash and the ordered map builders; a hash map argument takes the hash builder's trie-by-trie path. */
val mapOperations: List<BuilderOperation<PersistentMap.Builder<Any, String>>> = listOf(
    noCall,

    BuilderOperation("put(new key)") { put("new", "n") },
    BuilderOperation("remove(the first key)") { remove(keys.first()) },
    BuilderOperation("remove(the last key)") { remove(keys.last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("putAll(a map with a new key)") { putAll(mapOf("new" to "n")) },
    BuilderOperation("putAll(a hash map with a new key)") { putAll(persistentHashMapOf("new" to "n")) },
    BuilderOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },
    BuilderOperation("remove(every key)") { for (key in keys.toList()) remove(key) },
    BuilderOperation("entries.iterator().remove()") { entries.iterator().apply { val _ = next(); remove() } },
    BuilderOperation("keys.iterator().remove()") { keys.iterator().apply { val _ = next(); remove() } },
    BuilderOperation("entries.remove(the first entry)") { entries.remove(mutableMapOf(keys.first() to getValue(keys.first())).entries.first()) },
    BuilderOperation("keys.remove(the first key)") { keys.remove(keys.first()) },

    BuilderOperation("put(a new value for the first key)", throwsCME = false) { put(keys.first(), "changed") },
    BuilderOperation("put(a new value for the last key)", throwsCME = false) { put(keys.last(), "changed") },
    BuilderOperation("put(the stored value of the first key)", throwsCME = false) { put(keys.first(), getValue(keys.first())) },
    BuilderOperation("put(the stored value of the last key)", throwsCME = false) { put(keys.last(), getValue(keys.last())) },
    BuilderOperation("putAll(a new value for the first key)", throwsCME = false) { putAll(persistentHashMapOf(keys.first() to "changed")) },
    BuilderOperation("putAll(a new value for the last key)", throwsCME = false) { putAll(persistentHashMapOf(keys.last() to "changed")) },
    BuilderOperation("putAll(new values)", throwsCME = false) { putAll(newValuesFor(keys.toList())) },
    BuilderOperation("putAll(new values in the reverse key order)", throwsCME = false) { putAll(newValuesFor(keys.reversed())) },
    BuilderOperation("putAll(new values as a map)", throwsCME = false) { putAll(mapValues { it.value + "!" }) },
    BuilderOperation("putAll(the stored values)", throwsCME = false) { putAll(persistentHashMapOf(*toList().toTypedArray())) },
    BuilderOperation("putAll(the stored values as a map)", throwsCME = false) { putAll(toMap()) },
    BuilderOperation("putAll(the built map)", throwsCME = false) { putAll(build()) },
    BuilderOperation("putAll(the builder itself)", throwsCME = false) { putAll(this) },
    BuilderOperation("putAll(an empty map)", throwsCME = false) { putAll(emptyMap()) },
    BuilderOperation("putAll(an empty hash map)", throwsCME = false) { putAll(persistentHashMapOf<Any, String>()) },
    BuilderOperation("remove(a missing key)", throwsCME = false) { remove("missing") },
    BuilderOperation("entries.remove(the first key with another value)", throwsCME = false) { entries.remove(mutableMapOf(keys.first() to "other").entries.first()) },
    BuilderOperation("build() then put(a new value for the first key)", throwsCME = false) { val _ = build(); put(keys.first(), "changed") },
)

/** For the hash and the ordered set builders; a hash set argument takes the hash builder's trie-by-trie path. */
val setOperations: List<BuilderOperation<PersistentSet.Builder<Any>>> = listOf(
    noCall,

    BuilderOperation("add(a new element)") { add("new") },
    BuilderOperation("remove(the first element)") { remove(first()) },
    BuilderOperation("remove(the last element)") { remove(last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("addAll(a hash set with a new element)") { addAll(persistentHashSetOf("new")) },
    BuilderOperation("addAll(a list with a stored and a new element)") { addAll(listOf(first(), "new")) },
    BuilderOperation("removeAll(a hash set holding the first element)") { removeAll(persistentHashSetOf(first())) },
    BuilderOperation("removeAll(a list holding the first element)") { removeAll(listOf(first())) },
    BuilderOperation("removeAll(the built set)") { removeAll(build()) },
    BuilderOperation("retainAll(a hash set holding only the first element)") { retainAll(persistentHashSetOf(first())) },
    BuilderOperation("retainAll(a list holding only the first element)") { retainAll(listOf(first())) },
    BuilderOperation("retainAll(a hash set of missing elements)") { retainAll(persistentHashSetOf("missing")) },
    BuilderOperation("add(a new element) then remove(it)") { add("new"); remove("new") },
    BuilderOperation("remove(every element)") { for (element in toList()) remove(element) },
    BuilderOperation("iterator().remove()") { iterator().apply { val _ = next(); remove() } },

    BuilderOperation("add(a stored element)", throwsCME = false) { add(first()) },
    BuilderOperation("add(an equal copy of a stored element)", throwsCME = false) { add(first().equalCopy()) },
    BuilderOperation("remove(a missing element)", throwsCME = false) { remove("missing") },
    BuilderOperation("addAll(the built set)", throwsCME = false) { addAll(build()) },
    BuilderOperation("addAll(the stored elements as a list)", throwsCME = false) { addAll(toList()) },
    BuilderOperation("addAll(an equal hash set)", throwsCME = false) { addAll(toList().toPersistentHashSet()) },
    BuilderOperation("removeAll(a hash set of missing elements)", throwsCME = false) { removeAll(persistentHashSetOf("missing")) },
    BuilderOperation("removeAll(a list of missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
    BuilderOperation("retainAll(a superset as a hash set)", throwsCME = false) { retainAll((toList() + "missing").toPersistentHashSet()) },
    BuilderOperation("retainAll(a superset as a list)", throwsCME = false) { retainAll(toList() + "missing") },
)
