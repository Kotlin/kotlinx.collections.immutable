/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import tests.trie.trieShapes

/*
 * The builders the family tests run on, the iterators they open on them and the calls they make through them.
 *
 * A builder's origin decides who owns the nodes its first write touches, which is where the builder bugs live:
 * - from a persistent collection, the builder shares every node, and its first write copies;
 * - from adds, one element at a time, it owns every node and writes in place (an addAll or putAll of a persistent
 *   collection into an empty builder would adopt the argument's nodes, owned by nobody);
 * - from a persistent collection with a write, it owns the path the write copied and shares the rest; the two such
 *   origins write at different places, so they own different paths where the shape has more than one.
 * A new row in a table below costs, for each origin and view, 2 x size + 3 fresh builders per iterator call, plus
 * 2 x size + 2 when the row keeps the size.
 */

/** One list size for each shape of the trie. */
internal const val TAIL_ONLY = 3
internal const val FULL_TAIL = MAX_BUFFER_SIZE
internal const val ONE_LEAF = MAX_BUFFER_SIZE + 8
internal const val LEAVES = 3 * MAX_BUFFER_SIZE + 4
internal const val NODES = MAX_BUFFER_SIZE * MAX_BUFFER_SIZE + 76

/** The first index, the indices around the first leaf and level boundaries, and the last index of a list of [size]. */
internal fun boundaryIndices(size: Int): List<Int> =
    listOf(0, MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE, size - 1)
        .filter { it in 0..<size }
        .distinct()

/** The shapes of the list trie by size, for the tests that run at every depth. */
internal val listDepths: Map<Int, String> = mapOf(
    TAIL_ONLY to "the tail only",
    FULL_TAIL to "a full tail",
    ONE_LEAF to "a root that is one leaf",
    LEAVES to "a root over leaves",
    NODES to "a root over level-1 nodes",
)

/** Runs [test] on a fresh builder of every origin in [builders], one cell per origin; the message names the origin. */
fun <B> forEachBuilder(builders: List<Pair<String, () -> B>>, test: (message: String, builder: B) -> Unit): Unit = runCells {
    for ((origin, newBuilder) in builders) cell(origin) { test(origin, newBuilder()) }
}

/** Builders of a list of one of the [listDepths] sizes, each element equal to its index: from a list, which shares its leaves, and from adds, which own theirs. */
fun listBuildersOf(size: Int): List<Pair<String, () -> PersistentList.Builder<Int>>> {
    val elements = List(size) { it }
    val shape = listDepths.getValue(size)
    return listOf(
        "from a list of $size, $shape" to { elements.toPersistentList().builder() },
        "from adds of $size, $shape" to { persistentListOf<Int>().builder().apply { addAll(elements) } },
    )
}

/** Builders of [entries]: from the map [mapOf] makes of them, which shares its nodes, and from puts, which own theirs. */
private fun <K, V> mapBuildersOf(
    entries: List<Pair<K, V>>,
    mapOf: (List<Pair<K, V>>) -> PersistentMap<K, V>,
): List<Pair<String, () -> PersistentMap.Builder<K, V>>> = listOf(
    "from a map" to { mapOf(entries).builder() },
    "from puts" to { mapOf(emptyList()).builder().apply { for ((key, value) in entries) set(key, value) } },
)

/** Builders of [elements]: from the set [setOf] makes of them, which shares its nodes, and from adds, which own theirs. */
private fun <E> setBuildersOf(elements: List<E>, setOf: (List<E>) -> PersistentSet<E>): List<Pair<String, () -> PersistentSet.Builder<E>>> = listOf(
    "from a set" to { setOf(elements).builder() },
    "from adds" to { setOf(emptyList()).builder().apply { addAll(elements) } },
)

fun <K, V> hashMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOf(entries) { persistentHashMapOf(*it.toTypedArray()) }
fun <K, V> orderedMapBuildersOf(entries: List<Pair<K, V>>) = mapBuildersOf(entries) { persistentMapOf(*it.toTypedArray()) }
fun <E> hashSetBuildersOf(elements: List<E>) = setBuildersOf(elements) { it.toPersistentHashSet() }
fun <E> orderedSetBuildersOf(elements: List<E>) = setBuildersOf(elements) { it.toPersistentSet() }

/** List builders of the tail alone and of a leaf with a tail. */
val listBuilders: List<Pair<String, () -> PersistentList.Builder<Any>>> = listOf(TAIL_ONLY, FULL_TAIL + 1).flatMap { size ->
    val elements = List<Any>(size) { it }
    listOf(
        "from a list of $size" to { elements.toPersistentList().builder() },
        "from adds of $size" to { persistentListOf<Any>().builder().apply { addAll(elements) } },
        "from a list of $size with a set of the first element" to { elements.toPersistentList().builder().apply { this[0] = "written" } },
        "from a list of $size with a set of the last element" to { elements.toPersistentList().builder().apply { this[lastIndex] = "written" } },
    )
}

/**
 * Map builders of every trie shape from the maps [mapOf] makes, holding "v0", "v1", ... in the shape's key order; the
 * writes put a new value for the first key and for the last.
 */
private fun mapBuilders(mapOf: (List<Pair<Any, String>>) -> PersistentMap<Any, String>): List<Pair<String, () -> PersistentMap.Builder<Any, String>>> =
    trieShapes.flatMap { (shape, keys) ->
        val entries = keys.mapIndexed { index, key -> key to "v$index" }
        val first = keys.first()
        val last = keys.last()
        listOf(
            "from a map of $shape" to { mapOf(entries).builder() },
            "from puts of $shape" to { mapOf(emptyList()).builder().apply { for ((key, value) in entries) set(key, value) } },
            "from a map of $shape with a put of the first key" to { mapOf(entries).builder().apply { set(first, "put") } },
            "from a map of $shape with a put of the last key" to { mapOf(entries).builder().apply { set(last, "put") } },
        )
    }

/** An element no shape holds, which a set's write adds and removes again. */
private const val SPARE = "spare"

/**
 * Set builders of every trie shape from the sets [setOf] makes, in the shape's element order; the writes add the
 * [SPARE] element and remove it, and remove the last element and add it back.
 */
private fun setBuilders(setOf: (List<Any>) -> PersistentSet<Any>): List<Pair<String, () -> PersistentSet.Builder<Any>>> =
    trieShapes.flatMap { (shape, elements) ->
        val last = elements.last()
        listOf(
            "from a set of $shape" to { setOf(elements).builder() },
            "from adds of $shape" to { setOf(emptyList()).builder().apply { addAll(elements) } },
            "from a set of $shape with an add and a remove of a spare element" to { setOf(elements).builder().apply { add(SPARE); remove(SPARE) } },
            "from a set of $shape with a remove and an add of the last element" to { setOf(elements).builder().apply { remove(last); add(last) } },
        )
    }

val hashMapBuilders = mapBuilders { persistentHashMapOf(*it.toTypedArray()) }
val orderedMapBuilders = mapBuilders { persistentMapOf(*it.toTypedArray()) }
val hashSetBuilders = setBuilders { it.toPersistentHashSet() }
val orderedSetBuilders = setBuilders { it.toPersistentSet() }

val listViews: List<Pair<String, PersistentList.Builder<Any>.() -> MutableIterator<*>>> = listOf(
    "iterator" to { iterator() },
    "listIterator" to { listIterator() },
)

val mapViews: List<Pair<String, PersistentMap.Builder<Any, String>.() -> MutableIterator<*>>> = listOf(
    "entries" to { entries.iterator() },
    "keys" to { keys.iterator() },
    "values" to { values.iterator() },
)

val setViews: List<Pair<String, PersistentSet.Builder<Any>.() -> MutableIterator<*>>> = listOf(
    "iterator" to { iterator() },
)

/*
 * The calls a test makes through each builder behind a live iterator, as stdlib's CollectionOperation tables: the
 * calls that move the size first, then those that keep it (throwsCME = false).
 */

val listOperations: List<BuilderOperation<PersistentList.Builder<Any>>> = listOf(
    BuilderOperation("no call", throwsCME = false) { },

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

    BuilderOperation("set(1)", throwsCME = false) { this[1] = "changed" },
    BuilderOperation("set(lastIndex)", throwsCME = false) { this[lastIndex] = "changed" },
    BuilderOperation("set(the stored element)", throwsCME = false) { this[1] = this[1] },
    BuilderOperation("addAll(an empty list)", throwsCME = false) { addAll(emptyList()) },
    BuilderOperation("removeAll(missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
    BuilderOperation("retainAll(a superset)", throwsCME = false) { retainAll(toList() + "missing") },
    BuilderOperation("build()", throwsCME = false) { val _ = build() },
)

/** A hash map with a new value for each of [keys], put in the given order. */
private fun newValuesFor(keys: List<Any>): PersistentMap<Any, String> =
    persistentHashMapOf(*keys.mapIndexed { index, key -> key to "new $index" }.toTypedArray())

val hashMapOperations: List<BuilderOperation<PersistentMap.Builder<Any, String>>> = listOf(
    BuilderOperation("no call", throwsCME = false) { },

    BuilderOperation("put(new key)") { put("new", "n") },
    BuilderOperation("remove(the first key)") { remove(keys.first()) },
    BuilderOperation("remove(the last key)") { remove(keys.last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("putAll(a map with a new key)") { putAll(mapOf("new" to "n")) },
    BuilderOperation("putAll(a hash map with a new key)") { putAll(persistentHashMapOf("new" to "n")) },
    BuilderOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },
    BuilderOperation("remove(every key)") { for (key in keys.toList()) remove(key) },

    BuilderOperation("put(a new value for the first key)", throwsCME = false) { put(keys.first(), "changed") },
    BuilderOperation("put(a new value for the last key)", throwsCME = false) { put(keys.last(), "changed") },
    BuilderOperation("put(the stored value of the first key)", throwsCME = false) { put(keys.first(), getValue(keys.first())) },
    BuilderOperation("put(the stored value of the last key)", throwsCME = false) { put(keys.last(), getValue(keys.last())) },
    BuilderOperation("putAll(a new value for the first key)", throwsCME = false) { putAll(persistentHashMapOf(keys.first() to "changed")) },
    BuilderOperation("putAll(a new value for the last key)", throwsCME = false) { putAll(persistentHashMapOf(keys.last() to "changed")) },
    BuilderOperation("putAll(new values)", throwsCME = false) { putAll(newValuesFor(keys.toList())) },
    BuilderOperation("putAll(new values in the reverse key order)", throwsCME = false) { putAll(newValuesFor(keys.reversed())) },
    BuilderOperation("putAll(the stored values)", throwsCME = false) { putAll(persistentHashMapOf(*entries.map { it.key to it.value }.toTypedArray())) },
    BuilderOperation("putAll(the built map)", throwsCME = false) { putAll(build()) },
    BuilderOperation("putAll(the builder itself)", throwsCME = false) { putAll(this) },
    BuilderOperation("putAll(an empty map)", throwsCME = false) { putAll(persistentHashMapOf<Any, String>()) },
    BuilderOperation("remove(a missing key)", throwsCME = false) { remove("missing") },
)

val hashSetOperations: List<BuilderOperation<PersistentSet.Builder<Any>>> = listOf(
    BuilderOperation("no call", throwsCME = false) { },

    BuilderOperation("add(a new element)") { add("new") },
    BuilderOperation("remove(the first element)") { remove(first()) },
    BuilderOperation("remove(the last element)") { remove(last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("addAll(a hash set with a new element)") { addAll(persistentHashSetOf("new")) },
    BuilderOperation("addAll(a list with a new element)") { addAll(listOf("new")) },
    BuilderOperation("removeAll(a hash set holding the first element)") { removeAll(persistentHashSetOf(first())) },
    BuilderOperation("removeAll(a list holding the first element)") { removeAll(listOf(first())) },
    BuilderOperation("retainAll(a hash set holding only the first element)") { retainAll(persistentHashSetOf(first())) },
    BuilderOperation("retainAll(a list holding only the first element)") { retainAll(listOf(first())) },
    BuilderOperation("add(a new element) then remove(it)") { add("new"); remove("new") },
    BuilderOperation("remove(every element)") { for (element in toList()) remove(element) },

    BuilderOperation("add(a stored element)", throwsCME = false) { add(first()) },
    BuilderOperation("remove(a missing element)", throwsCME = false) { remove("missing") },
    BuilderOperation("addAll(the built set)", throwsCME = false) { addAll(build()) },
    BuilderOperation("addAll(the stored elements as a list)", throwsCME = false) { addAll(toList()) },
    BuilderOperation("addAll(an equal hash set)", throwsCME = false) { addAll(toList().toPersistentHashSet()) },
    BuilderOperation("removeAll(a hash set of missing elements)", throwsCME = false) { removeAll(persistentHashSetOf("missing")) },
    BuilderOperation("removeAll(a list of missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
    BuilderOperation("retainAll(a superset as a hash set)", throwsCME = false) { retainAll((toList() + "missing").toPersistentHashSet()) },
    BuilderOperation("retainAll(a superset as a list)", throwsCME = false) { retainAll(toList() + "missing") },
)

val orderedMapOperations: List<BuilderOperation<PersistentMap.Builder<Any, String>>> = listOf(
    BuilderOperation("no call", throwsCME = false) { },

    BuilderOperation("put(new key)") { put("new", "n") },
    BuilderOperation("remove(the first key)") { remove(keys.first()) },
    BuilderOperation("remove(the last key)") { remove(keys.last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("putAll(a map with a new key)") { putAll(persistentMapOf("new" to "n")) },
    BuilderOperation("put(new key) then remove(it)") { put("new", "n"); remove("new") },
    BuilderOperation("remove(every key)") { for (key in keys.toList()) remove(key) },

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

val orderedSetOperations: List<BuilderOperation<PersistentSet.Builder<Any>>> = listOf(
    BuilderOperation("no call", throwsCME = false) { },

    BuilderOperation("add(a new element)") { add("new") },
    BuilderOperation("remove(the first element)") { remove(first()) },
    BuilderOperation("remove(the last element)") { remove(last()) },
    BuilderOperation("clear()") { clear() },
    BuilderOperation("addAll(a list with a new element)") { addAll(listOf(first(), "new")) },
    BuilderOperation("removeAll(a list holding the first element)") { removeAll(listOf(first())) },
    BuilderOperation("retainAll(a list holding only the first element)") { retainAll(listOf(first())) },
    BuilderOperation("add(a new element) then remove(it)") { add("new"); remove("new") },
    BuilderOperation("remove(every element)") { for (element in toList()) remove(element) },

    BuilderOperation("add(a stored element)", throwsCME = false) { add(first()) },
    BuilderOperation("remove(a missing element)", throwsCME = false) { remove("missing") },
    BuilderOperation("addAll(the stored elements)", throwsCME = false) { addAll(toList()) },
    BuilderOperation("removeAll(missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
    BuilderOperation("retainAll(a superset)", throwsCME = false) { retainAll(toList() + "missing") },
)
